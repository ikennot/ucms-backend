package com.ucms_backend.controller;

import com.ucms_backend.exception.AppException;
import com.ucms_backend.exception.GlobalExceptionHandler;
import com.ucms_backend.service.AttachmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AttachmentControllerValidationTest {

    @Mock
    private AttachmentService attachmentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        AttachmentController attachmentController = new AttachmentController(attachmentService);
        mockMvc = MockMvcBuilders.standaloneSetup(attachmentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void upload_missingFileParam_returns400() throws Exception {
        MockMultipartHttpServletRequestBuilder requestBuilder =
                MockMvcRequestBuilders.multipart("/api/tickets/{id}/attachments", 1L);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(attachmentService);
    }

    @Test
    void upload_invalidFileType_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "script.sh",
                "application/x-sh",
                "#!/bin/bash\necho test\n".getBytes()
        );

        when(attachmentService.uploadAttachment(eq(1L), any()))
                .thenThrow(new AppException(400, "INVALID_FILE_TYPE", "File type not allowed"));

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/tickets/{id}/attachments", 1L).file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_FILE_TYPE"));
    }

    @Test
    void upload_oversizedFile_returns413() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "big.jpg",
                "image/jpeg",
                new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}
        );

        when(attachmentService.uploadAttachment(eq(1L), any()))
                .thenThrow(new AppException(413, "FILE_TOO_LARGE", "File exceeds the maximum allowed size of 10MB"));

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/tickets/{id}/attachments", 1L).file(file))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("FILE_TOO_LARGE"));
    }
}
