package com.bloodbank.dto.response;

import com.bloodbank.enums.BloodGroup;
import com.bloodbank.enums.BloodUnitStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BloodUnitResponse {
    private Long id;
    private String unitCode;
    private String donationCode;
    private BloodGroup bloodGroup;
    private LocalDate collectionDate;
    private LocalDate expiryDate;
    private BloodUnitStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
