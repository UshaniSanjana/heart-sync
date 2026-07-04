package com.heartsync.ai.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "ai_analysis_outbox")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAnalysisOutboxMessage {
    @Id
    private String requestId;

    @Indexed(unique = true)
    private String idempotencyKey;

    private String routingKey;
    private String payload;
    private Status status;
    private int attempts;
    private Instant createdAt;
    private Instant publishedAt;
    private String lastError;

    public enum Status {
        PENDING,
        PUBLISHED,
        FAILED
    }
}
