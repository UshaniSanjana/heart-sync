package com.heartsync.ai.repository;

import com.heartsync.ai.document.ProcessedAiRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProcessedAiRequestRepository extends MongoRepository<ProcessedAiRequest, String> {
}
