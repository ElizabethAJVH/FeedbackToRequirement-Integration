package com.sap.s4idea.rea.integration;

import com.sap.s4idea.rea.integration.twitter.DataSourceConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TwitterIntegrationApplication extends SpringBootServletInitializer {
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application)
    {
        return application.sources(TwitterIntegrationApplication.class, DataSourceConfiguration.class);
    }
    public static void main(String[] args) {
        SpringApplication.run(TwitterIntegrationApplication.class,args);
    }
}
