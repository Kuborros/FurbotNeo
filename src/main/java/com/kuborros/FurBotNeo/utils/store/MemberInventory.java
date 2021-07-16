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

package com.kuborros.FurBotNeo.utils.store;

import java.util.ArrayList;

import static com.kuborros.FurBotNeo.BotMain.cfg;
import static com.kuborros.FurBotNeo.BotMain.db;

public class MemberInventory {

    final String memberId;
    final String guildId;
    final String uId;
    final ArrayList<String> ownedRoles;
    int balance;
    final boolean VIP;
    boolean banned;
    String currentRole;

    //Initialises empty inventory
    public MemberInventory(String memberId, String guildId) {
        this.memberId = memberId;
        this.guildId = guildId;
        this.uId = memberId + "," + guildId;
        this.banned = false;
        this.ownedRoles = new ArrayList<>();
        this.currentRole = "";
        if (cfg.isDebugMode()) {
            //If in debug mode, give all new users unholy amount of tokens for testing
            this.balance = Integer.MAX_VALUE / 2;
            this.VIP = true;
        } else {
            this.balance = 0;
            //If buying vip is disabled, we make everyone vip to not lock features out
            this.VIP = !cfg.isBuyVipEnabled();
        }
    }

    //Initialise inventory with existing data
    public MemberInventory(String memberId, String guildId, int balance, ArrayList<String> ownedRoles, String currRole, boolean vip, boolean banned) {
        this.memberId = memberId;
        this.guildId = guildId;
        this.uId = memberId + "," + guildId;
        this.balance = balance;
        this.ownedRoles = ownedRoles;
        this.currentRole = currRole;
        this.banned = banned;
        if (cfg.isBuyVipEnabled()) this.VIP = vip;
        else this.VIP = true;
    }

    public void sync() {
        db.memberSetInventory(this);
    }

    //All add/remove methods return MemberInventory for easy chaining
    public MemberInventory addToRoles(String item) {
        if (!ownedRoles.contains(item)) ownedRoles.add(item);
        return this;
    }

    public MemberInventory addTokens(int amount) {
        balance += amount;
        return this;
    }

    public MemberInventory spendTokens(int amount) {
        balance -= amount;
        return this;
    }

    public MemberInventory setBotBan(boolean beaned) {
        this.banned = beaned;
        return this;
    }

    public MemberInventory setCurrentRole(String role) {
        this.currentRole = role;
        return this;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getGuildId() {
        return guildId;
    }

    public String getCurrentRole() {
        return currentRole;
    }

    public ArrayList<String> getOwnedRoles() {
        return ownedRoles;
    }

    public int getBalance() {
        return balance;
    }

    public boolean isBanned() {
        //Owner is never banned, even if db says so, to avoid permament lockout
        if (memberId.equals(cfg.getOwnerId())) return false;
        return banned;
    }
}
