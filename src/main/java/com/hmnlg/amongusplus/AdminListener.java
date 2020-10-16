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

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * A ListenerAdapter for the JDA Bot that will listen for admin commands from my
 * discord account
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
        if ("258698313266626560".equals(event.getAuthor().getId())) {
            if (event.getMessage().getContentRaw().equalsIgnoreCase("au+debug")) {
                if (currentGameListener.toggleDebug()) {
                    event.getMessage().getChannel().sendMessage("debug mode on").queue();
                } else {
                    event.getMessage().getChannel().sendMessage("debug mode off").queue();
                }
            }

            if (event.getMessage().getContentRaw().equalsIgnoreCase("au+adminping")) {
                event.getMessage().getChannel().sendMessage("adminpong").queue();
            }

            if (event.getMessage().getContentRaw().equalsIgnoreCase("au+restart")) {
                api.removeEventListener(currentGameListener);
                GameListener newGameListener = new GameListener(currentGameListener);
                api.addEventListener(newGameListener);
                currentGameListener = newGameListener;
                event.getMessage().getChannel().sendMessage("restarted game").queue();
            }
        }
    }
}
