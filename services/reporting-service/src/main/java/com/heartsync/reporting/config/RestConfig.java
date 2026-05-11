package com.heartsync.reporting.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestConfig {

    /**
     * @LoadBalanced makes RestTemplate resolve service names via Eureka.
     * Usage: restTemplate.getForObject("http://ecg-service/api/ecg/{id}", ...)
     * Eureka looks up the actual IP/port of ecg-service automatically.
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
