package org.example.server.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class ServerSentEvents {
    private final Set<SseEmitter> clients = ConcurrentHashMap.newKeySet();

    @GetMapping(path = "/api/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events() {
        // 0L = no timeout; client may still disconnect anytime
        SseEmitter em = new SseEmitter(0L);
        clients.add(em);
        em.onCompletion(() -> clients.remove(em));
        em.onTimeout(() -> clients.remove(em));
        em.onError((ex) -> clients.remove(em));
        try {
            em.send(SseEmitter.event().name("hello").data("connected"));
        } catch (Exception ignored) {}
        return em;
    }

    /**
     * Broadcast change to all active clients. Swallows failures from stale/completed emitters and
     * prunes them from the registry to avoid IllegalStateException bubbling into controllers.
     */
    public void broadcast(String msg) {
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter em : clients) {
            try {
                em.send(SseEmitter.event().name("change").data(msg));
            } catch (IllegalStateException | IOException ex) {
                // emitter completed/closed; schedule removal
                dead.add(em);
            } catch (Exception ex) {
                // any other send error — also remove
                dead.add(em);
            }
        }
        if (!dead.isEmpty()) {
            clients.removeAll(dead);
            for (SseEmitter em : dead) { try { em.complete(); } catch (Exception ignored) {} }
        }
    }
}