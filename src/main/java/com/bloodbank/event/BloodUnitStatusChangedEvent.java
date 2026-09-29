package com.bloodbank.event;

import com.bloodbank.enums.BloodGroup;
import com.bloodbank.enums.BloodUnitStatus;

import java.time.LocalDate;

public record BloodUnitStatusChangedEvent(
        Long unitId,
        String unitCode,
        BloodGroup bloodGroup,
        LocalDate expiryDate,
        BloodUnitStatus oldStatus,
        BloodUnitStatus newStatus,
        long daysRemaining
) {}
