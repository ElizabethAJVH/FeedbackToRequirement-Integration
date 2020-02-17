package com.sap.s4idea.rea.config;

import org.apache.http.HttpHost;

public class Proxy {
    private static final String ON_PREMISE_PROXY = "OnPremise";

    public static HttpHost getProxy(String proxyType) {
        String proxyHost;
        int proxyPort;
        if (ON_PREMISE_PROXY.equals(proxyType)) {
            proxyHost = System.getenv("HC_OP_HTTP_PROXY_HOST");
            proxyPort = Integer.parseInt(System.getenv("HC_OP_HTTP_PROXY_PORT"));
        } else {
            proxyHost = System.getProperty("http.proxyHost");
            proxyPort = Integer.parseInt(System.getProperty("http.proxyPort"));
        }
        return new HttpHost(proxyHost, proxyPort);
    }
}
