package com.bloodbank.dto.request;

import com.bloodbank.enums.BloodGroup;
import com.bloodbank.enums.Gender;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DonorUpdateRequest {

    @NotBlank(message = "Donor name is required")
    @Size(min = 2, max = 100, message = "Donor name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9+()\\-\\s]{7,25}$", message = "Invalid phone number format")
    private String phone;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required (MALE, FEMALE, OTHER)")
    private Gender gender;

    @NotNull(message = "Blood group is required (A+, A-, B+, B-, AB+, AB-, O+, O-)")
    private BloodGroup bloodGroup;

    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;

    private Boolean active;
}
