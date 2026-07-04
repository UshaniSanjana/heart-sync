package com.heartsync.ecg.repository;

import com.heartsync.ecg.entity.AiAnalysisOutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AiAnalysisOutboxRepository extends JpaRepository<AiAnalysisOutboxMessage, String> {
    List<AiAnalysisOutboxMessage> findTop25ByStatusInOrderByCreatedAtAsc(
            Collection<AiAnalysisOutboxMessage.Status> statuses);
}
