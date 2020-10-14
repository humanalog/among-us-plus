/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hmnlg.amongusplus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.dv8tion.jda.core.entities.User;
import org.joda.time.Instant;

/**
 * Used to manage and keep track of the status of an Among Us game
 *
 * @author maikotui
 */
public class GameManager {

    /**
     * The main list of players and the roles that each player holds
     */
    private final Map<User, List<GameRole>> playerToRolesMap;

    /**
     * A list of all roles that are usable for this game
     */
    private final List<GameRole> gameRoles;

    /**
     * The current state of the game
     */
    private GameState state;

    private Instant stateChangeTime;

    // Flags to keep track of when an action is used (and only has one use)
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
    public GameManager(List<User> players, List<GameRole> usableNondefaultRoles) {
        playerToRolesMap = new HashMap<>();
        players.forEach(player -> {
            playerToRolesMap.put(player, new ArrayList<>());
        });

        this.gameRoles = usableNondefaultRoles;

        state = GameState.NEW;
        stateChangeTime = new Instant();
    }

    public boolean isActive() {
        return state == GameState.ACTIVE;
    }

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
    public boolean attemptGameStartWithRoleAssignment(User user, GameRole roleToAssign) throws GeneralGameException {
        // This can only be done in pregame
        if (state == GameState.PREGAME) {
            // Catch bad role assignment
            if (!roleToAssign.isDefault) {
                throw new GeneralGameException("You cannot assign the '" + roleToAssign.toString() + "' role to yourself.");
            }

            // Replace the users last role with the new role.
            if (!playerToRolesMap.get(user).isEmpty()) {
                playerToRolesMap.get(user).clear();
            }

            playerToRolesMap.get(user).add(roleToAssign);

            // Check if all players have assigned roles
            if (!playerToRolesMap.entrySet().stream().noneMatch(entry -> (entry.getValue().isEmpty()))) {
                return false;
            }

            System.out.println("Game started --- " + playerToRolesMap.toString());

            state = GameState.ACTIVE;
            stateChangeTime = new Instant();
            return true;
        }
        throw new GeneralGameException("I'm not expecting a role assignment from you.");
    }

    public Map<User, List<GameRole>> giveOutNondefaultRoles() throws GeneralGameException {
        for (GameRole assignableRole : gameRoles) {
            User assignableUser = getUserWhoCanAcceptRole(getAllPlayers(), assignableRole);
            if (assignableUser != null) {
                playerToRolesMap.get(assignableUser).add(assignableRole);
            } else {
                throw new GeneralGameException(String.format("Could not find a player to assign %s.", assignableRole.name));
            }
        }

        return new HashMap<>(playerToRolesMap);
    }

    private User getUserWhoCanAcceptRole(List<User> playerList, GameRole targetRole) {
        // For generating a random value to pick a random player
        Random rand = new Random();
        
        // The user that can accept the targetRole
        User userToGiveRole = null;
        
        // If an acceptable player has been found
        boolean foundAcceptablePlayer = false;

        do {
            // Get a random player from the player list
            // Remove them so we don't get any repeats
            userToGiveRole = playerList.remove(rand.nextInt(playerList.size())); // Get a random user and remove them from the list (so they can't be picked again)

            // Assume we found an acceptable player
            foundAcceptablePlayer = true;

            // Check if player is really acceptable
            for (GameRole activeUserRole : playerToRolesMap.get(userToGiveRole)) { // For each role the user already has
                for (int unstackableRoleId : targetRole.unstackableRoleIds) { // For each unstackableRoleId
                    if (activeUserRole.id == unstackableRoleId) { // If a role the player has is an unstackable role for this game role, retry
                        foundAcceptablePlayer = false;
                    }
                }
            }
        } while (!playerList.isEmpty() && !foundAcceptablePlayer);

        return userToGiveRole;
    }

    public void resetGame() {
        // Clear roles
        Set<User> userSet = new HashSet<>(playerToRolesMap.keySet());
        playerToRolesMap.clear();
        userSet.forEach(player -> {
            playerToRolesMap.put(player, new ArrayList<>());
        });

        vetoUsed = false;
        executionUsed = false;
        detectUsed = false;
        state = GameState.NEW;
        stateChangeTime = new Instant();
    }

    public boolean useVeto() {
        if (vetoUsed) {
            return !vetoUsed;
        } else {
            vetoUsed = true;
            return vetoUsed;
        }
    }

    public boolean useExecution() {
        if (executionUsed) {
            return !executionUsed;
        } else {
            executionUsed = true;
            return executionUsed;
        }
    }

    public boolean useDetect() {
        if (detectUsed) {
            return !detectUsed;
        } else {
            detectUsed = true;
            return detectUsed;
        }
    }

    public List<User> getAllPlayers() {
        return new ArrayList<>(playerToRolesMap.keySet());
    }

    public void addPlayer(User player) {
        if (state == GameState.NEW && !playerToRolesMap.containsKey(player)) {
            playerToRolesMap.put(player, new ArrayList<>());
        }
    }

    public boolean removePlayer(User player) {
        return state == GameState.NEW && playerToRolesMap.remove(player) != null;
    }

    public void addRole(GameRole role) {
        if (state == GameState.NEW && !gameRoles.contains(role) && !role.isDefault) {
            gameRoles.add(role);
        }
    }

    public boolean removeRole(GameRole role) {
        return state == GameState.NEW && gameRoles.remove(role);
    }

    public List<GameRole> getRolesForPlayer(User player) {
        return playerToRolesMap.get(player);
    }

    @Override
    public String toString() {
        return String.format("State:[%s] Map:%s", state.toString(), playerToRolesMap.toString());
    }
}

enum GameState {
    NEW, ACTIVE, PREGAME
}

class GeneralGameException extends Exception {

    public GeneralGameException(String errorMessage) {
        super(errorMessage);
    }

    public GeneralGameException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}
