/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hmnlg.amongusplus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.commons.text.similarity.LevenshteinDistance;

/**
 *
 * @author maikotui
 */
public class GameListener extends ListenerAdapter {

    private final String prefix = "au+";

    private final List<GameRole> allRoles;

    private final Map<VoiceChannel, GameManager> gameDB;

    /**
     * Enables debug messages
     */
    private boolean debug;

    /**
     * Initializes a new listener for game commands
     *
     * @param allRoles
     * @param debug
     */
    public GameListener(List<GameRole> allRoles, boolean debug) {
        super();
        gameDB = new HashMap<>();
        this.allRoles = allRoles;
        this.debug = debug;
    }

    public GameListener(GameListener gameListener) {
        super();
        gameDB = new HashMap<>();
        this.allRoles = gameListener.allRoles;
        this.debug = gameListener.debug;
    }

    /**
     * Toggles debug
     *
     * @return returns the value of debug after toggle
     */
    public boolean debugToggle() {
        debug = !debug;
        return debug;
    }

    /**
     *
     * @param event
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }

        Message message = event.getMessage();
        String content = message.getContentRaw();

        // All Bot Commands
        if (content.startsWith(prefix)) {

            // ----- ALWAYS ACTIVE COMMANDS -----
            // Ping Pong
            if (content.equals(prefix + "ping")) {
                addReactionToMessage(event.getMessage(), "\uD83D\uDE04️️");
                 Logger.getLogger(GameListener.class.getName()).log(Level.INFO, "responded to ping");
                sendResponse(message, "Pong :)");
            }

            // Help Command
            if (content.equals(prefix + "help") || content.equals(prefix + "?")) {
                sendHelpInfo(event);
            }

            // List Roles
            if (content.equals(prefix + "roles")) {
                addReactionToMessage(event.getMessage(), "👌️️");
                String roleInfo = "";
                roleInfo = allRoles.stream().map(role -> role.name + "\n" + Arrays.toString(role.aliases) + "\n" + role.description + "\n\n").reduce(roleInfo, String::concat);
                sendResponse(message, roleInfo);
            }

            // ----- GAME COMMANDS ----
            // Create command
            if (content.startsWith(prefix + "create")) {
                VoiceChannel activeVoiceChannel = event.getMember().getVoiceState().getChannel();
                if (activeVoiceChannel != null) {
                    createGame(event.getMessage(), activeVoiceChannel);
                } else {
                    sendErrorResponse(event.getMessage(), "Please join a voice channel to create a game.");
                }
            }

            // Start command
            if (content.equals(prefix + "start")) {
                VoiceChannel activeVoiceChannel = event.getMember().getVoiceState().getChannel();
                if (activeVoiceChannel != null) {
                    GameManager game = gameDB.get(activeVoiceChannel);
                    if (game != null) {
                        startGame(event.getMessage(), game);
                    } else {
                        sendErrorResponse(event.getMessage(), "This voice channel is not running a game.");
                    }
                } else {
                    sendErrorResponse(event.getMessage(), "Please join a voice channel with an active game to run this command.");
                }
            }

            // Reset command
            if (content.equals(prefix + "reset")) {
                VoiceChannel activeVoiceChannel = event.getMember().getVoiceState().getChannel();
                if (activeVoiceChannel != null) {
                    GameManager game = gameDB.get(activeVoiceChannel);
                    if (game != null) {
                        game.resetGame();
                        sendResponse(event.getMessage(), "Game has been reset.");
                    } else {
                        sendErrorResponse(event.getMessage(), "This voice channel is not running a game.");
                    }
                } else {
                    sendErrorResponse(event.getMessage(), "Please join a voice channel with an active game to run this command.");
                }
            }

            if (content.equals(prefix + "stop")) {
                VoiceChannel activeVoiceChannel = event.getMember().getVoiceState().getChannel();
                if (activeVoiceChannel != null) {
                    if (gameDB.remove(activeVoiceChannel) != null) {
                        sendResponse(event.getMessage(), "Stopped game.");
                    } else {
                        sendErrorResponse(event.getMessage(), "This voice channel is not running a game.");
                    }
                } else {
                    sendErrorResponse(event.getMessage(), "Please join a voice channel with an active game to run this command.");
                }
            }

            if (content.startsWith(prefix + "padd") && debug) {
                VoiceChannel activeVoiceChannel = event.getMember().getVoiceState().getChannel();
                if (activeVoiceChannel != null) {
                    GameManager game = gameDB.get(activeVoiceChannel);
                    if (game != null) {
                        String[] postCommandArgs = event.getMessage().getContentRaw().substring((prefix + "padd").length() + 1).split(" ");
                        if (postCommandArgs.length > 0) {
                            User user = findUserInGuild(event.getGuild(), postCommandArgs[0]);
                            game.addPlayer(user);
                            sendResponse(event.getMessage(), "Added " + user.getName() + " to the game.");
                        }
                    } else {
                        sendErrorResponse(event.getMessage(), "This voice channel is not running a game.");
                    }
                } else {
                    sendErrorResponse(event.getMessage(), "Please join a voice channel with an active game to run this command.");
                }
            }

            if (content.startsWith(prefix + "prem") && debug) {
                VoiceChannel activeVoiceChannel = event.getMember().getVoiceState().getChannel();
                if (activeVoiceChannel != null) {
                    GameManager game = gameDB.get(activeVoiceChannel);
                    if (game != null) {
                        String[] postCommandArgs = event.getMessage().getContentRaw().substring((prefix + "padd").length() + 1).split(" ");
                        if (postCommandArgs.length > 0) {
                            User user = findUserInGuild(event.getGuild(), postCommandArgs[0]);
                            game.removePlayer(user);
                            sendResponse(event.getMessage(), "Added " + user.getName() + " to the game.");
                        }
                    } else {
                        sendErrorResponse(event.getMessage(), "This voice channel is not running a game.");
                    }
                } else {
                    sendErrorResponse(event.getMessage(), "Please join a voice channel with an active game to run this command.");
                }
            }

            if (content.startsWith(prefix + "radd") && debug) {
                VoiceChannel activeVoiceChannel = event.getMember().getVoiceState().getChannel();
                if (activeVoiceChannel != null) {
                    GameManager game = gameDB.get(activeVoiceChannel);
                    if (game != null) {
                        String[] postCommandArgs = event.getMessage().getContentRaw().substring((prefix + "radd").length() + 1).split(" ");
                        if (postCommandArgs.length > 0) {
                            GameRole role = findRoleFromString(postCommandArgs[0]);
                            game.addRole(role);
                            sendResponse(event.getMessage(), "Added role '" + role.name + "' to the game.");
                        }
                    } else {
                        sendErrorResponse(event.getMessage(), "This voice channel is not running a game.");
                    }
                } else {
                    sendErrorResponse(event.getMessage(), "Please join a voice channel with an active game to run this command.");
                }
            }

            if (content.startsWith(prefix + "rrem") && debug) {
                VoiceChannel activeVoiceChannel = event.getMember().getVoiceState().getChannel();
                if (activeVoiceChannel != null) {
                    GameManager game = gameDB.get(activeVoiceChannel);
                    if (game != null) {
                        String[] postCommandArgs = event.getMessage().getContentRaw().substring((prefix + "radd").length() + 1).split(" ");
                        if (postCommandArgs.length > 0) {
                            GameRole role = findRoleFromString(postCommandArgs[0]);
                            game.removeRole(role);
                            sendResponse(event.getMessage(), "Removes role '" + role.name + "' to the game.");
                        }
                    } else {
                        sendErrorResponse(event.getMessage(), "This voice channel is not running a game.");
                    }
                } else {
                    sendErrorResponse(event.getMessage(), "Please join a voice channel with an active game to run this command.");
                }
            }

            if (content.equals(prefix + "plist")) {
                VoiceChannel activeVoiceChannel = event.getMember().getVoiceState().getChannel();
                if (activeVoiceChannel != null) {
                    GameManager game = gameDB.get(activeVoiceChannel);
                    if (game != null) {
                        String playerList = "Player List:\n";
                        playerList = game.getAllPlayers().stream().map(user -> user.getName() + "\n").reduce(playerList, String::concat);
                        sendResponse(event.getMessage(), playerList);
                    } else {
                        sendErrorResponse(event.getMessage(), "This voice channel is not running a game.");
                    }
                } else {
                    sendErrorResponse(event.getMessage(), "Please join a voice channel with an active game to run this command.");
                }

            }
        }
    }

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

    private void sendHelpInfo(MessageReceivedEvent event) {
        addReactionToMessage(event.getMessage(), "👌️️");
        String helpText = prefix + "ping - Ping Pong to make sure I'm awake.\n\n";
        helpText += prefix + "roles - Gives a list of roles that you can use in game.\n\n";
        helpText += prefix + "create - Creates a new game in the voice channel you are in. (Best in Among Us VC). Note: Everyone in that voice channel will be added. Use the role names or alias to add them into the game on creation.\n\n";
        helpText += prefix + "start - Starts the game. After this command is issued, everyone playing should message the bot to ready up.\n\n";
        helpText += prefix + "restart - At the end of the round, use this command to restart the game. Everyone will have to ready up with their role again.\n\n";
        helpText += prefix + "stop - If you are done playing among us, issue this command to stop the current game.";
        sendResponse(event.getMessage(), helpText);
    }

    /**
     * Parses the create command. Creates a game for the user who sent the
     * message using the audio channel they're in.
     *
     * @param event Message received event that issued the command
     */
    private void createGame(Message sourceMessage, VoiceChannel vc) {
        // Send starting message
        sendResponse(sourceMessage, "Creating game for voice channel '" + vc.getName() + "'.");

        // Parse command for roles
        HashSet<GameRole> rolesForThisGame = new HashSet<>();
        if (sourceMessage.getContentRaw().length() > (prefix + "create").length()) {
            String[] postCommandArgs = sourceMessage.getContentRaw().substring((prefix + "create").length() + 1).split(" ");
            for (String roleString : postCommandArgs) {
                GameRole role = findRoleFromString(roleString);
                if (role != null && !role.isDefault) {
                    rolesForThisGame.add(role);
                }
            }
            sendResponse(sourceMessage, "Roles added: " + rolesForThisGame.toString());
        } else {
            sendErrorResponse(sourceMessage, "Please include additional roles to assign. Use the roles command for more info.");
            return;
        }

        // Get a list of gamemembers and their names
        ArrayList<User> gameMembers = new ArrayList<>();
        String playerList = "";
        playerList = vc.getMembers().stream().map(member -> {
            gameMembers.add(member.getUser());
            return member;
        }).map(member -> member.getUser().getName() + "\n").reduce(playerList, String::concat);

        // Try to start game with given member list
        gameDB.put(vc, new GameManager(gameMembers, new ArrayList<>(rolesForThisGame)));
        addReactionToMessage(sourceMessage, "✔️️");
        sendResponse(sourceMessage, "The players for this game are:\n" + playerList);
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
            addReactionToMessage(sourceMessage, "👌️️");
            sendResponse(sourceMessage, "Game is started and is waiting for everyone to ready up. Everyone playing should ready up by messaging me whether they are an imposter or crewmate. Message me 'ready [role]'.");
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
                        game.giveOutNondefaultRoles();

                        // Send role assignment message for non default roles and send everyone a game starting message
                        game.getAllPlayers().forEach(player -> {
                            player.openPrivateChannel().queue((channel) -> {
                                game.getRolesForPlayer(player).stream().filter(role -> (!role.isDefault)).forEachOrdered(role -> {
                                    channel.sendMessage(role.assignmentMessage).queue();
                                });
                                channel.sendMessage("Everyone's in! Game is starting.").queue();
                            });
                        });
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
        for (Guild guild : user.getMutualGuilds()) {
            for (VoiceChannel vc : guild.getVoiceChannels()) {
                GameManager game = gameDB.get(vc);
                if (game != null) {
                    if (game.getAllPlayers().contains(user)) {
                        return game;
                    }
                }
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
        addReactionToMessage(receivedMessage, "⚠️");
        receivedMessage.getChannel().sendMessage(message).queue();
    }

    /**
     * Adds a reaction to the given message.
     *
     * @param message
     * @param reaction
     */
    private void addReactionToMessage(Message message, String reaction) {
        message.addReaction(reaction).queue();
    }
}
