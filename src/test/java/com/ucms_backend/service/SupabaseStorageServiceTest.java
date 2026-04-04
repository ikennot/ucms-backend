package com.ucms_backend.service;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupabaseStorageServiceTest {

    @BeforeEach
    void setUp() {}

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Test
    void generateSignedUrl_usesConfiguredExpiryInRequestBody() throws Exception {
        String baseUrl = "https://supabase.local";
        String storagePath = "tickets/1/file.pdf";
        String expectedPath = "/storage/v1/object/sign/ticket-attachments/" + storagePath;
        Map<String, Object> expectedRequestBody = Map.of("expiresIn", 7200);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(expectedPath)).thenReturn(requestBodySpec);
        when(requestBodySpec.header(eq("apikey"), eq("service-role-key-value"))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(eq("Authorization"), eq("Bearer service-role-key-value"))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(expectedRequestBody)).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(
                "{\"signedURL\":\"/storage/v1/object/sign/ticket-attachments/tickets/1/file.pdf?token=abc\"}"
        );

        SupabaseStorageService service = new SupabaseStorageService(
                baseUrl,
                "service-role-key-value",
                "ticket-attachments",
                7200
        );
        ReflectionTestUtils.setField(service, "restClient", restClient);

        String signedUrl = service.generateSignedUrl(storagePath);

        assertEquals(baseUrl + "/storage/v1/object/sign/ticket-attachments/tickets/1/file.pdf?token=abc", signedUrl);
        verify(requestBodyUriSpec).uri(expectedPath);
        verify(requestBodySpec).header("apikey", "service-role-key-value");
        verify(requestBodySpec).header("Authorization", "Bearer service-role-key-value");
        verify(requestBodySpec).body(expectedRequestBody);
    }
}
