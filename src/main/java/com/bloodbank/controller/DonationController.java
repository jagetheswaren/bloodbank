package com.bloodbank.controller;

import com.bloodbank.dto.request.DonationCreateRequest;
import com.bloodbank.dto.response.DonationResponse;
import com.bloodbank.service.DonationService;
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
@RequestMapping("/api/donations")
@RequiredArgsConstructor
@Tag(name = "Donation Management", description = "APIs for recording donations and automatically generating inventory blood units")
public class DonationController {

    private final DonationService donationService;

    @PostMapping
    @Operation(
        summary = "Record a new blood donation",
        description = "Verifies donor eligibility first, records donation, and atomically generates individual blood units"
    )
    public ResponseEntity<DonationResponse> registerDonation(@Valid @RequestBody DonationCreateRequest request) {
        DonationResponse response = donationService.registerDonation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get paginated list of donations", description = "Retrieves all recorded donations with pagination and sorting")
    public ResponseEntity<Page<DonationResponse>> getAllDonations(
            @PageableDefault(size = 10, sort = "donationDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<DonationResponse> donations = donationService.getAllDonations(pageable);
        return ResponseEntity.ok(donations);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get donation by ID", description = "Fetches donation details and its generated blood units")
    public ResponseEntity<DonationResponse> getDonationById(@PathVariable Long id) {
        DonationResponse response = donationService.getDonationById(id);
        return ResponseEntity.ok(response);
    }
}
