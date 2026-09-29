package com.bloodbank.service;

import com.bloodbank.config.BloodBankProperties;
import com.bloodbank.dto.request.DonationCreateRequest;
import com.bloodbank.dto.response.BloodUnitResponse;
import com.bloodbank.dto.response.DonationResponse;
import com.bloodbank.dto.response.EligibilityResponse;
import com.bloodbank.entity.BloodUnit;
import com.bloodbank.entity.Donation;
import com.bloodbank.entity.Donor;
import com.bloodbank.enums.BloodUnitStatus;
import com.bloodbank.exception.DonorNotEligibleException;
import com.bloodbank.exception.ResourceNotFoundException;
import com.bloodbank.repository.BloodUnitRepository;
import com.bloodbank.repository.DonationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import com.bloodbank.event.DonationRecordedEvent;

@Slf4j
@Service
@RequiredArgsConstructor
public class DonationService {

    private final DonationRepository donationRepository;
    private final BloodUnitRepository bloodUnitRepository;
    private final DonorService donorService;
    private final BloodBankProperties bloodBankProperties;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public DonationResponse registerDonation(DonationCreateRequest request) {
        log.info("Processing donation registration for donorId={}, requestedUnits={}, donationDate={}",
                request.getDonorId(), request.getNumberOfUnits(), request.getDonationDate());

        // 1. Find donor and verify existence
        Donor donor = donorService.findDonorEntity(request.getDonorId());

        // 2. Enforce active donor rule
        if (!donor.isActive()) {
            throw new DonorNotEligibleException("Donor (ID: " + donor.getId() + ") is inactive/deactivated and cannot donate.");
        }

        // 3. CRITICAL: Check eligibility BEFORE persistence
        EligibilityResponse eligibility = donorService.checkEligibility(donor.getId());
        if (!eligibility.isEligible()) {
            log.warn("Donation rejected for donorId={}: {}", donor.getId(), eligibility.getMessage());
            throw new DonorNotEligibleException(eligibility.getMessage());
        }

        // 4. Generate unique donation code
        String donationCode = "DON-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        while (donationRepository.existsByDonationCode(donationCode)) {
            donationCode = "DON-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        // 5. Build and save Donation
        Donation donation = Donation.builder()
                .donationCode(donationCode)
                .donor(donor)
                .donationDate(request.getDonationDate())
                .numberOfUnits(request.getNumberOfUnits())
                .notes(request.getNotes())
                .bloodUnits(new ArrayList<>())
                .build();

        Donation savedDonation = donationRepository.save(donation);
        log.info("Donation record created: id={}, donationCode={}", savedDonation.getId(), savedDonation.getDonationCode());

        // 6. Generate BloodUnit for each requested unit
        int shelfLifeDays = bloodBankProperties.getBloodUnit().getShelfLifeDays();
        int nearExpiryDays = bloodBankProperties.getInventory().getNearExpiryDays();
        LocalDate today = LocalDate.now();

        List<BloodUnit> unitsToSave = new ArrayList<>();
        for (int i = 0; i < request.getNumberOfUnits(); i++) {
            String unitCode = "UNT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            while (bloodUnitRepository.existsByUnitCode(unitCode)) {
                unitCode = "UNT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            }

            LocalDate collectionDate = request.getDonationDate();
            LocalDate expiryDate = collectionDate.plusDays(shelfLifeDays);

            // Determine initial status based on configurable expiry & near-expiry rules
            BloodUnitStatus status;
            if (expiryDate.isBefore(today)) {
                status = BloodUnitStatus.EXPIRED;
            } else if (!expiryDate.isAfter(today.plusDays(nearExpiryDays))) {
                status = BloodUnitStatus.NEAR_EXPIRY;
            } else {
                status = BloodUnitStatus.AVAILABLE;
            }

            BloodUnit unit = BloodUnit.builder()
                    .unitCode(unitCode)
                    .donation(savedDonation)
                    .bloodGroup(donor.getBloodGroup())
                    .collectionDate(collectionDate)
                    .expiryDate(expiryDate)
                    .status(status)
                    .build();

            unitsToSave.add(unit);
        }

        List<BloodUnit> savedUnits = bloodUnitRepository.saveAll(unitsToSave);
        savedDonation.setBloodUnits(savedUnits);

        log.info("Atomic donation registration completed. donationCode={}, bloodUnitsCreated={}",
                savedDonation.getDonationCode(), savedUnits.size());

        for (BloodUnit bu : savedUnits) {
            log.info("BloodUnit created: code={}, group={}, expiry={}, status={}",
                    bu.getUnitCode(), bu.getBloodGroup(), bu.getExpiryDate(), bu.getStatus());
        }

        LocalDate nextEligible = request.getDonationDate().plusDays(bloodBankProperties.getDonation().getMinimumGapDays());
        eventPublisher.publishEvent(new DonationRecordedEvent(
                savedDonation.getId(),
                savedDonation.getDonationCode(),
                donor.getId(),
                donor.getName(),
                donor.getEmail(),
                donor.getBloodGroup(),
                savedUnits.size(),
                savedDonation.getDonationDate(),
                nextEligible
        ));

        return mapToResponse(savedDonation);
    }

    @Transactional(readOnly = true)
    public DonationResponse getDonationById(Long id) {
        Donation donation = donationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found with ID: " + id));
        return mapToResponse(donation);
    }

    @Transactional(readOnly = true)
    public Page<DonationResponse> getAllDonations(Pageable pageable) {
        return donationRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<DonationResponse> getDonationsByDonor(Long donorId, Pageable pageable) {
        return donationRepository.findByDonorId(donorId, pageable).map(this::mapToResponse);
    }

    public DonationResponse mapToResponse(Donation donation) {
        List<BloodUnitResponse> unitResponses = donation.getBloodUnits() != null
                ? donation.getBloodUnits().stream().map(this::mapUnitToResponse).collect(Collectors.toList())
                : new ArrayList<>();

        return DonationResponse.builder()
                .id(donation.getId())
                .donationCode(donation.getDonationCode())
                .donorId(donation.getDonor().getId())
                .donorCode(donation.getDonor().getDonorCode())
                .donorName(donation.getDonor().getName())
                .bloodGroup(donation.getDonor().getBloodGroup())
                .donationDate(donation.getDonationDate())
                .numberOfUnits(donation.getNumberOfUnits())
                .notes(donation.getNotes())
                .bloodUnits(unitResponses)
                .createdAt(donation.getCreatedAt())
                .build();
    }

    public BloodUnitResponse mapUnitToResponse(BloodUnit unit) {
        return BloodUnitResponse.builder()
                .id(unit.getId())
                .unitCode(unit.getUnitCode())
                .donationCode(unit.getDonation() != null ? unit.getDonation().getDonationCode() : null)
                .bloodGroup(unit.getBloodGroup())
                .collectionDate(unit.getCollectionDate())
                .expiryDate(unit.getExpiryDate())
                .status(unit.getStatus())
                .createdAt(unit.getCreatedAt())
                .updatedAt(unit.getUpdatedAt())
                .build();
    }
}
