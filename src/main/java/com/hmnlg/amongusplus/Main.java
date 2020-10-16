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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
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
        JDABuilder builder = JDABuilder.createLight(token);
        builder.enableIntents(GatewayIntent.GUILD_MEMBERS);
        builder.disableIntents(GatewayIntent.GUILD_BANS, GatewayIntent.GUILD_EMOJIS, GatewayIntent.GUILD_INVITES);
        builder.setMemberCachePolicy(MemberCachePolicy.ONLINE);
        builder.setChunkingFilter(ChunkingFilter.ALL);
        builder.enableCache(CacheFlag.VOICE_STATE);
        GameListener gameListener = new GameListener(roles, false);
        
        builder.addEventListeners(gameListener);
        
        final JDA api;
        try {
            api = builder.build();
            api.addEventListener(new AdminListener(api, gameListener));
        } catch (LoginException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
}
