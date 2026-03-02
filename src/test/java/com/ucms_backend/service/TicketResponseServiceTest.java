package com.ucms_backend.service;

import com.ucms_backend.dto.CreateResponseRequest;
import com.ucms_backend.dto.TicketResponseDto;
import com.ucms_backend.exception.AppException;
import com.ucms_backend.model.entity.Ticket;
import com.ucms_backend.model.entity.TicketResponse;
import com.ucms_backend.repository.TicketRepository;
import com.ucms_backend.repository.TicketResponseRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketResponseServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketResponseRepository ticketResponseRepository;

    @InjectMocks
    private TicketResponseService ticketResponseService;

    @Test
    void addResponse_success_returnsResponse() {
        Long ticketId = 10L;
        UUID adminId = UUID.randomUUID();
        CreateResponseRequest request = new CreateResponseRequest("We are looking into this");
        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .userId(UUID.randomUUID())
                .build();
        TicketResponse saved = TicketResponse.builder()
                .id(99L)
                .ticketId(ticketId)
                .adminId(adminId)
                .message(request.getMessage())
                .createdAt(LocalDateTime.now())
                .build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketResponseRepository.save(any(TicketResponse.class))).thenReturn(saved);

        TicketResponseDto response = ticketResponseService.addResponse(ticketId, adminId, "ADMIN", request);

        assertEquals(99L, response.getId());
        assertEquals(ticketId, response.getTicketId());
        assertEquals(adminId, response.getAdminId());
        assertEquals(request.getMessage(), response.getMessage());
        verify(ticketResponseRepository).save(any(TicketResponse.class));
    }

    @Test
    void addResponse_notAdmin_throws403() {
        Long ticketId = 10L;
        UUID adminId = UUID.randomUUID();
        CreateResponseRequest request = new CreateResponseRequest("We are looking into this");

        AppException exception = assertThrows(AppException.class,
                () -> ticketResponseService.addResponse(ticketId, adminId, "STUDENT", request));

        assertEquals(403, exception.getStatus());
        assertEquals("FORBIDDEN", exception.getErrorCode());
        verify(ticketRepository, never()).findById(anyLong());
    }

    @Test
    void addResponse_ticketNotFound_throws404() {
        Long ticketId = 10L;
        UUID adminId = UUID.randomUUID();
        CreateResponseRequest request = new CreateResponseRequest("We are looking into this");

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class,
                () -> ticketResponseService.addResponse(ticketId, adminId, "ADMIN", request));

        assertEquals(404, exception.getStatus());
        assertEquals("TICKET_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void getResponses_studentOwnTicket_successOldestFirst() {
        Long ticketId = 10L;
        UUID studentId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .userId(studentId)
                .build();
        TicketResponse older = TicketResponse.builder()
                .id(1L)
                .ticketId(ticketId)
                .adminId(UUID.randomUUID())
                .message("First")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();
        TicketResponse newer = TicketResponse.builder()
                .id(2L)
                .ticketId(ticketId)
                .adminId(UUID.randomUUID())
                .message("Second")
                .createdAt(LocalDateTime.now())
                .build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketResponseRepository.findByTicketIdOrderByCreatedAtAscIdAsc(ticketId))
                .thenReturn(List.of(older, newer));

        List<TicketResponseDto> responses = ticketResponseService.getResponses(ticketId, studentId, "STUDENT");

        assertEquals(2, responses.size());
        assertEquals("First", responses.get(0).getMessage());
        assertEquals("Second", responses.get(1).getMessage());
    }

    @Test
    void getResponses_studentOtherTicket_throws403() {
        Long ticketId = 10L;
        UUID studentId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .userId(UUID.randomUUID())
                .build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        AppException exception = assertThrows(AppException.class,
                () -> ticketResponseService.getResponses(ticketId, studentId, "STUDENT"));

        assertEquals(403, exception.getStatus());
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void getResponses_adminAnyTicket_success() {
        Long ticketId = 10L;
        UUID adminId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .userId(UUID.randomUUID())
                .build();
        TicketResponse response = TicketResponse.builder()
                .id(1L)
                .ticketId(ticketId)
                .adminId(adminId)
                .message("Admin response")
                .createdAt(LocalDateTime.now())
                .build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketResponseRepository.findByTicketIdOrderByCreatedAtAscIdAsc(ticketId))
                .thenReturn(List.of(response));

        List<TicketResponseDto> responses = ticketResponseService.getResponses(ticketId, adminId, "ADMIN");

        assertEquals(1, responses.size());
        assertEquals("Admin response", responses.get(0).getMessage());
    }
}
