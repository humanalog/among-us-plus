/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hmnlg.amongusplus;

import java.awt.Color;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

/**
 *
 * @author maikotui
 */
public class EmbedMessageDisplay implements GameDisplay {

    public Message message;
    
    private final User creator;

    private final User bot;

    public EmbedMessageDisplay(Message receivedCreateMessage) {
        creator = receivedCreateMessage.getAuthor();
        bot = receivedCreateMessage.getJDA().getSelfUser();
        message = receivedCreateMessage.getChannel().sendMessage("").complete();
    }

    @Override
    public void showStart(String playerList, String[] roleNames, String[] roleDescriptions) {
        EmbedBuilder eb = createDefaultEmbedBuilder();

        StringBuilder sb = new StringBuilder();
        if (roleNames.length == roleDescriptions.length) {
            for (int i = 0; i < roleNames.length && i < roleDescriptions.length; i++) {
                sb.append(String.format("> __%s__:\n> ```%s```\n", roleNames[i], roleDescriptions[i]));
            }
        } else {
            sb.append("Error loading roles for this game");
            // TODO: throw error for mismatched role name and description
        }
        eb.addField("Roles:", sb.toString(), true);

//        sb = new StringBuilder();
//        for (User user : gameMembers) {
//            sb.append(user.getAsMention());
//            sb.append("\n");
//        }
        eb.addField("Players", String.format(">>> %s", playerList), true);

        eb.addField("What Next?", "To add or remove players, use the ***padd*** or ***prem*** commands.\nTo add or remove roles, use the ***radd*** or ***rrem*** commands.\nTo start the game, click on the \u2705 emote.", false);

        message.editMessage(eb.build()).queue();
    }

    @Override
    public void showReadyUp(List<String> readyPlayers, List<String> notReadyPlayers) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void showActiveGame() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void showGameEnded() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void showErrorMessage(String errorMessage) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private EmbedBuilder createDefaultEmbedBuilder() {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setColor(Color.green);

        eb.setTitle(String.format("Among Us+ Game", creator.getName()));

        eb.setAuthor("Among Us+ Bot", "https://github.com/humanalog/among-us-plus/", bot.getAvatarUrl());

        eb.setFooter(String.format("Game created by %s", creator.getName()), creator.getName());

        return eb;
    }

}
