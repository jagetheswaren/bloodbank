package com.bloodbank.scheduler;

import com.bloodbank.config.BloodBankProperties;
import com.bloodbank.dto.response.EligibilityResponse;
import com.bloodbank.entity.Donor;
import com.bloodbank.enums.BloodGroup;
import com.bloodbank.enums.Gender;
import com.bloodbank.enums.NotificationType;
import com.bloodbank.repository.DonorRepository;
import com.bloodbank.repository.NotificationLogRepository;
import com.bloodbank.service.DonorService;
import com.bloodbank.service.notification.EmailService;
import com.bloodbank.service.notification.EmailTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DonorEligibilityReminderSchedulerTest {

    @Mock
    private DonorRepository donorRepository;

    @Mock
    private DonorService donorService;

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private EmailTemplateService emailTemplateService;

    @Mock
    private BloodBankProperties bloodBankProperties;

    @InjectMocks
    private DonorEligibilityReminderScheduler scheduler;

    private Donor testDonor;
    private BloodBankProperties.Mail mailProps;

    @BeforeEach
    void setUp() {
        mailProps = new BloodBankProperties.Mail();
        mailProps.setEligibilityRemindersEnabled(true);
        lenient().when(bloodBankProperties.getMail()).thenReturn(mailProps);

        testDonor = Donor.builder()
                .id(1L)
                .donorCode("DNR-TEST01")
                .name("Alex River")
                .email("alex@example.test")
                .phone("+15551234567")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .bloodGroup(BloodGroup.O_POSITIVE)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should send reminder when donor is eligible, has past donation, and was not previously notified")
    void testRunEligibilityReminders_Success() {
        when(donorRepository.findByActiveTrue()).thenReturn(List.of(testDonor));

        LocalDate lastDonation = LocalDate.now().minusDays(95);
        EligibilityResponse response = EligibilityResponse.builder()
                .donorId(1L)
                .donorCode("DNR-TEST01")
                .donorName("Alex River")
                .eligible(true)
                .lastDonationDate(lastDonation)
                .nextEligibleDate(LocalDate.now().minusDays(5))
                .remainingDays(0)
                .build();

        when(donorService.checkEligibility(1L)).thenReturn(response);
        when(notificationLogRepository.existsByNotificationTypeAndRelatedEntityTypeAndRelatedEntityIdAndCreatedAtAfter(
                eq(NotificationType.ELIGIBILITY_REMINDER),
                eq("DONOR"),
                eq(1L),
                any(LocalDateTime.class)
        )).thenReturn(false);

        when(emailTemplateService.renderTemplate(eq("eligibility-reminder"), anyMap())).thenReturn("<html>reminder</html>");

        scheduler.runEligibilityReminders();

        verify(emailService, times(1)).sendEmail(
                eq(NotificationType.ELIGIBILITY_REMINDER),
                eq("alex@example.test"),
                contains("Eligible to Donate Blood Again"),
                anyString(),
                eq("DONOR"),
                eq(1L)
        );
    }

    @Test
    @DisplayName("Should avoid duplicate reminder if already notified since last donation")
    void testRunEligibilityReminders_SkipWhenAlreadyNotified() {
        when(donorRepository.findByActiveTrue()).thenReturn(List.of(testDonor));

        LocalDate lastDonation = LocalDate.now().minusDays(95);
        EligibilityResponse response = EligibilityResponse.builder()
                .donorId(1L)
                .donorCode("DNR-TEST01")
                .donorName("Alex River")
                .eligible(true)
                .lastDonationDate(lastDonation)
                .nextEligibleDate(LocalDate.now().minusDays(5))
                .remainingDays(0)
                .build();

        when(donorService.checkEligibility(1L)).thenReturn(response);
        when(notificationLogRepository.existsByNotificationTypeAndRelatedEntityTypeAndRelatedEntityIdAndCreatedAtAfter(
                eq(NotificationType.ELIGIBILITY_REMINDER),
                eq("DONOR"),
                eq(1L),
                any(LocalDateTime.class)
        )).thenReturn(true);

        scheduler.runEligibilityReminders();

        verify(emailService, never()).sendEmail(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should skip when donor is ineligible")
    void testRunEligibilityReminders_SkipWhenIneligible() {
        when(donorRepository.findByActiveTrue()).thenReturn(List.of(testDonor));

        EligibilityResponse response = EligibilityResponse.builder()
                .donorId(1L)
                .donorCode("DNR-TEST01")
                .donorName("Alex River")
                .eligible(false)
                .lastDonationDate(LocalDate.now().minusDays(30))
                .nextEligibleDate(LocalDate.now().plusDays(60))
                .remainingDays(60)
                .build();

        when(donorService.checkEligibility(1L)).thenReturn(response);

        scheduler.runEligibilityReminders();

        verify(emailService, never()).sendEmail(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should skip first-time eligible donor with no previous donation history")
    void testRunEligibilityReminders_SkipFirstTimeDonor() {
        when(donorRepository.findByActiveTrue()).thenReturn(List.of(testDonor));

        EligibilityResponse response = EligibilityResponse.builder()
                .donorId(1L)
                .donorCode("DNR-TEST01")
                .donorName("Alex River")
                .eligible(true)
                .lastDonationDate(null)
                .nextEligibleDate(LocalDate.now())
                .remainingDays(0)
                .build();

        when(donorService.checkEligibility(1L)).thenReturn(response);

        scheduler.runEligibilityReminders();

        verify(emailService, never()).sendEmail(any(), any(), any(), any(), any(), any());
    }
}
