package com.hellochat.backend;

import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class HelloChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(HelloChatApplication.class, args);
    }
}
