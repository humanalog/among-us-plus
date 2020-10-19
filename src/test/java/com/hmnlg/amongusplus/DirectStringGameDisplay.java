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
 */
public class DirectStringGameDisplay implements GameDisplay {

    public List<Long> playerIDs;
    public String[] roleNames;
    public String[] roleDescriptions;
    public List<Long> readyPlayers;
    public List<Long> notReadyPlayers;
    public Long playerShownMessage;
    public String messageGiven;

    public DirectStringGameDisplay() {

    }

    @Override
    public void showStart(List<Long> playerIDs, String[] roleNames, String[] roleDescriptions) {
        this.playerIDs = playerIDs;
        this.roleNames = roleNames;
        this.roleDescriptions = roleDescriptions;
    }

    @Override
    public void showReadyUp(List<Long> readyPlayers, List<Long> notReadyPlayers) {
        this.readyPlayers = readyPlayers;
        this.notReadyPlayers = notReadyPlayers;
    }

    @Override
    public void showActiveGame(List<Long> playerIDs, String[] roleNames, String[] roleDescriptions) {
        this.playerIDs = playerIDs;
        this.roleNames = roleNames;
        this.roleDescriptions = roleDescriptions;
    }

    @Override
    public void showGameEnded() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void showPlayerMessage(Long playerID, String message) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void showErrorMessage(String errorMessage) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void reshowDisplay(GameState state) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
