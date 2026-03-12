package com.ucms_backend.service;

import com.ucms_backend.dto.AttachmentResponse;
import com.ucms_backend.exception.AppException;
import com.ucms_backend.model.entity.Profile;
import com.ucms_backend.model.entity.Ticket;
import com.ucms_backend.model.entity.TicketAttachment;
import com.ucms_backend.model.enums.TicketStatus;
import com.ucms_backend.repository.ProfileRepository;
import com.ucms_backend.repository.TicketAttachmentRepository;
import com.ucms_backend.repository.TicketRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketAttachmentRepository ticketAttachmentRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private SupabaseStorageService supabaseStorageService;

    private AttachmentService attachmentService;

    private static final long MAX_SIZE_BYTES = 10_485_760L; // 10MB
    private static final Set<String> ALLOWED_MIME_TYPES =
            Set.of("image/jpeg", "image/png", "application/pdf");

    @BeforeEach
    void setUp() {
        attachmentService = new AttachmentService(
                ticketRepository,
                ticketAttachmentRepository,
                profileRepository,
                supabaseStorageService,
                MAX_SIZE_BYTES,
                ALLOWED_MIME_TYPES
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void upload_ticketNotFound_throws404() {
        setAuthenticatedUser(UUID.randomUUID(), "STUDENT");

        when(ticketRepository.findById(1L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () ->
                attachmentService.uploadAttachment(1L, mockJpegFile())
        );

        assertEquals(404, exception.getStatus());
        assertEquals("TICKET_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void upload_notOwner_throws403() {
        UUID userId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(1L)
                .userId(UUID.randomUUID())
                .status(TicketStatus.PENDING)
                .build();

        setAuthenticatedUser(userId, "STUDENT");

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        AppException exception = assertThrows(AppException.class, () ->
                attachmentService.uploadAttachment(1L, mockJpegFile())
        );

        assertEquals(403, exception.getStatus());
        assertEquals("FORBIDDEN", exception.getErrorCode());
        verify(profileRepository, never()).findById(any());
    }

    @Test
    void upload_accountLimited_throws403() {
        UUID userId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(2L)
                .userId(userId)
                .status(TicketStatus.PENDING)
                .build();
        Profile profile = Profile.builder()
                .authUserId(userId)
                .emailVerified(false)
                .build();

        setAuthenticatedUser(userId, "STUDENT");

        when(ticketRepository.findById(2L)).thenReturn(Optional.of(ticket));
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));

        AppException exception = assertThrows(AppException.class, () ->
                attachmentService.uploadAttachment(2L, mockJpegFile())
        );

        assertEquals(403, exception.getStatus());
        assertEquals("ACCOUNT_LIMITED", exception.getErrorCode());
    }

    @Test
    void upload_ticketClosed_throws403() {
        UUID userId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(3L)
                .userId(userId)
                .status(TicketStatus.CLOSED)
                .build();
        Profile profile = Profile.builder()
                .authUserId(userId)
                .emailVerified(true)
                .build();

        setAuthenticatedUser(userId, "STUDENT");

        when(ticketRepository.findById(3L)).thenReturn(Optional.of(ticket));
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));

        AppException exception = assertThrows(AppException.class, () ->
                attachmentService.uploadAttachment(3L, mockJpegFile())
        );

        assertEquals(403, exception.getStatus());
        assertEquals("TICKET_CLOSED", exception.getErrorCode());
        verify(supabaseStorageService, never()).uploadFile(anyString(), any(), anyString());
    }

    @Test
    void upload_ticketResolved_throws403() {
        UUID userId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(4L)
                .userId(userId)
                .status(TicketStatus.RESOLVED)
                .build();
        Profile profile = Profile.builder()
                .authUserId(userId)
                .emailVerified(true)
                .build();

        setAuthenticatedUser(userId, "STUDENT");

        when(ticketRepository.findById(4L)).thenReturn(Optional.of(ticket));
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));

        AppException exception = assertThrows(AppException.class, () ->
                attachmentService.uploadAttachment(4L, mockJpegFile())
        );

        assertEquals(403, exception.getStatus());
        assertEquals("TICKET_CLOSED", exception.getErrorCode());
        verify(supabaseStorageService, never()).uploadFile(anyString(), any(), anyString());
    }

    @Test
    void upload_success_returnsResponse() {
        UUID userId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(5L)
                .userId(userId)
                .status(TicketStatus.PENDING)
                .build();
        Profile profile = Profile.builder()
                .authUserId(userId)
                .emailVerified(true)
                .build();

        setAuthenticatedUser(userId, "STUDENT");

        when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket));
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(supabaseStorageService.generateSignedUrl(anyString())).thenReturn("signed-url");
        when(ticketAttachmentRepository.save(any(TicketAttachment.class))).thenAnswer(invocation -> {
            TicketAttachment input = invocation.getArgument(0);
            return TicketAttachment.builder()
                    .id(10L)
                    .ticketId(input.getTicketId())
                    .storagePath(input.getStoragePath())
                    .originalFilename(input.getOriginalFilename())
                    .mimeType(input.getMimeType())
                    .sizeBytes(input.getSizeBytes())
                    .uploadedAt(LocalDateTime.now())
                    .build();
        });

        AttachmentResponse response = attachmentService.uploadAttachment(5L, mockJpegFile());

        assertEquals(10L, response.getId());
        assertEquals("signed-url", response.getSignedUrl());
        assertEquals("sample.jpg", response.getOriginalFilename());
        assertNotNull(response.getUploadedAt());
        verify(supabaseStorageService).uploadFile(anyString(), any(), anyString());
    }

    @Test
    void upload_fileTooLarge_throws413() {
        byte[] bigContent = new byte[11 * 1024 * 1024]; // 11MB — exceeds 10MB limit
        MockMultipartFile bigFile = new MockMultipartFile(
                "file", "big.jpg", "image/jpeg", bigContent);

        AppException exception = assertThrows(AppException.class, () ->
                attachmentService.uploadAttachment(1L, bigFile)
        );

        assertEquals(413, exception.getStatus());
        assertEquals("FILE_TOO_LARGE", exception.getErrorCode());
        // Rejected before any DB or storage calls
        verify(ticketRepository, never()).findById(any());
        verify(supabaseStorageService, never()).uploadFile(anyString(), any(), anyString());
    }

    @Test
    void upload_invalidMimeType_throws400() {
        // A shell script — Tika will detect text/x-shellscript (not in allowlist)
        // Validation happens before any DB lookups, so no stubs needed
        MockMultipartFile file = new MockMultipartFile(
                "file", "script.sh", "application/x-sh",
                "#!/bin/bash\necho hi\n".getBytes());

        AppException exception = assertThrows(AppException.class, () ->
                attachmentService.uploadAttachment(1L, file)
        );

        assertEquals(400, exception.getStatus());
        assertEquals("INVALID_FILE_TYPE", exception.getErrorCode());
        verify(ticketRepository, never()).findById(any());
        verify(supabaseStorageService, never()).uploadFile(anyString(), any(), anyString());
    }

    @Test
    void getAttachments_ticketNotFound_throws404() {
        setAuthenticatedUser(UUID.randomUUID(), "STUDENT");

        when(ticketRepository.findById(9L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () ->
                attachmentService.getAttachments(9L)
        );

        assertEquals(404, exception.getStatus());
        assertEquals("TICKET_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void getAttachments_studentOwnTicket_success() {
        UUID userId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(6L)
                .userId(userId)
                .status(TicketStatus.PENDING)
                .build();
        TicketAttachment attachment = TicketAttachment.builder()
                .id(1L)
                .ticketId(6L)
                .storagePath("tickets/6/one.jpg")
                .originalFilename("one.jpg")
                .mimeType("image/jpeg")
                .sizeBytes(10L)
                .uploadedAt(LocalDateTime.now())
                .build();

        when(ticketRepository.findById(6L)).thenReturn(Optional.of(ticket));
        when(ticketAttachmentRepository.findByTicketId(6L)).thenReturn(List.of(attachment));
        when(supabaseStorageService.generateSignedUrl("tickets/6/one.jpg")).thenReturn("signed-1");

        setAuthenticatedUser(userId, "STUDENT");

        List<AttachmentResponse> response = attachmentService.getAttachments(6L);

        assertEquals(1, response.size());
        assertEquals("signed-1", response.getFirst().getSignedUrl());
    }

    @Test
    void getAttachments_studentOtherTicket_throws403() {
        UUID userId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(7L)
                .userId(UUID.randomUUID())
                .status(TicketStatus.PENDING)
                .build();

        when(ticketRepository.findById(7L)).thenReturn(Optional.of(ticket));

        setAuthenticatedUser(userId, "STUDENT");

        AppException exception = assertThrows(AppException.class, () ->
                attachmentService.getAttachments(7L)
        );

        assertEquals(403, exception.getStatus());
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void getAttachments_adminAnyTicket_success() {
        Ticket ticket = Ticket.builder()
                .id(8L)
                .userId(UUID.randomUUID())
                .status(TicketStatus.PENDING)
                .build();
        TicketAttachment attachment = TicketAttachment.builder()
                .id(2L)
                .ticketId(8L)
                .storagePath("tickets/8/two.jpg")
                .originalFilename("two.jpg")
                .mimeType("image/jpeg")
                .sizeBytes(20L)
                .uploadedAt(LocalDateTime.now())
                .build();

        when(ticketRepository.findById(8L)).thenReturn(Optional.of(ticket));
        when(ticketAttachmentRepository.findByTicketId(8L)).thenReturn(List.of(attachment));
        when(supabaseStorageService.generateSignedUrl("tickets/8/two.jpg")).thenReturn("signed-2");

        setAuthenticatedUser(UUID.randomUUID(), "ADMIN");

        List<AttachmentResponse> response = attachmentService.getAttachments(8L);

        assertEquals(1, response.size());
        assertEquals("signed-2", response.getFirst().getSignedUrl());
    }

    /**
     * Returns a minimal valid JPEG — the JPEG magic bytes (FFD8FF) followed by
     * enough filler for Tika to identify it as image/jpeg.
     */
    private MockMultipartFile mockJpegFile() {
        byte[] jpegMagic = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01
        };
        return new MockMultipartFile(
                "file",
                "sample.jpg",
                "image/jpeg",
                jpegMagic
        );
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
