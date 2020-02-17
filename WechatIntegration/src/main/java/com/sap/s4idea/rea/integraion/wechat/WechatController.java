package com.sap.s4idea.rea.integraion.wechat;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

@RestController
public class WechatController {
    private static final Logger LOG = LoggerFactory.getLogger(WechatController.class);
    private static final String WECHAT_URL = "https://445824471.feedbacktorequirement.xyz/";
    private static final String BACKEND_URL = "https://reqanalysis.cfapps.sap.hana.ondemand.com/wechat";
    private static final String POST_TEXT = "post/myList?id=";
    private static final String COMMENT_TEXT = "comment/myList?id=";
    private static final String POST_COLUMN = "post_text";
    private static final String COMMENT_COLUMN = "comment_text";
    private static CloseableHttpClient getHttpClient = HttpClients.createDefault();
    private static CloseableHttpClient postHttpClient = HttpClients.createDefault();
    private static Statement statement;

    @Autowired
    private DataSource dataSource;

    @PostConstruct
    public void init() throws Exception {
        Connection connection = dataSource.getConnection();
        statement = connection.createStatement();
    }

    //    @RequestMapping(value = "/wechat", method = RequestMethod.GET)
    @Scheduled(fixedRate = 15000)
    public void postWechat() throws Exception {
        int postTextProID = getProcessedIDFromDB(POST_COLUMN);
        LOG.info("Post text process ID is " + postTextProID);
        Map<Integer, String> postTextMap = getPostTextFromWechat(postTextProID);
        if (postTextMap != null) {
            int newPostTextProID = (int) postTextMap.keySet().toArray()[0];
            String postText = postTextMap.get(newPostTextProID);
            if (executeHttpPost(postText) == 200) {
                LOG.info("Post text new process ID is " + newPostTextProID);
                if (!(updateProcessIDToDB(POST_COLUMN, newPostTextProID) > 0)) {
                    throw new Exception("Update post text process ID failed.");
                }
            }
        }
        LOG.info("---------------Post text from wechat done---------------");
        int commentTextProID = getProcessedIDFromDB(COMMENT_COLUMN);
        LOG.info("Comment text process ID is " + commentTextProID);
        Map<Integer, String> commentTextMap = getCommentTextFromWechat(commentTextProID);
        if (commentTextMap != null) {
            int newCommentTextProID = (int) commentTextMap.keySet().toArray()[0];
            String commentText = commentTextMap.get(newCommentTextProID);
            if (executeHttpPost(commentText) == 200) {
                LOG.info("Comment text new process ID is " + newCommentTextProID);
                if (!(updateProcessIDToDB(COMMENT_COLUMN, newCommentTextProID) > 0)) {
                    throw new Exception("Update post text process ID failed.");
                }
            }
        }
    }

    private Map<Integer, String> getCommentTextFromWechat(int processedID) {
        String result = executeHttpGet(COMMENT_TEXT, processedID);
        return processJSONString(result);
    }

    private Map<Integer, String> getPostTextFromWechat(int processedID) {
        String result = executeHttpGet(POST_TEXT, processedID);
        return processJSONString(result);
    }

    private Map<Integer, String> processJSONString(String jsonString) {
        JSONObject jsonObject = new JSONObject(jsonString);
        if (jsonObject.getInt("code") == 0 && jsonObject.getJSONArray("posts").length() > 0) {
            int key = 0;
            JSONObject resultObject = new JSONObject();
            JSONArray jsonMessage = new JSONArray();
            JSONArray postArray = jsonObject.getJSONArray("posts");
            for (int i = 0; i < postArray.length(); i++) {
                JSONObject postObject = postArray.getJSONObject(i);
                int id = postObject.getInt("id");
                if (id > key) key = id;
                String content = postObject.getString("content");
                int type = postObject.getInt("type");
                String tempFilePath = postObject.getString("tempFilePath");
                if (!content.isEmpty()) {
                    JSONObject resultJSON = new JSONObject();
                    resultJSON.put("content", content);
                    resultJSON.put("type", type);
                    resultJSON.put("tempFilePath", type == 2 ? tempFilePath : "");
                    jsonMessage.put(resultJSON);
                }
            }
            resultObject.put("message", jsonMessage);
            LOG.info("Wechat JSON is " + resultObject.toString());
            Map<Integer, String> map = new HashMap<>();
            map.put(key, resultObject.toString());
            return map;
        }
        LOG.info("Wechat JSON is null.");
        return null;
    }

    private int executeHttpPost(String jsonString) {
        try {
            HttpPost httpPost = new HttpPost(BACKEND_URL);
            StringEntity params = new StringEntity(jsonString, "utf-8");
            httpPost.setEntity(params);
            CloseableHttpResponse httpResponse = postHttpClient.execute(httpPost);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                return httpResponse.getStatusLine().getStatusCode();
            }
        } catch (IOException e) {
            LOG.info("Post to Java Backend Failed.");
            e.printStackTrace();
        }
        return -1;
    }

    private String executeHttpGet(String path, int processedID) {
        try {
            HttpGet httpGet = new HttpGet(WECHAT_URL + path + processedID);
            CloseableHttpResponse httpResponse = getHttpClient.execute(httpGet);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                LOG.info("Get wechat http client succeed : " + WECHAT_URL + path + processedID);
                HttpEntity httpEntity = httpResponse.getEntity();
                if (httpEntity != null) {
                    String result = EntityUtils.toString(httpEntity);
                    if (!result.isEmpty()) return result;
                } else {
                    throw new IOException("Get wechat http client entity is null.");
                }
            }
        } catch (IOException e) {
            LOG.info("Get wechat http client failed : " + WECHAT_URL + path + processedID);
            e.printStackTrace();
        }
        return null;
    }

    private int updateProcessIDToDB(String textType, int processedID) {
        String sql = "update integration.wechat set " + textType + " = " + processedID;
        LOG.info("Update processed ID SQL String is " + sql);
        try {
            return statement.executeUpdate(sql);
        } catch (SQLException e) {
            LOG.info("Update processedID to integration.wechat failed.");
            e.printStackTrace();
        }
        return -1;
    }

    private int getProcessedIDFromDB(String texType) {
        String sql = "select " + texType + " as processedID from integration.wechat";
        LOG.info("Get processed ID SQL String is " + sql);
        int processedID = -1;
        try {
            ResultSet resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                processedID = resultSet.getInt("processedID");
            }
        } catch (SQLException e) {
            LOG.info("Get processedID from integration.wechat failed.");
            e.printStackTrace();
        }
        return processedID;
    }
}
