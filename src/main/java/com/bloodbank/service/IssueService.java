package com.bloodbank.service;

import com.bloodbank.config.BloodBankProperties;
import com.bloodbank.dto.request.IssueRequest;
import com.bloodbank.dto.response.IssueRecordResponse;
import com.bloodbank.dto.response.IssueResponse;
import com.bloodbank.entity.BloodUnit;
import com.bloodbank.entity.IssueRecord;
import com.bloodbank.enums.BloodGroup;
import com.bloodbank.enums.BloodUnitStatus;
import com.bloodbank.exception.BloodUnitUnavailableException;
import com.bloodbank.exception.InsufficientStockException;
import com.bloodbank.exception.ResourceNotFoundException;
import com.bloodbank.repository.BloodUnitRepository;
import com.bloodbank.repository.IssueRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import com.bloodbank.event.BloodIssuedEvent;

@Slf4j
@Service
@RequiredArgsConstructor
public class IssueService {

    private final BloodUnitRepository bloodUnitRepository;
    private final IssueRecordRepository issueRecordRepository;
    private final BloodBankProperties bloodBankProperties;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public IssueResponse issueBlood(IssueRequest request) {
        BloodGroup requestedGroup = request.getBloodGroup();
        int requestedUnits = request.getNumberOfUnits();

        log.info("Processing blood issue request: bloodGroup={}, units={}, patient={}, hospital={}",
                requestedGroup.getDisplayValue(), requestedUnits, request.getPatientName(), request.getHospitalName());

        LocalDate today = LocalDate.now();
        int nearExpiryDays = bloodBankProperties.getInventory().getNearExpiryDays();
        LocalDate nearExpiryCutoff = today.plusDays(nearExpiryDays);

        // Query available units ordered by expiryDate ASC (FEFO: First Expired, First Out)
        List<BloodUnit> candidateUnits = bloodUnitRepository.findSafeAvailableUnitsForIssue(requestedGroup, nearExpiryCutoff);

        // Strict verification: exclude any expired, near-expiry, issued, or discarded units
        List<BloodUnit> safeUnits = candidateUnits.stream()
                .filter(u -> u.getStatus() == BloodUnitStatus.AVAILABLE)
                .filter(u -> !u.getExpiryDate().isBefore(today))
                .filter(u -> u.getExpiryDate().isAfter(nearExpiryCutoff))
                .filter(u -> !issueRecordRepository.existsByBloodUnitId(u.getId()))
                .collect(Collectors.toList());

        if (safeUnits.size() < requestedUnits) {
            log.warn("Insufficient safe stock for blood group {}: requested={}, available={}",
                    requestedGroup.getDisplayValue(), requestedUnits, safeUnits.size());
            throw new InsufficientStockException(String.format(
                    "Insufficient safe inventory for blood group %s. Requested: %d unit(s), Available: %d unit(s). " +
                    "Expired and near-expiry units cannot be issued.",
                    requestedGroup.getDisplayValue(), requestedUnits, safeUnits.size()
            ));
        }

        // Select the earliest safe expiring units (FEFO)
        List<BloodUnit> unitsToIssue = safeUnits.subList(0, requestedUnits);
        List<IssueRecordResponse> issuedRecordResponses = new ArrayList<>();

        for (BloodUnit unit : unitsToIssue) {
            // Guard against duplicate issuing
            if (unit.getStatus() == BloodUnitStatus.ISSUED || issueRecordRepository.existsByBloodUnitId(unit.getId())) {
                throw new BloodUnitUnavailableException("BloodUnit " + unit.getUnitCode() + " has already been issued");
            }

            // Mark unit as ISSUED
            unit.setStatus(BloodUnitStatus.ISSUED);
            bloodUnitRepository.save(unit);

            // Generate unique issue code
            String issueCode = "ISS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            while (issueRecordRepository.findByIssueCode(issueCode).isPresent()) {
                issueCode = "ISS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            }

            // Create IssueRecord
            IssueRecord issueRecord = IssueRecord.builder()
                    .issueCode(issueCode)
                    .bloodUnit(unit)
                    .patientName(request.getPatientName().trim())
                    .hospitalName(request.getHospitalName().trim())
                    .requestedBloodGroup(requestedGroup)
                    .issueDate(LocalDateTime.now())
                    .notes(request.getNotes())
                    .build();

            IssueRecord savedRecord = issueRecordRepository.save(issueRecord);

            log.info("Blood unit [{}] (Group: {}, Expiry: {}) successfully ISSUED. IssueCode: {}, Patient: {}, Hospital: {}",
                    unit.getUnitCode(), unit.getBloodGroup().getDisplayValue(), unit.getExpiryDate(),
                    savedRecord.getIssueCode(), savedRecord.getPatientName(), savedRecord.getHospitalName());

            eventPublisher.publishEvent(new BloodIssuedEvent(
                    savedRecord.getId(),
                    savedRecord.getIssueCode(),
                    unit.getUnitCode(),
                    unit.getBloodGroup(),
                    savedRecord.getPatientName(),
                    savedRecord.getHospitalName(),
                    savedRecord.getIssueDate()
            ));

            issuedRecordResponses.add(mapToRecordResponse(savedRecord));
        }

        return IssueResponse.builder()
                .patientName(request.getPatientName().trim())
                .hospitalName(request.getHospitalName().trim())
                .requestedBloodGroup(requestedGroup)
                .numberOfUnitsRequested(requestedUnits)
                .numberOfUnitsIssued(issuedRecordResponses.size())
                .issuedUnits(issuedRecordResponses)
                .message("Successfully issued " + issuedRecordResponses.size() + " unit(s) of " + requestedGroup.getDisplayValue() + " blood.")
                .build();
    }

    @Transactional(readOnly = true)
    public IssueRecordResponse getIssueById(Long id) {
        IssueRecord record = issueRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Issue record not found with ID: " + id));
        return mapToRecordResponse(record);
    }

    @Transactional(readOnly = true)
    public Page<IssueRecordResponse> getAllIssues(Pageable pageable) {
        return issueRecordRepository.findAllByOrderByIssueDateDesc(pageable).map(this::mapToRecordResponse);
    }

    public IssueRecordResponse mapToRecordResponse(IssueRecord record) {
        return IssueRecordResponse.builder()
                .id(record.getId())
                .issueCode(record.getIssueCode())
                .unitCode(record.getBloodUnit() != null ? record.getBloodUnit().getUnitCode() : null)
                .bloodGroup(record.getBloodUnit() != null ? record.getBloodUnit().getBloodGroup() : null)
                .requestedBloodGroup(record.getRequestedBloodGroup())
                .patientName(record.getPatientName())
                .hospitalName(record.getHospitalName())
                .issueDate(record.getIssueDate())
                .notes(record.getNotes())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
