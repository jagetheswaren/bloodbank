package com.bloodbank.service.notification;

import com.bloodbank.config.BloodBankProperties;
import com.bloodbank.entity.NotificationLog;
import com.bloodbank.enums.NotificationStatus;
import com.bloodbank.enums.NotificationType;
import com.bloodbank.repository.NotificationLogRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final BloodBankProperties bloodBankProperties;
    private final NotificationLogRepository notificationLogRepository;

    /**
     * Sends an HTML email notification asynchronously.
     * Records an audit log entry in NotificationLog table.
     * Never throws exceptions out to caller to protect business transactions.
     */
    @Async
    public void sendEmail(
            NotificationType type,
            String recipient,
            String subject,
            String htmlContent,
            String relatedEntityType,
            Long relatedEntityId
    ) {
        boolean mailEnabled = bloodBankProperties.getMail().isEnabled();
        String fromAddress = bloodBankProperties.getMail().getFrom();

        NotificationLog logEntry = NotificationLog.builder()
                .notificationType(type)
                .recipient(recipient)
                .subject(subject)
                .relatedEntityType(relatedEntityType)
                .relatedEntityId(relatedEntityId)
                .createdAt(LocalDateTime.now())
                .build();

        if (!mailEnabled) {
            log.info("[EMAIL SKIPPED] Email notifications disabled (bloodbank.mail.enabled=false). Type={}, To={}, Subject={}",
                    type, recipient, subject);
            logEntry.setStatus(NotificationStatus.SKIPPED);
            logEntry.setFailureReason("Mail delivery disabled via bloodbank.mail.enabled=false");
            notificationLogRepository.save(logEntry);
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("[EMAIL FAILED] JavaMailSender bean is unavailable. Type={}, To={}, Subject={}",
                    type, recipient, subject);
            logEntry.setStatus(NotificationStatus.FAILED);
            logEntry.setFailureReason("JavaMailSender bean is unavailable in application context");
            notificationLogRepository.save(logEntry);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromAddress, "BloodBank Healthcare Operations");
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("[EMAIL SENT] Successfully delivered email notification: Type={}, To={}, Subject={}",
                    type, recipient, subject);
            logEntry.setStatus(NotificationStatus.SENT);
            logEntry.setSentAt(LocalDateTime.now());
            notificationLogRepository.save(logEntry);

        } catch (Exception ex) {
            log.error("[EMAIL ERROR] Failed to send email: Type={}, To={}, Subject={}, Reason={}",
                    type, recipient, subject, ex.getMessage());
            logEntry.setStatus(NotificationStatus.FAILED);
            logEntry.setFailureReason(ex.getMessage() != null && ex.getMessage().length() > 500
                    ? ex.getMessage().substring(0, 497) + "..."
                    : ex.getMessage());
            notificationLogRepository.save(logEntry);
        }
    }
}
