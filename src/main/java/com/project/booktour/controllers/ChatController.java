package com.project.booktour.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.booktour.components.SseEmitterRegistry;
import com.project.booktour.services.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Mono;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("${api.prefix}")
@CrossOrigin(origins = "http://localhost:5173")
public class ChatController {

    private final ChatService chatService;
    private final SseEmitterRegistry sseEmitterRegistry;

    public ChatController(ChatService chatService, SseEmitterRegistry sseEmitterRegistry) {
        this.chatService = chatService;
        this.sseEmitterRegistry = sseEmitterRegistry;
    }

    @PostMapping("/chat")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String chatId = request.get("chatId");

        if (message == null || chatId == null) {
            return ResponseEntity.badRequest().body("Missing message or chatId");
        }

        return chatService.sendMessageToN8n(message, chatId)
                .map(response -> ResponseEntity.ok(Map.of("reply", response)))
                .onErrorResume(throwable -> {
                    Map<String, String> errorResponse = new HashMap<>();
                    if (throwable.getMessage().contains("ALL_SERVICES_OFFLINE")) {
                        errorResponse.put("error", "All N8N services are currently offline. Please try again later.");
                        errorResponse.put("code", "ALL_SERVICES_OFFLINE");
                        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse));
                    }
                    errorResponse.put("error", "Unable to connect to any N8N service");
                    errorResponse.put("code", "ALL_CONNECTIONS_FAILED");
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
                })
                .block();
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToEvents() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        sseEmitterRegistry.addEmitter(emitter);
        return emitter;
    }

    @PostMapping("/receive-message")
    public ResponseEntity<?> receiveMessage(@RequestBody Map<String, Object> request) throws JsonProcessingException {
        String chatId = (String) request.get("chatId");
        String reply = (String) request.get("reply");
        Object extra = request.get("extra");

        if (chatId == null || reply == null) {
            return ResponseEntity.badRequest().body("Missing chatId or reply");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("chatId", chatId);
        payload.put("reply", reply);
        payload.put("extra", extra);

        String data = new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(payload);

        sseEmitterRegistry.getEmitters().forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().data(data));
            } catch (Exception e) {
                System.err.println("Error sending SSE event: " + e.getMessage());
            }
        });

        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/health/n8n")
    public ResponseEntity<?> checkN8nHealth() {
        return chatService.checkN8nHealth()
                .map(isHealthy -> ResponseEntity.ok(Map.of("healthy", isHealthy)))
                .block();
    }
}