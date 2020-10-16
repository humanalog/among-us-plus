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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageEmbed.AuthorInfo;
import net.dv8tion.jda.api.entities.MessageEmbed.Footer;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.joda.time.Instant;
import org.joda.time.Interval;

/**
 * A ListenerAdapter for a JDA object that listens for game commands.
 *
 * @author maikotui
 */
public class CommandListener extends ListenerAdapter {

    /**
     * Prefix that must be used for commands to be recognized
     */
    private final String prefix = "au+";

    /**
     * A list of all the roles that this GameListener will listen for
     */
    private final List<GameRole> allRoles;

    /**
     * Role for the crewmates
     */
    private GameRole crewRole = new GameRole();

    /**
     * Role for the imposters
     */
    private GameRole imposterRole = new GameRole();

    /**
     * The database of all created games. This will be purged periodically (see
     * purgeIntervalInMinutes)
     */
    private final Map<User, GameController<EmbedMessageDisplay>> gameDB;

    /**
     * The timer that will run the purge command
     */
    private final Timer purgeTimer;

    /**
     * The amount of time in minutes between each purge of the database
     */
    private final int purgeIntervalInMinutes = 15;

    /**
     * The amount of time in minutes since a state change for a game to be
     * purged from the database
     */
    private final int maximumInactiveTimeInMinutes = 30;

    /**
     * Toggle for debug mode
     */
    private boolean debug;

    /**
     * Initializes a new listener for game commands. This will also start the
     * listener's database purge timer.
     *
     * @param allRoles A list of all the roles this GameListener accepts
     * @param debug Whether to start the GameListener in debug mode or not
     */
    public CommandListener(List<GameRole> allRoles, boolean debug) {
        super();

        // Assign from arguments
        this.allRoles = allRoles;
        for (GameRole role : this.allRoles) {
            if (role.id == 1) {
                crewRole = role;
            }
            if (role.id == 2) {
                imposterRole = role;
            }
        }
        this.debug = debug;

        // Create a new database
        gameDB = new HashMap<>();

        // Create a timer for purging the database of old 
        purgeTimer = new Timer();
        purgeTimer.scheduleAtFixedRate(new PurgeTimerTask(this), purgeIntervalInMinutes * 60000L, purgeIntervalInMinutes * 60000L);
    }

    /**
     * Creates a new GameListener with the same roles and debug state as the
     * previous. The game database will be empty and a new timer for database
     * purging will be created and started.
     *
     * @param gameListener The game listener to inherit values from
     */
    public CommandListener(CommandListener gameListener) {
        super();

        // Assign from arguments
        this.allRoles = gameListener.allRoles;
        for (GameRole role : this.allRoles) {
            if (role.id == 1) {
                crewRole = role;
            }
            if (role.id == 2) {
                imposterRole = role;
            }
        }
        this.debug = gameListener.debug;

        // Create a new database
        gameDB = new HashMap<>();

        // Stop old timer from running
        gameListener.purgeTimer.cancel();

        // Create a timer for purging the database of old 
        purgeTimer = new Timer();
        purgeTimer.scheduleAtFixedRate(new PurgeTimerTask(this), purgeIntervalInMinutes * 60000L, purgeIntervalInMinutes * 60000L);
    }

    /**
     * Toggles debug
     *
     * @return returns the value of debug after toggle
     */
    public boolean toggleDebug() {
        debug = !debug;
        return debug;
    }

    /**
     * Ran every time a message is received (excludes private messages). This
     * will look for the GameListener prefix and if it is present, parse the
     * given command.
     *
     * @param event Information regarding the message received
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Ignore bots
        if (event.getAuthor().isBot()) {
            return;
        }

        // All Bot Commands
        if (event.getMessage().getContentRaw().startsWith(prefix)) {

            // Get and store message and text content (used to simplify calls
            Message message = event.getMessage();
            String content = message.getContentRaw();

            // ----- ALWAYS ACTIVE COMMANDS -----
            // Ping Pong
            if (content.equals(prefix + "ping")) {
                message.addReaction("\u2705").queue();
                sendResponse(message, "Pong :)");
                if (debug) {
                    Logger.getLogger(CommandListener.class.getName()).log(Level.INFO, String.format("Responded to ping from %s", message.getAuthor().getName()));
                }
            }

            // Help Command
            if (content.equals(prefix + "help") || content.equals(prefix + "?")) {
                String helpText = prefix + "ping - Ping Pong to make sure I'm awake.\n\n";
                helpText += prefix + "roles - Gives a list of roles that you can use in game.\n\n";
                helpText += prefix + "create - Creates a new game in the voice channel you are in. (Best in Among Us VC). Note: Everyone in that voice channel will be added. Use the role names or alias to add them into the game on creation.\n\n";
                helpText += prefix + "info - Shows the information for the game you are hosting. If you need to move the message to another chat, this is the best way to do it.";
                helpText += prefix + "padd/prem - Adds or removes a player from a game.";
                helpText += prefix + "radd/rrem - Adds or removes a role from a game.";
                helpText += prefix + "stop - If you are done playing among us, issue this command at any time to stop the current game.";
                sendResponse(message, helpText);
                if (debug) {
                    Logger.getLogger(CommandListener.class.getName()).log(Level.INFO, String.format("Responded to help command from %s", message.getAuthor().getName()));
                }
            }

            // List Roles
            if (content.equals(prefix + "roles")) {
                String roleInfo = "";
                roleInfo = allRoles.stream().map(role -> role.name + "\n" + Arrays.toString(role.aliases) + "\n" + role.description + "\n\n").reduce(roleInfo, String::concat);
                message.addReaction("\u2705").queue();
                sendResponse(message, roleInfo);
                if (debug) {
                    Logger.getLogger(CommandListener.class.getName()).log(Level.INFO, String.format("Responded to roles command from %s", message.getAuthor().getName()));
                }
            }

            // ----- GAME COMMANDS ----
            // Create command
            if (content.startsWith(prefix + "create")) {

                createGameFromMessage(event.getMessage());
            }

            // Info command
            if (content.equals(prefix + "info")) {
                GameController game = gameDB.get(event.getAuthor());
                if (game != null) {
                    game.redisplayGame();
                }
            }

            // Stop command
            if (content.equals(prefix + "stop")) {
                tryDeleteUsersGame(event.getAuthor());
            }

            // Add player command
            if (content.startsWith(prefix + "padd")) {
                // Ensure there is a player name provided
                if (content.length() < prefix.length() + 5) {
                    sendErrorResponse(event.getMessage(), "Please specify a player to add.");
                    return;
                }

                // Get the game the author is the owner of
                GameData game = gameDB.get(event.getAuthor());
                if (game != null) {
                    // Parse the name provided
                    String nameToAdd = content.substring(prefix.length() + 5);
                    User user = findUserInGuild(event.getGuild(), nameToAdd);

                    // Add the player
                    if (user != null) {
                        game.addPlayer(user);
                        refreshNewGameMessage(game);
                    }
                } else {
                    sendErrorResponse(event.getMessage(), "You are not the owner of any active games.");
                }
            }

            if (content.startsWith(prefix + "prem")) {
                // Ensure there is a player name provided
                if (content.length() < prefix.length() + 5) {
                    sendErrorResponse(event.getMessage(), "Please specify a player to remove.");
                    return;
                }

                // Get the game the author is the owner of
                GameData game = gameDB.get(event.getAuthor());

                if (game != null) {
                    // Parse the name provided
                    String playerToRemove = content.substring(prefix.length() + 5);
                    User user = findUserInGuild(event.getGuild(), playerToRemove);

                    // Remove the player
                    if (user != null) {
                        if (user.equals(event.getAuthor())) {
                            sendErrorResponse(event.getMessage(), "You can't remove yourself from the game since you're the game leader.");
                            return;
                        }

                        if (game.removePlayer(user)) {
                            refreshNewGameMessage(game);
                        }
                    }
                } else {
                    sendErrorResponse(event.getMessage(), "You are not the owner of any active games.");
                }
            }

            if (content.startsWith(prefix + "radd")) {
                if (content.length() < prefix.length() + 5) {
                    sendErrorResponse(event.getMessage(), "Please specify a role to add.");
                    return;
                }

                // Get the game the author is the owner of
                GameData game = gameDB.get(event.getAuthor());
                if (game != null) {
                    // Parse the role provided
                    String roleToAdd = content.substring(prefix.length() + 5);
                    GameRole role = findRoleFromString(roleToAdd);

                    // Add the role
                    if (role != null) {
                        game.addRole(role);
                        refreshNewGameMessage(game);
                    }

                } else {
                    sendErrorResponse(event.getMessage(), "You are not the owner of any active games.");
                }
            }

            if (content.startsWith(prefix + "rrem")) {
                if (content.length() < prefix.length() + 5) {
                    sendErrorResponse(event.getMessage(), "Please specify a role to add.");
                    return;
                }

                // Get the game the author is the owner of
                GameData game = gameDB.get(event.getAuthor());
                if (game != null) {
                    // Parse the role provided
                    String roleToRemove = content.substring(prefix.length() + 5);
                    GameRole role = findRoleFromString(roleToRemove);

                    // Add the role
                    if (role != null) {
                        if (game.removeRole(role)) {
                            refreshNewGameMessage(game);
                        }
                    }

                } else {
                    sendErrorResponse(event.getMessage(), "You are not the owner of any active games.");
                }
            }
        }
    }

    /**
     * Triggered when a new reaction is added in a Guild that the Bot is in.
     * Used to interact with chat.
     *
     * @param event
     */
    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        event.retrieveMessage().queue(message -> {
            event.retrieveMember().queue((member) -> {
                User reactor = member.getUser();
                if (message.getAuthor().equals(event.getJDA().getSelfUser()) && !reactor.equals(event.getJDA().getSelfUser())) { // If message is from this bot and a user other than this bot reacted to a message
                    String reactionText = event.getReactionEmote().getName();
                    event.getReaction().removeReaction(reactor).queue();

                    // Check if this message is a game display message
                    for (GameData game : gameDB.values()) {
                        if (game.displayMessge.getId().equals(message.getId())) { // Message is a game message
                            onDisplayMessageUpdate(reactor, reactionText, game);
                            return;
                        }
                    }
                }
            });
        });
    }

    /**
     * Ran every time a private message is received.
     *
     * @param event Information regarding the message received
     */
    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
        // Ready up moved to embed reaction
//        // Ready up message
//        if (event.getMessage().getContentRaw().startsWith("ready")) {
//            GameManager game = findGameForUser(event.getAuthor());
//            if (game != null) {
//                readyUp(event.getMessage(), game);
//            } else {
//                sendErrorResponse(event.getMessage(), "Could not find a game you are a member of.");
//            }
//        }

        // Veto command
        if (event.getMessage().getContentRaw().startsWith("veto")) {
            GameData game = gameDB.get(event.getAuthor());
            if (game != null) {
                useVeto(event.getMessage(), game);
            } else {
                sendErrorResponse(event.getMessage(), "Could not find a game you are a member of.");
            }
        }

        // Execute command
        if (event.getMessage().getContentRaw().startsWith("execute")) {
            GameData game = gameDB.get(event.getAuthor());
            if (game != null) {
                useExecute(event.getMessage(), game);
            } else {
                sendErrorResponse(event.getMessage(), "Could not find a game you are a member of.");
            }
        }

        // Detective command
        if (event.getMessage().getContentRaw().startsWith("detect")) {
            GameData game = gameDB.get(event.getAuthor());
            if (game != null) {
                useDetect(event.getMessage(), game);
            } else {
                sendErrorResponse(event.getMessage(), "Could not find a game you are a member of.");
            }
        }
    }

    /**
     * Triggered when a display message is updated with a reaction.
     *
     * @param updater
     * @param updateText
     * @param game
     */
    private void onDisplayMessageUpdate(User updater, String updateText, GameController game) {
        switch (game.getState()) {
            case NEW -> {
                if (updateText.contains("\u2705")) { // Checkmark
                    game.startGame();
                }
            }
            case PREGAME -> {
                GameRole chosenRole = null;
                if (updateText.contains("\uD83C\uDDE8")) { // C
                    game.readyUp(updater.getIdLong(), crewRole);
                } else if (updateText.contains("\uD83C\uDDEE")) { // I
                    game.readyUp(updater.getIdLong(), imposterRole);
                }

            }
            case ACTIVE -> {
                if (updateText.contains("\uD83D\uDD04")) { // Restart command
                    game.restartGame();
                } else if (updateText.contains("\uD83D\uDED1")) { // Stop command
                    GameController removedGame = gameDB.remove(updater);
                    if (removedGame != null) {
                        game.stopGame();
                    }
                }
            }
            default -> {
            }
        }
    }

    /**
     * Refreshes the game message (only works for games with "New" state).
     *
     * @param game
     */
    private void refreshNewGameMessage(GameData game) {
        if (game.displayMessge != null) {
            List<MessageEmbed> gameMessageEmbeds = game.displayMessge.getEmbeds();
            if (!gameMessageEmbeds.isEmpty()) {
                MessageEmbed originalEmbed = gameMessageEmbeds.get(0);

                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(originalEmbed.getColor());
                eb.setTitle(originalEmbed.getTitle());
                AuthorInfo authorInfo = originalEmbed.getAuthor();
                if (authorInfo != null) {
                    eb.setAuthor(authorInfo.getName(), authorInfo.getUrl(), authorInfo.getIconUrl());
                }

                StringBuilder sb = new StringBuilder();
                for (GameRole role : game.playableRoles) {
                    sb.append(String.format("> __%s__:\n> ```%s```\n", role.name, role.description));
                }
                eb.addField("Roles:", sb.toString(), true);

                sb = new StringBuilder();
                for (User user : game.getAllPlayers()) {
                    sb.append(user.getAsMention());
                    sb.append("\n");
                }
                eb.addField("Players", String.format(">>> %s", sb.toString()), true);

                eb.addField("What Next?", "To add or remove players, use the ***padd*** or ***prem*** commands.\nTo add or remove roles, use the ***radd*** or ***rrem*** commands.\nTo start the game, click on the \u2705 emote.", false);

                Footer originalFooter = originalEmbed.getFooter();
                if (originalFooter != null) {
                    eb.setFooter(originalFooter.getText(), originalFooter.getIconUrl());
                }

                game.displayMessge.editMessage(eb.build()).queue();
            }
        }
    }

    /**
     * Parses the create command. Creates a game for the user who sent the
     * message using the audio channel they're in.
     *
     * @param event Message received event that issued the command
     */
    private void createGameFromMessage(Message sourceMessage) {
        if (gameDB.containsKey(sourceMessage.getAuthor())) {
            sendErrorResponse(sourceMessage, "You are already the creator of another game.");
            return;
        }

        // Create the list of game members and add the author of the message as a member
        ArrayList<Long> gameMembers = new ArrayList<>();
        gameMembers.add(sourceMessage.getAuthor().getIdLong());

        // Parse out the arguments given
        HashSet<GameRole> rolesForThisGame = new HashSet<>();
        if (sourceMessage.getContentRaw().length() > (prefix + "create").length()) {
            String[] postCommandArgs = sourceMessage.getContentRaw().substring((prefix + "create").length() + 1).split(" ");
            for (String arg : postCommandArgs) {
                GameRole role = findRoleFromString(arg);
                if (role != null && !role.isDefault) {
                    rolesForThisGame.add(role);
                }
            }
        }

        // Add all users from voice chat except for the author to the game
        Member authorAsMember = sourceMessage.getMember();
        if (authorAsMember != null) {
            GuildVoiceState vs = authorAsMember.getVoiceState();
            if (vs != null && vs.inVoiceChannel() && vs.getChannel() != null) {
                VoiceChannel vc = vs.getChannel();
                if (vc != null) {
                    for (Member vcMember : vc.getMembers()) {
                        if (!vcMember.getUser().equals(sourceMessage.getAuthor())) {
                            gameMembers.add(vcMember.getIdLong());
                        }
                    }
                }
            }
        }

        GameController<EmbedMessageDisplay> controller = new GameController<>(new EmbedMessageDisplay(sourceMessage), gameMembers, new ArrayList<>(rolesForThisGame));
        gameDB.put(sourceMessage.getAuthor(), controller);
    }

    /**
     * Attempts to delete a game for the given user.
     *
     * @param user
     * @return
     */
    private boolean tryDeleteUsersGame(User user) {

    }

    /**
     * Attempts to use a veto for the author of the given message in the given
     * game
     *
     * @param sourceMessage
     * @param game
     */
    private void useVeto(Message sourceMessage, GameData game) {
        game.getRolesForPlayer(sourceMessage.getAuthor()).stream().filter(role -> (role.id == 3)).forEachOrdered(_item -> {
            if (game.useVeto()) {
                game.getAllPlayers().forEach(player -> {
                    player.openPrivateChannel().queue((channel)
                            -> {
                        channel.sendMessage("VETO USED! SKIP VOTE IMMEDIATELY.").queue();
                    });
                });
            } else {
                sourceMessage.getChannel().sendMessage("You've already used a veto.").queue();
            }
        });
    }

    /**
     * Attempts to use an execute for the author of the given message in the
     * given game
     *
     * @param sourceMessage
     * @param game
     */
    private void useExecute(Message sourceMessage, GameData game) {
        game.getRolesForPlayer(sourceMessage.getAuthor()).stream().filter(role -> (role.id == 4)).forEachOrdered(_item -> {
            if (game.useExecution()) {
                game.getAllPlayers().forEach(player -> {
                    player.openPrivateChannel().queue((channel)
                            -> {
                        channel.sendMessage("EXECUTE USED! Vote for " + sourceMessage.getContentRaw().substring(8)).queue();
                    });
                });
            } else {
                sourceMessage.getChannel().sendMessage("You've already used your execution.").queue();
            }
        });
    }

    /**
     * Attempts to use a detect for the author of the given message in the given
     * game
     *
     * @param sourceMessage
     * @param game
     */
    private void useDetect(Message sourceMessage, GameData game) {
        String[] postCommandArgs = sourceMessage.getContentRaw().substring(("detect").length() + 1).split(" ");

        // Incorrect arguments
        if (postCommandArgs.length < 2) {
            sourceMessage.getChannel().sendMessage("Invalid format. Command should be 'detect [discord user name] [suspected role]'.").queue();
            return;
        }

        // Parse given role
        GameRole givenRole = findRoleFromString(postCommandArgs[1]);
        if (givenRole == null) {
            sourceMessage.getChannel().sendMessage("Invalid role. Check role list.").queue();
            return;
        }

        // Get the user
        User mostLikelyUser = findUserInGame(postCommandArgs[0], game);

        // Ensure sender has detective role then run detective search
        game.getRolesForPlayer(sourceMessage.getAuthor()).forEach(role -> {
            if (role.id == 7) {
                if (game.useDetect()) {
                    if (game.getRolesForPlayer(mostLikelyUser).contains(role)) {
                        sourceMessage.getChannel().sendMessage(mostLikelyUser.getName() + " IS a(n) " + role.name).queue();
                    } else {
                        sourceMessage.getChannel().sendMessage(mostLikelyUser.getName() + " IS NOT a(n) " + role.name).queue();
                    }
                } else {
                    sourceMessage.getChannel().sendMessage("You've already used your detect.").queue();
                }
            } else {
                sourceMessage.getChannel().sendMessage("You are not able to detect this game.").queue();
            }
        });
    }

    /**
     * Go through the database and remove any old games
     */
    private void purgeDatabase() {
        Logger.getLogger(CommandListener.class.getName()).log(Level.INFO, "Started automatic gameDB purge");

        if (debug) {
            Logger.getLogger(CommandListener.class.getName()).log(Level.INFO, String.format(String.format("DEBUG - Previous gameDB: %s", gameDB.toString())));
        }

        List<User> gameOwners = new ArrayList<>(gameDB.keySet());
        gameOwners.forEach(gameOwner -> {
            GameData game = gameDB.get(gameOwner);
            if (game.getState() != GameState.ACTIVE && new Interval(game.getLastGameStateChangeTime(), new Instant()).toDurationMillis() > maximumInactiveTimeInMinutes * 60000L) {
                tryDeleteUsersGame(gameOwner);
                gameOwner.openPrivateChannel().queue((channel) -> {
                    channel.sendMessage("Your Among Us+ game has been inactive for longer than 20 minutes. It has been automatically stopped.").queue();
                });
            }
        });

        if (debug) {
            Logger.getLogger(CommandListener.class.getName()).log(Level.INFO, String.format(String.format("DEBUG - New gameDB: %s", gameDB.toString())));
        }
    }

    /**
     * Find a user in the given game whose name matches the given query
     *
     * @param query
     * @param game
     * @return
     */
    private User findUserInGame(String query, GameData game) {
        String playersName = query;
        int shortestDistance = Integer.MAX_VALUE;
        User mostLikelyUser = null;
        LevenshteinDistance ld = new LevenshteinDistance();
        for (User user : game.getAllPlayers()) {
            int newDistance = ld.apply(playersName, user.getName());
            if (shortestDistance > newDistance) {
                mostLikelyUser = user;
                shortestDistance = newDistance;
            }
        }

        return mostLikelyUser;
    }

    /**
     * Find a user in the given guild whose name matches the given query
     *
     * @param guild
     * @param query
     * @return
     */
    private User findUserInGuild(Guild guild, String query) {

        List<Member> membersWithQueriedName = guild.getMembersByEffectiveName(query, true);
        if (membersWithQueriedName.size() == 1) {
            return membersWithQueriedName.get(0).getUser();
        } else if (membersWithQueriedName.size() > 1) {
            return null;
        } else {
            String playersName = query;
            int shortestDistance = Integer.MAX_VALUE;
            User mostLikelyUser = null;
            LevenshteinDistance ld = new LevenshteinDistance();
            for (Member member : membersWithQueriedName) {
                int newDistance = ld.apply(playersName, member.getEffectiveName());
                if (shortestDistance > newDistance) {
                    mostLikelyUser = member.getUser();
                    shortestDistance = newDistance;
                }
            }
            return mostLikelyUser;
        }
    }

    /**
     * Get the GameRole from a string (uses the GameRole name or alias(es))
     *
     * @param query String to use to search
     * @return A GameRole with a name or alias matching the query if found. Null
     * otherwise.
     */
    private GameRole findRoleFromString(String query) {
        for (GameRole role : allRoles) {
            // Check if role name = query
            if (query.equalsIgnoreCase(role.name)) {
                return role;
            } else {
                // Check if any role aliases = query
                for (String roleAlias : role.aliases) {
                    if (query.equalsIgnoreCase(roleAlias)) {
                        return role;
                    }
                }
            }
        }

        return null;
    }

    // --------- HELPER METHODS ------------
    // -- Message Helpers --
    /**
     * Sends a response to the given message
     *
     * @param event
     * @param message
     */
    private void sendResponse(Message receivedMessage, String message) {
        receivedMessage.getChannel().sendMessage(message).queue();
    }

    /**
     * Sends an error response to the given message
     *
     * @param event
     * @param message
     */
    private void sendErrorResponse(Message receivedMessage, String message) {
        receivedMessage.addReaction("⚠️").queue();
        receivedMessage.getChannel().sendMessage(message).queue();
    }

    /**
     * A TimerTask that will run the database purge command on the GameListener
     * it was given
     */
    class PurgeTimerTask extends TimerTask {

        /**
         * Game Listener to purge
         */
        private final CommandListener gl;

        /**
         * Stores the GameListener to use for purging when this command is ran.
         *
         * @param gameListener
         */
        public PurgeTimerTask(CommandListener gameListener) {
            gl = gameListener;
        }

        @Override
        public void run() {
            gl.purgeDatabase();
        }
    }
}
