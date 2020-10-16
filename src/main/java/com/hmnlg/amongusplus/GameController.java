/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hmnlg.amongusplus;

import java.util.List;
import net.dv8tion.jda.api.entities.User;

/**
 *
 * @author maikotui
 * @param <T>
 */
public class GameController<T extends GameDisplay> {
    
    private final T display;
    
    private final GameData data;
    
    public GameController(T d, List<User> players, List<GameRole> roles) {
        display = d;
        data = new GameData(players, roles);
    }
    
    public void startGame() {
        
    }
}