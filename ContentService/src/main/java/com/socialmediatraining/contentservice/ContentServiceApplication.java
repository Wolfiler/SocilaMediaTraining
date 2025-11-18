package com.socialmediatraining.contentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {
        "com.socialmediatraining.contentservice",
        "com.socialmediatraining.exceptioncommons",
        "com.socialmediatraining.authenticationcommons"})
@EnableDiscoveryClient
@EnableCaching
public class ContentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentServiceApplication.class, args);
    }

}
