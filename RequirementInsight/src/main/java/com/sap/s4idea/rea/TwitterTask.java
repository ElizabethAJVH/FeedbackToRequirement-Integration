package com.sap.s4idea.rea;

import com.sap.cloud.account.TenantContext;
import com.sap.core.connectivity.api.configuration.ConnectivityConfiguration;
import com.sap.core.connectivity.api.configuration.DestinationConfiguration;
import com.sap.s4idea.rea.config.Proxy;
import com.sap.s4idea.rea.config.TwitterController;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.TimerTask;

public class TwitterTask extends TimerTask {
    private static final Logger LOG = LoggerFactory.getLogger(TwitterTask.class);
    private static final String DESTINATION_NAME = "twitter";

    @Override
    public void run() {
        try {
            Context ctx = new InitialContext();
            ConnectivityConfiguration configuration = (ConnectivityConfiguration) ctx.lookup("java:comp/env/connectivityConfiguration");
            TenantContext tenantContext = (TenantContext) ctx.lookup("java:comp/env/TenantContext");
            DestinationConfiguration destConfiguration = configuration.getConfiguration(DESTINATION_NAME);
            if (destConfiguration == null) {
                LOG.info("Destination %s is not found. Hint: Make sure to have the destination configured.", DESTINATION_NAME);
                return;
            }
            String value = destConfiguration.getProperty("URL");
            String proxyType = destConfiguration.getProperty("ProxyType");
            HttpHost proxy = Proxy.getProxy(proxyType);
            DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);

            CloseableHttpClient httpClient = HttpClients.custom().setRoutePlanner(routePlanner).build();
            HttpPost httpPost = new HttpPost(value);

            TwitterController twitterController = new TwitterController();
            StringEntity params = new StringEntity(twitterController.getTweets(), "UTF-8");
            httpPost.setEntity(params);

            httpPost.addHeader("content-type", "application/json");
            httpPost.addHeader("SAP-Connectivity-ConsumerAccount", tenantContext.getTenant().getAccount().getId());

            CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
            LOG.info("Task: " + httpResponse.getStatusLine().getStatusCode());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
