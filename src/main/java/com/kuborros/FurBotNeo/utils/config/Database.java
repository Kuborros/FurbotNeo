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

import com.kuborros.FurBotNeo.net.TwitterFollow;
import com.kuborros.FurBotNeo.utils.store.MemberInventory;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.utils.cache.SnowflakeCacheView;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

public class Database {

    private static final Logger LOG = LoggerFactory.getLogger(Database.class);

    private static final String DRIVER = "org.sqlite.JDBC";
    private static final String DB = "jdbc:sqlite:database.db";

    private Connection conn;
    private Statement stat;

    private final Map<String, Boolean> needsUpdate = new HashMap<>();


    public Database() {
        try {
            Class.forName(Database.DRIVER);
        } catch (ClassNotFoundException e) {
            LOG.error("No JDBC driver detected! ", e);
        }

        try {
            conn = DriverManager.getConnection(DB);
            stat = conn.createStatement();
        } catch (SQLException e) {
            LOG.error("Database connection error occurred! ", e);
        }
    }

    void close() {
        try {
            stat.close();
            conn.commit();
            conn.close();
        } catch (SQLException ignored) {
        }
    }

    public void createTables() {

        try {
            stat = conn.createStatement();

            String guild = "CREATE TABLE IF NOT EXISTS Guilds " +

                    "(guild_id TEXT UNIQUE PRIMARY KEY NOT NULL, " +

                    " music_id TEXT NOT NULL, " +

                    " name TEXT NOT NULL, " +

                    " members INTEGER, " +

                    " bot_name TEXT NOT NULL, " +

                    " bot_prefix TEXT NOT NULL, " +

                    " isNSFW BOOLEAN DEFAULT FALSE, " +

                    " isFurry BOOLEAN DEFAULT TRUE, " +

                    " welcomeMsg BOOLEAN DEFAULT FALSE)";

            stat.executeUpdate(guild);

            String shop = "CREATE TABLE IF NOT EXISTS Shop " +

                    "(id INTEGER PRIMARY KEY AUTOINCREMENT," +

                    " member_id TEXT NOT NULL," +

                    " guild_id TEXT NOT NULL, " +

                    " balance INTEGER DEFAULT 0, " +

                    " level INTEGER DEFAULT 0, " +

                    " role_owned TEXT DEFAULT 'default'," +

                    " currRole TEXT DEFAULT 'default'," +

                    " isVIP BOOLEAN DEFAULT FALSE," +

                    " isBanned BOOLEAN DEFAULT FALSE) ";

            stat.executeUpdate(shop);

            String count = "CREATE TABLE IF NOT EXISTS CommandStats " +

                    "(user_id TEXT UNIQUE PRIMARY KEY NOT NULL) ";

            stat.executeUpdate(count);

            String tweet = "CREATE TABLE IF NOT EXISTS TwitterWatch " +

                    "(id INTEGER UNIQUE PRIMARY KEY AUTOINCREMENT, " +

                    " t_handle TEXT NOT NULL, " +

                    " guild_id TEXT NOT NULL, " +

                    " channel_id TEXT NOT NULL, " +

                    " last_id INTEGER DEFAULT 0) ";

            stat.executeUpdate(tweet);

        } catch (SQLException e) {
            LOG.error("Failure while creating database tables: ", e);
        }
    }

    public void setGuilds(JDA jda) {
        SnowflakeCacheView<Guild> guilds;
        if (jda.getShardManager() != null) {
            guilds = jda.getShardManager().getGuildCache();
        } else {
            guilds = jda.getGuildCache();
        }
        if (guilds.isEmpty()) return;
        try {
            for (Guild guild : guilds) {
                addGuildToDb(guild);
            }
        } catch (SQLException e) {
            LOG.error("Failure while adding guilds database: ", e);
        }

    }

    public void setGuild(Guild guild) {
        try {
            addGuildToDb(guild);
        } catch (SQLException e) {
            LOG.error("Failure while adding guild to database: ", e);
        }
    }

    private void addGuildToDb(@NotNull Guild guild) throws SQLException {

        needsUpdate.put(guild.getId(), false);

        String sql = "INSERT OR IGNORE INTO Guilds(guild_id,music_id,name,members,bot_name,bot_prefix,isNSFW,isFurry,welcomeMsg) VALUES(?,?,?,?,?,?,?,?,?)";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, guild.getId());
        pstmt.setString(2, findBotChat(guild).getId());
        pstmt.setString(3, guild.getName());
        pstmt.setInt(4, guild.getMembers().size());
        pstmt.setString(5, guild.getJDA().getSelfUser().getName());
        pstmt.setString(6, "!");
        pstmt.setBoolean(7, false);
        pstmt.setBoolean(8, true);
        pstmt.setBoolean(9, false);
        pstmt.executeUpdate();
    }

    private TextChannel findBotChat(Guild guild) {
        List<TextChannel> channels = guild.getTextChannels();
        for (TextChannel channel : channels) {
            if (channel.getName().contains("bot"))
                return channel;
        }
        return guild.getDefaultChannel();
    }

    public boolean updateGuildBotName(String name, Guild guild) {
        try {
            String sql = "UPDATE Guilds SET bot_name = ? WHERE guild_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, name);
            pstmt.setString(2, guild.getId());
            pstmt.executeUpdate();
            needsUpdate.put(guild.getId(), true);
            guild.getSelfMember().modifyNickname(name).complete();
            return true;
        } catch (SQLException e) {
            LOG.error("Unable to update per-guild configuration: ", e);
            return false;
        }
    }

    public boolean updateGuildPrefix(String prefix, Guild guild) {
        try {
            String sql = "UPDATE Guilds SET bot_prefix = ? WHERE guild_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, prefix);
            pstmt.setString(2, guild.getId());
            pstmt.executeUpdate();
            needsUpdate.put(guild.getId(), true);
            return true;
        } catch (SQLException e) {
            LOG.error("Unable to update per-guild configuration: ", e);
            return false;
        }
    }

    public boolean updateGuildAudio(String audio, Guild guild) {
        try {
            String sql = "UPDATE Guilds SET music_id = ? WHERE guild_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, audio);
            pstmt.setString(2, guild.getId());
            pstmt.executeUpdate();
            needsUpdate.put(guild.getId(), true);
            return true;
        } catch (SQLException e) {
            LOG.error("Unable to update per-guild configuration: ", e);
            return false;
        }
    }

    public boolean updateGuildIsNSFW(boolean gai, Guild guild) {
        try {
            String nsfw = gai ? "1" : "0";
            String sql = "UPDATE Guilds SET isNSFW = ? WHERE guild_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, nsfw);
            pstmt.setString(2, guild.getId());
            pstmt.executeUpdate();
            needsUpdate.put(guild.getId(), true);
            return true;
        } catch (SQLException e) {
            LOG.error("Unable to update per-guild configuration: ", e);
            return false;
        }
    }

    public boolean updateGuildWelcomeMsg(boolean hai, Guild guild) {
        try {
            String welcome = hai ? "1" : "0";
            String sql = "UPDATE Guilds SET welcomeMsg = ? WHERE guild_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, welcome);
            pstmt.setString(2, guild.getId());
            pstmt.executeUpdate();
            needsUpdate.put(guild.getId(), true);
            return true;
        } catch (SQLException e) {
            LOG.error("Unable to update per-guild configuration: ", e);
            return false;
        }
    }

    public boolean updateGuildIsFurry(boolean furfags, Guild guild) {
        try {
            String furry = furfags ? "1" : "0";
            String sql = "UPDATE Guilds SET isNSFW = ? WHERE guild_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, furry);
            pstmt.setString(2, guild.getId());
            pstmt.executeUpdate();
            needsUpdate.put(guild.getId(), true);
            return true;
        } catch (SQLException e) {
            LOG.error("Unable to update per-guild configuration: ", e);
            return false;
        }
    }

    public void updateGuildMembers(GuildMemberJoinEvent event) {
        updateGuildMembers(event.getGuild());
    }

    public void updateGuildMembers(GuildMemberRemoveEvent event) {
        updateGuildMembers(event.getGuild());
    }

    private void updateGuildMembers(Guild guild) {
        int members = guild.getMembers().size();
        try {
            String sql = "UPDATE Guilds SET members= ? WHERE guild_id= ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, members);
            pstmt.setString(2, guild.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOG.error("Failure while updating member counts: ", e);
        }
    }

    boolean guildNeedsUpdate(Guild guild) {
        return needsUpdate.get(guild.getId());
    }

    public void setCommandStats(JDA jda) {
        List<User> users = jda.getUsers();
        try {
            stat = conn.createStatement();
            for (User user : users) {
                stat.addBatch("INSERT OR IGNORE INTO CommandStats (user_id) VALUES (" + user.getId() + ")");
            }
            stat.executeBatch();
        } catch (SQLException e) {
            LOG.debug("Possibly harmless exception:", e);
        }
    }

    public void registerCommand(String command) {
        try {
            stat = conn.createStatement();
            stat.executeUpdate("ALTER TABLE CommandStats ADD COLUMN " + command + " INTEGER DEFAULT 0");
        } catch (SQLException e) {
            LOG.debug("Possibly harmless exception:", e);
        }
    }

    public void updateCommandStats(String memberID, String command) {
        try {
            stat = conn.createStatement();
            stat.executeUpdate("UPDATE CommandStats SET " + command + "=" + command + " + 1 WHERE user_id=" + memberID);
        } catch (SQLException e) {
            LOG.debug("Possibly harmless exception:", e);
        }
    }

    public Map<String, String> getCommandStats(String memberID) throws SQLException{

        Map<String, String> map = new HashMap<>();
        int counter;
        stat = conn.createStatement();
        ResultSet rs = stat.executeQuery("SELECT * FROM CommandStats WHERE user_id=" + memberID);
        ResultSetMetaData rsmd = rs.getMetaData();
        int columnCount = rsmd.getColumnCount();
        List<String> names = new ArrayList<>(10);
        for (int i = 1; i <= columnCount; i++ ) {
            names.add(rsmd.getColumnName(i));
        }
        names.remove(0);
        while (rs.next()) {
            for (String name : names) {
                counter = rs.getInt(name);
                map.put(name, Integer.toString(counter));
            }
        }
        return map;

    }

    //Same member can exist in multiple guilds, and for each needs separate set of store tables
    //Member id and guild id are kept separate for reasons of clarity. If it becomes problematic they can be turned into single unique value
    //Returns true if created, false if already exists.
    private boolean addMemberToStore(String memberId, String guildID) {
        try {
            stat = conn.createStatement();
            stat.executeUpdate("INSERT INTO Shop (member_id, guild_id) VALUES (" + memberId + "," + guildID + ")");
            return true;
        } catch (SQLException e) {
            LOG.debug("User likely already exists: ", e);
            return false;
        }
    }

    //Get whole inventory
    public MemberInventory memberGetInventory(String memberId, String guildId) {
        try {
            ArrayList<String> roles = new ArrayList<>();
            stat = conn.createStatement();
            ResultSet rs = stat.executeQuery("SELECT * FROM Shop WHERE member_id=" + memberId + " AND guild_id=" + guildId);
            Collections.addAll(roles, rs.getString(6).split(","));
            return new MemberInventory(memberId, guildId, rs.getInt(4), roles, rs.getString(7), rs.getBoolean(8), rs.getBoolean(9));
        } catch (SQLException | NullPointerException e) {
            if (addMemberToStore(memberId, guildId)) {
                //They might just not exist in store database!
                LOG.debug("Added user to store with member id: {}, and guild id: {}", memberId, guildId);
            } else {
                //If they exist and we messed up print the stacktrace
                LOG.error("Exception while obtaining user inventory:", e);
            }
            return new MemberInventory(memberId, guildId);
        }
    }

    //Set whole inventory at once
    public void memberSetInventory(MemberInventory inventory) {
        String roles;
        StringBuilder builder = new StringBuilder();
        if (inventory.getOwnedRoles().isEmpty()) {
            roles = "";
        } else {
            inventory.getOwnedRoles().forEach(item -> builder.append(item).append(","));
            roles = builder.toString();
            builder.delete(0, builder.length());
        }

        try {
            stat = conn.createStatement();
            String sql = "UPDATE Shop SET (balance,level,role_owned,currRole,isBanned) = (?,?,?,?,?)" +
                    "WHERE member_id= ? AND guild_id= ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, inventory.getBalance());
            pstmt.setInt(2, 100);
            pstmt.setString(3, roles);
            pstmt.setString(4, inventory.getCurrentRole());
            pstmt.setBoolean(5, inventory.isBanned());
            pstmt.setString(6, inventory.getMemberId());
            pstmt.setString(7, inventory.getGuildId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOG.error("Exception while writing user inventory:", e);
        }
    }

    FurConfig getGuildConfig(Guild guild) throws SQLException {
        stat = conn.createStatement();
        ResultSet rs = stat.executeQuery("SELECT * FROM Guilds WHERE guild_id=" + guild.getId());
        if (!rs.isClosed()) {
            return new FurConfig(rs.getString(5), rs.getBoolean(9), rs.getBoolean(8), rs.getBoolean(7), rs.getString(6), rs.getString(2));
        } else {
            //Try to re-add the guild.
            setGuild(guild);
            return getGuildConfig(guild);
        }
    }

    public LinkedList <TwitterFollow> getTwitterSubs() {
        LinkedList<TwitterFollow> follows = new LinkedList<>();
        try {
            stat = conn.createStatement();
            ResultSet rs = stat.executeQuery("SELECT * FROM TwitterWatch");
            while (rs.next()) {
                follows.add(new TwitterFollow(rs.getString(2),rs.getString(3),rs.getString(4),rs.getLong(5)));
            }
        } catch (Exception e) {
            LOG.error("",e);
        }
        return follows;
    }

    public void setLastTweet(String handle, String channel, Long id) {
        try {
            stat = conn.createStatement();
            stat.executeUpdate("UPDATE TwitterWatch SET last_id= '" + id + "' WHERE t_handle= '" + handle + "' AND channel_id= '" + channel + "'");
        } catch (Exception e) {
            LOG.error("",e);
        }
    }

    public boolean addTwitterFollow(String handle, String channelId, String guildId, long id) {
        try {
            stat = conn.createStatement();
            stat.executeUpdate("INSERT INTO TwitterWatch (t_handle, channel_id, guild_id, last_id) VALUES ('" + handle + "','" + channelId + "','" + guildId + "'," + id + ")");
            return true;
        } catch (SQLException e) {
            LOG.debug("Follow likely already exists: ", e);
            return false;
        }
    }

    public boolean removeTwitterFollow(String handle, String channelId, String guildID) {
        try {
            stat = conn.createStatement();
            stat.executeUpdate("DELETE FROM TwitterFollow WHERE t_handle=" + handle + " AND channel_id=" + channelId);
            return true;
        } catch (SQLException e) {
            LOG.debug("Failed to remove follow: ", e);
            return false;
        }
    }
}
