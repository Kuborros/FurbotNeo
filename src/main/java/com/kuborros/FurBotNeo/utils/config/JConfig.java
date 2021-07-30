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

package com.kuborros.FurBotNeo.utils.config;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Optional;

public class JConfig {

    private static final int confVersion = 4;
    private static final Logger LOG = LoggerFactory.getLogger(JConfig.class);
    private static final File CONFILE = new File("config.json");
    private String botToken, ownerId;
    private int skipPercent,tweetCount;
    private boolean debugMode, invidioEnabled, shardingEnabled, shopEnabled, buyVipEnabled, legacySkipAudio,twitterEnabled;
    private JSONArray bannedGuilds = new JSONArray();

    public JConfig() {
        Optional<JSONObject> configOpt = Optional.ofNullable(loadOrCreateConfig());
        if (configOpt.isEmpty()) {
            //We assume file just got created, or we failed at creating/reading it. As such we heve no way of obtaining token, so we should just terminate right here.
            LOG.error("Unable to obtain token - the configuration file is not filled or broken. \n" +
                    "If file was generated in this run, please fill in the token and owner id! \n" +
                    "Shutting down, since there's not much we can do.");
            System.exit(255);
        } else {
            JSONObject config = configOpt.get();
            //Setting version to -1 is an (outside of here) undocumented switch to enable debug mode - several sanity checks are ignored and store lets you do anything.
            debugMode = (config.getInt("version") == -1);
            if (config.getInt("version") < confVersion) {
                if (debugMode) LOG.warn("Debug mode engaged!");
                else
                    LOG.error("Your configuration file is outdated! I can still use it, but i recommend recreating it, as you might miss out on some new cool features!");
            }

            //These always should exist, no matter the file version.
            botToken = config.getString("bot_token");
            ownerId = config.getString("owner_id");

            //Make sure they are set to something that makes sense
            if (botToken.equals("0")) {
                //Fatal, also means the configuration file was likely not filled in
                LOG.error("Unable to obtain token - the configuration file is not filled in or broken. Shutting down, since there's not much we can do.");
                System.exit(255);
            }
            if (ownerId.equals("0")) {
                //Non-fatal, we can continue but at degraded functionality
                LOG.warn("Owner id in configuration set to 0! That means nobody can use owner-specific features! Functionality will be degraded when option is not set.");
            }

            //Remaining options are optional, and do not need to exist in config file to work - they will use default values if not present.

            Optional<JSONObject> bools = Optional.ofNullable(config.optJSONObject("config_options"));
            if (bools.isPresent()) {
                //Returns "false" if key not found. This way more options can be added later, and if missing, will default to false.
                //Version 1 booleans:
                invidioEnabled = bools.get().optBoolean("invidio");
                shardingEnabled = bools.get().optBoolean("shard");
                //Version 2 booleans:
                shopEnabled = bools.get().optBoolean("shop");
                buyVipEnabled = bools.get().optBoolean("buy_vip");
                //Version 3 booleans:
                legacySkipAudio = bools.get().optBoolean("audio_all_can_skip");
                //Version 4 booleans:
                twitterEnabled = bools.get().optBoolean("tweet_enabled");

            }
            //Version 3 ints:
            skipPercent = config.optInt("audio_skip_percent");

            //Version 4 ints:
            tweetCount = config.optInt("tweet_count");

            //Default int values
            if (skipPercent == 0) skipPercent = 75;
            if (tweetCount == 0) tweetCount = 50;

            //Version 1 arrays:
            Optional<JSONArray> banned = Optional.ofNullable(config.optJSONArray("blacklist_servers"));
            banned.ifPresent(objects -> bannedGuilds = objects);

        }
    }

    public String getBotToken() {
        return botToken;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public boolean isInvidioEnabled() {
        return invidioEnabled;
    }

    public boolean isShardingEnabled() {
        return shardingEnabled;
    }

    public boolean isShopEnabled() {
        return shopEnabled;
    }

    public void setShopEnabled(boolean enabled) {
        this.shopEnabled = enabled;
    }

    public boolean isBuyVipEnabled() {
        return buyVipEnabled;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public boolean isLegacySkipAudio() {
        return legacySkipAudio;
    }

    public int getSkipPercent() {
        return skipPercent;
    }

    public JSONArray getBannedGuilds() {
        return bannedGuilds;
    }

    public int getTweetCount() {
        return tweetCount;
    }

    public boolean isTwitterEnabled() {
        return twitterEnabled;
    }

    @Nullable
    private JSONObject loadOrCreateConfig() {
        if (CONFILE.canRead()) {
            try {
                String content = new String(Files.readAllBytes(CONFILE.toPath()));
                return new JSONObject(content);
            } catch (IOException e) {
                LOG.error("Loading configuration failed: ", e);
                return null;
            }
        } else {
            LOG.info("Generating default configuration file...");
            try (InputStream def = getClass().getClassLoader().getResourceAsStream("defaultConfig.json")) {
                assert def != null;
                FileUtils.copyInputStreamToFile(def, CONFILE);
                LOG.info("You need to populate this file with your bot token and you userID!");
            } catch (IOException e) {
                LOG.error("... and failed! ", e);
            }
            return null;
        }
    }

}
