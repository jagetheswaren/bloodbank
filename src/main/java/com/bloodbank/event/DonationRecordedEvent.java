package com.bloodbank.event;

import com.bloodbank.enums.BloodGroup;

import java.time.LocalDate;

public record DonationRecordedEvent(
        Long donationId,
        String donationCode,
        Long donorId,
        String donorName,
        String donorEmail,
        BloodGroup bloodGroup,
        int units,
        LocalDate donationDate,
        LocalDate nextEligibleDate
) {}
