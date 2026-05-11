package com.heartsync.ai.repository;

import com.heartsync.ai.document.AnalysisResult;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnalysisResultRepository extends MongoRepository<AnalysisResult, String> {
    Optional<AnalysisResult> findByEcgRecordId(String ecgRecordId);
    List<AnalysisResult> findByPatientIdOrderByAnalyzedAtDesc(String patientId);
}
