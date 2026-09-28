package com.bloodbank.service;

import com.bloodbank.config.BloodBankProperties;
import com.bloodbank.dto.request.DonorCreateRequest;
import com.bloodbank.dto.response.DonorResponse;
import com.bloodbank.dto.response.EligibilityResponse;
import com.bloodbank.entity.Donation;
import com.bloodbank.entity.Donor;
import com.bloodbank.enums.BloodGroup;
import com.bloodbank.enums.Gender;
import com.bloodbank.exception.DuplicateResourceException;
import com.bloodbank.exception.ResourceNotFoundException;
import com.bloodbank.repository.DonationRepository;
import com.bloodbank.repository.DonorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DonorServiceTest {

    @Mock
    private DonorRepository donorRepository;

    @Mock
    private DonationRepository donationRepository;

    @Spy
    private BloodBankProperties bloodBankProperties = new BloodBankProperties();

    @InjectMocks
    private DonorService donorService;

    private Donor donor;

    @BeforeEach
    void setUp() {
        bloodBankProperties.getDonation().setMinimumGapDays(90);

        donor = Donor.builder()
                .id(1L)
                .donorCode("DNR-TEST0001")
                .name("John Doe")
                .email("john.doe@example.com")
                .phone("+1234567890")
                .dateOfBirth(LocalDate.of(1995, 5, 20))
                .gender(Gender.MALE)
                .bloodGroup(BloodGroup.O_POSITIVE)
                .address("123 Main St")
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should create donor successfully when inputs are valid")
    void testCreateDonor_Success() {
        DonorCreateRequest request = DonorCreateRequest.builder()
                .name("John Doe")
                .email("john.doe@example.com")
                .phone("+1234567890")
                .dateOfBirth(LocalDate.of(1995, 5, 20))
                .gender(Gender.MALE)
                .bloodGroup(BloodGroup.O_POSITIVE)
                .address("123 Main St")
                .build();

        when(donorRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(donorRepository.existsByPhone("+1234567890")).thenReturn(false);
        when(donorRepository.save(any(Donor.class))).thenAnswer(invocation -> {
            Donor d = invocation.getArgument(0);
            d.setId(1L);
            return d;
        });

        DonorResponse response = donorService.createDonor(request);

        assertNotNull(response);
        assertEquals("John Doe", response.getName());
        assertEquals("john.doe@example.com", response.getEmail());
        assertEquals(BloodGroup.O_POSITIVE, response.getBloodGroup());
        assertTrue(response.isActive());
        assertNotNull(response.getDonorCode());
        verify(donorRepository, times(1)).save(any(Donor.class));
    }

    @Test
    @DisplayName("Should reject donor creation with duplicate email")
    void testCreateDonor_DuplicateEmail_ThrowsException() {
        DonorCreateRequest request = DonorCreateRequest.builder()
                .name("Jane Doe")
                .email("john.doe@example.com")
                .phone("+9876543210")
                .dateOfBirth(LocalDate.of(1996, 6, 15))
                .gender(Gender.FEMALE)
                .bloodGroup(BloodGroup.A_POSITIVE)
                .build();

        when(donorRepository.existsByEmail("john.doe@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> donorService.createDonor(request));
        verify(donorRepository, never()).save(any(Donor.class));
    }

    @Test
    @DisplayName("Should reject donor creation with duplicate phone")
    void testCreateDonor_DuplicatePhone_ThrowsException() {
        DonorCreateRequest request = DonorCreateRequest.builder()
                .name("Jane Doe")
                .email("jane.doe@example.com")
                .phone("+1234567890")
                .dateOfBirth(LocalDate.of(1996, 6, 15))
                .gender(Gender.FEMALE)
                .bloodGroup(BloodGroup.A_POSITIVE)
                .build();

        when(donorRepository.existsByEmail("jane.doe@example.com")).thenReturn(false);
        when(donorRepository.existsByPhone("+1234567890")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> donorService.createDonor(request));
        verify(donorRepository, never()).save(any(Donor.class));
    }

    @Test
    @DisplayName("Should retrieve donor by ID")
    void testGetDonorById_Success() {
        when(donorRepository.findById(1L)).thenReturn(Optional.of(donor));

        DonorResponse response = donorService.getDonorById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("John Doe", response.getName());
    }

    @Test
    @DisplayName("Should throw 404 when donor does not exist")
    void testGetDonorById_NotFound() {
        when(donorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> donorService.getDonorById(99L));
    }

    @Test
    @DisplayName("Should return eligible for first-time donor with no previous donations")
    void testCheckEligibility_FirstTimeDonor() {
        when(donorRepository.findById(1L)).thenReturn(Optional.of(donor));
        when(donationRepository.findTopByDonorOrderByDonationDateDescCreatedAtDesc(donor)).thenReturn(Optional.empty());

        EligibilityResponse response = donorService.checkEligibility(1L);

        assertNotNull(response);
        assertTrue(response.isEligible());
        assertNull(response.getLastDonationDate());
        assertEquals(0, response.getRemainingDays());
        assertTrue(response.getMessage().contains("First-time donor"));
    }

    @Test
    @DisplayName("Should return eligible when last donation is older than minimum gap (90 days)")
    void testCheckEligibility_EligibleAfterGap() {
        Donation pastDonation = Donation.builder()
                .id(10L)
                .donor(donor)
                .donationDate(LocalDate.now().minusDays(95))
                .numberOfUnits(1)
                .build();

        when(donorRepository.findById(1L)).thenReturn(Optional.of(donor));
        when(donationRepository.findTopByDonorOrderByDonationDateDescCreatedAtDesc(donor)).thenReturn(Optional.of(pastDonation));

        EligibilityResponse response = donorService.checkEligibility(1L);

        assertNotNull(response);
        assertTrue(response.isEligible());
        assertEquals(0, response.getRemainingDays());
        assertEquals(LocalDate.now().minusDays(95), response.getLastDonationDate());
    }

    @Test
    @DisplayName("Should return ineligible when last donation was within minimum gap (<90 days)")
    void testCheckEligibility_IneligibleWithinGap() {
        Donation recentDonation = Donation.builder()
                .id(11L)
                .donor(donor)
                .donationDate(LocalDate.now().minusDays(30))
                .numberOfUnits(1)
                .build();

        when(donorRepository.findById(1L)).thenReturn(Optional.of(donor));
        when(donationRepository.findTopByDonorOrderByDonationDateDescCreatedAtDesc(donor)).thenReturn(Optional.of(recentDonation));

        EligibilityResponse response = donorService.checkEligibility(1L);

        assertNotNull(response);
        assertFalse(response.isEligible());
        assertEquals(60, response.getRemainingDays());
        assertTrue(response.getMessage().contains("60 day(s) remaining"));
    }

    @Test
    @DisplayName("Should return ineligible when donor is deactivated/inactive")
    void testCheckEligibility_InactiveDonor() {
        donor.setActive(false);
        when(donorRepository.findById(1L)).thenReturn(Optional.of(donor));

        EligibilityResponse response = donorService.checkEligibility(1L);

        assertNotNull(response);
        assertFalse(response.isEligible());
        assertTrue(response.getMessage().contains("inactive"));
    }

    @Test
    @DisplayName("Should safely deactivate donor preserving history")
    void testDeactivateDonor() {
        when(donorRepository.findById(1L)).thenReturn(Optional.of(donor));
        when(donorRepository.save(any(Donor.class))).thenReturn(donor);

        DonorResponse response = donorService.deactivateDonor(1L);

        assertNotNull(response);
        assertFalse(donor.isActive());
        verify(donorRepository, times(1)).save(donor);
    }
}
