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
    
    public void showStart(String playerList, String[] roleNames, String[] roleDescriptions);

    public void showReadyUp(List<String> readyPlayers, List<String> notReadyPlayers);
    
    public void showActiveGame();
    
    public void showGameEnded();
    
    public void showErrorMessage(String errorMessage);
    
}
