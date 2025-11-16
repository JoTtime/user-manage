package com.app.Harvest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient  // Use this instead of @EnableEurekaClient
public class HarvestApplication {
    public static void main(String[] args) {
        SpringApplication.run(HarvestApplication.class, args);
    }
}