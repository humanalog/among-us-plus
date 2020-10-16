/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hmnlg.amongusplus;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author maikotui
 * @param <T>
 */
public class GameController<T extends GameDisplay> {
    
    private final T display;

    private final GameData data;

    public GameState state;
    
    public GameController(T d, List<Long> playerIDs, List<GameRole> roles) {
        data = new GameData(playerIDs, roles);

        String[] roleNames = new String[roles.size()];
        String[] roleDescriptions = new String[roles.size()];
        for (int i = 0; i < roles.size(); i++) {
            GameRole role = roles.get(i);
            roleNames[i] = role.name;
            roleDescriptions[i] = role.description;
        }

        d.showStart(playerIDs, roleNames, roleDescriptions);
        display = d;
    }

    public void redisplayGame() {
        display.reshowDisplay(GameState.ACTIVE);
    }

    public void startGame() {
        try {
            data.moveToPregame();

            List<Long> playersNotReady = data.getPlayersWithoutRoles();
            List<Long> allPlayers = data.getAllPlayers();
            allPlayers.removeAll(playersNotReady);
            display.showReadyUp(allPlayers, playersNotReady);
        } catch (GeneralGameException ex) {
            Logger.getLogger(GameController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void restartGame() {
        data.resetGame();
        
        String[] roleNames = new String[data.playableRoles.size()];
                    String[] roleDescriptions = new String[data.playableRoles.size()];
                    for (int i = 0; i < data.playableRoles.size(); i++) {
                        GameRole role = data.playableRoles.get(i);
                        roleNames[i] = role.name;
                        roleDescriptions[i] = role.description;
                    }

        display.showStart(data.getAllPlayers(), roleNames, roleDescriptions);
    }

    public void readyUp(Long user, GameRole chosenRole) {
        if (chosenRole != null) {
            try {
                // Try to start the game
                if (data.attemptGameStartWithRoleAssignment(user, chosenRole)) {
                    // Game has started, distribute non default roles
                    data.distributeNonDefaultRoles();

                    // Send role assignment message for non default roles and send everyone a game starting message
                    data.getAllPlayers().forEach(playerID -> {
                        data.getRolesForPlayer(playerID).stream().filter(role -> (!role.isDefault)).forEachOrdered(role -> {
                            display.showPlayerMessage(playerID, role.assignmentMessage);
                        });
                    });

                    String[] roleNames = new String[data.playableRoles.size()];
                    String[] roleDescriptions = new String[data.playableRoles.size()];
                    for (int i = 0; i < data.playableRoles.size(); i++) {
                        GameRole role = data.playableRoles.get(i);
                        roleNames[i] = role.name;
                        roleDescriptions[i] = role.description;
                    }

                    display.showActiveGame(data.getAllPlayers(), roleNames, roleDescriptions);
                } else {
                    List<Long> playersNotReady = data.getPlayersWithoutRoles();
                    List<Long> allPlayers = data.getAllPlayers();
                    allPlayers.removeAll(playersNotReady);
                    display.showReadyUp(allPlayers, playersNotReady);
                }
            } catch (GeneralGameException ex) {
                Logger.getLogger(CommandListener.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);

            }
        }
    }

    public void stopGame() {
        display.showGameEnded();
    }
    
    public GameState getState() {
        return data.getState();
    }
}
