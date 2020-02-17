//package com.sap.s4idea.rea;
//
//import com.sap.cloud.account.TenantContext;
//import com.sap.core.connectivity.api.configuration.ConnectivityConfiguration;
//import com.sap.core.connectivity.api.configuration.DestinationConfiguration;
//import com.sap.s4idea.rea.controller.Proxy;
//import com.sap.s4idea.rea.controller.TwitterController;
//import org.apache.http.HttpHost;
//import org.apache.http.client.methods.CloseableHttpResponse;
//import org.apache.http.client.methods.HttpPost;
//import org.apache.http.entity.StringEntity;
//import org.apache.http.impl.client.CloseableHttpClient;
//import org.apache.http.impl.client.HttpClients;
//import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import javax.naming.Context;
//import javax.naming.InitialContext;
//import javax.naming.NamingException;
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServlet;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import java.io.IOException;
//import java.io.PrintWriter;
//import java.util.stream.Collectors;
//
//public class CommonServlet extends HttpServlet {
//    private static final Logger LOG = LoggerFactory.getLogger(CommonServlet.class);
//    private ConnectivityConfiguration configuration;
//    private TenantContext tenantContext;
//
//
//    CommonServlet() throws NamingException {
//        super();
//        Context ctx = new InitialContext();
//        configuration = (ConnectivityConfiguration) ctx.lookup("java:comp/env/connectivityConfiguration");
//        tenantContext = (TenantContext) ctx.lookup("java:comp/env/TenantContext");
//    }
//
//    @Override
//    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        req.setCharacterEncoding("UTF-8");
//        String destinationName = this.getInitParameter("destination");
//        try {
//            DestinationConfiguration destConfiguration = configuration.getConfiguration(destinationName);
//            if (destConfiguration == null) {
//                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, String.format("Destination %s is not found. Hint: Make sure to have the destination configured.", destinationName));
//                throw new ServletException();
//            }
//            String value = destConfiguration.getProperty("URL");
//            String proxyType = destConfiguration.getProperty("ProxyType");
//            HttpHost proxy = Proxy.getProxy(proxyType);
//            DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
//
//            CloseableHttpClient httpClient = HttpClients.custom().setRoutePlanner(routePlanner).build();
//            HttpPost httpPost = new HttpPost(value);
//
//            if (destinationName.equalsIgnoreCase("twitter")) {
//                TwitterController twitterController = new TwitterController();
//                StringEntity params = new StringEntity(twitterController.getTweets());
//                httpPost.setEntity(params);
//            } else if (destinationName.equalsIgnoreCase("wechat")) {
//                String s = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
//                LOG.info("WeChat Request Body: " + s);
//                StringEntity params = new StringEntity(s, "UTF-8");
//                httpPost.setEntity(params);
//            }
//
//            httpPost.addHeader("content-type", "application/json");
//            httpPost.addHeader("SAP-Connectivity-ConsumerAccount", tenantContext.getTenant().getAccount().getId());
//
//            CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
//
//            PrintWriter printWriter = resp.getWriter();
//            printWriter.print(httpResponse.getStatusLine().getStatusCode());
//        } catch (Exception e) {
//            // Connectivity operation failed
//            String errorMessage = "Connectivity operation failed with reason: "
//                    + e.getMessage()
//                    + ". See "
//                    + "logs for details. Hint: Make sure to have an HTTP proxy configured in your "
//                    + "local environment in case your environment uses "
//                    + "an HTTP proxy for the outbound Internet "
//                    + "communication.";
//            LOG.error("Connectivity operation failed", e);
//            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
//                    errorMessage);
//        }
//    }
//}
