package com.sap.s4idea.rea.config;

import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class TwitterController {
    private static final Path PROCESSED_TWITTER_PATH = Paths.get("processedTwitter.txt");

    private static final String TWITTER_CONSUMER_KEY = "hp1o6gCNcQgFRI4u0SQO8auG2";
    private static final String TWITTER_SECRET_KEY = "cOs6j2nQ6VNnqcfXrkN2NRXvK8rJr6r7ZeaptLFcMFKJHOOshW";
    private static final String TWITTER_ACCESS_TOKEN = "990935749-bXdjTmCjve3bEyDCwEyLEWTGEkELV59X2RfMGKY7";
    private static final String TWITTER_ACCESS_TOKEN_SECRET = "d4ltDlGiFVtJWki1BFnYLKbigjkVCxzuTIHO3shiHBQit";
//    private static final String SEARCH_TAG_1 = "#Tesla";
//    private static final String SEARCH_TAG_2 = "#Byton";
    private static final String SEARCH_TAG_3 = "#IPD4ML";


    public String getTweets() throws Exception {
        if (!Files.exists(PROCESSED_TWITTER_PATH)) {
            Files.createFile(PROCESSED_TWITTER_PATH);
        }

        List<String> processedTwitterID = Files.readAllLines(PROCESSED_TWITTER_PATH, Charset.defaultCharset());
        List<Status> tweetList = searchTweets();

        JSONObject jsonObject = new JSONObject();
        JSONArray message = new JSONArray();

        for (Status tweet : tweetList) {
            String twitterID = String.valueOf(tweet.getId());
            if (!processedTwitterID.contains(twitterID)) {
                JSONObject content = new JSONObject();
                content.put("content", removeBadChars(tweet.getText().replace("\n", "")));
                message.put(content);
                Files.write(PROCESSED_TWITTER_PATH, (String.valueOf(tweet.getId()) + System.lineSeparator()).getBytes(Charset.defaultCharset()), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        }
        jsonObject.put("message", message);
        return jsonObject.toString();
    }

    private List<Status> searchTweets() throws Exception {
        List<Status> statusList = new ArrayList<>();

        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setDebugEnabled(true).setOAuthConsumerKey(TWITTER_CONSUMER_KEY).setOAuthConsumerSecret(TWITTER_SECRET_KEY)
                .setOAuthAccessToken(TWITTER_ACCESS_TOKEN).setOAuthAccessTokenSecret(TWITTER_ACCESS_TOKEN_SECRET);
        TwitterFactory twitterFactory = new TwitterFactory(builder.build());
        Twitter twitter = twitterFactory.getInstance();
//        Query query1 = new Query(SEARCH_TAG_1);
//        QueryResult result1 = twitter.search(query1);
//        if (result1.getTweets().size() > 10) {
//            statusList.addAll(result1.getTweets().subList(0, 10));
//        } else {
//            statusList.addAll(result1.getTweets());
//        }
//        Query query2 = new Query(SEARCH_TAG_2);
//        QueryResult result2 = twitter.search(query2);
//        if (result2.getTweets().size() > 10) {
//            statusList.addAll(result2.getTweets().subList(0, 10));
//        } else {
//            statusList.addAll(result2.getTweets());
//        }
        Query query3 = new Query(SEARCH_TAG_3);
        QueryResult result3 = twitter.search(query3);
        if (result3.getTweets().size() > 10) {
            statusList.addAll(result3.getTweets().subList(0, 10));
        } else {
            statusList.addAll(result3.getTweets());
        }
        return statusList;
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
