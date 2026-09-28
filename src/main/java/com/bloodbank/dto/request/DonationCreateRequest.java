package com.bloodbank.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DonationCreateRequest {

    @NotNull(message = "Donor ID is required")
    private Long donorId;

    @NotNull(message = "Donation date is required")
    @PastOrPresent(message = "Donation date cannot be in the future")
    private LocalDate donationDate;

    @NotNull(message = "Number of units is required")
    @Positive(message = "Number of units must be at least 1")
    @com.fasterxml.jackson.annotation.JsonAlias({"numberOfUnits", "units"})
    private Integer numberOfUnits;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
