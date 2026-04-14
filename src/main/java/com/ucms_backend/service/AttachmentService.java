package com.ucms_backend.service;

import com.ucms_backend.config.AttachmentProperties;
import com.ucms_backend.dto.AttachmentResponse;
import com.ucms_backend.exception.AppException;
import com.ucms_backend.model.entity.Profile;
import com.ucms_backend.model.entity.Ticket;
import com.ucms_backend.model.entity.TicketAttachment;
import com.ucms_backend.model.enums.TicketStatus;
import com.ucms_backend.repository.ProfileRepository;
import com.ucms_backend.repository.TicketAttachmentRepository;
import com.ucms_backend.repository.TicketRepository;
import com.ucms_backend.security.SecurityUtils;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AttachmentService {

    private final TicketRepository ticketRepository;
    private final TicketAttachmentRepository ticketAttachmentRepository;
    private final ProfileRepository profileRepository;
    private final NotificationService notificationService;
    private final SupabaseStorageService supabaseStorageService;
    private final long maxSizeBytes;
    private final Set<String> allowedMimeTypes;

    public AttachmentService(
            TicketRepository ticketRepository,
            TicketAttachmentRepository ticketAttachmentRepository,
            ProfileRepository profileRepository,
            NotificationService notificationService,
            SupabaseStorageService supabaseStorageService,
            AttachmentProperties attachmentProperties
    ) {
        this.ticketRepository = ticketRepository;
        this.ticketAttachmentRepository = ticketAttachmentRepository;
        this.profileRepository = profileRepository;
        this.notificationService = notificationService;
        this.supabaseStorageService = supabaseStorageService;
        this.maxSizeBytes = attachmentProperties.getMaxSizeBytes();
        this.allowedMimeTypes = attachmentProperties.getAllowedMimeTypes();
    }

    public AttachmentResponse uploadAttachment(Long ticketId, MultipartFile file) {
        byte[] content = validateAndReadFile(file);
        String detectedMimeType = new Tika().detect(content);
        UUID userId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentRole();

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new AppException(404, "TICKET_NOT_FOUND", "Ticket not found"));

        boolean isAdmin = "ADMIN".equals(role);
        boolean isStudent = "STUDENT".equals(role);
        if (!isAdmin && !isStudent) {
            throw new AppException(403, "FORBIDDEN", "Access denied");
        }

        if (isStudent) {
            if (ticket.getUserId() == null || !ticket.getUserId().equals(userId)) {
                throw new AppException(403, "FORBIDDEN", "Access denied");
            }

            Profile profile = profileRepository.findById(userId)
                    .orElseThrow(() -> new AppException(404, "PROFILE_NOT_FOUND", "Profile not found"));

            if (!profile.isEmailVerified()) {
                throw new AppException(403, "ACCOUNT_LIMITED", "Verified email required to upload attachments");
            }
        }

        if (ticket.getStatus() == TicketStatus.CLOSED || ticket.getStatus() == TicketStatus.RESOLVED) {
            throw new AppException(403, "TICKET_CLOSED", "Cannot upload to a closed ticket");
        }

        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        String storagePath = buildStoragePath(ticketId, originalFilename);

        supabaseStorageService.uploadFile(storagePath, content, detectedMimeType);

        TicketAttachment attachment = TicketAttachment.builder()
                .ticketId(ticketId)
                .storagePath(storagePath)
                .originalFilename(originalFilename)
                .mimeType(detectedMimeType)
                .sizeBytes(file.getSize())
                .uploaderRole(role)
                .build();

        TicketAttachment saved = ticketAttachmentRepository.save(attachment);
        String signedUrl = supabaseStorageService.generateSignedUrl(storagePath);

        if (isStudent) {
            List<Profile> admins = profileRepository.findByRole("ADMIN");
            String filename = saved.getOriginalFilename() != null ? saved.getOriginalFilename() : "file";
            for (Profile admin : admins) {
                if (admin != null && admin.getAuthUserId() != null) {
                    notificationService.createNotification(
                            admin.getAuthUserId(),
                            ticket.getId(),
                            "Student uploaded attachment on ticket #" + ticket.getTicketNumber() + ": " + filename
                    );
                }
            }
        }

        return AttachmentResponse.from(saved, signedUrl);
    }

    public List<AttachmentResponse> getAttachments(Long ticketId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentRole();

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

    private byte[] validateAndReadFile(MultipartFile file) {
        if (file.getSize() > maxSizeBytes) {
            throw new AppException(413, "FILE_TOO_LARGE",
                    "File exceeds the maximum allowed size of 10MB");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException ex) {
            throw new AppException(500, "FILE_READ_ERROR", "Failed to read uploaded file");
        }

        String detectedMimeType = new Tika().detect(content);
        if (!allowedMimeTypes.contains(detectedMimeType)) {
            throw new AppException(400, "INVALID_FILE_TYPE",
                    "File type not allowed. Accepted types: image/jpeg, image/png, application/pdf");
        }

        return content;
    }

    private String buildStoragePath(Long ticketId, String originalFilename) {
        return "tickets/" + ticketId + "/" + UUID.randomUUID() + "-" + originalFilename;
    }

    private String sanitizeFilename(String originalFilename) {
        String name = originalFilename;
        if (name == null || name.isBlank()) {
            return "file";
        }

        // Strip path separators
        name = name.replace("\\", "/");
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash >= 0) {
            name = name.substring(lastSlash + 1);
        }

        if (name.isBlank()) {
            return "file";
        }

        // Replace URL-unsafe characters (colon, space, hash, etc.) with underscore
        name = name.replaceAll("[^a-zA-Z0-9._\\-]", "_");

        if (name.isBlank()) {
            return "file";
        }

        return name;
    }
}
