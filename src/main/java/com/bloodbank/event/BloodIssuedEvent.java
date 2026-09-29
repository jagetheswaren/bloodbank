package com.bloodbank.event;

import com.bloodbank.enums.BloodGroup;

import java.time.LocalDateTime;

public record BloodIssuedEvent(
        Long issueId,
        String issueCode,
        String unitCode,
        BloodGroup bloodGroup,
        String patientName,
        String hospitalName,
        LocalDateTime issueDate
) {}
