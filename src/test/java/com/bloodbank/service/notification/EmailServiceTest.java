package com.bloodbank.service.notification;

import com.bloodbank.config.BloodBankProperties;
import com.bloodbank.entity.NotificationLog;
import com.bloodbank.enums.NotificationStatus;
import com.bloodbank.enums.NotificationType;
import com.bloodbank.repository.NotificationLogRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    private NotificationLogRepository notificationLogRepository;

    private BloodBankProperties properties;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        properties = new BloodBankProperties();
        emailService = new EmailService(mailSenderProvider, properties, notificationLogRepository);
    }

    @Test
    @DisplayName("Should skip sending and log SKIPPED when mail is disabled")
    void testSendEmail_WhenMailDisabled_LogsSkipped() {
        properties.getMail().setEnabled(false);

        emailService.sendEmail(
                NotificationType.DONOR_REGISTRATION,
                "donor@example.test",
                "Registration",
                "<p>Welcome</p>",
                "DONOR",
                1L
        );

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(captor.capture());

        NotificationLog saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.SKIPPED);
        assertThat(saved.getRecipient()).isEqualTo("donor@example.test");
        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("Should successfully send email and log SENT when mail is enabled")
    void testSendEmail_WhenMailEnabled_SendsAndLogsSent() {
        properties.getMail().setEnabled(true);
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendEmail(
                NotificationType.DONATION_RECORDED,
                "donor@example.test",
                "Donation Successful",
                "<p>Thank you</p>",
                "DONATION",
                10L
        );

        verify(mailSender).send(mimeMessage);
        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(captor.capture());

        NotificationLog saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(saved.getSentAt()).isNotNull();
    }

    @Test
    @DisplayName("Should catch MailException gracefully, protect business transaction, and log FAILED")
    void testSendEmail_WhenSmtpThrowsException_LogsFailedAndNeverPropagates() {
        properties.getMail().setEnabled(true);
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("SMTP connection timed out")).when(mailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() -> emailService.sendEmail(
                NotificationType.ISSUE_CONFIRMATION,
                "admin@bloodbank.org",
                "Issue Confirmation",
                "<p>Issued</p>",
                "ISSUE_RECORD",
                5L
        ));

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(captor.capture());

        NotificationLog saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(saved.getFailureReason()).contains("SMTP connection timed out");
    }
}
