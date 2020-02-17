package com.sap.s4idea.rea.integration.twitter;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class TwitterController {
    private static final Logger LOG = LoggerFactory.getLogger(TwitterController.class);
    private static final String BACKEND_URL = "https://reqanalysis.cfapps.sap.hana.ondemand.com/twitter";
    private static final String TWITTER_CONSUMER_KEY = "hp1o6gCNcQgFRI4u0SQO8auG2";
    private static final String TWITTER_SECRET_KEY = "cOs6j2nQ6VNnqcfXrkN2NRXvK8rJr6r7ZeaptLFcMFKJHOOshW";
    private static final String TWITTER_ACCESS_TOKEN = "990935749-bXdjTmCjve3bEyDCwEyLEWTGEkELV59X2RfMGKY7";
    private static final String TWITTER_ACCESS_TOKEN_SECRET = "d4ltDlGiFVtJWki1BFnYLKbigjkVCxzuTIHO3shiHBQit";
    private static final String SEARCH_TAG_1 = "#IPD4ML";
    private static Statement statement;
    private static CloseableHttpClient httpClient = HttpClients.createDefault();

    @Autowired
    private DataSource dataSource;

    @PostConstruct
    public void init() throws Exception {
        Connection connection = dataSource.getConnection();
        statement = connection.createStatement();
    }

    @Scheduled(fixedRate = 159000)
//    @RequestMapping(value = "/twitter", method = RequestMethod.GET)
    public void postTwitter() throws Exception {
        HttpPost httpPost = new HttpPost(BACKEND_URL);
        Map<Timestamp, String> resultMap = getTweets();
        if (resultMap != null) {
            Timestamp lastestDate = (Timestamp) resultMap.keySet().toArray()[0];
            String jsonString = resultMap.get(lastestDate);
            StringEntity params = new StringEntity(jsonString);
            httpPost.setEntity(params);
            try {
                CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
                if (httpResponse.getStatusLine().getStatusCode() == 200) {
                    if (!(updateLastestTwitterDate(lastestDate) > 0)) {
                        throw new Exception("Update post text lastest date failed.");
                    }
                }
            } catch (Exception e) {
                LOG.info("Post to Java Backend Failed.");
                e.printStackTrace();
            }
            LOG.info("Lastest date is " + lastestDate.toString());
        }
    }

    private Map<Timestamp, String> getTweets() throws Exception {
        List<Status> tweetList = searchTweets();
        if (tweetList.size() > 0) {
            JSONObject jsonObject = new JSONObject();
            JSONArray message = new JSONArray();
            Timestamp lastestDate = getLastestTwitterDate();
            Timestamp twitterTime = null;
            for (Status tweet : tweetList) {
                if (!StringUtils.isEmpty(tweet.getText()) && tweet.getCreatedAt().getTime() > lastestDate.getTime()) {
                    JSONObject content = new JSONObject();
                    content.put("content", removeBadChars(tweet.getText().replace("\n", "")));
                    message.put(content);
                    if (twitterTime == null || tweet.getCreatedAt().getTime() > twitterTime.getTime()) {
                        twitterTime = new Timestamp(tweet.getCreatedAt().getTime());
                    }
                }
            }
            if (twitterTime != null && twitterTime.getTime() > lastestDate.getTime()) {
                lastestDate = twitterTime;
            }
            jsonObject.put("message", message);
            LOG.info("Twitter Content:" + jsonObject.toString());
            LOG.info("Twitter Lastest Date:" + lastestDate);
            Map<Timestamp, String> map = new HashMap<>();
            map.put(lastestDate, jsonObject.toString());
            return map;
        }
        return null;
    }

    private List<Status> searchTweets() throws Exception {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setDebugEnabled(true).setOAuthConsumerKey(TWITTER_CONSUMER_KEY).setOAuthConsumerSecret(TWITTER_SECRET_KEY)
                .setOAuthAccessToken(TWITTER_ACCESS_TOKEN).setOAuthAccessTokenSecret(TWITTER_ACCESS_TOKEN_SECRET);
        TwitterFactory twitterFactory = new TwitterFactory(builder.build());
        Twitter twitter = twitterFactory.getInstance();
        Query query = new Query(SEARCH_TAG_1);
        QueryResult result = twitter.search(query);
        return new ArrayList<>(result.getTweets());
    }

    private int updateLastestTwitterDate(Timestamp newDate) {
        String sql = "update integration.twitter set post_text = '" + newDate.toString() + "'";
        LOG.info("Update lastest date SQL String is " + sql);
        try {
            return statement.executeUpdate(sql);
        } catch (SQLException e) {
            LOG.info("Update lastest date to integration.twitter failed.");
            e.printStackTrace();
        }
        return -1;
    }

    private Timestamp getLastestTwitterDate() {
        String sql = "select post_text as lastestDate from integration.twitter";
        LOG.info("Get lastest date SQL String is " + sql);
        Timestamp timestamp = null;
        try {
            ResultSet resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                timestamp = resultSet.getTimestamp("lastestDate");
            }
        } catch (SQLException e) {
            LOG.info("Get lastest date from integration.twitter failed.");
            e.printStackTrace();
        }
        return timestamp;
    }

    private String removeBadChars(String s) {
        if (s == null) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (Character.isHighSurrogate(s.charAt(i))) continue;
            sb.append(s.charAt(i));
        }
        return sb.toString();
    }
}
