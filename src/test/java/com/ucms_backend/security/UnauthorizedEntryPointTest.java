package com.ucms_backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;

class UnauthorizedEntryPointTest {

    private final UnauthorizedEntryPoint unauthorizedEntryPoint = new UnauthorizedEntryPoint(new ObjectMapper());

    @Test
    void commence_returnsUnauthorizedResponseWithErrorCode() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        unauthorizedEntryPoint.commence(
                request,
                response,
                new InsufficientAuthenticationException("Authentication required")
        );

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString()).contains("\"errorCode\":\"UNAUTHORIZED\"");
        assertThat(response.getContentAsString()).contains("\"message\":\"Authentication required\"");
    }
}
