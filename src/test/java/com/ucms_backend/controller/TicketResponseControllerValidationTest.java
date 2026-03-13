package com.ucms_backend.controller;

import com.ucms_backend.exception.GlobalExceptionHandler;
import com.ucms_backend.service.TicketResponseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TicketResponseControllerValidationTest {

    @Mock
    private TicketResponseService ticketResponseService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        TicketResponseController ticketResponseController = new TicketResponseController(ticketResponseService);
        mockMvc = MockMvcBuilders.standaloneSetup(ticketResponseController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void addResponse_blankMessage_returnsValidationError() throws Exception {
        String payload = """
                {
                  "message": ""
                }
                """;

        mockMvc.perform(post("/api/tickets/{id}/responses", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data").value(nullValue()));

        verifyNoInteractions(ticketResponseService);
    }
}
