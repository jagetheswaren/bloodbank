package com.bloodbank.event;

import com.bloodbank.enums.BloodGroup;

public record DonorRegisteredEvent(
        Long donorId,
        String donorCode,
        String name,
        String email,
        BloodGroup bloodGroup
) {}
