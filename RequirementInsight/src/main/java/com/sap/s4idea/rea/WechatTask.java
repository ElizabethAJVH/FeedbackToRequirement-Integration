package com.sap.s4idea.rea;

import com.sap.cloud.account.TenantContext;
import com.sap.core.connectivity.api.configuration.ConnectivityConfiguration;
import com.sap.core.connectivity.api.configuration.DestinationConfiguration;
import com.sap.s4idea.rea.config.Proxy;
import com.sap.s4idea.rea.config.StatusUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.io.IOException;
import java.util.TimerTask;


public class WechatTask extends TimerTask {
    private static final Logger LOG = LoggerFactory.getLogger(WechatTask.class);
    private static final String DESTINATION_NAME_WECHAT_LITTLE_PROGRAM = "wechat_little_program";
    private static final String DESTINATION_NAME_WECHAT = "wechat";

    @Override
    public void run() {
        try {
            Context ctx = new InitialContext();
            ConnectivityConfiguration configuration = (ConnectivityConfiguration) ctx.lookup("java:comp/env/connectivityConfiguration");
            DestinationConfiguration littleProgramConfiguration = configuration.getConfiguration(DESTINATION_NAME_WECHAT_LITTLE_PROGRAM);
            if (littleProgramConfiguration == null) {
                LOG.error("Destination is not found. Hint: Make sure to have the destination configured.");
                return;
            }
            String littleProgramURL = littleProgramConfiguration.getProperty("URL");
            String littleProgramProxyType = littleProgramConfiguration.getProperty("ProxyType");
            HttpHost littleProgramProxy = Proxy.getProxy(littleProgramProxyType);
            DefaultProxyRoutePlanner littleProgramRoutePlanner = new DefaultProxyRoutePlanner(littleProgramProxy);
            CloseableHttpClient getHttpClient = HttpClients.custom().setRoutePlanner(littleProgramRoutePlanner).build();
            LOG.error("Destination %s URL is %s", DESTINATION_NAME_WECHAT_LITTLE_PROGRAM, littleProgramURL);

            postWechat(littleProgramURL, getHttpClient);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void postWechat(String littleProgramURL, CloseableHttpClient getHttpClient) throws Exception {
        StatusUtils.Status status = StatusUtils.getStatus();
        //识别post语音
        LOG.error("识别意见语音...");
        while (true) {
            String result = httpGet(littleProgramURL, "byton/voiceToText1?id=" + status.getPostVoiceId(), getHttpClient);
            JSONObject jsonResult = new JSONObject(result);
            if (jsonResult.getInt("code") == 0) {
                JSONArray jsonArray = jsonResult.getJSONArray("posts3");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonPost = jsonArray.getJSONObject(i);
                    long id = jsonPost.getLong("id");
                    status.setPostVoiceId(id);
                    LOG.error("recognize post " + jsonPost);
                }
                StatusUtils.saveStatus(status);
                if (jsonArray.length() <= 0) {
                    break;
                }
            } else {
                throw new Exception(jsonResult.getString("error"));
            }
        }
        //识别comment语音
        LOG.error("识别评论语音...");
        while (true) {
            String result = httpGet(littleProgramURL, "byton/voiceToText2?id=" + status.getCommentVoiceId(), getHttpClient);
            JSONObject jsonResult = new JSONObject(result);
            if (jsonResult.getInt("code") == 0) {
                JSONArray jsonArray = jsonResult.getJSONArray("posts3");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonPost = jsonArray.getJSONObject(i);
                    long id = jsonPost.getLong("id");
                    status.setCommentVoiceId(id);
                    LOG.error("recognize comment " + jsonPost);
                }
                StatusUtils.saveStatus(status);
                if (jsonArray.length() <= 0) {
                    break;
                }
            } else {
                throw new Exception(jsonResult.getString("error"));
            }
        }
        LOG.error("提交意见...");
        while (true) {
            boolean out = false;
            String result = httpGet(littleProgramURL, "post/myList?id=" + status.getPostSubmitId(), getHttpClient);
            JSONObject jsonResult = new JSONObject(result);
            JSONObject jsonMessageObject = new JSONObject();
            JSONArray jsonMessage = new JSONArray();
            if (jsonResult.getInt("code") == 0) {
                JSONArray jsonArray = jsonResult.getJSONArray("posts");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonPost = jsonArray.getJSONObject(i);
                    long id = jsonPost.getLong("id");
                    String content = jsonPost.getString("content");
                    int type = jsonPost.getInt("type");
                    String tempFilePath = jsonPost.getString("tempFilePath");

                    if (id > status.getPostVoiceId() && type == 2) {//如果遇到新的没有识别的语音信息
                        out = true;
                        break;
                    }
                    status.setPostSubmitId(id);
                    if (!content.isEmpty()) {
                        JSONObject obj = new JSONObject();
                        obj.put("content", content);
                        obj.put("type", type);
                        obj.put("tempFilePath", tempFilePath);
                        jsonMessage.put(obj);
                    }
                    LOG.error("submit post " + jsonPost);
                }
                if (jsonMessage.length() > 0) {
                    jsonMessageObject.put("message", jsonMessage);
                    if (httpPost(jsonMessageObject.toString()) != 200) {
                        throw new Exception("Post to Java Backend Failed.");
                    }
                }
                StatusUtils.saveStatus(status);
                if (jsonArray.length() <= 0 || out) {
                    break;
                }
            } else {
                throw new Exception(jsonResult.getString("error"));
            }
        }
        LOG.error("提交评论...");
        while (true) {
            boolean out = false;
            String result = httpGet(littleProgramURL, "comment/myList?id=" + status.getCommentSubmitId(), getHttpClient);
            JSONObject jsonResult = new JSONObject(result);
            JSONObject jsonMessageObject = new JSONObject();
            JSONArray jsonMessage = new JSONArray();

            if (jsonResult.getInt("code") == 0) {
                JSONArray jsonArray = jsonResult.getJSONArray("posts");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonPost = jsonArray.getJSONObject(i);
                    long id = jsonPost.getLong("id");
                    String content = jsonPost.getString("content");
                    int type = jsonPost.getInt("type");
                    String tempFilePath = jsonPost.getString("tempFilePath");

                    if (id > status.getCommentVoiceId() && type == 2) {//如果遇到新的没有识别的语音信息
                        out = true;
                        break;
                    }

                    status.setCommentSubmitId(id);
                    if (!content.isEmpty()) {
                        JSONObject obj = new JSONObject();
                        obj.put("content", content);
                        obj.put("type", type);
                        obj.put("tempFilePath", tempFilePath);
                        jsonMessage.put(obj);
                    }
                    LOG.error("submit comment " + jsonPost);
                }
                if (jsonMessage.length() > 0) {
                    jsonMessageObject.put("message", jsonMessage);
                    if (httpPost(jsonMessageObject.toString()) != 200) {
                        throw new Exception("Post to Java Backend Failed.");
                    }
                }
                StatusUtils.saveStatus(status);
                if (jsonArray.length() <= 0 || out) {
                    LOG.error("Wechat 执行完毕！");
                    break;
                }
            } else {
                throw new Exception(jsonResult.getString("error"));
            }
        }
    }

    private String httpGet(String baseURL, String path, CloseableHttpClient getHttpClient) throws IOException {
        HttpGet httpget = new HttpGet(baseURL + path);
        LOG.error("Executing request " + httpget.getRequestLine());
        ResponseHandler<String> responseHandler = response -> {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                HttpEntity entity = response.getEntity();
                return entity != null ? EntityUtils.toString(entity) : null;
            } else {
                throw new ClientProtocolException("Unexpected response status: " + status);
            }
        };
        String responseBody = getHttpClient.execute(httpget, responseHandler);
        LOG.error("--------------------------------------------------------------------------------");
        LOG.error(responseBody);
        return responseBody;
    }

    private int httpPost(String data) {
        try {
            Context ctx = new InitialContext();
            ConnectivityConfiguration configuration = (ConnectivityConfiguration) ctx.lookup("java:comp/env/connectivityConfiguration");
            TenantContext tenantContext = (TenantContext) ctx.lookup("java:comp/env/TenantContext");
            DestinationConfiguration destConfiguration = configuration.getConfiguration(DESTINATION_NAME_WECHAT);
            if (destConfiguration == null) {
                LOG.info("Destination %s is not found. Hint: Make sure to have the destination configured.", DESTINATION_NAME_WECHAT);
                throw new Exception();
            }
            String wechatURL = destConfiguration.getProperty("URL");
            String wechatProxyType = destConfiguration.getProperty("ProxyType");
            HttpHost wechatProxy = Proxy.getProxy(wechatProxyType);
            DefaultProxyRoutePlanner wechatRoutePlanner = new DefaultProxyRoutePlanner(wechatProxy);

            CloseableHttpClient httpClient = HttpClients.custom().setRoutePlanner(wechatRoutePlanner).build();
            HttpPost httpPost = new HttpPost(wechatURL);

            StringEntity params = new StringEntity(data, "utf-8");
            httpPost.setEntity(params);

            httpPost.addHeader("content-type", "application/json");
            httpPost.addHeader("SAP-Connectivity-ConsumerAccount", tenantContext.getTenant().getAccount().getId());

            LOG.error("Executing request " + httpPost.getRequestLine());
            LOG.error("request body: " + data);

            CloseableHttpResponse response = httpClient.execute(httpPost);

            if (response.getStatusLine().getStatusCode() != 200) {
                LOG.error("Wechat Integration API failed");
                throw new Exception();
            }
            return response.getStatusLine().getStatusCode();
        } catch (Exception e) {
            LOG.error("Post to Java Backend Failed.");
            e.printStackTrace();
        }
        return -1;
    }


}
