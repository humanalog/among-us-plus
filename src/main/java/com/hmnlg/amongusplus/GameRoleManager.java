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
import org.joda.time.Instant;

/**
 * Used to manage and keep track of the status of an Among Us game
 *
 * @author maikotui
 */
public class GameRoleManager {

    /**
     * The main list of players and the roles that each player holds
     */
    private final Map<Long, List<GameRole>> playerRoles;

    /**
     * A list of all roles that are usable for this game
     */
    public final List<GameRole> playableRoles;

    /**
     * Creates a new game with the given players and the given roles.
     *
     * @param players A list of all discord users who will be playing.
     * @param usableNondefaultRoles All roles that can be assigned during this
     * game.
     */
    public GameRoleManager(List<Long> players, List<GameRole> usableNondefaultRoles) {
        // Instantiate all ArrayLists in map
        playerRoles = new HashMap<>();
        players.forEach(player -> {
            playerRoles.put(player, new ArrayList<>());
        });

        this.playableRoles = usableNondefaultRoles;
    }

    public boolean tryAssignRole(Long user, GameRole assignmentRole) {
        List<GameRole> roles = playerRoles.get(user);

        if (assignmentRole.isDefault) {
            roles.removeIf((role) -> role.isDefault);
            roles.add(assignmentRole);

            return true;
        } else {
            for (GameRole role : roles) {
                for (int unstackableRoleID : assignmentRole.unstackableRoleIds) {
                    if (unstackableRoleID == role.id) {
                        return false;
                    }
                }
            }

            roles.add(assignmentRole);
            return true;
        }
    }

    public boolean allPlayersAreReady() {
        for (Long userID : playerRoles.keySet()) {
            if (!playerHasDefaultRole(userID)) {
                return false;
            }
        }
        return true;
    }

    private boolean playerHasDefaultRole(Long userID) {
        for (GameRole role : playerRoles.get(userID)) {
            if (role.isDefault) {
                return true;
            }
        }
        return false;
    }

    /**
     * Distributes non-default roles to all available players.
     *
     * @return
     * @throws GeneralGameException
     */
    public void distributeNonDefaultRoles() throws GeneralGameException {
        List<Long> assignablePlayers = new ArrayList<>(playerRoles.keySet());
        Random rand = new Random();

        for (GameRole assignableRole : playableRoles) {
            boolean hasAssignedRole;
            do {
                int indexToAssign = rand.nextInt(assignablePlayers.size());
                hasAssignedRole = tryAssignRole(assignablePlayers.get(indexToAssign), assignableRole);
                if (!hasAssignedRole) {
                    throw new GeneralGameException(String.format("Could not find a player to assign the role '%s'.", assignableRole.name));
                }
            } while (!assignablePlayers.isEmpty() && !hasAssignedRole);
        }
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
     * Resets the game so another round can be played. This will take the game
     * back to the "NEW" state.
     */
    public void resetRoles() {
        // Clear roles
        Set<Long> userSet = new HashSet<>(playerRoles.keySet());
        playerRoles.clear();
        userSet.forEach(player -> {
            playerRoles.put(player, new ArrayList<>());
        });
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
        if (!playerRoles.containsKey(player)) {
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
    public boolean removePlayer(Long player) {
        return  playerRoles.remove(player) != null;
    }

    /**
     * Add the given role as a playable role in this game.
     *
     * @param role
     */
    public void addRole(GameRole role) {
        if (!playableRoles.contains(role) && !role.isDefault) {
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
        return playableRoles.remove(role);
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
        return String.format("Role Map:%s", playerRoles.toString());
    }
}

/**
 * Represents a state that a game can be at any given time.
 *
 * @author maikotui
 */
enum GameState {
    NEW, ACTIVE, READYUP
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
