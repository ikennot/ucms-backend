package com.ucms_backend.controller;

import com.ucms_backend.dto.ApiResponse;
import com.ucms_backend.dto.CreateResponseRequest;
import com.ucms_backend.dto.TicketResponseDto;
import com.ucms_backend.service.TicketResponseService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets/{id}/responses")
public class TicketResponseController {

    private final TicketResponseService ticketResponseService;

    public TicketResponseController(TicketResponseService ticketResponseService) {
        this.ticketResponseService = ticketResponseService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
    public ResponseEntity<ApiResponse<TicketResponseDto>> addResponse(
            @PathVariable Long id,
            @Valid @RequestBody CreateResponseRequest request
    ) {
        TicketResponseDto response = ticketResponseService.addResponse(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Response created", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ResponseEntity<ApiResponse<List<TicketResponseDto>>> getResponses(@PathVariable Long id) {
        List<TicketResponseDto> responses = ticketResponseService.getResponses(id);
        return ResponseEntity.ok(ApiResponse.ok("Responses retrieved", responses));
    }
}
