package com.heartsync.ai.repository;

import com.heartsync.ai.document.AiAnalysisOutboxMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface AiAnalysisOutboxRepository extends MongoRepository<AiAnalysisOutboxMessage, String> {
    List<AiAnalysisOutboxMessage> findTop25ByStatusInOrderByCreatedAtAsc(
            Collection<AiAnalysisOutboxMessage.Status> statuses);
}
