/*
 * Copyright (C) 2020 maikotui
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.hmnlg.amongusplus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.joda.time.Instant;

/**
 * Used to manage and keep track of the status of an Among Us game
 *
 * @author maikotui
 */
public class GameData {

    /**
     * The message associated with this game that shows discord users a
     * responsive display
     */
    public Message displayMessge;

    /**
     * The main list of players and the roles that each player holds
     */
    private final Map<Long, List<GameRole>> playerRoles;

    /**
     * A list of all roles that are usable for this game
     */
    public final List<GameRole> playableRoles;

    /**
     * The current state of the game
     */
    private GameState state = GameState.NEW;

    /**
     * The last time the game's state changed
     */
    private Instant stateChangeTime;

    // Flags to keep track of when an action is used (All of these have only has one use)
    private boolean vetoUsed = false;
    private boolean executionUsed = false;
    private boolean detectUsed = false;

    /**
     * Creates a new game with the given players and the given roles.
     *
     * @param players A list of all discord users who will be playing.
     * @param usableNondefaultRoles All roles that can be assigned during this
     * game.
     */
    public GameData(List<Long> players, List<GameRole> usableNondefaultRoles) {
        // Instantiate all ArrayLists in map
        playerRoles = new HashMap<>();
        players.forEach(player -> {
            playerRoles.put(player, new ArrayList<>());
        });

        this.playableRoles = usableNondefaultRoles;

        state = GameState.NEW;
        stateChangeTime = new Instant();
    }

    /**
     * Gives the current state of the game
     *
     * @return
     */
    public GameState getState() {
        return state;
    }

    /**
     * Gives the last Instant that the game state was changed
     *
     * @return
     */
    public Instant getLastGameStateChangeTime() {
        return stateChangeTime;
    }

    /**
     * Moves the game to PREGAME state where players can dm the bot and get
     * assigned their roles.
     *
     * @throws GeneralGameException
     */
    public void moveToPregame() throws GeneralGameException {
        // Check for starting game with invalid gamestate
        if (state == GameState.PREGAME || state == GameState.ACTIVE) {
            throw new GeneralGameException("Among Us+ game is " + GameState.ACTIVE.toString().toLowerCase() + ". Please run stop command first.");
        }

        state = GameState.PREGAME;
        stateChangeTime = new Instant();
    }

    /**
     * Assigns the given user the given role. If all users have been assigned at
     * least one role, starts the game.
     *
     * @param user
     * @param roleToAssign
     * @return
     * @throws GeneralGameException
     */
    public boolean attemptGameStartWithRoleAssignment(Long user, GameRole roleToAssign) throws GeneralGameException {
        // This can only be done in pregame
        if (state == GameState.PREGAME) {
            // Catch bad role assignment
            if (!roleToAssign.isDefault) {
                throw new GeneralGameException("You cannot assign the '" + roleToAssign.toString() + "' role to yourself.");
            }

            // Replace the users last role with the new role.
            if (!playerRoles.get(user).isEmpty()) {
                playerRoles.get(user).clear();
            }

            playerRoles.get(user).add(roleToAssign);

            // Check if all players have assigned roles
            if (!playerRoles.entrySet().stream().noneMatch(entry -> (entry.getValue().isEmpty()))) {
                return false;
            }

            state = GameState.ACTIVE;
            stateChangeTime = new Instant();
            return true;
        }
        throw new GeneralGameException("I'm not expecting a role assignment from you.");
    }

    /**
     * Get a list of all players without a role
     *
     * @return
     */
    public List<Long> getPlayersWithoutRoles() {
        List<Long> list = new ArrayList<>();
        for (Entry<Long, List<GameRole>> entry : playerRoles.entrySet()) {
            if (entry.getValue().isEmpty()) {
                list.add(entry.getKey());
            }
        }
        return list;
    }

    /**
     * Distributes non-default roles to all available players.
     *
     * @return
     * @throws GeneralGameException
     */
    public Map<Long, List<GameRole>> distributeNonDefaultRoles() throws GeneralGameException {
        List<Long> playersWithAssignableRoles = getAllPlayers();
        for (GameRole assignableRole : playableRoles) {
            if (!playersWithAssignableRoles.isEmpty()) {
                Long assignableUser = getUserWhoCanAcceptRole(playersWithAssignableRoles, assignableRole);
                if (assignableUser != null) {
                    playersWithAssignableRoles.remove(assignableUser);
                    playerRoles.get(assignableUser).add(assignableRole);
                } else {
                    throw new GeneralGameException(String.format("Could not find a player to assign the role '%s'.", assignableRole.name));
                }
            } else {
                throw new GeneralGameException(String.format("Could not find a player to assign '%s'.", assignableRole.name));
            }
        }

        return new HashMap<>(playerRoles);
    }

    /**
     * Finds a User from the given list of players that can accept the given
     * role.
     *
     * @param playerList
     * @param targetRole
     * @return
     */
    private Long getUserWhoCanAcceptRole(List<Long> playerList, GameRole targetRole) {
        // For generating a random value to pick a random player
        Random rand = new Random();

        // The user that can accept the targetRole
        Long userToGiveRole = null;

        // If an acceptable player has been found
        boolean foundAcceptablePlayer;

        do {
            // Get a random player from the player list
            // Remove them so we don't get any repeats
            userToGiveRole = playerList.remove(rand.nextInt(playerList.size())); // Get a random user and remove them from the list (so they can't be picked again)

            // Assume we found an acceptable player
            foundAcceptablePlayer = true;

            // Check if player is really acceptable
            for (GameRole activeUserRole : playerRoles.get(userToGiveRole)) { // For each role the user already has
                for (int unstackableRoleId : targetRole.unstackableRoleIds) { // For each unstackableRoleId
                    if (activeUserRole.id == unstackableRoleId) { // If a role the player has is an unstackable role for this game role, retry
                        foundAcceptablePlayer = false;
                    }
                }
            }
        } while (!playerList.isEmpty() && !foundAcceptablePlayer);

        return userToGiveRole;
    }

    /**
     * Resets the game so another round can be played. This will take the game
     * back to the "NEW" state.
     */
    public void resetGame() {
        // Clear roles
        Set<Long> userSet = new HashSet<>(playerRoles.keySet());
        playerRoles.clear();
        userSet.forEach(player -> {
            playerRoles.put(player, new ArrayList<>());
        });

        vetoUsed = false;
        executionUsed = false;
        detectUsed = false;
        state = GameState.NEW;
        stateChangeTime = new Instant();
    }

    /**
     * Uses a veto if able
     *
     * @return
     */
    public boolean useVeto() {
        if (vetoUsed) {
            return !vetoUsed;
        } else {
            vetoUsed = true;
            return vetoUsed;
        }
    }

    /**
     * Uses an execution if able to
     *
     * @return
     */
    public boolean useExecution() {
        if (executionUsed) {
            return !executionUsed;
        } else {
            executionUsed = true;
            return executionUsed;
        }
    }

    /**
     * Uses a detect if able to
     *
     * @return
     */
    public boolean useDetect() {
        if (detectUsed) {
            return !detectUsed;
        } else {
            detectUsed = true;
            return detectUsed;
        }
    }

    /**
     * Retrieves a list of all players in this game
     *
     * @return
     */
    public List<Long> getAllPlayers() {
        return new ArrayList<>(playerRoles.keySet());
    }

    /**
     * Adds the given user as a player of this game
     *
     * @param player
     */
    public void addPlayer(Long player) {
        if (state == GameState.NEW && !playerRoles.containsKey(player)) {
            playerRoles.put(player, new ArrayList<>());
        }
    }

    /**
     * Removes the given user from this game if they are in it. Does nothing if
     * not present in current list.
     *
     * @param player
     * @return
     */
    public boolean removePlayer(User player) {
        return state == GameState.NEW && playerRoles.remove(player) != null;
    }

    /**
     * Add the given role as a playable role in this game.
     *
     * @param role
     */
    public void addRole(GameRole role) {
        if (state == GameState.NEW && !playableRoles.contains(role) && !role.isDefault) {
            playableRoles.add(role);
        }
    }

    /**
     * Removes the given role from the list of playable roles. Does nothing if
     * not present in current list.
     *
     * @param role
     * @return
     */
    public boolean removeRole(GameRole role) {
        return state == GameState.NEW && playableRoles.remove(role);
    }

    /**
     * Gets all held roles for a given player
     *
     * @param player
     * @return
     */
    public List<GameRole> getRolesForPlayer(Long player) {
        return playerRoles.get(player);
    }

    /**
     * Used for debugging
     *
     * @return
     */
    @Override
    public String toString() {
        return String.format("State:[%s] Map:%s", state.toString(), playerRoles.toString());
    }
}

/**
 * Represents a state that a game can be at any given time.
 *
 * @author maikotui
 */
enum GameState {
    NEW, ACTIVE, PREGAME
}

/**
 * A general exception for any game-related errors that could be ran into during
 * the game cycle
 *
 * @author maikotui
 */
class GeneralGameException extends Exception {

    /**
     * Creates an exception with the given message
     *
     * @param errorMessage
     */
    public GeneralGameException(String errorMessage) {
        super(errorMessage);
    }

    /**
     * Creates an exception with the given message and traceable exception
     *
     * @param errorMessage
     * @param err
     */
    public GeneralGameException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}
