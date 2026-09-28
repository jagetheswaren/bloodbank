package com.bloodbank.dto.response;

import com.bloodbank.enums.BloodGroup;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueResponse {
    private String patientName;
    private String hospitalName;
    private BloodGroup requestedBloodGroup;
    private int numberOfUnitsRequested;
    private int numberOfUnitsIssued;
    private List<IssueRecordResponse> issuedUnits;
    private String message;
}
