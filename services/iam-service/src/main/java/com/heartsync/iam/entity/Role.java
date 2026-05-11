package com.heartsync.iam.entity;

/**
 * Clinical roles map directly to what data each person can see.
 * DOCTOR    — full access: patients, ECG, AI results, reports
 * RADIOLOGIST — ECG + AI results (no patient demographics)
 * NURSE     — patient records only
 * ADMIN     — user management only
 */
public enum Role {
    DOCTOR,
    RADIOLOGIST,
    NURSE,
    ADMIN
}
