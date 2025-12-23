package com.graduation.notificationservice.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.scheduling.annotation.Scheduled;
import jakarta.annotation.PreDestroy;

@Service
public class SseService {
    // Stores a list of emitters by userId to support multiple devices/tabs
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter createConnection(Long userId) {
        // Set a long timeout (e.g., 30 mins)
        SseEmitter emitter = new SseEmitter(1800_000L);

        // Add to the list safely
        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // Remove ONLY this specific emitter upon completion/timeout
        Runnable removeEmitter = () -> {
            List<SseEmitter> userEmitters = emitters.get(userId);
            if (userEmitters != null) {
                userEmitters.remove(emitter);
                if (userEmitters.isEmpty()) {
                    emitters.remove(userId);
                }
            }
        };

        emitter.onCompletion(removeEmitter);
        emitter.onTimeout(removeEmitter);
        emitter.onError((e) -> removeEmitter.run());

        // Send an initial "connected" event
        try {
            emitter.send(SseEmitter.event().name("INIT").data("Connected"));
        } catch (IOException e) {
            removeEmitter.run();
        }

        return emitter;
    }

    public void sendToUser(Long userId, Object payload) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters != null && !userEmitters.isEmpty()) {
            userEmitters.forEach(emitter -> {
                try {
                    emitter.send(SseEmitter.event()
                            .name("NEW_NOTIFICATION")
                            .data(payload));
                } catch (IOException e) {
                    // Specific emitter failed, remove it
                    userEmitters.remove(emitter);
                    if (userEmitters.isEmpty()) {
                        emitters.remove(userId);
                    }
                }
            });
        }
    }

    /**
     * Send a heartbeat to all active clients every 45 seconds
     * to prevent Gateway timeouts (usually 60s).
     */
    @Scheduled(fixedRate = 45000)
    public void sendHeartbeat() {
        if (emitters.isEmpty())
            return;

        emitters.forEach((userId, userEmitters) -> {
            userEmitters.forEach(emitter -> {
                try {
                    emitter.send(SseEmitter.event().name("ping").data("keep-alive"));
                } catch (Exception e) {
                    // Client disconnected, remove emitter
                    userEmitters.remove(emitter);
                }
            });
            // Cleanup empty lists after iteration
            if (userEmitters.isEmpty()) {
                emitters.remove(userId);
            }
        });
    }

    /**
     * Notify clients before server shutdown so they can stop reconnecting
     * or switch to health-probe mode immediately.
     */
    @PreDestroy
    public void shutdown() {
        if (emitters.isEmpty())
            return;

        emitters.forEach((userId, userEmitters) -> {
            userEmitters.forEach(emitter -> {
                try {
                    emitter.send(SseEmitter.event().name("SHUTDOWN").data("Server shutting down"));
                    emitter.complete();
                } catch (IOException e) {
                    // Ignore errors during shutdown
                }
            });
        });
        emitters.clear();
    }
}