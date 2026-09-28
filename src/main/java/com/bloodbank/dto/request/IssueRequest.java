package com.bloodbank.dto.request;

import com.bloodbank.enums.BloodGroup;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueRequest {

    @NotNull(message = "Blood group is required")
    @JsonAlias({"bloodGroup", "requestedBloodGroup"})
    private BloodGroup bloodGroup;

    @NotNull(message = "Number of units is required")
    @Positive(message = "Number of units must be at least 1")
    @JsonAlias({"numberOfUnits", "units"})
    private Integer numberOfUnits;

    @NotBlank(message = "Patient name is required")
    @Size(max = 100, message = "Patient name must not exceed 100 characters")
    private String patientName;

    @NotBlank(message = "Hospital name is required")
    @Size(max = 150, message = "Hospital name must not exceed 150 characters")
    private String hospitalName;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
