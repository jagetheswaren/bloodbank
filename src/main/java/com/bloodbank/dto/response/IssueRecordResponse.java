package com.bloodbank.dto.response;

import com.bloodbank.enums.BloodGroup;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueRecordResponse {
    private Long id;
    private String issueCode;
    private String unitCode;
    private BloodGroup bloodGroup;
    private BloodGroup requestedBloodGroup;
    private String patientName;
    private String hospitalName;
    private LocalDateTime issueDate;
    private String notes;
    private LocalDateTime createdAt;
}
