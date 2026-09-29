package com.bloodbank.controller;

import com.bloodbank.config.BloodBankProperties;
import com.bloodbank.entity.NotificationLog;
import com.bloodbank.repository.NotificationLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Real Email Notification Audit & Dispatch Inspection API")
public class NotificationController {

    private final NotificationLogRepository notificationLogRepository;
    private final BloodBankProperties bloodBankProperties;

    @GetMapping
    @Operation(summary = "Get recent notification audit logs", description = "Retrieves the latest 50 email notification attempts (Sent, Failed, or Skipped) for audit inspection.")
    public ResponseEntity<List<NotificationLog>> getRecentNotifications() {
        return ResponseEntity.ok(notificationLogRepository.findTop50ByOrderByCreatedAtDesc());
    }

    @GetMapping("/status")
    @Operation(summary = "Get notification system status", description = "Checks whether real email notifications and SMTP sending are active.")
    public ResponseEntity<Map<String, Object>> getNotificationStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("mailEnabled", bloodBankProperties.getMail().isEnabled());
        status.put("fromAddress", bloodBankProperties.getMail().getFrom());
        status.put("adminRecipient", bloodBankProperties.getMail().getAdminEmail());
        status.put("eligibilityRemindersEnabled", bloodBankProperties.getMail().isEligibilityRemindersEnabled());
        status.put("totalLogsRecorded", notificationLogRepository.count());
        return ResponseEntity.ok(status);
    }
}
