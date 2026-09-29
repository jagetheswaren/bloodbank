package com.bloodbank.service;

import com.bloodbank.config.BloodBankProperties;
import com.bloodbank.dto.request.DonorCreateRequest;
import com.bloodbank.dto.request.DonorUpdateRequest;
import com.bloodbank.dto.response.DonorResponse;
import com.bloodbank.dto.response.EligibilityResponse;
import com.bloodbank.entity.Donation;
import com.bloodbank.entity.Donor;
import com.bloodbank.exception.DuplicateResourceException;
import com.bloodbank.exception.ResourceNotFoundException;
import com.bloodbank.repository.DonationRepository;
import com.bloodbank.repository.DonorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import com.bloodbank.event.DonorRegisteredEvent;

@Slf4j
@Service
@RequiredArgsConstructor
public class DonorService {

    private final DonorRepository donorRepository;
    private final DonationRepository donationRepository;
    private final BloodBankProperties bloodBankProperties;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public DonorResponse createDonor(DonorCreateRequest request) {
        if (donorRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new DuplicateResourceException("A donor with email '" + request.getEmail() + "' already exists");
        }
        if (donorRepository.existsByPhone(request.getPhone().trim())) {
            throw new DuplicateResourceException("A donor with phone '" + request.getPhone() + "' already exists");
        }

        String donorCode = "DNR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        while (donorRepository.existsByDonorCode(donorCode)) {
            donorCode = "DNR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        Donor donor = Donor.builder()
                .donorCode(donorCode)
                .name(request.getName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .phone(request.getPhone().trim())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .bloodGroup(request.getBloodGroup())
                .address(request.getAddress())
                .active(true)
                .build();

        Donor savedDonor = donorRepository.save(donor);
        log.info("Donor registered successfully: id={}, donorCode={}, name={}, bloodGroup={}",
                savedDonor.getId(), savedDonor.getDonorCode(), savedDonor.getName(), savedDonor.getBloodGroup());

        eventPublisher.publishEvent(new DonorRegisteredEvent(
                savedDonor.getId(),
                savedDonor.getDonorCode(),
                savedDonor.getName(),
                savedDonor.getEmail(),
                savedDonor.getBloodGroup()
        ));

        return mapToResponse(savedDonor);
    }

    @Transactional(readOnly = true)
    public DonorResponse getDonorById(Long id) {
        Donor donor = findDonorEntity(id);
        return mapToResponse(donor);
    }

    @Transactional(readOnly = true)
    public Page<DonorResponse> getAllDonors(Pageable pageable, Boolean activeOnly) {
        Page<Donor> donors;
        if (Boolean.TRUE.equals(activeOnly)) {
            donors = donorRepository.findByActive(true, pageable);
        } else {
            donors = donorRepository.findAll(pageable);
        }
        return donors.map(this::mapToResponse);
    }

    @Transactional
    public DonorResponse updateDonor(Long id, DonorUpdateRequest request) {
        Donor donor = findDonorEntity(id);

        String email = request.getEmail().trim().toLowerCase();
        donorRepository.findByEmail(email).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new DuplicateResourceException("A donor with email '" + email + "' already exists");
            }
        });

        String phone = request.getPhone().trim();
        donorRepository.findByPhone(phone).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new DuplicateResourceException("A donor with phone '" + phone + "' already exists");
            }
        });

        donor.setName(request.getName().trim());
        donor.setEmail(email);
        donor.setPhone(phone);
        donor.setDateOfBirth(request.getDateOfBirth());
        donor.setGender(request.getGender());
        donor.setBloodGroup(request.getBloodGroup());
        donor.setAddress(request.getAddress());
        if (request.getActive() != null) {
            donor.setActive(request.getActive());
        }

        Donor updated = donorRepository.save(donor);
        log.info("Donor updated successfully: id={}, donorCode={}", updated.getId(), updated.getDonorCode());
        return mapToResponse(updated);
    }

    @Transactional
    public DonorResponse deactivateDonor(Long id) {
        Donor donor = findDonorEntity(id);
        donor.setActive(false);
        Donor updated = donorRepository.save(donor);
        log.info("Donor deactivated safely (historical data preserved): id={}, donorCode={}", updated.getId(), updated.getDonorCode());
        return mapToResponse(updated);
    }

    @Transactional(readOnly = true)
    public EligibilityResponse checkEligibility(Long id) {
        Donor donor = findDonorEntity(id);

        if (!donor.isActive()) {
            return EligibilityResponse.builder()
                    .donorId(donor.getId())
                    .donorCode(donor.getDonorCode())
                    .donorName(donor.getName())
                    .eligible(false)
                    .lastDonationDate(null)
                    .nextEligibleDate(null)
                    .remainingDays(0)
                    .message("Donor is currently inactive/deactivated and ineligible to donate.")
                    .build();
        }

        int minGapDays = bloodBankProperties.getDonation().getMinimumGapDays();
        Optional<Donation> latestDonationOpt = donationRepository
                .findTopByDonorOrderByDonationDateDescCreatedAtDesc(donor);

        if (latestDonationOpt.isEmpty()) {
            return EligibilityResponse.builder()
                    .donorId(donor.getId())
                    .donorCode(donor.getDonorCode())
                    .donorName(donor.getName())
                    .eligible(true)
                    .lastDonationDate(null)
                    .nextEligibleDate(LocalDate.now())
                    .remainingDays(0)
                    .message("First-time donor has no previous donations and is fully eligible to donate.")
                    .build();
        }

        Donation latestDonation = latestDonationOpt.get();
        LocalDate lastDonationDate = latestDonation.getDonationDate();
        LocalDate nextEligibleDate = lastDonationDate.plusDays(minGapDays);
        LocalDate today = LocalDate.now();

        if (!today.isBefore(nextEligibleDate)) {
            return EligibilityResponse.builder()
                    .donorId(donor.getId())
                    .donorCode(donor.getDonorCode())
                    .donorName(donor.getName())
                    .eligible(true)
                    .lastDonationDate(lastDonationDate)
                    .nextEligibleDate(nextEligibleDate)
                    .remainingDays(0)
                    .message("Donor has completed the minimum required gap of " + minGapDays + " days and is eligible to donate.")
                    .build();
        } else {
            long remainingDays = ChronoUnit.DAYS.between(today, nextEligibleDate);
            return EligibilityResponse.builder()
                    .donorId(donor.getId())
                    .donorCode(donor.getDonorCode())
                    .donorName(donor.getName())
                    .eligible(false)
                    .lastDonationDate(lastDonationDate)
                    .nextEligibleDate(nextEligibleDate)
                    .remainingDays(remainingDays)
                    .message("Donor has not completed the minimum donation gap of " + minGapDays + " days. "
                            + remainingDays + " day(s) remaining until next eligible donation date (" + nextEligibleDate + ").")
                    .build();
        }
    }

    public Donor findDonorEntity(Long id) {
        return donorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Donor not found with ID: " + id));
    }

    public DonorResponse mapToResponse(Donor donor) {
        return DonorResponse.builder()
                .id(donor.getId())
                .donorCode(donor.getDonorCode())
                .name(donor.getName())
                .email(donor.getEmail())
                .phone(donor.getPhone())
                .dateOfBirth(donor.getDateOfBirth())
                .gender(donor.getGender())
                .bloodGroup(donor.getBloodGroup())
                .address(donor.getAddress())
                .active(donor.isActive())
                .createdAt(donor.getCreatedAt())
                .updatedAt(donor.getUpdatedAt())
                .build();
    }
}
