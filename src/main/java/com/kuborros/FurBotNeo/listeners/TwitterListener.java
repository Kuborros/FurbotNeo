package com.kuborros.FurBotNeo.listeners;

import com.kuborros.FurBotNeo.net.TwitterFollow;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.*;

import javax.annotation.Nonnull;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import static com.kuborros.FurBotNeo.BotMain.db;

public class TwitterListener extends ListenerAdapter {

        private static final Logger LOG = LoggerFactory.getLogger("TwitterPoster");

        @Override
        public void onReady(@Nonnull ReadyEvent event) {

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    LinkedList <TwitterFollow> follows = db.getTwitterSubs();
                    if (follows.isEmpty()) return;
                    Twitter twitter = TwitterFactory.getSingleton();
                    follows.forEach(twitterFollow -> {
                        Query query = new Query("from:" + twitterFollow.getHandle() + " -filter:replies -filter:retweets").count(10).sinceId(twitterFollow.getLastId()).resultType(Query.ResultType.recent);
                        LOG.info("Twitter lookup for " + twitterFollow.getHandle());
                        Long id = twitterFollow.getLastId();
                        TextChannel channel = Objects.requireNonNull(event.getJDA().getGuildById(twitterFollow.getGuildId())).getTextChannelById(twitterFollow.getChannelId());
                        try {
                            QueryResult result = twitter.search(query);
                            LOG.info("Found " + result.getTweets().size() + " results.");
                            for (Status status : result.getTweets()) {
                                if (id < status.getId()) id = status.getId();
                                String url = "https://twitter.com/" + status.getUser().getScreenName() + "/status/" + status.getId();
                                Objects.requireNonNull(channel).sendMessage(url).complete();
                                LOG.info("@" + status.getUser().getScreenName() + ":" + status.getText() + " id=" + status.getId());
                            }
                            db.setLastTweet(twitterFollow.getHandle(),twitterFollow.getChannelId(),id);
                        } catch (Exception e) {
                            LOG.error("Exception has happened:",e);
                        }
                    });
                }
            }, 1000, 3600000);
    }
}
