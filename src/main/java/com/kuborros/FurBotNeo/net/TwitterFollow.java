package com.kuborros.FurBotNeo.net;

public class TwitterFollow {

    private final String handle;
    private final String guildId;
    private final String channelId;
    private final Long lastId;
    public TwitterFollow(String handle, String guildId, String channelId, Long lastId ) {
       this.handle = handle;
       this.guildId = guildId;
       this.channelId = channelId;
       this.lastId = lastId;
    }

    public String getHandle() {
        return handle;
    }

    public String getGuildId() {
        return guildId;
    }

    public String getChannelId() {
        return channelId;
    }

    public Long getLastId() {
        return lastId;
    }
}
