package com.ucms_backend.service;

import com.ucms_backend.dto.CreateTicketRequest;
import com.ucms_backend.dto.TicketResponse;
import com.ucms_backend.dto.UpdateStatusRequest;
import com.ucms_backend.exception.AppException;
import com.ucms_backend.model.entity.Profile;
import com.ucms_backend.model.entity.Ticket;
import com.ucms_backend.model.enums.TicketStatus;
import com.ucms_backend.repository.CategoryRepository;
import com.ucms_backend.repository.ProfileRepository;
import com.ucms_backend.repository.TicketRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private TicketNumberGenerator ticketNumberGenerator;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TicketService ticketService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createTicket_profileNotFound_throws404() {
        UUID userId = UUID.randomUUID();
        CreateTicketRequest request = new CreateTicketRequest(1L, "Title", "Description");

        setAuthenticatedUser(userId, "STUDENT");

        when(profileRepository.findById(userId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> ticketService.createTicket(request));

        assertEquals(404, exception.getStatus());
        assertEquals("PROFILE_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void createTicket_accountLimited_throws403() {
        UUID userId = UUID.randomUUID();
        CreateTicketRequest request = new CreateTicketRequest(1L, "Title", "Description");
        Profile profile = Profile.builder()
                .authUserId(userId)
                .emailVerified(false)
                .build();

        setAuthenticatedUser(userId, "STUDENT");

        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));

        AppException exception = assertThrows(AppException.class, () -> ticketService.createTicket(request));

        assertEquals(403, exception.getStatus());
        assertEquals("ACCOUNT_LIMITED", exception.getErrorCode());
    }

    @Test
    void createTicket_categoryNotFound_throws404() {
        UUID userId = UUID.randomUUID();
        CreateTicketRequest request = new CreateTicketRequest(1L, "Title", "Description");
        Profile profile = Profile.builder()
                .authUserId(userId)
                .emailVerified(true)
                .build();

        setAuthenticatedUser(userId, "STUDENT");

        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(categoryRepository.existsById(request.getCategoryId())).thenReturn(false);

        AppException exception = assertThrows(AppException.class, () -> ticketService.createTicket(request));

        assertEquals(404, exception.getStatus());
        assertEquals("CATEGORY_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void createTicket_success_returnsTicketResponse() {
        UUID userId = UUID.randomUUID();
        CreateTicketRequest request = new CreateTicketRequest(1L, "Title", "Description");
        Profile profile = Profile.builder()
                .authUserId(userId)
                .emailVerified(true)
                .build();
        Ticket saved = Ticket.builder()
                .id(10L)
                .userId(userId)
                .categoryId(request.getCategoryId())
                .ticketNumber("TKT-20260228-0001")
                .title(request.getTitle())
                .description(request.getDescription())
                .status(TicketStatus.PENDING)
                .build();

        setAuthenticatedUser(userId, "STUDENT");

        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(categoryRepository.existsById(request.getCategoryId())).thenReturn(true);
        when(ticketNumberGenerator.generate()).thenReturn("TKT-20260228-0001");
        when(ticketRepository.save(any(Ticket.class))).thenReturn(saved);

        TicketResponse response = ticketService.createTicket(request);

        assertEquals(10L, response.getId());
        assertEquals("TKT-20260228-0001", response.getTicketNumber());
        assertEquals("PENDING", response.getStatus());
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    void getTicketById_notFound_throws404() {
        setAuthenticatedUser(UUID.randomUUID(), "STUDENT");

        when(ticketRepository.findById(1L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> ticketService.getTicketById(1L));

        assertEquals(404, exception.getStatus());
        assertEquals("TICKET_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void getTicketById_studentForbidden_throws403() {
        UUID userId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(1L)
                .userId(UUID.randomUUID())
                .status(TicketStatus.PENDING)
                .build();

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        setAuthenticatedUser(userId, "STUDENT");

        AppException exception = assertThrows(AppException.class, () -> ticketService.getTicketById(1L));

        assertEquals(403, exception.getStatus());
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void getTicketById_studentOwnTicket_success() {
        UUID userId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(1L)
                .userId(userId)
                .status(TicketStatus.PENDING)
                .ticketNumber("TKT-20260228-0001")
                .title("Title")
                .description("Description")
                .categoryId(1L)
                .build();

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        setAuthenticatedUser(userId, "STUDENT");

        TicketResponse response = ticketService.getTicketById(1L);

        assertEquals(1L, response.getId());
        assertEquals("TKT-20260228-0001", response.getTicketNumber());
    }

    @Test
    void getTicketById_adminAnyTicket_success() {
        UUID userId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(2L)
                .userId(UUID.randomUUID())
                .status(TicketStatus.IN_PROGRESS)
                .ticketNumber("TKT-20260228-0002")
                .title("Title")
                .description("Description")
                .categoryId(2L)
                .build();

        when(ticketRepository.findById(2L)).thenReturn(Optional.of(ticket));

        setAuthenticatedUser(userId, "ADMIN");

        TicketResponse response = ticketService.getTicketById(2L);

        assertEquals(2L, response.getId());
        assertEquals("IN_PROGRESS", response.getStatus());
    }

    @Test
    void updateStatus_notFound_throws404() {
        UpdateStatusRequest request = new UpdateStatusRequest("IN_PROGRESS");

        when(ticketRepository.findById(1L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> ticketService.updateStatus(1L, request));

        assertEquals(404, exception.getStatus());
        assertEquals("TICKET_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void updateStatus_closedTicket_throws403() {
        Ticket ticket = Ticket.builder()
                .id(1L)
                .status(TicketStatus.CLOSED)
                .build();
        UpdateStatusRequest request = new UpdateStatusRequest("CLOSED");

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        AppException exception = assertThrows(AppException.class, () -> ticketService.updateStatus(1L, request));

        assertEquals(403, exception.getStatus());
        assertEquals("TICKET_CLOSED", exception.getErrorCode());
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void updateStatus_invalidTransition_throws409() {
        Ticket ticket = Ticket.builder()
                .id(1L)
                .status(TicketStatus.PENDING)
                .build();
        UpdateStatusRequest request = new UpdateStatusRequest("RESOLVED");

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        AppException exception = assertThrows(AppException.class, () -> ticketService.updateStatus(1L, request));

        assertEquals(409, exception.getStatus());
        assertEquals("INVALID_STATUS_TRANSITION", exception.getErrorCode());
    }

    @Test
    void updateStatus_validTransition_success() {
        Ticket ticket = Ticket.builder()
                .id(1L)
                .status(TicketStatus.PENDING)
                .ticketNumber("TKT-20260228-0001")
                .title("Title")
                .description("Description")
                .categoryId(1L)
                .build();
        UpdateStatusRequest request = new UpdateStatusRequest("IN_PROGRESS");
        Ticket updated = Ticket.builder()
                .id(1L)
                .status(TicketStatus.IN_PROGRESS)
                .ticketNumber("TKT-20260228-0001")
                .title("Title")
                .description("Description")
                .categoryId(1L)
                .build();

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(updated);

        TicketResponse response = ticketService.updateStatus(1L, request);

        assertEquals("IN_PROGRESS", response.getStatus());
        verify(ticketRepository).save(ticket);
    }

    // -------------------------------------------------------------------------
    // confirmResolved tests
    // -------------------------------------------------------------------------

    @Test
    void confirmResolved_success_setsConfirmedTrue() {
        UUID userId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(1L)
                .userId(userId)
                .status(TicketStatus.RESOLVED)
                .confirmedResolved(false)
                .ticketNumber("TKT-20260228-0001")
                .title("Title")
                .description("Description")
                .categoryId(1L)
                .build();
        Ticket saved = Ticket.builder()
                .id(1L)
                .userId(userId)
                .status(TicketStatus.RESOLVED)
                .confirmedResolved(true)
                .ticketNumber("TKT-20260228-0001")
                .title("Title")
                .description("Description")
                .categoryId(1L)
                .build();

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(saved);

        setAuthenticatedUser(userId, "STUDENT");

        TicketResponse response = ticketService.confirmResolved(1L);

        assertTrue(response.isConfirmedResolved());
        assertEquals("RESOLVED", response.getStatus());
        verify(ticketRepository).save(ticket);
        verify(notificationService).createNotification(
                eq(userId),
                eq(1L),
                eq("You have confirmed your ticket as resolved.")
        );
    }

    @Test
    void confirmResolved_ticketNotFound_throws404() {
        UUID userId = UUID.randomUUID();

        setAuthenticatedUser(userId, "STUDENT");

        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class,
                () -> ticketService.confirmResolved(99L));

        assertEquals(404, exception.getStatus());
        assertEquals("TICKET_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void confirmResolved_notOwner_throws403() {
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(1L)
                .userId(ownerId)
                .status(TicketStatus.RESOLVED)
                .confirmedResolved(false)
                .build();

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        setAuthenticatedUser(otherId, "STUDENT");

        AppException exception = assertThrows(AppException.class,
                () -> ticketService.confirmResolved(1L));

        assertEquals(403, exception.getStatus());
        assertEquals("FORBIDDEN", exception.getErrorCode());
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void confirmResolved_wrongStatus_throws409() {
        UUID userId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(1L)
                .userId(userId)
                .status(TicketStatus.IN_PROGRESS)
                .confirmedResolved(false)
                .build();

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        setAuthenticatedUser(userId, "STUDENT");

        AppException exception = assertThrows(AppException.class,
                () -> ticketService.confirmResolved(1L));

        assertEquals(409, exception.getStatus());
        assertEquals("INVALID_STATUS_TRANSITION", exception.getErrorCode());
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void confirmResolved_alreadyConfirmed_throws409() {
        UUID userId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(1L)
                .userId(userId)
                .status(TicketStatus.RESOLVED)
                .confirmedResolved(true)
                .build();

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        setAuthenticatedUser(userId, "STUDENT");

        AppException exception = assertThrows(AppException.class,
                () -> ticketService.confirmResolved(1L));

        assertEquals(409, exception.getStatus());
        assertEquals("ALREADY_CONFIRMED", exception.getErrorCode());
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void updateStatus_resolvedToClosedWithoutConfirmation_throws409() {
        Ticket ticket = Ticket.builder()
                .id(1L)
                .userId(UUID.randomUUID())
                .status(TicketStatus.RESOLVED)
                .confirmedResolved(false)
                .ticketNumber("TKT-20260228-0001")
                .title("Title")
                .description("Description")
                .categoryId(1L)
                .build();
        UpdateStatusRequest request = new UpdateStatusRequest("CLOSED");

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        AppException exception = assertThrows(AppException.class,
                () -> ticketService.updateStatus(1L, request));

        assertEquals(409, exception.getStatus());
        assertEquals("CONFIRMATION_REQUIRED", exception.getErrorCode());
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    private void setAuthenticatedUser(UUID userId, String role) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
