package com.bloodbank.repository;

import com.bloodbank.entity.NotificationLog;
import com.bloodbank.enums.NotificationStatus;
import com.bloodbank.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    List<NotificationLog> findByNotificationType(NotificationType notificationType);

    List<NotificationLog> findByStatus(NotificationStatus status);

    List<NotificationLog> findByRecipientOrderByCreatedAtDesc(String recipient);

    List<NotificationLog> findTop50ByOrderByCreatedAtDesc();

    boolean existsByNotificationTypeAndRelatedEntityTypeAndRelatedEntityIdAndStatus(
            NotificationType notificationType,
            String relatedEntityType,
            Long relatedEntityId,
            NotificationStatus status
    );
}
