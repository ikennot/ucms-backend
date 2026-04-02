package com.ucms_backend.service;

import com.ucms_backend.dto.RealtimeEventResponse;
import com.ucms_backend.model.entity.Profile;
import com.ucms_backend.repository.ProfileRepository;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class RealtimeSseService {

    private static final long SSE_TIMEOUT_MS = 0L;
    private static final String ROLE_ADMIN = "ADMIN";

    private final ProfileRepository profileRepository;
    private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

    public RealtimeSseService(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    public SseEmitter subscribe(UUID userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emittersByUser.computeIfAbsent(userId, key -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(ex -> removeEmitter(userId, emitter));

        sendToEmitter(userId, emitter, RealtimeEventResponse.builder()
                .domain("system")
                .eventType("CONNECTED")
                .entityId(userId.toString())
                .updatedAt(LocalDateTime.now(ZoneOffset.UTC))
                .actorRole("SYSTEM")
                .build());

        return emitter;
    }

    public void publishToUser(UUID userId, RealtimeEventResponse event) {
        List<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            sendToEmitter(userId, emitter, event);
        }
    }

    public void publishToAdmins(RealtimeEventResponse event) {
        List<Profile> admins = profileRepository.findByRole(ROLE_ADMIN);
        for (Profile admin : admins) {
            if (admin.getAuthUserId() != null) {
                publishToUser(admin.getAuthUserId(), event);
            }
        }
    }

    private void sendToEmitter(UUID userId, SseEmitter emitter, RealtimeEventResponse event) {
        try {
            emitter.send(SseEmitter.event()
                    .name("sync")
                    .data(event));
        } catch (IOException ex) {
            removeEmitter(userId, emitter);
        }
    }

    private void removeEmitter(UUID userId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByUser.remove(userId);
        }
    }
}
