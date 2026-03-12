package com.ucms_backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SupabaseStorageServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void generateSignedUrl_usesConfiguredExpiryInRequestBody() throws IOException {
        AtomicReference<String> capturedMethod = new AtomicReference<>();
        AtomicReference<String> capturedPath = new AtomicReference<>();
        AtomicReference<String> capturedApiKey = new AtomicReference<>();
        AtomicReference<String> capturedAuthorization = new AtomicReference<>();
        AtomicReference<String> capturedBody = new AtomicReference<>();

        String storagePath = "tickets/1/file.pdf";
        String expectedPath = "/storage/v1/object/sign/ticket-attachments/" + storagePath;

        server.createContext(expectedPath, exchange -> {
            captureRequest(exchange, capturedMethod, capturedPath, capturedApiKey, capturedAuthorization, capturedBody);
            byte[] response = "{\"signedURL\":\"/storage/v1/object/sign/ticket-attachments/tickets/1/file.pdf?token=abc\"}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        SupabaseStorageService service = new SupabaseStorageService(
                baseUrl,
                "service-role-key-value",
                "ticket-attachments",
                7200
        );

        String signedUrl = service.generateSignedUrl(storagePath);

        assertEquals(baseUrl + "/storage/v1/object/sign/ticket-attachments/tickets/1/file.pdf?token=abc", signedUrl);
        assertEquals("POST", capturedMethod.get());
        assertEquals(expectedPath, capturedPath.get());
        assertEquals("service-role-key-value", capturedApiKey.get());
        assertEquals("Bearer service-role-key-value", capturedAuthorization.get());

        Map<String, Object> requestBody = OBJECT_MAPPER.readValue(capturedBody.get(), new TypeReference<>() {
        });
        assertEquals(7200, requestBody.get("expiresIn"));
    }

    private static void captureRequest(
            HttpExchange exchange,
            AtomicReference<String> method,
            AtomicReference<String> path,
            AtomicReference<String> apiKey,
            AtomicReference<String> authorization,
            AtomicReference<String> body
    ) throws IOException {
        method.set(exchange.getRequestMethod());
        path.set(exchange.getRequestURI().getPath());
        apiKey.set(exchange.getRequestHeaders().getFirst("apikey"));
        authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
        body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        assertNotNull(body.get());
    }
}
