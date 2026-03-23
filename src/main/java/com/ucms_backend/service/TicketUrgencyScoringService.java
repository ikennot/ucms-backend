package com.ucms_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ucms_backend.model.entity.Ticket;
import com.ucms_backend.model.entity.TicketResponse;
import com.ucms_backend.model.enums.TicketStatus;
import com.ucms_backend.repository.TicketAttachmentRepository;
import com.ucms_backend.repository.TicketResponseRepository;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class TicketUrgencyScoringService {

    private static final Logger log = LoggerFactory.getLogger(TicketUrgencyScoringService.class);
    private static final Set<String> CRITICAL_KEYWORDS = Set.of(
            "threat", "kill", "hurt", "violence", "weapon", "self-harm", "suicide",
            "sasaktan", "saktan kita", "papatayin", "patayin kita", "hulga", "pananakot",
            "papatay", "baril", "patalim", "tatagain", "barilin", "bomba",
            "patyon", "pusilon", "ti umay", "bunal"
    );
    private static final Set<String> HARASSMENT_KEYWORDS = Set.of(
            "bully", "bullying", "binubully", "binu-bully", "binubuli", "nambubully",
            "harass", "harassment", "hina-harass", "hinaharass", "ginugulo", "ginugulo ako",
            "pambubully", "pang-aapi", "inaapi", "intimidate", "intimidation",
            "abuse", "abused", "abusive", "inaabuso", "inabuso", "pang-aabuso", "sexual abuse"
    );

    private final RestClient geminiClient;
    private final ObjectMapper objectMapper;
    private final TicketResponseRepository ticketResponseRepository;
    private final TicketAttachmentRepository ticketAttachmentRepository;
    private final String apiKey;
    private final String model;

    public TicketUrgencyScoringService(
            TicketResponseRepository ticketResponseRepository,
            TicketAttachmentRepository ticketAttachmentRepository,
            @Value("${gemini.api-key:${GEMINI_API_KEY:}}") String apiKey,
            @Value("${gemini.model:${GEMINI_MODEL:gemini-2.5-flash}}") String model,
            @Value("${gemini.timeout-ms:${GEMINI_TIMEOUT_MS:4000}}") int timeoutMs
    ) {
        this.ticketResponseRepository = ticketResponseRepository;
        this.ticketAttachmentRepository = ticketAttachmentRepository;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.objectMapper = new ObjectMapper();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);
        this.geminiClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .requestFactory(requestFactory)
                .build();
    }

    public TicketUrgencyEvaluation evaluate(Ticket ticket, String categoryName) {
        TicketUrgencyInput input = buildInput(ticket, categoryName);

        if (apiKey.isBlank()) {
            return applyFallback(input, "Gemini key missing");
        }

        try {
            String modelReply = callGemini(input);
            return parseAndValidateGeminiPayload(modelReply, input);
        } catch (Exception ex) {
            log.warn("Gemini urgency scoring failed, using fallback: {}", ex.getMessage());
            return applyFallback(input, "Gemini unavailable or invalid response");
        }
    }

    public Map<String, Object> healthCheck() {
        if (apiKey.isBlank()) {
            return Map.of(
                    "ok", false,
                    "provider", "gemini",
                    "reason", "GEMINI_API_KEY is missing"
            );
        }

        TicketUrgencyInput probe = new TicketUrgencyInput(
                "Health check probe",
                "Test urgency classification connectivity.",
                "System",
                TicketStatus.PENDING,
                LocalDateTime.now(ZoneOffset.UTC).minusMinutes(5),
                null,
                0L,
                0L,
                0
        );

        try {
            String raw = callGemini(probe);
            TicketUrgencyEvaluation evaluation = parseAndValidateGeminiPayload(raw, probe);
            return Map.of(
                    "ok", true,
                    "provider", "gemini",
                    "model", model,
                    "score", evaluation.score(),
                    "priorityLevel", evaluation.priorityLevel(),
                    "confidence", evaluation.confidence()
            );
        } catch (Exception ex) {
            return Map.of(
                    "ok", false,
                    "provider", "gemini",
                    "model", model,
                    "reason", ex.getMessage() == null ? "Gemini check failed" : ex.getMessage()
            );
        }
    }

    TicketUrgencyEvaluation parseAndValidateGeminiPayload(String rawModelJson, TicketUrgencyInput input) throws Exception {
        JsonNode node = objectMapper.readTree(cleanJson(rawModelJson));

        boolean unsupportedLanguage = node.path("unsupportedLanguage").asBoolean(false);
        if (unsupportedLanguage) {
            TicketUrgencyEvaluation keywordEvaluation = applyFallback(input, "Unsupported language; routed to human review");
            if (keywordEvaluation.score() >= 80) {
                return new TicketUrgencyEvaluation(
                        true,
                        keywordEvaluation.score(),
                        keywordEvaluation.priorityLevel(),
                        0.55,
                        "Potentially critical content detected with unsupported language; requires immediate human review.",
                        keywordEvaluation.signals(),
                        LocalDateTime.now(ZoneOffset.UTC),
                        true
                );
            }
            return new TicketUrgencyEvaluation(
                    false,
                    50,
                    "LOW",
                    0.0,
                    "Language outside English/Philippine dialect policy. Routed for human review.",
                    "Unsupported language; manual assessment required",
                    LocalDateTime.now(ZoneOffset.UTC),
                    true
            );
        }

        int rawScore = node.path("score").asInt(-1);
        if (rawScore < 0) {
            throw new IllegalArgumentException("Missing score from Gemini response");
        }
        int score = clamp(rawScore, 0, 100);

        String priority = normalizePriority(node.path("priorityLevel").asText(null), score);
        double confidence = clamp(node.path("confidence").asDouble(0.65), 0.0, 1.0);
        String reason = truncate(node.path("reason").asText("Urgency scored by Gemini"), 500);
        String signals = parseSignals(node.path("signals"));

        boolean urgent = node.has("urgent")
                ? node.path("urgent").asBoolean(false)
                : score >= 60 || "HIGH".equals(priority) || "CRITICAL".equals(priority);

        TicketUrgencyEvaluation modelEvaluation = new TicketUrgencyEvaluation(
                urgent,
                score,
                priority,
                confidence,
                reason,
                signals,
                LocalDateTime.now(ZoneOffset.UTC),
                false
        );
        return applyKeywordOverrides(input, modelEvaluation);
    }

    TicketUrgencyEvaluation applyFallback(TicketUrgencyInput input, String source) {
        String combined = ((input.title() == null ? "" : input.title()) + " "
                + (input.description() == null ? "" : input.description()))
                .toLowerCase(Locale.ROOT);

        for (String keyword : CRITICAL_KEYWORDS) {
            if (combined.contains(keyword)) {
                return new TicketUrgencyEvaluation(
                        true,
                        95,
                        "CRITICAL",
                        0.70,
                        "Critical safety keyword detected in ticket content.",
                        "Explicit harm or threat phrase detected",
                        LocalDateTime.now(ZoneOffset.UTC),
                        false
                );
            }
        }

        for (String keyword : HARASSMENT_KEYWORDS) {
            if (combined.contains(keyword)) {
                return new TicketUrgencyEvaluation(
                        true,
                        90,
                        "CRITICAL",
                        0.78,
                        "Bullying, harassment, or abuse signal detected in ticket content.",
                        "Bullying/harassment/abuse phrase detected",
                        LocalDateTime.now(ZoneOffset.UTC),
                        false
                );
            }
        }

        long ageHours = 0;
        if (input.createdAt() != null) {
            ageHours = Math.max(0, Duration.between(input.createdAt(), LocalDateTime.now(ZoneOffset.UTC)).toHours());
        }

        int score;
        if (input.status() == TicketStatus.PENDING || input.status() == TicketStatus.IN_PROGRESS) {
            if (ageHours >= 72) {
                score = 78;
            } else if (ageHours >= 48) {
                score = 64;
            } else if (ageHours >= 24) {
                score = 52;
            } else {
                score = 40;
            }

            if (input.lastAdminResponseAt() == null && ageHours >= 36) {
                score += 8;
            }
            if (input.attachmentCount() != null && input.attachmentCount() >= 3) {
                score += 8;
            }
        } else if (input.status() == TicketStatus.RESOLVED) {
            score = 22;
        } else {
            score = 10;
        }

        score = clamp(score, 0, 100);
        String priority = normalizePriority(null, score);
        boolean urgent = score >= 60 || "HIGH".equals(priority) || "CRITICAL".equals(priority);

        TicketUrgencyEvaluation fallbackEvaluation = new TicketUrgencyEvaluation(
                urgent,
                score,
                priority,
                0.58,
                truncate("Fallback urgency scoring applied: " + source, 500),
                deriveFallbackSignals(input, ageHours),
                LocalDateTime.now(ZoneOffset.UTC),
                false
        );
        return applyKeywordOverrides(input, fallbackEvaluation);
    }

    private TicketUrgencyEvaluation applyKeywordOverrides(TicketUrgencyInput input, TicketUrgencyEvaluation base) {
        String combined = ((input.title() == null ? "" : input.title()) + " "
                + (input.description() == null ? "" : input.description()))
                .toLowerCase(Locale.ROOT);

        for (String keyword : CRITICAL_KEYWORDS) {
            if (combined.contains(keyword)) {
                return new TicketUrgencyEvaluation(
                        true,
                        Math.max(base.score(), 95),
                        "CRITICAL",
                        Math.max(base.confidence(), 0.75),
                        "Critical safety keyword detected in ticket content.",
                        "Explicit harm or threat phrase detected",
                        LocalDateTime.now(ZoneOffset.UTC),
                        base.needsHumanReview()
                );
            }
        }

        for (String keyword : HARASSMENT_KEYWORDS) {
            if (combined.contains(keyword)) {
                return new TicketUrgencyEvaluation(
                        true,
                        Math.max(base.score(), 90),
                        "CRITICAL",
                        Math.max(base.confidence(), 0.78),
                        "Bullying, harassment, or abuse signal detected in ticket content.",
                        "Bullying/harassment/abuse phrase detected",
                        LocalDateTime.now(ZoneOffset.UTC),
                        base.needsHumanReview()
                );
            }
        }

        return base;
    }

    private TicketUrgencyInput buildInput(Ticket ticket, String categoryName) {
        Long responseCount = 0L;
        Long attachmentCount = 0L;
        LocalDateTime lastAdminResponseAt = null;

        if (ticket.getId() != null) {
            responseCount = ticketResponseRepository.countByTicketId(ticket.getId());
            attachmentCount = ticketAttachmentRepository.countByTicketId(ticket.getId());
            Optional<TicketResponse> latestResponse = ticketResponseRepository
                    .findTopByTicketIdOrderByCreatedAtDescIdDesc(ticket.getId());
            lastAdminResponseAt = latestResponse.map(TicketResponse::getCreatedAt).orElse(null);
        }

        return new TicketUrgencyInput(
                sanitizeText(ticket.getTitle()),
                sanitizeText(ticket.getDescription()),
                categoryName,
                ticket.getStatus() == null ? TicketStatus.PENDING : ticket.getStatus(),
                ticket.getCreatedAt(),
                lastAdminResponseAt,
                responseCount,
                attachmentCount,
                null
        );
    }

    private String callGemini(TicketUrgencyInput input) throws Exception {
        String systemInstruction = "You classify urgency for university concern tickets. "
                + "Return valid JSON only. Inputs may include English, Tagalog, and Philippine languages/dialects "
                + "(including Cebuano, Ilocano, Hiligaynon, Waray, Kapampangan, Bicolano, Pangasinan, Tausug, "
                + "Maranao, Maguindanaon, and Chavacano), including mixed-language text (Taglish/code-switching). "
                + "Detect urgency across these and do not penalize grammar, spelling, slang, or dialect variation. "
                + "If language is outside English or Philippine languages/dialects, set unsupportedLanguage=true and "
                + "keep other fields best-effort for human review.";

        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put("title", input.title());
        requestInput.put("description", input.description());
        requestInput.put("category", input.categoryName());
        requestInput.put("status", input.status() == null ? null : input.status().name());
        requestInput.put("createdAt", input.createdAt() == null ? null : input.createdAt().toString());
        requestInput.put("lastAdminResponseAt", input.lastAdminResponseAt() == null ? null : input.lastAdminResponseAt().toString());
        requestInput.put("adminResponseCount", input.adminResponseCount());
        requestInput.put("attachmentCount", input.attachmentCount());
        requestInput.put("followUpCount", input.followUpCount());

        Map<String, Object> outputSchema = Map.of(
                "urgent", "boolean",
                "score", "integer 0-100",
                "priorityLevel", "MUTED|LOW|HIGH|CRITICAL",
                "confidence", "number 0-1",
                "reason", "string",
                "signals", List.of("string"),
                "recommendedSlaHours", "integer (optional)",
                "unsupportedLanguage", "boolean"
        );

        String userPayload = objectMapper.writeValueAsString(Map.of(
                "input", requestInput,
                "required_output_schema", outputSchema
        ));

        Map<String, Object> requestBody = Map.of(
                "system_instruction", Map.of(
                        "parts", List.of(Map.of("text", systemInstruction))
                ),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", userPayload))
                )),
                "generationConfig", Map.of(
                        "temperature", 0.1,
                        "responseMimeType", "application/json"
                )
        );

        String rawResponse = geminiClient.post()
                .uri("/v1beta/models/{model}:generateContent?key={key}", model, apiKey)
                .body(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    throw new IllegalStateException("Gemini error " + response.getStatusCode() + ": " + body);
                })
                .body(String.class);

        if (rawResponse == null || rawResponse.isBlank()) {
            throw new IllegalStateException("Gemini returned empty response");
        }

        JsonNode root = objectMapper.readTree(rawResponse);
        JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (textNode.isMissingNode() || textNode.asText().isBlank()) {
            throw new IllegalStateException("Gemini response missing content text");
        }
        return textNode.asText();
    }

    private String cleanJson(String jsonText) {
        String trimmed = jsonText == null ? "" : jsonText.trim();
        if (trimmed.startsWith("```") && trimmed.endsWith("```")) {
            int firstNewLine = trimmed.indexOf('\n');
            if (firstNewLine > 0) {
                trimmed = trimmed.substring(firstNewLine + 1, trimmed.length() - 3).trim();
            }
        }
        return trimmed;
    }

    private String parseSignals(JsonNode signalsNode) {
        if (signalsNode == null || !signalsNode.isArray() || signalsNode.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (JsonNode signal : signalsNode) {
            if (signal == null || signal.asText().isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append("; ");
            }
            builder.append(signal.asText().trim());
        }
        String combined = builder.toString().trim();
        return combined.isEmpty() ? null : truncate(combined, 1000);
    }

    private String deriveFallbackSignals(TicketUrgencyInput input, long ageHours) {
        String statusName = input.status() == null ? "UNKNOWN" : input.status().name();
        if (input.lastAdminResponseAt() == null && ageHours >= 36) {
            return "No admin response yet; status=" + statusName + "; ageHours=" + ageHours;
        }
        return "Rule-based fallback; status=" + statusName + "; ageHours=" + ageHours;
    }

    private String sanitizeText(String text) {
        if (text == null) {
            return null;
        }
        String noEmails = text.replaceAll("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+", "[redacted-email]");
        String noLongDigits = noEmails.replaceAll("\\b\\d{7,}\\b", "[redacted-number]");
        return truncate(noLongDigits, 3000);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private String normalizePriority(String priorityLevel, int score) {
        if (priorityLevel != null) {
            String normalized = priorityLevel.trim().toUpperCase(Locale.ROOT);
            if ("MEDIUM".equals(normalized)) {
                return "LOW";
            }
            if (List.of("MUTED", "LOW", "HIGH", "CRITICAL").contains(normalized)) {
                return normalized;
            }
        }

        if (score >= 80) {
            return "CRITICAL";
        }
        if (score >= 55) {
            return "HIGH";
        }
        if (score >= 25) {
            return "LOW";
        }
        return "MUTED";
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
