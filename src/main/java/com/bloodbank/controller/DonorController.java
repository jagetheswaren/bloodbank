package com.bloodbank.controller;

import com.bloodbank.dto.request.DonorCreateRequest;
import com.bloodbank.dto.request.DonorUpdateRequest;
import com.bloodbank.dto.response.DonorResponse;
import com.bloodbank.dto.response.EligibilityResponse;
import com.bloodbank.service.DonorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/donors")
@RequiredArgsConstructor
@Tag(name = "Donor Management", description = "APIs for donor registration, updates, soft deactivation, and donation eligibility tracking")
public class DonorController {

    private final DonorService donorService;

    @PostMapping
    @Operation(summary = "Register a new blood donor", description = "Creates a new donor with unique donor code and validation rules")
    public ResponseEntity<DonorResponse> createDonor(@Valid @RequestBody DonorCreateRequest request) {
        DonorResponse response = donorService.createDonor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get paginated list of donors", description = "Retrieves all donors with pagination, sorting, and optional active filter")
    public ResponseEntity<Page<DonorResponse>> getAllDonors(
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<DonorResponse> donors = donorService.getAllDonors(pageable, active);
        return ResponseEntity.ok(donors);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get donor by ID", description = "Fetches a specific donor by their database ID")
    public ResponseEntity<DonorResponse> getDonorById(@PathVariable Long id) {
        DonorResponse response = donorService.getDonorById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing donor", description = "Updates donor details while preventing email/phone duplicate collisions")
    public ResponseEntity<DonorResponse> updateDonor(
            @PathVariable Long id,
            @Valid @RequestBody DonorUpdateRequest request) {
        DonorResponse response = donorService.updateDonor(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate donor (Safe Soft Delete)", description = "Deactivates a donor while preserving historic donation and inventory records")
    public ResponseEntity<DonorResponse> deactivateDonor(@PathVariable Long id) {
        DonorResponse response = donorService.deactivateDonor(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/eligibility")
    @Operation(summary = "Check donor donation eligibility", description = "Evaluates whether donor has satisfied the 90-day minimum donation gap rule")
    public ResponseEntity<EligibilityResponse> checkEligibility(@PathVariable Long id) {
        EligibilityResponse response = donorService.checkEligibility(id);
        return ResponseEntity.ok(response);
    }
}
