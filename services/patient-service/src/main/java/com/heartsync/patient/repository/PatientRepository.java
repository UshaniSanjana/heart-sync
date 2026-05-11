package com.heartsync.patient.repository;

import com.heartsync.patient.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientRepository extends JpaRepository<Patient, String> {

    @Query("SELECT p FROM Patient p WHERE " +
           "LOWER(p.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
           "LOWER(p.lastName)  LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Patient> searchByName(String name);

    List<Patient> findByReferringPhysician(String physician);
}
