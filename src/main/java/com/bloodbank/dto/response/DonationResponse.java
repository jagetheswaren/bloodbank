package com.bloodbank.dto.response;

import com.bloodbank.enums.BloodGroup;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DonationResponse {
    private Long id;
    private String donationCode;
    private Long donorId;
    private String donorCode;
    private String donorName;
    private BloodGroup bloodGroup;
    private LocalDate donationDate;
    private Integer numberOfUnits;
    private String notes;
    private List<BloodUnitResponse> bloodUnits;
    private LocalDateTime createdAt;
}
