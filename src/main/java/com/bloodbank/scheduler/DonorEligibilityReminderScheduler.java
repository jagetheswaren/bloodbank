package com.bloodbank.scheduler;

import com.bloodbank.config.BloodBankProperties;
import com.bloodbank.dto.response.EligibilityResponse;
import com.bloodbank.entity.Donor;
import com.bloodbank.enums.NotificationType;
import com.bloodbank.repository.DonorRepository;
import com.bloodbank.repository.NotificationLogRepository;
import com.bloodbank.service.DonorService;
import com.bloodbank.service.notification.EmailService;
import com.bloodbank.service.notification.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "bloodbank.mail.eligibility-reminders-enabled", havingValue = "true")
public class DonorEligibilityReminderScheduler {

    private final DonorRepository donorRepository;
    private final DonorService donorService;
    private final NotificationLogRepository notificationLogRepository;
    private final EmailService emailService;
    private final EmailTemplateService emailTemplateService;
    private final BloodBankProperties bloodBankProperties;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    /**
     * Runs periodically to detect donors who recently completed the 90-day recovery interval
     * and sends a one-time reminder email without spamming.
     */
    @Scheduled(cron = "${bloodbank.mail.eligibility-cron:0 0 9 * * ?}")
    public void runEligibilityReminders() {
        if (!bloodBankProperties.getMail().isEligibilityRemindersEnabled()) {
            return;
        }

        log.info("Starting scheduled donor eligibility reminder evaluation...");
        List<Donor> activeDonors = donorRepository.findByActiveTrue();
        int remindersSent = 0;

        for (Donor donor : activeDonors) {
            try {
                EligibilityResponse eligibility = donorService.checkEligibility(donor.getId());
                // Only send to donors who have completed their gap and had a previous donation date
                if (eligibility.isEligible() && eligibility.getLastDonationDate() != null) {
                    LocalDate lastDonation = eligibility.getLastDonationDate();
                    // Check if already notified since last donation to avoid duplicate spam
                    boolean alreadyNotified = notificationLogRepository
                            .existsByNotificationTypeAndRelatedEntityTypeAndRelatedEntityIdAndCreatedAtAfter(
                                    NotificationType.ELIGIBILITY_REMINDER,
                                    "DONOR",
                                    donor.getId(),
                                    lastDonation.atStartOfDay()
                            );

                    if (!alreadyNotified) {
                        Map<String, Object> vars = new HashMap<>();
                        vars.put("donorName", donor.getName());
                        vars.put("donorCode", donor.getDonorCode());
                        vars.put("bloodGroup", donor.getBloodGroup().getDisplayValue());
                        vars.put("eligibleDate", eligibility.getNextEligibleDate() != null
                                ? eligibility.getNextEligibleDate().format(DATE_FORMATTER)
                                : "Today");

                        String content = emailTemplateService.renderTemplate("eligibility-reminder", vars);

                        emailService.sendEmail(
                                NotificationType.ELIGIBILITY_REMINDER,
                                donor.getEmail(),
                                "BloodBank — You Are Eligible to Donate Blood Again",
                                content,
                                "DONOR",
                                donor.getId()
                        );
                        remindersSent++;
                    }
                }
            } catch (Exception ex) {
                log.error("Failed to process eligibility reminder for donor id={}: {}", donor.getId(), ex.getMessage());
            }
        }
        log.info("Completed donor eligibility reminder evaluation. Dispatched {} reminder(s).", remindersSent);
    }
}
