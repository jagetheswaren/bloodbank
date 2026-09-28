package com.bloodbank.dto.response;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EligibilityResponse {
    private Long donorId;
    private String donorCode;
    private String donorName;
    private boolean eligible;
    private LocalDate lastDonationDate;
    private LocalDate nextEligibleDate;
    private long remainingDays;
    private String message;
}
