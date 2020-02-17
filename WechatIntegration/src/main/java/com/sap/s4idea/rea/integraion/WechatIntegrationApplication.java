package com.sap.s4idea.rea.integraion;

import com.sap.s4idea.rea.integraion.wechat.DataSourceConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WechatIntegrationApplication extends SpringBootServletInitializer {
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application)
    {
        return application.sources(WechatIntegrationApplication.class, DataSourceConfiguration.class);
    }
    public static void main(String[] args) {
        SpringApplication.run(WechatIntegrationApplication.class,args);
    }
}
