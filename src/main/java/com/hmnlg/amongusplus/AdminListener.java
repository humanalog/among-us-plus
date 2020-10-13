/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hmnlg.amongusplus;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

/**
 *
 * @author maikotui
 */
public class AdminListener extends ListenerAdapter {
    
    private final JDA api;
    private GameListener currentGameListener;
    
    public AdminListener(JDA api, GameListener gameListener) {
        this.api = api;
        currentGameListener = gameListener;
    }
    
    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
        if("258698313266626560".equals(event.getAuthor().getId())) {
            if(event.getMessage().getContentRaw().equalsIgnoreCase("au+debug")) {
                if(currentGameListener.toggleDebug()) {
                    event.getMessage().getChannel().sendMessage("debug mode on").queue();
                }
                else {
                    event.getMessage().getChannel().sendMessage("debug mode off").queue();
                }
            }
            
            if(event.getMessage().getContentRaw().equalsIgnoreCase("au+adminping")) {
                event.getMessage().getChannel().sendMessage("adminpong").queue();
            }
            
            if(event.getMessage().getContentRaw().equalsIgnoreCase("au+restart")) {
                api.removeEventListener(currentGameListener);
                GameListener newGameListener = new GameListener(currentGameListener);
                api.addEventListener(newGameListener);
                currentGameListener = newGameListener;
                event.getMessage().getChannel().sendMessage("restarted game").queue();
            }
        }
    }
}
