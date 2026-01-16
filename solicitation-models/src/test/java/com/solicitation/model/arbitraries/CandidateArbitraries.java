package com.solicitation.model.arbitraries;

import com.solicitation.model.*;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.time.api.DateTimes;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Arbitrary generators for Candidate model and related classes.
 * Used for property-based testing with jqwik.
 */
public class CandidateArbitraries {
    
    /**
     * Generates arbitrary Context instances with valid type and id.
     */
    public static Arbitrary<Context> contexts() {
        Arbitrary<String> types = Arbitraries.of("marketplace", "program", "vertical", "category");
        Arbitrary<String> ids = Arbitraries.strings()
            .alpha()
            .numeric()
            .ofMinLength(1)
            .ofMaxLength(20);
        
        return Combinators.combine(types, ids)
            .as((type, id) -> Context.builder()
                .type(type)
                .id(id)
                .build());
    }
    
    /**
     * Generates arbitrary Subject instances with valid type and id.
     */
    public static Arbitrary<Subject> subjects() {
        Arbitrary<String> types = Arbitraries.of("product", "video", "track", "service", "event", "experience");
        Arbitrary<String> ids = Arbitraries.strings()
            .alpha()
            .numeric()
            .ofMinLength(1)
            .ofMaxLength(30);
        Arbitrary<Map<String, Object>> metadata = Arbitraries.maps(
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10),
            Arbitraries.of("value1", "value2", 123, true)
        ).ofMaxSize(5);
        
        return Combinators.combine(types, ids, metadata)
            .as((type, id, meta) -> Subject.builder()
                .type(type)
                .id(id)
                .metadata(meta.isEmpty() ? null : meta)
                .build());
    }
    
    /**
     * Generates arbitrary Score instances with valid constraints.
     */
    public static Arbitrary<Score> scores() {
        Arbitrary<String> modelIds = Arbitraries.strings()
            .alpha()
            .ofMinLength(1)
            .ofMaxLength(20);
        Arbitrary<Double> values = Arbitraries.doubles()
            .between(0.0, 1.0);
        Arbitrary<Double> confidences = Arbitraries.doubles()
            .between(0.0, 1.0);
        Arbitrary<Instant> timestamps = DateTimes.instants()
            .atTheEarliest(Instant.parse("2020-01-01T00:00:00Z"))
            .atTheLatest(Instant.parse("2030-12-31T23:59:59Z"));
        
        return Combinators.combine(modelIds, values, confidences, timestamps)
            .as((modelId, value, confidence, timestamp) -> Score.builder()
                .modelId(modelId)
                .value(value)
                .confidence(confidence)
                .timestamp(timestamp)
                .build());
    }
    
    /**
     * Generates arbitrary CandidateAttributes instances.
     */
    public static Arbitrary<CandidateAttributes> candidateAttributes() {
        Arbitrary<Instant> eventDates = DateTimes.instants()
            .atTheEarliest(Instant.parse("2020-01-01T00:00:00Z"))
            .atTheLatest(Instant.parse("2030-12-31T23:59:59Z"));
        Arbitrary<Instant> deliveryDates = DateTimes.instants()
            .atTheEarliest(Instant.parse("2020-01-01T00:00:00Z"))
            .atTheLatest(Instant.parse("2030-12-31T23:59:59Z"))
            .injectNull(0.3);
        Arbitrary<String> timingWindows = Arbitraries.of("immediate", "1-day", "3-day", "7-day")
            .injectNull(0.3);
        Arbitrary<Double> orderValues = Arbitraries.doubles()
            .between(0.0, 10000.0)
            .injectNull(0.3);
        Arbitrary<Boolean> mediaEligible = Arbitraries.of(true, false)
            .injectNull(0.2);
        Arbitrary<Map<String, Boolean>> channelEligibility = Arbitraries.maps(
            Arbitraries.of("email", "in-app", "push", "sms", "voice"),
            Arbitraries.of(true, false)
        ).ofMinSize(1).ofMaxSize(5);
        
        return Combinators.combine(
            eventDates, deliveryDates, timingWindows, orderValues, 
            mediaEligible, channelEligibility
        ).as((eventDate, deliveryDate, timingWindow, orderValue, media, channels) -> 
            CandidateAttributes.builder()
                .eventDate(eventDate)
                .deliveryDate(deliveryDate)
                .timingWindow(timingWindow)
                .orderValue(orderValue)
                .mediaEligible(media)
                .channelEligibility(channels)
                .build()
        );
    }
    
    /**
     * Generates arbitrary CandidateMetadata instances.
     */
    public static Arbitrary<CandidateMetadata> candidateMetadata() {
        Arbitrary<Instant> createdAt = DateTimes.instants()
            .atTheEarliest(Instant.parse("2020-01-01T00:00:00Z"))
            .atTheLatest(Instant.parse("2030-12-31T23:59:59Z"));
        Arbitrary<Long> versions = Arbitraries.longs()
            .between(1L, 1000L);
        Arbitrary<String> connectorIds = Arbitraries.strings()
            .alpha()
            .ofMinLength(1)
            .ofMaxLength(20);
        Arbitrary<String> workflowIds = Arbitraries.strings()
            .alpha()
            .numeric()
            .ofMinLength(1)
            .ofMaxLength(30);
        
        return Combinators.combine(createdAt, versions, connectorIds, workflowIds)
            .as((created, version, connectorId, workflowId) -> {
                Instant updated = created.plusSeconds(Arbitraries.longs().between(0, 86400).sample());
                Instant expires = updated.plusSeconds(Arbitraries.longs().between(86400, 2592000).sample());
                
                return CandidateMetadata.builder()
                    .createdAt(created)
                    .updatedAt(updated)
                    .expiresAt(expires)
                    .version(version)
                    .sourceConnectorId(connectorId)
                    .workflowExecutionId(workflowId)
                    .build();
            });
    }
    
    /**
     * Generates arbitrary valid Candidate instances with all required fields.
     */
    public static Arbitrary<Candidate> validCandidates() {
        Arbitrary<String> customerIds = Arbitraries.strings()
            .alpha()
            .numeric()
            .ofMinLength(1)
            .ofMaxLength(30);
        Arbitrary<List<Context>> contextList = contexts()
            .list()
            .ofMinSize(1)
            .ofMaxSize(5);
        Arbitrary<Map<String, Score>> scoresMap = Arbitraries.maps(
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10),
            scores()
        ).ofMaxSize(5);
        
        return Combinators.combine(
            customerIds,
            contextList,
            subjects(),
            scoresMap,
            candidateAttributes(),
            candidateMetadata()
        ).as((customerId, context, subject, scores, attributes, metadata) ->
            Candidate.builder()
                .customerId(customerId)
                .context(context)
                .subject(subject)
                .scores(scores.isEmpty() ? null : scores)
                .attributes(attributes)
                .metadata(metadata)
                .rejectionHistory(null)
                .build()
        );
    }
    
    /**
     * Generates Candidate instances with missing required fields for validation testing.
     */
    public static Arbitrary<Candidate> candidatesWithMissingFields() {
        return Arbitraries.of(
            // Missing customerId
            Candidate.builder()
                .customerId(null)
                .context(List.of(Context.builder().type("marketplace").id("US").build()))
                .subject(Subject.builder().type("product").id("123").build())
                .attributes(CandidateAttributes.builder()
                    .eventDate(Instant.now())
                    .channelEligibility(Map.of("email", true))
                    .build())
                .metadata(CandidateMetadata.builder()
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(86400))
                    .version(1L)
                    .sourceConnectorId("test")
                    .workflowExecutionId("test-123")
                    .build())
                .build(),
            
            // Missing context (empty list)
            Candidate.builder()
                .customerId("customer123")
                .context(List.of())
                .subject(Subject.builder().type("product").id("123").build())
                .attributes(CandidateAttributes.builder()
                    .eventDate(Instant.now())
                    .channelEligibility(Map.of("email", true))
                    .build())
                .metadata(CandidateMetadata.builder()
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(86400))
                    .version(1L)
                    .sourceConnectorId("test")
                    .workflowExecutionId("test-123")
                    .build())
                .build(),
            
            // Missing subject
            Candidate.builder()
                .customerId("customer123")
                .context(List.of(Context.builder().type("marketplace").id("US").build()))
                .subject(null)
                .attributes(CandidateAttributes.builder()
                    .eventDate(Instant.now())
                    .channelEligibility(Map.of("email", true))
                    .build())
                .metadata(CandidateMetadata.builder()
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(86400))
                    .version(1L)
                    .sourceConnectorId("test")
                    .workflowExecutionId("test-123")
                    .build())
                .build(),
            
            // Missing attributes
            Candidate.builder()
                .customerId("customer123")
                .context(List.of(Context.builder().type("marketplace").id("US").build()))
                .subject(Subject.builder().type("product").id("123").build())
                .attributes(null)
                .metadata(CandidateMetadata.builder()
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(86400))
                    .version(1L)
                    .sourceConnectorId("test")
                    .workflowExecutionId("test-123")
                    .build())
                .build(),
            
            // Missing metadata
            Candidate.builder()
                .customerId("customer123")
                .context(List.of(Context.builder().type("marketplace").id("US").build()))
                .subject(Subject.builder().type("product").id("123").build())
                .attributes(CandidateAttributes.builder()
                    .eventDate(Instant.now())
                    .channelEligibility(Map.of("email", true))
                    .build())
                .metadata(null)
                .build()
        );
    }
}
