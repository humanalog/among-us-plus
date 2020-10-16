/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hmnlg.amongusplus;

import java.awt.Color;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
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
    public void showStart(List<Long> playerIDs, String[] roleNames, String[] roleDescriptions) {
        message.clearReactions().queue();

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

        sb = new StringBuilder();
        for (Long playerID : playerIDs) {
            Member member = message.getGuild().retrieveMemberById(playerID).complete();
            sb.append(member.getAsMention());
            sb.append("\n");
        }
        eb.addField("Players", String.format(">>> %s", playerIDs), true);

        eb.addField("What Next?", "To add or remove players, use the ***padd*** or ***prem*** commands.\nTo add or remove roles, use the ***radd*** or ***rrem*** commands.\nTo start the game, click on the \u2705 emote.", false);

        message.editMessage(eb.build()).queue((newMessage) -> newMessage.addReaction("\u2705").queue()); // Checkmark
    }

    @Override
    public void showReadyUp(List<Long> readyPlayers, List<Long> notReadyPlayers) {
        message.clearReactions().queue();

        EmbedBuilder eb = createDefaultEmbedBuilder();

        StringBuilder sb = new StringBuilder();
        if (!notReadyPlayers.isEmpty()) {
            sb.append(">>> ");
        }
        for (Long notReadyPlayer : notReadyPlayers) {
            Member member = message.getGuild().retrieveMemberById(notReadyPlayer).complete();
            sb.append(member.getAsMention()).append("\n");
        }
        eb.addField("Not Ready", sb.toString(), true);

        sb = new StringBuilder();
        if (!notReadyPlayers.isEmpty()) {
            sb.append(">>> ");
        }
        for (Long readyPlayer : readyPlayers) {
            Member member = message.getGuild().retrieveMemberById(readyPlayer).complete();
            sb.append(member.getAsMention()).append("\n");
        }
        eb.addField("Not Ready", sb.toString(), true);

        eb.addField("Choose Your Role", "Choose \uD83C\uDDE8 for crewmate and \uD83C\uDDEE for imposter.", false);

        message.editMessage(eb.build()).queue((newMessage) -> {
            newMessage.addReaction("\uD83C\uDDE8").queue(); // C
            newMessage.addReaction("\uD83C\uDDEE").queue(); // I
        });
    }

    @Override
    public void showActiveGame(List<Long> playerIDs, String[] roleNames, String[] roleDescriptions) {
        message.clearReactions().queue();

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

        sb = new StringBuilder();
        for (Long playerID : playerIDs) {
            Member member = message.getGuild().retrieveMemberById(playerID).complete();
            sb.append(member.getAsMention());
            sb.append("\n");
        }
        eb.addField("Players", String.format(">>> %s", playerIDs), true);

        eb.addField("What's Next?", "Choose \uD83D\uDD04 to restart the game.\nChoose \uD83D\uDED1 to stop the game.", false);

        message.editMessage(eb.build()).queue((newMessage) -> {
            newMessage.addReaction("\uD83D\uDD04").queue(); // Redo
            newMessage.addReaction("\uD83D\uDED1").queue(); // Stop
        }); // Checkmark
    }

    @Override
    public void showGameEnded() {
        message.clearReactions().queue();

        EmbedBuilder eb = createDefaultEmbedBuilder();

        eb.addField("Game has been stopped.", "Thanks for playing!", false);

        message.editMessage(eb.build()).queue();
    }

    @Override
    public void showErrorMessage(String errorMessage) {
        message.clearReactions().queue();

        EmbedBuilder eb = createDefaultEmbedBuilder();

        eb.addField("Game has been stopped.", "Thanks for playing!", false);

        message.editMessage(eb.build()).queue();
    }

    private EmbedBuilder createDefaultEmbedBuilder() {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setColor(Color.green);

        eb.setTitle(String.format("Among Us+ Game", creator.getName()));

        eb.setAuthor("Among Us+ Bot", "https://github.com/humanalog/among-us-plus/", bot.getAvatarUrl());

        eb.setFooter(String.format("Game created by %s", creator.getName()), creator.getName());

        return eb;
    }

    @Override
    public void showPlayerMessage(Long playerID, String message) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public void reshowDisplay(GameState state) {
        message.getChannel().sendMessage("").queue((newMessage) -> {
            if (message.getEmbeds().size() > 0) {
                MessageEmbed embed = message.getEmbeds().get(0);
                newMessage.editMessage(embed).queue();
            }
            switch (state) { // Add reactions based on the gamestate
                case NEW -> {
                    newMessage.addReaction("\u2705").queue(); // Checkmark
                }
                case PREGAME -> {
                    newMessage.addReaction("\uD83C\uDDE8").queue(); // C
                    newMessage.addReaction("\uD83C\uDDEE").queue(); // I
                }
                case ACTIVE -> {
                    newMessage.addReaction("\uD83D\uDD04").queue(); // Restart
                    newMessage.addReaction("\uD83D\uDED1").queue(); // Stop
                }
                default -> {
                }
            }
            message.delete().queue();
            message = newMessage;
        });
    }
}
