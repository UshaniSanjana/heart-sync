package com.heartsync.ecg.repository;

import com.heartsync.ecg.entity.EcgRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EcgRecordRepository extends JpaRepository<EcgRecord, String> {
    List<EcgRecord> findByPatientIdOrderByCreatedAtDesc(String patientId);
}
