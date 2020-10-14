/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.joda.time.Instant;
import org.joda.time.Interval;

/**
 * A ListenerAdapter for a JDA object that listens for game commands.
 *
 * @author maikotui
 */
public class GameListener extends ListenerAdapter {

    /**
     * Prefix that must be used for commands to be recognized
     */
    private final String prefix = "au+";

    /**
     * A list of all the roles that this GameListener will listen for
     */
    private final List<GameRole> allRoles;

    /**
     * The database of all created games. This will be purged periodically (see
     * purgeIntervalInMinutes)
     */
    private final Map<Message, GameManager> gameDB;

    /**
     * The timer that will run the purge command
     */
    private final Timer purgeTimer;

    /**
     * The amount of time in minutes between each purge of the database
     */
    private final int purgeIntervalInMinutes = 20;

    /**
     * The amount of time in minutes since a state change for a game to be
     * purged from the database
     */
    private final int maximumInactiveTimeInMinutes = 20;

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
    public GameListener(List<GameRole> allRoles, boolean debug) {
        super();

        // Assign from arguments
        this.allRoles = allRoles;
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
    public GameListener(GameListener gameListener) {
        super();

        // Assign from arguments
        this.allRoles = gameListener.allRoles;
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

        // Get and store message and text content (used to simplify calls
        Message message = event.getMessage();
        String content = message.getContentRaw();

        // All Bot Commands
        if (content.startsWith(prefix)) {

            // ----- ALWAYS ACTIVE COMMANDS -----
            // Ping Pong
            if (content.equals(prefix + "ping")) {
                message.addReaction("\u2705").queue();
                sendResponse(message, "Pong :)");
                if (debug) {
                    Logger.getLogger(GameListener.class.getName()).log(Level.INFO, String.format("Responded to ping from %s", message.getAuthor().getName()));
                }
            }

            // Help Command
            if (content.equals(prefix + "help") || content.equals(prefix + "?")) {
                String helpText = prefix + "ping - Ping Pong to make sure I'm awake.\n\n";
                helpText += prefix + "roles - Gives a list of roles that you can use in game.\n\n";
                helpText += prefix + "create - Creates a new game in the voice channel you are in. (Best in Among Us VC). Note: Everyone in that voice channel will be added. Use the role names or alias to add them into the game on creation.\n\n";
                helpText += prefix + "start - Starts the game. After this command is issued, everyone playing should message the bot to ready up.\n\n";
                helpText += prefix + "restart - At the end of the round, use this command to restart the game. Everyone will have to ready up with their role again.\n\n";
                helpText += prefix + "stop - If you are done playing among us, issue this command to stop the current game.";
                message.addReaction("\u2705").queue();
                sendResponse(message, helpText);
                if (debug) {
                    Logger.getLogger(GameListener.class.getName()).log(Level.INFO, String.format("Responded to help command from %s", message.getAuthor().getName()));
                }
            }

            // List Roles
            if (content.equals(prefix + "roles")) {
                String roleInfo = "";
                roleInfo = allRoles.stream().map(role -> role.name + "\n" + Arrays.toString(role.aliases) + "\n" + role.description + "\n\n").reduce(roleInfo, String::concat);
                message.addReaction("\u2705").queue();
                sendResponse(message, roleInfo);
                if (debug) {
                    Logger.getLogger(GameListener.class.getName()).log(Level.INFO, String.format("Responded to roles command from %s", message.getAuthor().getName()));
                }
            }

            // ----- GAME COMMANDS ----
            // Create command
            if (content.startsWith(prefix + "create")) {
                createGame(event.getMessage());
            }

            // Start command
            if (content.equals(prefix + "start")) {
                GameManager game = findGameForUser(event.getAuthor());
                if (game != null) {
                    startGame(event.getMessage(), game);
                } else {
                    sendErrorResponse(event.getMessage(), "You are not in a game.");
                }
            }

            // Reset command
            if (content.equals(prefix + "reset")) {
                GameManager game = findGameForUser(event.getAuthor());
                if (game != null) {
                    game.resetGame();
                    sendResponse(event.getMessage(), "Game has been reset.");
                } else {
                    sendErrorResponse(event.getMessage(), "You are not in a game.");
                }
            }

            if (content.equals(prefix + "stop")) {
                if (gameDB.values().removeIf(game -> game.getAllPlayers().contains(event.getAuthor()))) {
                    sendResponse(event.getMessage(), "Stopped game.");
                } else {
                    sendErrorResponse(event.getMessage(), "You are not in a game.");
                }
            }

            if (content.startsWith(prefix + "padd") && debug) {
                GameManager game = findGameForUser(event.getAuthor());
                if (game != null) {
                    String[] postCommandArgs = event.getMessage().getContentRaw().substring((prefix + "padd").length() + 1).split(" ");
                    if (postCommandArgs.length > 0) {
                        User user = findUserInGuild(event.getGuild(), postCommandArgs[0]);
                        game.addPlayer(user);
                        sendResponse(event.getMessage(), "Added " + user.getName() + " to the game.");
                    }
                } else {
                    sendErrorResponse(event.getMessage(), "You are not in a game.");
                }
            }

            if (content.startsWith(prefix + "prem") && debug) {
                GameManager game = findGameForUser(event.getAuthor());
                if (game != null) {
                    String[] postCommandArgs = event.getMessage().getContentRaw().substring((prefix + "padd").length() + 1).split(" ");
                    if (postCommandArgs.length > 0) {
                        User user = findUserInGuild(event.getGuild(), postCommandArgs[0]);
                        game.removePlayer(user);
                        sendResponse(event.getMessage(), "Added " + user.getName() + " to the game.");
                    }
                } else {
                    sendErrorResponse(event.getMessage(), "You are not in a game.");
                }
            }

            if (content.startsWith(prefix + "radd") && debug) {
                GameManager game = findGameForUser(event.getAuthor());
                if (game != null) {
                    String[] postCommandArgs = event.getMessage().getContentRaw().substring((prefix + "radd").length() + 1).split(" ");
                    if (postCommandArgs.length > 0) {
                        GameRole role = findRoleFromString(postCommandArgs[0]);
                        game.addRole(role);
                        sendResponse(event.getMessage(), "Added role '" + role.name + "' to the game.");
                    }
                } else {
                    sendErrorResponse(event.getMessage(), "You are not in a game.");
                }
            }

            if (content.startsWith(prefix + "rrem") && debug) {
                GameManager game = findGameForUser(event.getAuthor());
                if (game != null) {
                    String[] postCommandArgs = event.getMessage().getContentRaw().substring((prefix + "radd").length() + 1).split(" ");
                    if (postCommandArgs.length > 0) {
                        GameRole role = findRoleFromString(postCommandArgs[0]);
                        game.removeRole(role);
                        sendResponse(event.getMessage(), "Removes role '" + role.name + "' to the game.");
                    }
                } else {
                    sendErrorResponse(event.getMessage(), "You are not in a game.");
                }
            }

            if (content.equals(prefix + "plist")) {
                GameManager game = findGameForUser(event.getAuthor());
                if (game != null) {
                    String playerList = "Player List:\n";
                    playerList = game.getAllPlayers().stream().map(user -> user.getName() + "\n").reduce(playerList, String::concat);
                    sendResponse(event.getMessage(), playerList);
                } else {
                    sendErrorResponse(event.getMessage(), "You are not in a game.");
                }
            }
        }
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        event.getChannel().getMessageById(event.getMessageId()).queue(message -> {
            if (message.getAuthor().equals(event.getJDA().getSelfUser())) { // If message is from this bot
                if (!event.getUser().equals(event.getJDA().getSelfUser())) { // If a user other than this bot reacted to a message
                    String reaction = event.getReactionEmote().getName();
                    if (reaction.contains("\uD83C\uDDE8")) { // C
                        System.out.println("C");
                    } else if (reaction.contains("\uD83C\uDDEE")) { // I
                        System.out.println("I");
                    }

//                    try {
//                    if (game.attemptGameStartWithRoleAssignment(sourceMessage.getAuthor(), readyRole)) {
//                        // Everyone is ready, time to start the game.
//                        try {
//                            Map<User, List<GameRole>> roleMap = game.giveOutNondefaultRoles();
//
//                            // Send role assignment message for non default roles and send everyone a game starting message
//                            game.getAllPlayers().forEach(player -> {
//                                player.openPrivateChannel().queue((channel) -> {
//                                    game.getRolesForPlayer(player).stream().filter(role -> (!role.isDefault)).forEachOrdered(role -> {
//                                        channel.sendMessage(role.assignmentMessage).queue();
//                                    });
//                                    channel.sendMessage("Everyone's in! Game is starting.").queue();
//                                });
//                            });
//
//                            // Send a debug message
//                            if (debug) {
//                                Logger.getLogger(GameListener.class.getName()).log(Level.INFO, String.format("Game is starting. Rolemap: %s", roleMap));
//                            }
//                        } catch (GeneralGameException err) {
//                            sendErrorResponse(sourceMessage, err.getMessage());
//                            Logger.getLogger(GameListener.class.getName()).log(Level.WARNING, err.getMessage());
//                        }
//                    } else {
//                        sourceMessage.getChannel().sendMessage("You're in. Waiting on others to ready up.").queue();
//                    }
//                } catch (GeneralGameException err) {
//                    sourceMessage.getChannel().sendMessage(err.getMessage()).queue();
//                }
                    event.getReaction().removeReaction(event.getUser()).queue();
                }
            }
        });
    }

    /**
     * Ran every time a private message is received.
     *
     * @param event Information regarding the message received
     */
    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
        // Ready up message
        if (event.getMessage().getContentRaw().startsWith("ready")) {
            GameManager game = findGameForUser(event.getAuthor());
            if (game != null) {
                readyUp(event.getMessage(), game);
            } else {
                sendErrorResponse(event.getMessage(), "Could not find a game you are a member of.");
            }
        }

        // Veto command
        if (event.getMessage().getContentRaw().startsWith("veto")) {
            GameManager game = findGameForUser(event.getAuthor());
            if (game != null) {
                useVeto(event.getMessage(), game);
            } else {
                sendErrorResponse(event.getMessage(), "Could not find a game you are a member of.");
            }
        }

        // Execute command
        if (event.getMessage().getContentRaw().startsWith("execute")) {
            GameManager game = findGameForUser(event.getAuthor());
            if (game != null) {
                useExecute(event.getMessage(), game);
            } else {
                sendErrorResponse(event.getMessage(), "Could not find a game you are a member of.");
            }
        }

        // Detective command
        if (event.getMessage().getContentRaw().startsWith("detect")) {
            GameManager game = findGameForUser(event.getAuthor());
            if (game != null) {
                useDetect(event.getMessage(), game);
            } else {
                sendErrorResponse(event.getMessage(), "Could not find a game you are a member of.");
            }
        }
    }

    /**
     * Parses the create command. Creates a game for the user who sent the
     * message using the audio channel they're in.
     *
     * @param event Message received event that issued the command
     */
    private void createGame(Message sourceMessage) {
        // Create the list of game members and add the author of the message as a member
        ArrayList<User> gameMembers = new ArrayList<>();
        gameMembers.add(sourceMessage.getAuthor());

        // Parse out the arguments given
        HashSet<GameRole> rolesForThisGame = new HashSet<>();
        if (sourceMessage.getContentRaw().length() > (prefix + "create").length()) {
            String[] postCommandArgs = sourceMessage.getContentRaw().substring((prefix + "create").length() + 1).split(" ");
            for (String arg : postCommandArgs) {
                if (arg == "auto") {
                    // Get a list of gamemembers and their names
                    VoiceChannel vc = sourceMessage.getMember().getVoiceState().getChannel();
                    if (vc != null) {
                        for (Member vcMember : vc.getMembers()) {
                            if (!vcMember.getUser().equals(sourceMessage.getAuthor())) {
                                gameMembers.add(vcMember.getUser());
                            }
                        }
                    }
                }
                GameRole role = findRoleFromString(arg);
                if (role != null && !role.isDefault) {
                    rolesForThisGame.add(role);
                }
            }
            sendResponse(sourceMessage, "Roles added: " + rolesForThisGame.toString());
        } else {
            sendErrorResponse(sourceMessage, "Please include additional roles to assign. Use the roles command for more info.");
            return;
        }

        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.red);
        eb.setTitle(String.format("%s's Game", sourceMessage.getAuthor().getAsMention()));

        StringBuilder sb = new StringBuilder();
        for (GameRole role : rolesForThisGame) {
            sb.append(String.format("%s: %s", role.name, role.description));
        }
        eb.addField("Roles", sb.toString(), true);

        sb = new StringBuilder();
        for (User user : gameMembers) {
            sb.append(user.getAsMention());
        }
        eb.setFooter("Created by humanalog#2515", sourceMessage.getJDA().getSelfUser().getAvatarUrl());
        eb.setThumbnail(sourceMessage.getJDA().getSelfUser().getAvatarUrl());

        sourceMessage.getChannel().sendMessage(eb.build()).queue(message -> {
            // Try to start game with given member list
            gameDB.put(message, new GameManager(gameMembers, new ArrayList<>(rolesForThisGame)));
        });

        // Send a message to let the user know the game was created
        sourceMessage.addReaction("\u2705").queue();
    }

    /**
     * Attempts to move the game to pregame.
     *
     * @param event
     */
    private void startGame(Message sourceMessage, GameManager game) {
        // Try to move to pregame.
        try {
            game.moveToPregame();

            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(Color.red);
            eb.setTitle("Game Title");
            eb.addField("Choose Role", "Choose \uD83C\uDDE8 for crewmate and \uD83C\uDDEE for imposter.", false);
            eb.setFooter("Created by humanalog#2515", sourceMessage.getJDA().getSelfUser().getAvatarUrl());
            eb.setThumbnail(sourceMessage.getJDA().getSelfUser().getAvatarUrl());
            sourceMessage.getChannel().sendMessage(eb.build()).queue((message) -> {
                message.addReaction("\uD83C\uDDE8").queue(); // C
                message.addReaction("\uD83C\uDDEE").queue(); // I
            });
            sourceMessage.addReaction("\u2705").queue();
        } catch (GeneralGameException err) {
            sendErrorResponse(sourceMessage, err.getMessage());
        }
    }

    private void readyUp(Message sourceMessage, GameManager game) {
        String[] postCommandArgs = sourceMessage.getContentRaw().substring(("ready").length() + 1).split(" ");
        if (postCommandArgs.length > 0) {
            GameRole readyRole = findRoleFromString(postCommandArgs[0]);
            if (readyRole != null) {
                try {
                    if (game.attemptGameStartWithRoleAssignment(sourceMessage.getAuthor(), readyRole)) {
                        // Everyone is ready, time to start the game.
                        try {
                            Map<User, List<GameRole>> roleMap = game.giveOutNondefaultRoles();

                            // Send role assignment message for non default roles and send everyone a game starting message
                            game.getAllPlayers().forEach(player -> {
                                player.openPrivateChannel().queue((channel) -> {
                                    game.getRolesForPlayer(player).stream().filter(role -> (!role.isDefault)).forEachOrdered(role -> {
                                        channel.sendMessage(role.assignmentMessage).queue();
                                    });
                                    channel.sendMessage("Everyone's in! Game is starting.").queue();
                                });
                            });

                            // Send a debug message
                            if (debug) {
                                Logger.getLogger(GameListener.class.getName()).log(Level.INFO, String.format("Game is starting. Rolemap: %s", roleMap));
                            }
                        } catch (GeneralGameException err) {
                            sendErrorResponse(sourceMessage, err.getMessage());
                            Logger.getLogger(GameListener.class.getName()).log(Level.WARNING, err.getMessage());
                        }
                    } else {
                        sourceMessage.getChannel().sendMessage("You're in. Waiting on others to ready up.").queue();
                    }
                } catch (GeneralGameException err) {
                    sourceMessage.getChannel().sendMessage(err.getMessage()).queue();
                }
            } else {
                sourceMessage.getChannel().sendMessage("Could not identify the '" + postCommandArgs[0] + "' role.").queue();
            }
        } else {
            sourceMessage.getChannel().sendMessage("Please give the role you are readying up as (use 'ready [role]').").queue();
        }
    }

    private void useVeto(Message sourceMessage, GameManager game) {
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

    private void useExecute(Message sourceMessage, GameManager game) {
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

    private void useDetect(Message sourceMessage, GameManager game) {
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

    private void purgeDatabase() {
        Logger.getLogger(GameListener.class.getName()).log(Level.INFO, "Started automatic gameDB purge");

        if (debug) {
            Logger.getLogger(GameListener.class.getName()).log(Level.INFO, String.format(String.format("DEBUG - Previous gameDB: %s", gameDB.toString())));
        }

        gameDB.values().removeIf(game -> game.getState() != GameState.ACTIVE && new Interval(game.getLastGameStateChangeTime(), new Instant()).toDurationMillis() > maximumInactiveTimeInMinutes * 60000L);

        if (debug) {
            Logger.getLogger(GameListener.class.getName()).log(Level.INFO, String.format(String.format("DEBUG - New gameDB: %s", gameDB.toString())));
        }
    }

    private User findUserInGame(String query, GameManager game) {
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

    private GameManager findGameForUser(User user) {
        for (GameManager game : gameDB.values()) {
            if (game.getAllPlayers().contains(user)) {
                return game;
            }
        }
        return null;
    }

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
        private final GameListener gl;

        /**
         * Stores the GameListener to use for purging when this command is ran.
         *
         * @param gameListener
         */
        public PurgeTimerTask(GameListener gameListener) {
            gl = gameListener;
        }

        @Override
        public void run() {
            gl.purgeDatabase();
        }
    }
}
