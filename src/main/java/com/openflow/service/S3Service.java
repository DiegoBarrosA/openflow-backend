package com.openflow.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Service
public class S3Service {
    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);

    @Value("${aws.s3.bucket-name:openflow-attachments}")
    private String bucketName;

    @Value("${aws.s3.region:us-east-1}")
    private String awsRegion;

    @Value("${aws.s3.enabled:}")
    private String s3EnabledStr;

    private boolean s3Enabled;
    private S3Client s3Client;
    private S3Presigner s3Presigner;

    @PostConstruct
    public void init() {
        // Parse s3Enabled from string, handling empty values
        s3Enabled = "true".equalsIgnoreCase(s3EnabledStr);
        
        if (s3Enabled) {
            try {
                Region region = Region.of(awsRegion);
                
                // Check for AWS credentials in environment variables
                String accessKeyId = System.getenv("AWS_ACCESS_KEY_ID");
                String secretAccessKey = System.getenv("AWS_SECRET_ACCESS_KEY");
                String sessionToken = System.getenv("AWS_SESSION_TOKEN");
                
                // Use session credentials if session token is present (AWS Academy)
                if (accessKeyId != null && secretAccessKey != null && sessionToken != null && !sessionToken.isEmpty()) {
                    logger.info("Using AWS session credentials (AWS Academy mode)");
                    AwsSessionCredentials sessionCredentials = AwsSessionCredentials.create(
                            accessKeyId, secretAccessKey, sessionToken);
                    StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(sessionCredentials);
                    
                    s3Client = S3Client.builder()
                            .region(region)
                            .credentialsProvider(credentialsProvider)
                            .build();
                    s3Presigner = S3Presigner.builder()
                            .region(region)
                            .credentialsProvider(credentialsProvider)
                            .build();
                } else {
                    // Fall back to default credentials provider
                    logger.info("Using default AWS credentials provider");
                    s3Client = S3Client.builder()
                            .region(region)
                            .credentialsProvider(DefaultCredentialsProvider.create())
                            .build();
                    s3Presigner = S3Presigner.builder()
                            .region(region)
                            .credentialsProvider(DefaultCredentialsProvider.create())
                            .build();
                }
                
                logger.info("AWS S3 client initialized successfully. Bucket: {}", bucketName);
            } catch (Exception e) {
                logger.warn("Failed to initialize AWS S3 client: {}. File storage disabled.", e.getMessage());
                s3Enabled = false;
            }
        } else {
            logger.info("AWS S3 is disabled. File attachments will not be available.");
        }
    }

    public boolean isEnabled() {
        return s3Enabled && s3Client != null;
    }

    public String getBucketName() {
        return bucketName;
    }

    /**
     * Upload a file to S3 for a task and return the S3 key.
     */
    public String uploadFile(MultipartFile file, Long taskId) throws IOException {
        String s3Key = "tasks/" + taskId + "/" + UUID.randomUUID().toString() + getFileExtension(file);
        return uploadToS3(file, s3Key);
    }

    /**
     * Upload a profile picture to S3 and return the S3 key.
     */
    public String uploadProfilePicture(MultipartFile file, Long userId) throws IOException {
        String s3Key = "profiles/" + userId + "/" + UUID.randomUUID().toString() + getFileExtension(file);
        return uploadToS3(file, s3Key);
    }

    private String getFileExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return "";
    }

    private String uploadToS3(MultipartFile file, String s3Key) throws IOException {
        if (!isEnabled()) {
            throw new RuntimeException("S3 storage is not enabled");
        }

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        
        logger.info("File uploaded to S3: {}", s3Key);
        return s3Key;
    }

    /**
     * Generate a presigned URL for downloading a file (valid for 1 hour).
     */
    public String getPresignedUrl(String s3Key) {
        if (!isEnabled()) {
            throw new RuntimeException("S3 storage is not enabled");
        }

        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))
                .getObjectRequest(getRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    /**
     * Delete a file from S3.
     */
    public void deleteFile(String s3Key) {
        if (!isEnabled()) {
            logger.warn("S3 is disabled, cannot delete file: {}", s3Key);
            return;
        }

        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            s3Client.deleteObject(deleteRequest);
            logger.info("File deleted from S3: {}", s3Key);
        } catch (Exception e) {
            logger.error("Failed to delete file from S3: {}", e.getMessage());
        }
    }
}

