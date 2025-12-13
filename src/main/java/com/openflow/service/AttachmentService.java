package com.openflow.service;

import com.openflow.model.Attachment;
import com.openflow.repository.AttachmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class AttachmentService {

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private S3Service s3Service;

    public List<Attachment> getAttachmentsByTaskId(Long taskId) {
        return attachmentRepository.findByTaskId(taskId);
    }

    public Attachment getAttachmentById(Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found"));
    }

    public Attachment uploadAttachment(Long taskId, Long userId, MultipartFile file) throws IOException {
        if (!s3Service.isEnabled()) {
            throw new RuntimeException("File storage is not enabled");
        }

        String s3Key = s3Service.uploadFile(file, taskId);

        Attachment attachment = new Attachment();
        attachment.setTaskId(taskId);
        attachment.setFileName(file.getOriginalFilename());
        attachment.setOriginalName(file.getOriginalFilename());
        attachment.setContentType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setS3Key(s3Key);
        attachment.setUploadedBy(userId);

        return attachmentRepository.save(attachment);
    }

    public String getDownloadUrl(Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found"));
        return s3Service.getPresignedUrl(attachment.getS3Key());
    }

    @Transactional
    public void deleteAttachment(Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found"));
        s3Service.deleteFile(attachment.getS3Key());
        attachmentRepository.delete(attachment);
    }

    @Transactional
    public void deleteAttachmentsByTaskId(Long taskId) {
        List<Attachment> attachments = attachmentRepository.findByTaskId(taskId);
        for (Attachment attachment : attachments) {
            s3Service.deleteFile(attachment.getS3Key());
        }
        attachmentRepository.deleteByTaskId(taskId);
    }

    public boolean isS3Enabled() {
        return s3Service.isEnabled();
    }
}

