/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hmnlg.amongusplus;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import org.yaml.snakeyaml.Yaml;

/**
 * Main class; starts the bot.
 *
 * @author maikotui
 */
public class Main {

    /**
     * Starts the Among Us Plus discord bot.
     *
     * @param args Give the bot token as the first argument.
     */
    public static void main(String[] args){
        // Ensure that a token was provided
        String token;
        if (args.length < 1) {
            if(System.getenv("BOT_TOKEN") == null) {
            System.out.println("Please provide a token.");
            return;
            }
            else {
                token = System.getenv("BOT_TOKEN");
            }
        }
        else {
            token = args[0];
        }

        // Get the list of usable game roles
        final Yaml yaml = new Yaml();
        final List<GameRole> roles;
        try ( InputStream in = Main.class.getClassLoader().getResourceAsStream("roles.yml")) {
            Map<Object, List<GameRole>> temp = yaml.load(in);
            roles = temp.get("roles");
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            return;
        }

        // Build the API
        JDABuilder builder = new JDABuilder(AccountType.BOT);
        builder.setToken(token);
        GameListener gameListener = new GameListener(roles, false);
        builder.addEventListener(gameListener);
        
        final JDA api;
        try {
            api = builder.build();
            api.addEventListener(new AdminListener(api, gameListener));
        } catch (LoginException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
}
