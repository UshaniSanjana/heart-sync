package com.heartsync.ai.repository;

import com.heartsync.ai.document.AngiogramResult;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AngiogramResultRepository extends MongoRepository<AngiogramResult, String> {
    Optional<AngiogramResult> findTopByPatientIdOrderByIdDesc(String patientId);
}
