package com.bloodbank.service.notification;

import com.bloodbank.config.BloodBankProperties;
import com.bloodbank.enums.BloodUnitStatus;
import com.bloodbank.enums.NotificationType;
import com.bloodbank.event.BloodIssuedEvent;
import com.bloodbank.event.BloodUnitStatusChangedEvent;
import com.bloodbank.event.DonationRecordedEvent;
import com.bloodbank.event.DonorRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final EmailService emailService;
    private final EmailTemplateService emailTemplateService;
    private final BloodBankProperties bloodBankProperties;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDonorRegistered(DonorRegisteredEvent event) {
        log.info("Processing post-commit DonorRegisteredEvent for donor: {}", event.donorCode());

        Map<String, Object> vars = new HashMap<>();
        vars.put("donorName", event.name());
        vars.put("donorCode", event.donorCode());
        vars.put("bloodGroup", event.bloodGroup().getDisplayValue());

        String content = emailTemplateService.renderTemplate("donor-registration", vars);

        emailService.sendEmail(
                NotificationType.DONOR_REGISTRATION,
                event.email(),
                "BloodBank — Donor Registration Successful",
                content,
                "DONOR",
                event.donorId()
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDonationRecorded(DonationRecordedEvent event) {
        log.info("Processing post-commit DonationRecordedEvent for donation: {}", event.donationCode());

        Map<String, Object> vars = new HashMap<>();
        vars.put("donorName", event.donorName());
        vars.put("donationCode", event.donationCode());
        vars.put("bloodGroup", event.bloodGroup().getDisplayValue());
        vars.put("units", event.units());
        vars.put("donationDate", event.donationDate().format(DATE_FORMATTER));
        vars.put("nextEligibleDate", event.nextEligibleDate().format(DATE_FORMATTER));

        String content = emailTemplateService.renderTemplate("donation-confirmation", vars);

        emailService.sendEmail(
                NotificationType.DONATION_RECORDED,
                event.donorEmail(),
                "BloodBank — Donation Recorded Successfully",
                content,
                "DONATION",
                event.donationId()
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBloodUnitStatusChanged(BloodUnitStatusChangedEvent event) {
        String adminEmail = bloodBankProperties.getMail().getAdminEmail();

        if (event.newStatus() == BloodUnitStatus.NEAR_EXPIRY) {
            log.info("Processing near-expiry alert event for unit: {}", event.unitCode());

            Map<String, Object> vars = new HashMap<>();
            vars.put("unitCode", event.unitCode());
            vars.put("bloodGroup", event.bloodGroup().getDisplayValue());
            vars.put("expiryDate", event.expiryDate().format(DATE_FORMATTER));
            vars.put("daysRemaining", event.daysRemaining());

            String content = emailTemplateService.renderTemplate("near-expiry-alert", vars);

            emailService.sendEmail(
                    NotificationType.NEAR_EXPIRY_ALERT,
                    adminEmail,
                    "BloodBank Alert — Blood Unit Near Expiry",
                    content,
                    "BLOOD_UNIT",
                    event.unitId()
            );

        } else if (event.newStatus() == BloodUnitStatus.EXPIRED) {
            log.info("Processing expired alert event for unit: {}", event.unitCode());

            Map<String, Object> vars = new HashMap<>();
            vars.put("unitCode", event.unitCode());
            vars.put("bloodGroup", event.bloodGroup().getDisplayValue());
            vars.put("expiryDate", event.expiryDate().format(DATE_FORMATTER));

            String content = emailTemplateService.renderTemplate("expired-alert", vars);

            emailService.sendEmail(
                    NotificationType.EXPIRED_ALERT,
                    adminEmail,
                    "BloodBank Alert — Blood Unit Expired",
                    content,
                    "BLOOD_UNIT",
                    event.unitId()
            );
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBloodIssued(BloodIssuedEvent event) {
        log.info("Processing post-commit BloodIssuedEvent for issue: {}", event.issueCode());

        String adminEmail = bloodBankProperties.getMail().getAdminEmail();

        Map<String, Object> vars = new HashMap<>();
        vars.put("issueCode", event.issueCode());
        vars.put("unitCode", event.unitCode());
        vars.put("bloodGroup", event.bloodGroup().getDisplayValue());
        vars.put("patientName", event.patientName());
        vars.put("hospitalName", event.hospitalName());
        vars.put("issueDate", event.issueDate().format(DATE_TIME_FORMATTER));

        String content = emailTemplateService.renderTemplate("issue-confirmation", vars);

        emailService.sendEmail(
                NotificationType.ISSUE_CONFIRMATION,
                adminEmail,
                "BloodBank — Blood Issue Recorded",
                content,
                "ISSUE_RECORD",
                event.issueId()
        );
    }
}
