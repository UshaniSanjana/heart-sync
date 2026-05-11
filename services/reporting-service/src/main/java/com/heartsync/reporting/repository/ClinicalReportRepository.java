package com.heartsync.reporting.repository;

import com.heartsync.reporting.entity.ClinicalReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClinicalReportRepository extends JpaRepository<ClinicalReport, String> {
    List<ClinicalReport> findByPatientIdOrderByCreatedAtDesc(String patientId);
    Optional<ClinicalReport> findByEcgRecordId(String ecgRecordId);
}
