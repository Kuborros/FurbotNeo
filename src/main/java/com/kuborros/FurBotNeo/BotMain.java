/*
 * Copyright © 2020 Kuborros (kuborros@users.noreply.github.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kuborros.FurBotNeo;

import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.examples.command.AboutCommand;
import com.kuborros.FurBotNeo.commands.AdminCommands.*;
import com.kuborros.FurBotNeo.commands.GeneralCommands.*;
import com.kuborros.FurBotNeo.commands.LewdCommands.*;
import com.kuborros.FurBotNeo.commands.MusicCommands.*;
import com.kuborros.FurBotNeo.commands.PicCommands.*;
import com.kuborros.FurBotNeo.commands.ShopCommands.*;
import com.kuborros.FurBotNeo.listeners.*;
import com.kuborros.FurBotNeo.utils.config.Database;
import com.kuborros.FurBotNeo.utils.config.FurrySettingsManager;
import com.kuborros.FurBotNeo.utils.config.JConfig;
import com.kuborros.FurBotNeo.utils.msg.HelpConsumer;
import com.kuborros.FurBotNeo.utils.msg.RandomResponse;
import com.kuborros.FurBotNeo.utils.store.JShopInventory;
import com.kuborros.FurBotNeo.utils.store.MemberInventoryCache;
import com.kuborros.FurBotNeo.utils.store.MemberInventoryCacheImpl;
import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.SessionController;
import net.dv8tion.jda.api.utils.SessionControllerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.awt.*;

public class BotMain {

    private static final Logger LOG = LoggerFactory.getLogger("Main");
    public static JConfig cfg;
    public static Database db;
    public static RandomResponse randomResponse;
    public static final FurrySettingsManager settingsManager = new FurrySettingsManager();
    public static SessionController controller;
    public static MemberInventoryCache inventoryCache;
    public static JShopInventory storeItems;

    public static void main(String[] args) {

        if (!System.getProperty("file.encoding").equals("UTF-8")) {
            LOG.info("Not running in UTF-8 mode! This ~might~ end badly for us!");
        }

        String arch = System.getProperty("os.arch");
        boolean x86 = (arch.contains("x86") || arch.contains("amd64") || arch.contains("i386"));

        EventWaiter waiter = new EventWaiter();

        db = new Database();
        db.createTables();


        cfg = new JConfig();

        randomResponse = new RandomResponse(settingsManager);
        inventoryCache = new MemberInventoryCacheImpl();
        controller = new SessionControllerAdapter();

        CommandClientBuilder client = new CommandClientBuilder();
        client.setOwnerId(cfg.getOwnerId());
        client.setEmojis("\u2705", "\u2757", "\u274C");
        client.setPrefix("!");
        client.setGuildSettingsManager(settingsManager);
        client.setHelpConsumer(new HelpConsumer());
        client.addCommands(

                //Default about command

                new AboutCommand(Color.CYAN, "and im here to make this server a better place!",
                        new String[]{"Picture commands!", "Music player!", "Cute furry mascot!"},
                        Permission.ADMINISTRATOR, Permission.MANAGE_ROLES,
                        Permission.MANAGE_SERVER, Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_READ,
                        Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_HISTORY, Permission.MESSAGE_EXT_EMOJI,
                        Permission.MESSAGE_MANAGE, Permission.VOICE_CONNECT, Permission.VOICE_MOVE_OTHERS, Permission.VOICE_DEAF_OTHERS,
                        Permission.VOICE_MUTE_OTHERS, Permission.NICKNAME_CHANGE, Permission.NICKNAME_MANAGE),

                //General
                //Intended for simple commands with next to no interaction, and basic text response.

                new R8BallCmd(),
                new ProfPicCmd(),
                new DiceCmd(),
                new BadJokeCmd(),
                new VoteCommand(),
                new BigTextCmd(),

                //Imageboards
                //All picture search commands go here:

                new E621Cmd(waiter),
                new PokeCmd(waiter),
                new DanCmd(waiter),
                new GelCmd(waiter),
                new KonachanCmd(waiter),
                new YandereCmd(waiter),
                new SafeCmd(waiter),
                new E926Cmd(waiter),
                new R34Cmd(waiter),

                //Music
                //All music player commands here:

                new PlayCommand(),
                new PlayNextCmd(),
                new PlayShuffleCmd(),
                new MusicTimeCommand(),
                new MusicInfoCmd(),
                new MusicPauseCmd(),
                new MusicQueueCmd(waiter),
                new MusicResetCmd(),
                new MusicShuffleCmd(),
                new MusicClearCmd(),
                new MusicSkipCmd(waiter),
                new MusicStopCmd(),
                new MusicVolumeCmd(),

                //Admin
                //These commands are usually restricted to server admins or owner.

                new InfoCommand(),
                new BotBanCmd(),
                new BotUnBanCmd(),
                new StatsCommand(),
                new GuildConfigCommand(),
                new ReloadItemsCommand(),
                new ShutdownCommand(),

                //Lewd
                //All NSFW commands go here, along with all questionable ones.

                new CommandStatCmd(),
                new BoopCommand(),
                new CuddleCommand(),
                new HugCommand(),
                new KissCommand(),
                new PetCommand(),
                new LickCommand(),
                new ShipCommand());

        //Shop
        //Most store commands are subcommands.
        if (cfg.isShopEnabled()) {
            //Only initialise items.json if needed.
            storeItems = new JShopInventory();
            //If it fails, store will be set to disabled, so we check *again* before adding the commands
            if (cfg.isShopEnabled()) {
                client.addCommands(
                        new BuyCommand(waiter),
                        new CoinsCommand(),
                        new GrantRoleCommand(waiter),
                        new SetRoleCommand(waiter),
                        new FullRolesCommand(waiter));
            }
        }


        try {

            if (cfg.isShardingEnabled()) {

                DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.createDefault(cfg.getBotToken());
                builder.setStatus(OnlineStatus.ONLINE)
                        .enableIntents(GatewayIntent.GUILD_MEMBERS)
                        .addEventListeners(waiter, client.build(), new LogListener(), new MemberEventListener(), new BotEventListener())
                        .setAutoReconnect(true)
                        .setSessionController(controller)
                        .setShardsTotal(-1)
                        .setContextEnabled(true)
                        .setEnableShutdownHook(true);
                if (x86) builder.setAudioSendFactory(new NativeAudioSendFactory());
                if (cfg.isShopEnabled()) builder.addEventListeners(new ShopTokenListener());

                builder.build();

            } else {

                JDABuilder builder = JDABuilder.createDefault(cfg.getBotToken());
                builder.setStatus(OnlineStatus.ONLINE)
                        .enableIntents(GatewayIntent.GUILD_MEMBERS)
                        .addEventListeners(waiter, client.build(), new LogListener(), new MemberEventListener(), new BotEventListener(), new TwitterListener())
                        .setAutoReconnect(true)
                        .setChunkingFilter(ChunkingFilter.ALL)
                        .setEnableShutdownHook(true);
                if (x86) builder.setAudioSendFactory(new NativeAudioSendFactory());
                if (cfg.isShopEnabled()) builder.addEventListeners(new ShopTokenListener());

                builder.build();
            }
        } catch (IllegalArgumentException e) {
            LOG.error("Error occurred while starting: ", e);
            LOG.error("Please check if your discord bot token is in the configuration file, as this is most common cause of this error.");
            System.exit(101);
        } catch (LoginException e) {
            LOG.error("Stored token was rejected!", e);
            LOG.error("Please double-check your token.");
            System.exit(102);
        }
    }
}