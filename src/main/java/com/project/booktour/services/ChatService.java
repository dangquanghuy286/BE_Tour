package com.project.booktour.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;


import java.time.Duration;

@Service
public class ChatService {

    private final WebClient webClient;
    private final String n8nWebhookUrl;
    private final String backupWebhookUrl;

    public ChatService(
            @Value("${n8n.webhook.url}") String n8nWebhookUrl,
            @Value("${n8n.webhook.backup}") String backupWebhookUrl) {
        this.n8nWebhookUrl = n8nWebhookUrl;
        this.backupWebhookUrl = backupWebhookUrl;
        this.webClient = WebClient.builder()
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public Mono<String> sendMessageToN8n(String message, String chatId) {
        return webClient.get()
                .uri(n8nWebhookUrl, uriBuilder -> uriBuilder
                        .queryParam("message", message)
                        .queryParam("chatId", chatId)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(throwable -> throwable instanceof java.net.ConnectException))
                .onErrorResume(throwable -> {
                    return webClient.get()
                            .uri(backupWebhookUrl, uriBuilder -> uriBuilder
                                    .queryParam("message", message)
                                    .queryParam("chatId", chatId)
                                    .build())
                            .retrieve()
                            .bodyToMono(String.class)
                            .timeout(Duration.ofSeconds(10))
                            .onErrorMap(e -> {
                                if (e instanceof java.net.ConnectException) {
                                    return new RuntimeException("ALL_SERVICES_OFFLINE", e);
                                }
                                return new RuntimeException("ALL_CONNECTIONS_FAILED", e);
                            });
                });
    }

    public Mono<Boolean> checkN8nHealth() {
        return webClient.get()
                .uri(n8nWebhookUrl)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> true)
                .onErrorResume(throwable -> Mono.just(false));
    }
}