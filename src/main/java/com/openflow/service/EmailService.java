package com.openflow.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import jakarta.annotation.PostConstruct;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${aws.ses.region:us-east-1}")
    private String awsRegion;

    @Value("${aws.ses.from-email:noreply@openflow.world}")
    private String fromEmail;

    @Value("${aws.ses.enabled:false}")
    private boolean sesEnabled;

    private SesClient sesClient;

    @PostConstruct
    public void init() {
        if (sesEnabled) {
            try {
                sesClient = SesClient.builder()
                        .region(Region.of(awsRegion))
                        .credentialsProvider(DefaultCredentialsProvider.create())
                        .build();
                logger.info("AWS SES client initialized successfully. From email: {}", fromEmail);
            } catch (Exception e) {
                logger.warn("Failed to initialize AWS SES client: {}. Email notifications disabled.", e.getMessage());
                sesEnabled = false;
            }
        } else {
            logger.info("AWS SES is disabled. Email notifications will be logged only.");
        }
    }

    /**
     * Send a notification email.
     */
    public void sendNotificationEmail(String toEmail, String notificationType, String message,
                                       String referenceType, Long referenceId) {
        String subject = getSubjectForNotificationType(notificationType);
        String htmlBody = buildHtmlBody(notificationType, message, referenceType, referenceId);
        String textBody = buildTextBody(notificationType, message, referenceType, referenceId);

        sendEmail(toEmail, subject, htmlBody, textBody);
    }

    /**
     * Send an email using AWS SES.
     */
    public void sendEmail(String toEmail, String subject, String htmlBody, String textBody) {
        if (!sesEnabled || sesClient == null) {
            logger.info("Email (simulated): To={}, Subject={}, Body={}", toEmail, subject, textBody);
            return;
        }

        try {
            Destination destination = Destination.builder()
                    .toAddresses(toEmail)
                    .build();

            Content subjectContent = Content.builder()
                    .data(subject)
                    .charset("UTF-8")
                    .build();

            Content htmlContent = Content.builder()
                    .data(htmlBody)
                    .charset("UTF-8")
                    .build();

            Content textContent = Content.builder()
                    .data(textBody)
                    .charset("UTF-8")
                    .build();

            Body body = Body.builder()
                    .html(htmlContent)
                    .text(textContent)
                    .build();

            Message emailMessage = Message.builder()
                    .subject(subjectContent)
                    .body(body)
                    .build();

            SendEmailRequest request = SendEmailRequest.builder()
                    .source(fromEmail)
                    .destination(destination)
                    .message(emailMessage)
                    .build();

            SendEmailResponse response = sesClient.sendEmail(request);
            logger.info("Email sent successfully to {}. MessageId: {}", toEmail, response.messageId());

        } catch (SesException e) {
            logger.error("Failed to send email to {}: {}", toEmail, e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            logger.error("Unexpected error sending email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String getSubjectForNotificationType(String notificationType) {
        return switch (notificationType) {
            case "TASK_CREATED" -> "[OpenFlow] New Task Created";
            case "TASK_UPDATED" -> "[OpenFlow] Task Updated";
            case "TASK_DELETED" -> "[OpenFlow] Task Deleted";
            case "TASK_MOVED" -> "[OpenFlow] Task Moved";
            case "BOARD_UPDATED" -> "[OpenFlow] Board Updated";
            case "STATUS_CREATED" -> "[OpenFlow] New Status Created";
            case "STATUS_UPDATED" -> "[OpenFlow] Status Updated";
            case "STATUS_DELETED" -> "[OpenFlow] Status Deleted";
            default -> "[OpenFlow] Notification";
        };
    }

    private String buildHtmlBody(String notificationType, String message, String referenceType, Long referenceId) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #82AAFF; color: white; padding: 20px; border-radius: 8px 8px 0 0; }
                    .content { background-color: #f5f5f5; padding: 20px; border-radius: 0 0 8px 8px; }
                    .button { display: inline-block; padding: 10px 20px; background-color: #82AAFF; color: white; 
                              text-decoration: none; border-radius: 5px; margin-top: 15px; }
                    .footer { margin-top: 20px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>OpenFlow Notification</h1>
                    </div>
                    <div class="content">
                        <p>%s</p>
                        <p><strong>Reference:</strong> %s #%d</p>
                        <a href="https://app.openflow.world" class="button">View in OpenFlow</a>
                    </div>
                    <div class="footer">
                        <p>You received this email because you subscribed to notifications for this %s.</p>
                        <p>To manage your notification preferences, visit your OpenFlow settings.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(message, referenceType, referenceId, referenceType.toLowerCase());
    }

    private String buildTextBody(String notificationType, String message, String referenceType, Long referenceId) {
        return """
            OpenFlow Notification
            
            %s
            
            Reference: %s #%d
            
            View in OpenFlow: https://app.openflow.world
            
            ---
            You received this email because you subscribed to notifications for this %s.
            To manage your notification preferences, visit your OpenFlow settings.
            """.formatted(message, referenceType, referenceId, referenceType.toLowerCase());
    }
}

