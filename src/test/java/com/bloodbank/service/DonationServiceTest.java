package com.bloodbank.service;

import com.bloodbank.config.BloodBankProperties;
import com.bloodbank.dto.request.DonationCreateRequest;
import com.bloodbank.dto.response.DonationResponse;
import com.bloodbank.dto.response.EligibilityResponse;
import com.bloodbank.entity.BloodUnit;
import com.bloodbank.entity.Donation;
import com.bloodbank.entity.Donor;
import com.bloodbank.enums.BloodGroup;
import com.bloodbank.enums.BloodUnitStatus;
import com.bloodbank.enums.Gender;
import com.bloodbank.exception.DonorNotEligibleException;
import com.bloodbank.repository.BloodUnitRepository;
import com.bloodbank.repository.DonationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DonationServiceTest {

    @Mock
    private DonationRepository donationRepository;

    @Mock
    private BloodUnitRepository bloodUnitRepository;

    @Mock
    private DonorService donorService;

    @Spy
    private BloodBankProperties bloodBankProperties = new BloodBankProperties();

    @InjectMocks
    private DonationService donationService;

    private Donor donor;

    @BeforeEach
    void setUp() {
        bloodBankProperties.getDonation().setMinimumGapDays(90);
        bloodBankProperties.getBloodUnit().setShelfLifeDays(42);
        bloodBankProperties.getInventory().setNearExpiryDays(7);

        donor = Donor.builder()
                .id(1L)
                .donorCode("DNR-TEST001")
                .name("Alice Smith")
                .email("alice@example.com")
                .phone("+1122334455")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.FEMALE)
                .bloodGroup(BloodGroup.A_POSITIVE)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should successfully register donation and generate correct BloodUnits with 42-day expiry")
    void testRegisterDonation_Success() {
        DonationCreateRequest request = DonationCreateRequest.builder()
                .donorId(1L)
                .donationDate(LocalDate.now())
                .numberOfUnits(2)
                .notes("Regular volunteer donation")
                .build();

        EligibilityResponse eligibleResponse = EligibilityResponse.builder()
                .donorId(1L)
                .eligible(true)
                .message("Donor is eligible")
                .build();

        when(donorService.findDonorEntity(1L)).thenReturn(donor);
        when(donorService.checkEligibility(1L)).thenReturn(eligibleResponse);
        when(donationRepository.save(any(Donation.class))).thenAnswer(inv -> {
            Donation d = inv.getArgument(0);
            d.setId(100L);
            return d;
        });
        when(bloodUnitRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        DonationResponse response = donationService.registerDonation(request);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(2, response.getNumberOfUnits());
        assertEquals(BloodGroup.A_POSITIVE, response.getBloodGroup());
        assertEquals(2, response.getBloodUnits().size());

        assertEquals(LocalDate.now(), response.getDonationDate());
        assertEquals(LocalDate.now().plusDays(42), response.getBloodUnits().get(0).getExpiryDate());
        assertEquals(BloodUnitStatus.AVAILABLE, response.getBloodUnits().get(0).getStatus());

        verify(donationRepository, times(1)).save(any(Donation.class));
        verify(bloodUnitRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("Should reject donation BEFORE persistence if donor is ineligible (gap not met)")
    void testRegisterDonation_IneligibleDonor_ThrowsException() {
        DonationCreateRequest request = DonationCreateRequest.builder()
                .donorId(1L)
                .donationDate(LocalDate.now())
                .numberOfUnits(1)
                .build();

        EligibilityResponse ineligibleResponse = EligibilityResponse.builder()
                .donorId(1L)
                .eligible(false)
                .remainingDays(45)
                .message("Donor has not completed minimum gap. 45 days remaining.")
                .build();

        when(donorService.findDonorEntity(1L)).thenReturn(donor);
        when(donorService.checkEligibility(1L)).thenReturn(ineligibleResponse);

        DonorNotEligibleException ex = assertThrows(
                DonorNotEligibleException.class,
                () -> donationService.registerDonation(request)
        );

        assertTrue(ex.getMessage().contains("45 days remaining"));
        // Verify NOTHING was persisted
        verify(donationRepository, never()).save(any(Donation.class));
        verify(bloodUnitRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should reject donation if donor is inactive")
    void testRegisterDonation_InactiveDonor_ThrowsException() {
        donor.setActive(false);
        DonationCreateRequest request = DonationCreateRequest.builder()
                .donorId(1L)
                .donationDate(LocalDate.now())
                .numberOfUnits(1)
                .build();

        when(donorService.findDonorEntity(1L)).thenReturn(donor);

        assertThrows(
                DonorNotEligibleException.class,
                () -> donationService.registerDonation(request)
        );

        verify(donationRepository, never()).save(any(Donation.class));
    }
}
