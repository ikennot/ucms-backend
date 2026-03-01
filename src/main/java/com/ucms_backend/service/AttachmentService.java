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
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AttachmentService {

    private final TicketRepository ticketRepository;
    private final TicketAttachmentRepository ticketAttachmentRepository;
    private final ProfileRepository profileRepository;
    private final SupabaseStorageService supabaseStorageService;

    public AttachmentService(
            TicketRepository ticketRepository,
            TicketAttachmentRepository ticketAttachmentRepository,
            ProfileRepository profileRepository,
            SupabaseStorageService supabaseStorageService
    ) {
        this.ticketRepository = ticketRepository;
        this.ticketAttachmentRepository = ticketAttachmentRepository;
        this.profileRepository = profileRepository;
        this.supabaseStorageService = supabaseStorageService;
    }

    public AttachmentResponse uploadAttachment(Long ticketId, UUID userId, MultipartFile file) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new AppException(404, "TICKET_NOT_FOUND", "Ticket not found"));

        if (!ticket.getUserId().equals(userId)) {
            throw new AppException(403, "FORBIDDEN", "Access denied");
        }

        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new AppException(404, "PROFILE_NOT_FOUND", "Profile not found"));

        if (!profile.isEmailVerified()) {
            throw new AppException(403, "ACCOUNT_LIMITED", "Verified email required to upload attachments");
        }

        if (ticket.getStatus() == TicketStatus.CLOSED || ticket.getStatus() == TicketStatus.RESOLVED) {
            throw new AppException(403, "TICKET_CLOSED", "Cannot upload to a closed ticket");
        }

        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        String mimeType = file.getContentType();
        if (mimeType == null || mimeType.isBlank()) {
            mimeType = "application/octet-stream";
        }

        long sizeBytes = file.getSize();
        String storagePath = buildStoragePath(ticketId, originalFilename);

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException ex) {
            throw new AppException(500, "FILE_READ_ERROR", "Failed to read uploaded file");
        }

        supabaseStorageService.uploadFile(storagePath, content, mimeType);

        TicketAttachment attachment = TicketAttachment.builder()
                .ticketId(ticketId)
                .storagePath(storagePath)
                .originalFilename(originalFilename)
                .mimeType(mimeType)
                .sizeBytes(sizeBytes)
                .build();

        TicketAttachment saved = ticketAttachmentRepository.save(attachment);
        String signedUrl = supabaseStorageService.generateSignedUrl(storagePath);

        return AttachmentResponse.from(saved, signedUrl);
    }

    public List<AttachmentResponse> getAttachments(Long ticketId, UUID userId, String role) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new AppException(404, "TICKET_NOT_FOUND", "Ticket not found"));

        if ("STUDENT".equals(role) && !ticket.getUserId().equals(userId)) {
            throw new AppException(403, "FORBIDDEN", "Access denied");
        }

        return ticketAttachmentRepository.findByTicketId(ticketId).stream()
                .map(attachment -> AttachmentResponse.from(
                        attachment,
                        supabaseStorageService.generateSignedUrl(attachment.getStoragePath())
                ))
                .toList();
    }

    private String buildStoragePath(Long ticketId, String originalFilename) {
        return "tickets/" + ticketId + "/" + UUID.randomUUID() + "-" + originalFilename;
    }

    private String sanitizeFilename(String originalFilename) {
        String name = originalFilename;
        if (name == null || name.isBlank()) {
            return "file";
        }

        name = name.replace("\\", "/");
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash >= 0) {
            name = name.substring(lastSlash + 1);
        }

        if (name.isBlank()) {
            return "file";
        }

        return name;
    }
}
