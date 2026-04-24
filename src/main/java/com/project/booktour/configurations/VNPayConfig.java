package com.project.booktour.configurations;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.vnpay")
@Getter
@Setter
public class VNPayConfig {
    private String version;
    private String command;
    private String tmnCode;
    private String hashSecret;
    private String returnUrl;
    private String paymentUrl;
    private String apiUrl;


}