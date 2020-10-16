/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hmnlg.amongusplus;

import java.util.List;

/**
 *
 * @author maikotui
 * @param <T>
 */
public class GameController<T extends GameDisplay> {
    
    private final T display;
    
    private final GameData data;
    
    public GameController(T d, List<Long> playerIDs, List<GameRole> roles) {
        String[] roleNames = new String[roles.size()]; 
        String[] roleDescriptions = new String[roles.size()];
        d.showStart(playerIDs ,roleNames, roleDescriptions);
        display = d;
        
        data = new GameData(playerIDs, roles);
    }
    
    public void startGame() {
        List<Long> playersNotReady = data.getPlayersWithoutRoles();
        List<Long> allPlayers = data.getAllPlayers();
        allPlayers.removeAll(playersNotReady);
        display.showReadyUp(allPlayers, playersNotReady);
    }
}
