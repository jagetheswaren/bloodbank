package com.bloodbank.service;

import com.bloodbank.config.BloodBankProperties;
import com.bloodbank.dto.request.IssueRequest;
import com.bloodbank.dto.response.IssueResponse;
import com.bloodbank.entity.BloodUnit;
import com.bloodbank.entity.IssueRecord;
import com.bloodbank.enums.BloodGroup;
import com.bloodbank.enums.BloodUnitStatus;
import com.bloodbank.exception.InsufficientStockException;
import com.bloodbank.repository.BloodUnitRepository;
import com.bloodbank.repository.IssueRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueServiceTest {

    @Mock
    private BloodUnitRepository bloodUnitRepository;

    @Mock
    private IssueRecordRepository issueRecordRepository;

    @Spy
    private BloodBankProperties bloodBankProperties = new BloodBankProperties();

    @InjectMocks
    private IssueService issueService;

    @BeforeEach
    void setUp() {
        bloodBankProperties.getInventory().setNearExpiryDays(7);
    }

    @Test
    @DisplayName("Should issue blood units according to FEFO (earliest safe expiry first)")
    void testIssueBlood_Success_FEFOOrder() {
        LocalDate today = LocalDate.now();

        // Unit A expires in 15 days (earlier safe expiry)
        BloodUnit unitA = BloodUnit.builder()
                .id(1L)
                .unitCode("UNT-FEFO-A")
                .bloodGroup(BloodGroup.O_POSITIVE)
                .collectionDate(today.minusDays(27))
                .expiryDate(today.plusDays(15))
                .status(BloodUnitStatus.AVAILABLE)
                .build();

        // Unit B expires in 30 days (later safe expiry)
        BloodUnit unitB = BloodUnit.builder()
                .id(2L)
                .unitCode("UNT-FEFO-B")
                .bloodGroup(BloodGroup.O_POSITIVE)
                .collectionDate(today.minusDays(12))
                .expiryDate(today.plusDays(30))
                .status(BloodUnitStatus.AVAILABLE)
                .build();

        // Repository returns in FEFO order (unitA first, then unitB)
        when(bloodUnitRepository.findSafeAvailableUnitsForIssue(eq(BloodGroup.O_POSITIVE), any(LocalDate.class)))
                .thenReturn(new ArrayList<>(List.of(unitA, unitB)));
        when(issueRecordRepository.existsByBloodUnitId(anyLong())).thenReturn(false);
        when(issueRecordRepository.findByIssueCode(anyString())).thenReturn(Optional.empty());
        when(issueRecordRepository.save(any(IssueRecord.class))).thenAnswer(inv -> {
            IssueRecord r = inv.getArgument(0);
            r.setId(10L);
            return r;
        });

        IssueRequest request = IssueRequest.builder()
                .bloodGroup(BloodGroup.O_POSITIVE)
                .numberOfUnits(1)
                .patientName("Robert Brown")
                .hospitalName("City General Hospital")
                .notes("Urgent surgery requirement")
                .build();

        IssueResponse response = issueService.issueBlood(request);

        assertNotNull(response);
        assertEquals(1, response.getNumberOfUnitsIssued());
        assertEquals("Robert Brown", response.getPatientName());
        assertEquals("City General Hospital", response.getHospitalName());
        assertEquals(BloodGroup.O_POSITIVE, response.getRequestedBloodGroup());

        // FEFO verification: unitA (earliest expiry) was issued, NOT unitB
        assertEquals("UNT-FEFO-A", response.getIssuedUnits().get(0).getUnitCode());
        assertEquals(BloodUnitStatus.ISSUED, unitA.getStatus());
        assertEquals(BloodUnitStatus.AVAILABLE, unitB.getStatus()); // unitB remains available

        verify(bloodUnitRepository, times(1)).save(unitA);
        verify(issueRecordRepository, times(1)).save(any(IssueRecord.class));
    }

    @Test
    @DisplayName("Should reject issue request when available safe stock is insufficient")
    void testIssueBlood_InsufficientStock_ThrowsException() {
        when(bloodUnitRepository.findSafeAvailableUnitsForIssue(eq(BloodGroup.AB_NEGATIVE), any(LocalDate.class)))
                .thenReturn(new ArrayList<>());

        IssueRequest request = IssueRequest.builder()
                .bloodGroup(BloodGroup.AB_NEGATIVE)
                .numberOfUnits(2)
                .patientName("Emily Davis")
                .hospitalName("Metro Health")
                .build();

        InsufficientStockException ex = assertThrows(
                InsufficientStockException.class,
                () -> issueService.issueBlood(request)
        );

        assertTrue(ex.getMessage().contains("Insufficient safe inventory"));
        verify(issueRecordRepository, never()).save(any(IssueRecord.class));
    }

    @Test
    @DisplayName("Should refuse to issue unit nearing expiry within 7 days")
    void testIssueBlood_NearExpiryUnit_Refused() {
        LocalDate today = LocalDate.now();

        // Unit nearing expiry (expires in 3 days, within 7-day window)
        BloodUnit nearExpiryUnit = BloodUnit.builder()
                .id(3L)
                .unitCode("UNT-NEAR-03")
                .bloodGroup(BloodGroup.B_POSITIVE)
                .expiryDate(today.plusDays(3))
                .status(BloodUnitStatus.AVAILABLE) // even if marked available in DB, date check must exclude it
                .build();

        // Repository returns it, but Java filtering must discard it because expiryDate <= nearExpiryCutoff
        when(bloodUnitRepository.findSafeAvailableUnitsForIssue(eq(BloodGroup.B_POSITIVE), any(LocalDate.class)))
                .thenReturn(new ArrayList<>(List.of(nearExpiryUnit)));

        IssueRequest request = IssueRequest.builder()
                .bloodGroup(BloodGroup.B_POSITIVE)
                .numberOfUnits(1)
                .patientName("Tom Clark")
                .hospitalName("Care Clinic")
                .build();

        assertThrows(InsufficientStockException.class, () -> issueService.issueBlood(request));
        verify(issueRecordRepository, never()).save(any(IssueRecord.class));
    }

    @Test
    @DisplayName("Should prevent already issued unit from being issued twice")
    void testIssueBlood_AlreadyIssuedUnit_Prevented() {
        LocalDate today = LocalDate.now();

        BloodUnit alreadyIssuedUnit = BloodUnit.builder()
                .id(4L)
                .unitCode("UNT-ISSUED-04")
                .bloodGroup(BloodGroup.A_POSITIVE)
                .expiryDate(today.plusDays(20))
                .status(BloodUnitStatus.ISSUED)
                .build();

        when(bloodUnitRepository.findSafeAvailableUnitsForIssue(eq(BloodGroup.A_POSITIVE), any(LocalDate.class)))
                .thenReturn(new ArrayList<>(List.of(alreadyIssuedUnit)));

        IssueRequest request = IssueRequest.builder()
                .bloodGroup(BloodGroup.A_POSITIVE)
                .numberOfUnits(1)
                .patientName("Sam Green")
                .hospitalName("Hope Hospital")
                .build();

        assertThrows(InsufficientStockException.class, () -> issueService.issueBlood(request));
        verify(issueRecordRepository, never()).save(any(IssueRecord.class));
    }

    @Test
    @DisplayName("Section 38 FEFO Spec: Must select earliest safe unit and skip near-expiry unit")
    void testIssueBlood_FEFO_SelectsEarliestSafeAndSkipsNearExpiry() {
        LocalDate today = LocalDate.now();

        // 1 near-expiry unit (expires in 3 days, inside <= 7 days window)
        BloodUnit nearExpiryUnit = BloodUnit.builder()
                .id(10L)
                .unitCode("UNT-DEMO-O-FEFO-NE")
                .bloodGroup(BloodGroup.O_POSITIVE)
                .collectionDate(today.minusDays(39))
                .expiryDate(today.plusDays(3))
                .status(BloodUnitStatus.AVAILABLE)
                .build();

        // Unit A (expires in 10 days: earliest SAFE expiry)
        BloodUnit unitA = BloodUnit.builder()
                .id(11L)
                .unitCode("UNT-DEMO-O-FEFO-A")
                .bloodGroup(BloodGroup.O_POSITIVE)
                .collectionDate(today.minusDays(32))
                .expiryDate(today.plusDays(10))
                .status(BloodUnitStatus.AVAILABLE)
                .build();

        // Unit B (expires in 15 days)
        BloodUnit unitB = BloodUnit.builder()
                .id(12L)
                .unitCode("UNT-DEMO-O-FEFO-B")
                .bloodGroup(BloodGroup.O_POSITIVE)
                .collectionDate(today.minusDays(27))
                .expiryDate(today.plusDays(15))
                .status(BloodUnitStatus.AVAILABLE)
                .build();

        // Unit C (expires in 25 days)
        BloodUnit unitC = BloodUnit.builder()
                .id(13L)
                .unitCode("UNT-DEMO-O-FEFO-C")
                .bloodGroup(BloodGroup.O_POSITIVE)
                .collectionDate(today.minusDays(17))
                .expiryDate(today.plusDays(25))
                .status(BloodUnitStatus.AVAILABLE)
                .build();

        // Repository returns ordered by expiryDate ASC
        when(bloodUnitRepository.findSafeAvailableUnitsForIssue(eq(BloodGroup.O_POSITIVE), any(LocalDate.class)))
                .thenReturn(new ArrayList<>(List.of(nearExpiryUnit, unitA, unitB, unitC)));
        when(issueRecordRepository.existsByBloodUnitId(anyLong())).thenReturn(false);
        when(issueRecordRepository.findByIssueCode(anyString())).thenReturn(Optional.empty());
        when(issueRecordRepository.save(any(IssueRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        IssueRequest request = IssueRequest.builder()
                .bloodGroup(BloodGroup.O_POSITIVE)
                .numberOfUnits(1)
                .patientName("Ravi Demo")
                .hospitalName("City Care Demo Hospital")
                .notes("FEFO allocation test")
                .build();

        IssueResponse response = issueService.issueBlood(request);

        assertNotNull(response);
        assertEquals(1, response.getNumberOfUnitsIssued());
        // Verify FEFO chose unitA (earliest SAFE expiry: +10 days), skipping nearExpiryUnit (+3 days)
        assertEquals("UNT-DEMO-O-FEFO-A", response.getIssuedUnits().get(0).getUnitCode());
        assertEquals(BloodUnitStatus.ISSUED, unitA.getStatus());
        assertEquals(BloodUnitStatus.AVAILABLE, unitB.getStatus());
        assertEquals(BloodUnitStatus.AVAILABLE, unitC.getStatus());
        assertEquals(BloodUnitStatus.AVAILABLE, nearExpiryUnit.getStatus()); // Untouched by issue
    }
}
