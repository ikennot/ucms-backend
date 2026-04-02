package com.ucms_backend.controller;

import com.ucms_backend.security.SecurityUtils;
import com.ucms_backend.service.RealtimeSseService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/realtime")
public class RealtimeController {

    private final RealtimeSseService realtimeSseService;

    public RealtimeController(RealtimeSseService realtimeSseService) {
        this.realtimeSseService = realtimeSseService;
    }

    @GetMapping("/stream")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public SseEmitter stream() {
        return realtimeSseService.subscribe(SecurityUtils.getCurrentUserId());
    }
}
