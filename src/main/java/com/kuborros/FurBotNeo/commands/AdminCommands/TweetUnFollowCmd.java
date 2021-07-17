package com.kuborros.FurBotNeo.commands.AdminCommands;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.doc.standard.CommandInfo;
import com.jagrosh.jdautilities.examples.doc.Author;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;

import static com.kuborros.FurBotNeo.BotMain.db;

@CommandInfo(
        name = "UnFollow Tweets",
        description = "Follows the twitter user on the selected channel."
)
@Author("Kuborros")
public class TweetUnFollowCmd extends AdminCommand {

    public TweetUnFollowCmd() {
        this.name = "untweet";
        this.help = "Removes tweet reposts for given handle";
        this.guildOnly = true;
        this.ownerCommand = false;
        this.category = new Category("Moderation");
        this.userPermissions = new Permission[]{Permission.BAN_MEMBERS};
    }

    @Override
    protected void doCommand(CommandEvent event) {

        if (event.getArgs().isEmpty()) {
            event.replyWarning("Please provide the twitter handle~.");
            return;
        }

        TextChannel channel;
        if (event.getMessage().getMentionedChannels().isEmpty()) {
            channel = event.getTextChannel();
        } else channel = event.getMessage().getMentionedChannels().get(0);
        String channelId = channel.getId();
        String handle = event.getArgs().split(" ")[0];

        if (db.removeTwitterFollow(handle,channelId)){
            event.replySuccess("Unfollowed " + handle);
        } else event.replyError("Failed to unfollow " + handle + "! Were they followed in the first place?");

    }
}
