package com.ucms_backend.dto;

import com.ucms_backend.model.entity.TicketAttachment;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentResponse {

    private Long id;
    private String originalFilename;
    private String mimeType;
    private long sizeBytes;
    private String signedUrl;
    private LocalDateTime uploadedAt;
    private String uploaderRole;

    public static AttachmentResponse from(TicketAttachment attachment, String signedUrl) {
        return AttachmentResponse.builder()
                .id(attachment.getId())
                .originalFilename(attachment.getOriginalFilename())
                .mimeType(attachment.getMimeType())
                .sizeBytes(attachment.getSizeBytes())
                .signedUrl(signedUrl)
                .uploadedAt(attachment.getUploadedAt())
                .uploaderRole(attachment.getUploaderRole())
                .build();
    }
}
