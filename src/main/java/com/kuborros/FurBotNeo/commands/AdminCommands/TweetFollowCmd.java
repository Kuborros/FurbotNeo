package com.kuborros.FurBotNeo.commands.AdminCommands;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.doc.standard.CommandInfo;
import com.jagrosh.jdautilities.examples.doc.Author;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;
import twitter4j.*;

import static com.kuborros.FurBotNeo.BotMain.db;

    @CommandInfo(
            name = "Follow Tweets",
            description = "Follows the twitter user on the selected channel."
    )
    @Author("Kuborros")
    public class TweetFollowCmd extends AdminCommand {

        public TweetFollowCmd() {
            this.name = "ftweet";
            this.help = "Reposts tweets from user on given channel. Updates every hour.";
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
            String guild = event.getGuild().getId();

            Twitter twitter = TwitterFactory.getSingleton();
            Query query = new Query("from:" + handle + " -filter:replies -filter:retweets").count(1).sinceId(0).resultType(Query.ResultType.recent);
            long id;
            try {
                QueryResult result = twitter.search(query);
                if (result.getTweets().isEmpty()) {
                    event.replyWarning("Unable to find tweets for this account!");
                    return;
                }
                Status status = result.getTweets().get(0);
                id = status.getId()-1;
                if (db.addTwitterFollow(handle,channelId,guild,id)) {
                    event.reply(sendGenericEmbed("Account followed successfully!","We are now following " + handle + " in " + channel.getName(),"✅"));
                    event.reply("https://twitter.com/" + status.getUser().getScreenName() + "/status/" + status.getId());
                } else {
                    event.replyError("Things broke while adding follow to db.");
                }
            } catch (Exception e) {
                errorResponseEmbed(e);
            }
        }
    }
