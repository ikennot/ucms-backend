package com.ucms_backend.service;

import com.ucms_backend.model.enums.TicketStatus;
import com.ucms_backend.repository.TicketAttachmentRepository;
import com.ucms_backend.repository.TicketResponseRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class TicketUrgencyScoringServiceTest {

    @Mock
    private TicketResponseRepository ticketResponseRepository;

    @Mock
    private TicketAttachmentRepository ticketAttachmentRepository;

    @Test
    void parseAndValidateGeminiPayload_validJson_returnsExpectedEvaluation() throws Exception {
        TicketUrgencyScoringService service = new TicketUrgencyScoringService(
                ticketResponseRepository,
                ticketAttachmentRepository,
                "dummy",
                "gemini-2.5-flash",
                4000
        );
        TicketUrgencyInput input = sampleInput();

        String payload = """
                {
                  "urgent": true,
                  "score": 84,
                  "priorityLevel": "CRITICAL",
                  "confidence": 0.91,
                  "reason": "Direct threat language and imminent risk",
                  "signals": ["explicit harm threat"]
                }
                """;

        TicketUrgencyEvaluation evaluation = service.parseAndValidateGeminiPayload(payload, input);

        assertTrue(evaluation.urgent());
        assertTrue(evaluation.score() >= 84);
        assertEquals("CRITICAL", evaluation.priorityLevel());
        assertEquals(0.91, evaluation.confidence(), 0.0001);
        assertFalse(evaluation.needsHumanReview());
    }

    @Test
    void parseAndValidateGeminiPayload_unsupportedLanguage_setsHumanReview() throws Exception {
        TicketUrgencyScoringService service = new TicketUrgencyScoringService(
                ticketResponseRepository,
                ticketAttachmentRepository,
                "dummy",
                "gemini-2.5-flash",
                4000
        );
        TicketUrgencyInput input = sampleInput();

        String payload = """
                {
                  "unsupportedLanguage": true,
                  "urgent": false,
                  "score": 30,
                  "priorityLevel": "LOW",
                  "confidence": 0.2,
                  "reason": "Unknown language"
                }
                """;

        TicketUrgencyEvaluation evaluation = service.parseAndValidateGeminiPayload(payload, input);

        assertTrue(evaluation.needsHumanReview());
        assertTrue(evaluation.score() >= 50);
    }

    @Test
    void applyFallback_criticalKeyword_forcesCriticalUrgent() {
        TicketUrgencyScoringService service = new TicketUrgencyScoringService(
                ticketResponseRepository,
                ticketAttachmentRepository,
                "",
                "gemini-2.5-flash",
                4000
        );
        TicketUrgencyInput input = new TicketUrgencyInput(
                "Need help now",
                "May nagsabi na sasaktan ako sa campus bukas",
                "Safety and Conduct",
                TicketStatus.PENDING,
                LocalDateTime.now(ZoneOffset.UTC).minusHours(1),
                null,
                0L,
                0L,
                null
        );

        TicketUrgencyEvaluation evaluation = service.applyFallback(input, "Gemini unavailable");

        assertTrue(evaluation.urgent());
        assertEquals("CRITICAL", evaluation.priorityLevel());
        assertEquals(95, evaluation.score());
    }

    @Test
    void applyFallback_pendingAgedWithoutResponse_marksHighUrgency() {
        TicketUrgencyScoringService service = new TicketUrgencyScoringService(
                ticketResponseRepository,
                ticketAttachmentRepository,
                "",
                "gemini-2.5-flash",
                4000
        );
        TicketUrgencyInput input = new TicketUrgencyInput(
                "Repeated unresolved issue",
                "No update for 3 days",
                "Academic",
                TicketStatus.PENDING,
                LocalDateTime.now(ZoneOffset.UTC).minusHours(72),
                null,
                0L,
                1L,
                null
        );

        TicketUrgencyEvaluation evaluation = service.applyFallback(input, "Gemini timeout");

        assertTrue(evaluation.urgent());
        assertTrue(evaluation.score() >= 60);
    }

    @Test
    void applyFallback_bullyingKeyword_marksCriticalUrgency() {
        TicketUrgencyScoringService service = new TicketUrgencyScoringService(
                ticketResponseRepository,
                ticketAttachmentRepository,
                "",
                "gemini-2.5-flash",
                4000
        );
        TicketUrgencyInput input = new TicketUrgencyInput(
                "Binubully ako sa classroom",
                "Palagi nila akong binubully at hinaharass after class",
                "Safety and Conduct",
                TicketStatus.PENDING,
                LocalDateTime.now(ZoneOffset.UTC).minusHours(4),
                null,
                0L,
                0L,
                null
        );

        TicketUrgencyEvaluation evaluation = service.applyFallback(input, "Gemini unavailable");

        assertTrue(evaluation.urgent());
        assertEquals("CRITICAL", evaluation.priorityLevel());
        assertTrue(evaluation.score() >= 80);
    }

    @Test
    void healthCheck_withoutApiKey_returnsNotOk() {
        TicketUrgencyScoringService service = new TicketUrgencyScoringService(
                ticketResponseRepository,
                ticketAttachmentRepository,
                "",
                "gemini-2.5-flash",
                4000
        );

        Map<String, Object> result = service.healthCheck();

        assertFalse((Boolean) result.get("ok"));
        assertEquals("gemini", result.get("provider"));
    }

    private TicketUrgencyInput sampleInput() {
        return new TicketUrgencyInput(
                "Threatening messages from classmate",
                "Classmate sent threats last night",
                "Safety and Conduct",
                TicketStatus.PENDING,
                LocalDateTime.now(ZoneOffset.UTC).minusHours(3),
                null,
                0L,
                1L,
                null
        );
    }
}
