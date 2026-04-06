package com.smarthire.candidate_service.service;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    public String uploadResume(MultipartFile file, UUID candidateId) {
        try {
            String extension = getExtension(file.getOriginalFilename());
            String objectName = "resumes/" + candidateId + "/" + UUID.randomUUID() + extension;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("Uploaded resume for candidate {}: {}", candidateId, objectName);
            return objectName;

        } catch (Exception e) {
            log.error("Failed to upload resume for candidate {}: {}", candidateId, e.getMessage());
            throw new RuntimeException("Failed to upload resume: " + e.getMessage());
        }
    }

    public String generatePresignedUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .method(Method.GET)
                            .expiry(1, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for {}: {}", objectName, e.getMessage());
            return null;
        }
    }

    public void deleteResume(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
            log.info("Deleted resume: {}", objectName);
        } catch (Exception e) {
            log.warn("Failed to delete resume {}: {}", objectName, e.getMessage());
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".pdf";
        return filename.substring(filename.lastIndexOf("."));
    }
}
