package com.heartsync.reporting.service;

import io.minio.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

@Service
public class MinioStorageService {

    private final MinioClient minioClient;
    private final String bucket;

    public MinioStorageService(
            @Value("${minio.endpoint}") String endpoint,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey,
            @Value("${minio.bucket:clinical-reports}") String bucket) {

        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        this.bucket = bucket;
        ensureBucketExists();
    }

    public String uploadPdf(String reportId, byte[] pdfBytes) {
        String key = "reports/" + reportId + ".pdf";
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(new ByteArrayInputStream(pdfBytes), pdfBytes.length, -1)
                    .contentType("application/pdf")
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload PDF to MinIO", e);
        }
        return key;
    }

    public byte[] downloadPdf(String key) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .build()).readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to download PDF from MinIO", e);
        }
    }

    private void ensureBucketExists() {
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize MinIO bucket: " + bucket, e);
        }
    }
}
