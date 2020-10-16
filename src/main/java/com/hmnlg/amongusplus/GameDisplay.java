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
public interface GameDisplay {
    
    public void showStart(List<Long> playerIDs, String[] roleNames, String[] roleDescriptions);

    public void showReadyUp(List<Long> readyPlayers, List<Long> notReadyPlayers);
    
    public void showActiveGame(List<Long> playerIDs, String[] roleNames, String[] roleDescriptions);
    
    public void showGameEnded();
    
    public void showErrorMessage(String errorMessage);
    
    public void reshowDisplay(GameState state);
    
}
