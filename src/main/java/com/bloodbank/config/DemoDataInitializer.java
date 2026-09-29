package com.bloodbank.config;

import com.bloodbank.entity.BloodUnit;
import com.bloodbank.entity.Donation;
import com.bloodbank.entity.Donor;
import com.bloodbank.entity.IssueRecord;
import com.bloodbank.enums.BloodGroup;
import com.bloodbank.enums.BloodUnitStatus;
import com.bloodbank.enums.Gender;
import com.bloodbank.repository.BloodUnitRepository;
import com.bloodbank.repository.DonationRepository;
import com.bloodbank.repository.DonorRepository;
import com.bloodbank.repository.IssueRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Synthetic Demo Data Initializer
 * Populates the database with realistic synthetic demonstration data for college assessment,
 * viva presentations, and immediate local evaluation.
 *
 * All data is strictly synthetic and uses safe demo domains (@example.test).
 * Fully idempotent: will not insert duplicate records on restart.
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class DemoDataInitializer implements CommandLineRunner {

    private final DonorRepository donorRepository;
    private final DonationRepository donationRepository;
    private final BloodUnitRepository bloodUnitRepository;
    private final IssueRecordRepository issueRecordRepository;
    private final BloodBankProperties bloodBankProperties;

    @Override
    @Transactional
    public void run(String... args) {
        if (!bloodBankProperties.getDemo().isSeedEnabled()) {
            log.info("Demo data seeding is disabled (bloodbank.demo.seed-enabled=false)");
            return;
        }

        // Idempotency check: avoid reseeding if demo records already exist
        if (donorRepository.findByDonorCode("DNR-DEMO-001").isPresent()) {
            log.info("Synthetic demo dataset already initialized. Skipping seeding.");
            return;
        }

        log.info("Initializing synthetic demonstration dataset for BloodBank...");
        LocalDate today = LocalDate.now();

        // ==========================================
        // 1. SYNTHETIC DONORS (24 Donors covering all 8 Blood Groups)
        // ==========================================
        List<Donor> donors = new ArrayList<>();

        // Eligibility Case 1: First-time active donor (eligible = true)
        Donor d01 = createDonor("DNR-DEMO-001", "Arjun Kumar", "arjun.kumar01@example.test", "+91-9000000001",
                LocalDate.of(1995, 4, 12), Gender.MALE, BloodGroup.O_POSITIVE, "Coimbatore, Tamil Nadu", true);

        // Eligibility Case 2: Last donated 30 days ago (eligible = false, ~60 days remaining)
        Donor d02 = createDonor("DNR-DEMO-002", "Priya Sharma", "priya.sharma02@example.test", "+91-9000000002",
                LocalDate.of(1998, 8, 23), Gender.FEMALE, BloodGroup.A_POSITIVE, "Chennai, Tamil Nadu", true);

        // Eligibility Case 3: Last donated 89 days ago (eligible = false, 1 day remaining)
        Donor d03 = createDonor("DNR-DEMO-003", "Rajesh Patel", "rajesh.patel03@example.test", "+91-9000000003",
                LocalDate.of(1992, 11, 5), Gender.MALE, BloodGroup.B_POSITIVE, "Ahmedabad, Gujarat", true);

        // Eligibility Case 4: Last donated exactly 90 days ago (eligible = true)
        Donor d04 = createDonor("DNR-DEMO-004", "Sneha Nair", "sneha.nair04@example.test", "+91-9000000004",
                LocalDate.of(1996, 2, 17), Gender.FEMALE, BloodGroup.O_NEGATIVE, "Kochi, Kerala", true);

        // Eligibility Case 5: Last donated 120 days ago (eligible = true)
        Donor d05 = createDonor("DNR-DEMO-005", "Vikram Singh", "vikram.singh05@example.test", "+91-9000000005",
                LocalDate.of(1990, 9, 30), Gender.MALE, BloodGroup.AB_POSITIVE, "Jaipur, Rajasthan", true);

        // Eligibility Case 6: Inactive donor (eligible = false, deactivated)
        Donor d06 = createDonor("DNR-DEMO-006", "Ananya Desai", "ananya.desai06@example.test", "+91-9000000006",
                LocalDate.of(2001, 3, 14), Gender.FEMALE, BloodGroup.A_NEGATIVE, "Pune, Maharashtra", false);

        // Additional active donors across all blood groups
        Donor d07 = createDonor("DNR-DEMO-007", "Karthik Swaminathan", "karthik.s07@example.test", "+91-9000000007",
                LocalDate.of(1993, 7, 22), Gender.MALE, BloodGroup.O_POSITIVE, "Bangalore, Karnataka", true);
        Donor d08 = createDonor("DNR-DEMO-008", "Meera Krishnan", "meera.k08@example.test", "+91-9000000008",
                LocalDate.of(1997, 12, 8), Gender.FEMALE, BloodGroup.B_NEGATIVE, "Madurai, Tamil Nadu", true);
        Donor d09 = createDonor("DNR-DEMO-009", "Rohan Verma", "rohan.v09@example.test", "+91-9000000009",
                LocalDate.of(1994, 5, 19), Gender.MALE, BloodGroup.AB_NEGATIVE, "Lucknow, Uttar Pradesh", true);
        Donor d10 = createDonor("DNR-DEMO-010", "Deepa Sundaram", "deepa.s10@example.test", "+91-9000000010",
                LocalDate.of(1999, 10, 11), Gender.FEMALE, BloodGroup.A_POSITIVE, "Salem, Tamil Nadu", true);
        Donor d11 = createDonor("DNR-DEMO-011", "Manoj Joshi", "manoj.j11@example.test", "+91-9000000011",
                LocalDate.of(1988, 6, 25), Gender.MALE, BloodGroup.B_POSITIVE, "Indore, Madhya Pradesh", true);
        Donor d12 = createDonor("DNR-DEMO-012", "Divya Pillai", "divya.p12@example.test", "+91-9000000012",
                LocalDate.of(2000, 1, 30), Gender.FEMALE, BloodGroup.O_POSITIVE, "Thiruvananthapuram, Kerala", true);
        Donor d13 = createDonor("DNR-DEMO-013", "Suresh Reddy", "suresh.r13@example.test", "+91-9000000013",
                LocalDate.of(1991, 4, 18), Gender.MALE, BloodGroup.A_POSITIVE, "Hyderabad, Telangana", true);
        Donor d14 = createDonor("DNR-DEMO-014", "Kavita Roy", "kavita.r14@example.test", "+91-9000000014",
                LocalDate.of(1996, 8, 9), Gender.FEMALE, BloodGroup.B_POSITIVE, "Kolkata, West Bengal", true);
        Donor d15 = createDonor("DNR-DEMO-015", "Sanjay Hegde", "sanjay.h15@example.test", "+91-9000000015",
                LocalDate.of(1989, 12, 3), Gender.MALE, BloodGroup.O_POSITIVE, "Mangalore, Karnataka", true);
        Donor d16 = createDonor("DNR-DEMO-016", "Pooja Iyer", "pooja.i16@example.test", "+91-9000000016",
                LocalDate.of(1998, 2, 27), Gender.FEMALE, BloodGroup.AB_POSITIVE, "Tiruchirappalli, Tamil Nadu", true);
        Donor d17 = createDonor("DNR-DEMO-017", "Harish Bhat", "harish.b17@example.test", "+91-9000000017",
                LocalDate.of(1995, 9, 15), Gender.MALE, BloodGroup.A_NEGATIVE, "Mysuru, Karnataka", true);
        Donor d18 = createDonor("DNR-DEMO-018", "Ritu Agarwal", "ritu.a18@example.test", "+91-9000000018",
                LocalDate.of(1993, 3, 8), Gender.FEMALE, BloodGroup.O_NEGATIVE, "New Delhi, Delhi", true);
        Donor d19 = createDonor("DNR-DEMO-019", "Alok Mukherjee", "alok.m19@example.test", "+91-9000000019",
                LocalDate.of(1992, 7, 4), Gender.MALE, BloodGroup.B_NEGATIVE, "Asansol, West Bengal", true);
        Donor d20 = createDonor("DNR-DEMO-020", "Shreya Menon", "shreya.m20@example.test", "+91-9000000020",
                LocalDate.of(2002, 11, 20), Gender.FEMALE, BloodGroup.AB_NEGATIVE, "Thrissur, Kerala", true);
        Donor d21 = createDonor("DNR-DEMO-021", "Balaji Venkat", "balaji.v21@example.test", "+91-9000000021",
                LocalDate.of(1987, 5, 11), Gender.MALE, BloodGroup.O_POSITIVE, "Vellore, Tamil Nadu", true);
        Donor d22 = createDonor("DNR-DEMO-022", "Sunita Rao", "sunita.r22@example.test", "+91-9000000022",
                LocalDate.of(1996, 1, 16), Gender.FEMALE, BloodGroup.A_POSITIVE, "Vijayawada, Andhra Pradesh", true);
        Donor d23 = createDonor("DNR-DEMO-023", "Prakash Chawla", "prakash.c23@example.test", "+91-9000000023",
                LocalDate.of(1994, 10, 2), Gender.MALE, BloodGroup.B_POSITIVE, "Chandigarh, Punjab", true);
        Donor d24 = createDonor("DNR-DEMO-024", "Nithya Nambiar", "nithya.n24@example.test", "+91-9000000024",
                LocalDate.of(2001, 8, 25), Gender.FEMALE, BloodGroup.O_POSITIVE, "Kozhikode, Kerala", true);

        donors.addAll(List.of(d01, d02, d03, d04, d05, d06, d07, d08, d09, d10,
                d11, d12, d13, d14, d15, d16, d17, d18, d19, d20, d21, d22, d23, d24));
        donorRepository.saveAll(donors);

        // ==========================================
        // 2. HISTORICAL & RECENT DONATIONS
        // ==========================================
        // Donation for d02: 30 days ago (blocking repeat donation)
        Donation don02 = createDonation("DON-DEMO-002", d02, today.minusDays(30), 1, "Regular voluntary donor");
        // Donation for d03: 89 days ago (1 day remaining before 90-day threshold)
        Donation don03 = createDonation("DON-DEMO-003", d03, today.minusDays(89), 1, "Community blood donation camp");
        // Donation for d04: 90 days ago (exact threshold, now eligible)
        Donation don04 = createDonation("DON-DEMO-004", d04, today.minusDays(90), 1, "Voluntary campus blood drive");
        // Donation for d05: 120 days ago (well past 90 days, eligible)
        Donation don05 = createDonation("DON-DEMO-005", d05, today.minusDays(120), 1, "Rotary Club blood donation initiative");

        // FEFO Demonstration donations for O+
        // Donation A: 32 days ago -> expiry in today + 10 days
        Donation donFEFO_A = createDonation("DON-DEMO-FEFO-A", d07, today.minusDays(32), 1, "FEFO Demo: Unit A earliest safe expiry");
        // Donation B: 27 days ago -> expiry in today + 15 days
        Donation donFEFO_B = createDonation("DON-DEMO-FEFO-B", d15, today.minusDays(27), 1, "FEFO Demo: Unit B medium expiry");
        // Donation C: 17 days ago -> expiry in today + 25 days
        Donation donFEFO_C = createDonation("DON-DEMO-FEFO-C", d21, today.minusDays(17), 1, "FEFO Demo: Unit C latest expiry");
        // Donation Near-Expiry: 39 days ago -> expiry in today + 3 days (must be skipped by FEFO)
        Donation donFEFO_NE = createDonation("DON-DEMO-FEFO-NE", d12, today.minusDays(39), 1, "FEFO Demo: Near-expiry unit (<=7 days)");

        // Donations across other donors
        Donation don08 = createDonation("DON-DEMO-008", d08, today.minusDays(21), 1, "Hospital voluntary collection");
        Donation don09 = createDonation("DON-DEMO-009", d09, today.minusDays(13), 1, "Red Cross blood camp");
        Donation don10 = createDonation("DON-DEMO-010", d10, today.minusDays(24), 2, "Corporate CSR blood drive (2 units)");
        Donation don11 = createDonation("DON-DEMO-011", d11, today.minusDays(28), 1, "General voluntary donation");
        Donation don13 = createDonation("DON-DEMO-013", d13, today.minusDays(12), 2, "College youth festival drive (2 units)");
        Donation don14 = createDonation("DON-DEMO-014", d14, today.minusDays(7), 1, "Rotary voluntary camp");
        Donation don16 = createDonation("DON-DEMO-016", d16, today.minusDays(16), 1, "Hospital blood bank drive");
        Donation don17 = createDonation("DON-DEMO-017", d17, today.minusDays(22), 1, "Voluntary regular donation");
        Donation don18 = createDonation("DON-DEMO-018", d18, today.minusDays(23), 1, "Emergency donor panel call-in");
        Donation don19 = createDonation("DON-DEMO-019", d19, today.minusDays(14), 1, "Community blood donation initiative");
        Donation don20 = createDonation("DON-DEMO-020", d20, today.minusDays(13), 1, "Medical college blood camp");
        Donation don22 = createDonation("DON-DEMO-022", d22, today.minusDays(18), 1, "Regular voluntary donor");
        Donation don23 = createDonation("DON-DEMO-023", d23, today.minusDays(20), 1, "Civic center blood drive");
        Donation don24 = createDonation("DON-DEMO-024", d24, today.minusDays(11), 1, "Voluntary blood donation");

        // Historical expired / issued donations
        Donation donHist1 = createDonation("DON-DEMO-HIST-1", d10, today.minusDays(50), 1, "Historical donation (expired unit)");
        Donation donHist2 = createDonation("DON-DEMO-HIST-2", d11, today.minusDays(60), 1, "Historical donation (expired unit)");
        Donation donHist3 = createDonation("DON-DEMO-HIST-3", d04, today.minusDays(45), 1, "Historical donation (expired O- unit)");
        Donation donIss1 = createDonation("DON-DEMO-HIST-4", d21, today.minusDays(20), 1, "Historical donation (issued O+ unit)");
        Donation donIss2 = createDonation("DON-DEMO-HIST-5", d23, today.minusDays(25), 1, "Historical donation (issued B+ unit)");
        Donation donIss3 = createDonation("DON-DEMO-HIST-6", d22, today.minusDays(15), 1, "Historical donation (issued A+ unit)");
        Donation donDisc = createDonation("DON-DEMO-HIST-7", d13, today.minusDays(10), 1, "Historical donation (discarded unit)");

        List<Donation> allDonations = List.of(
                don02, don03, don04, don05, donFEFO_A, donFEFO_B, donFEFO_C, donFEFO_NE,
                don08, don09, don10, don11, don13, don14, don16, don17, don18, don19, don20,
                don22, don23, don24, donHist1, donHist2, donHist3, donIss1, donIss2, donIss3, donDisc
        );
        donationRepository.saveAll(allDonations);

        // ==========================================
        // 3. BLOOD UNITS (Safe, Near-Expiry, Expired, Issued, Discarded)
        // ==========================================
        List<BloodUnit> units = new ArrayList<>();

        // --- FEFO Test Demonstration Units (O+) ---
        // Unit A (expires in 10 days: Earliest safe unit -> FEFO MUST ISSUE THIS FIRST)
        BloodUnit uFEFO_A = createUnit("UNT-DEMO-O-FEFO-A", donFEFO_A, BloodGroup.O_POSITIVE,
                today.minusDays(32), today.plusDays(10), BloodUnitStatus.AVAILABLE);

        // Unit B (expires in 15 days)
        BloodUnit uFEFO_B = createUnit("UNT-DEMO-O-FEFO-B", donFEFO_B, BloodGroup.O_POSITIVE,
                today.minusDays(27), today.plusDays(15), BloodUnitStatus.AVAILABLE);

        // Unit C (expires in 25 days)
        BloodUnit uFEFO_C = createUnit("UNT-DEMO-O-FEFO-C", donFEFO_C, BloodGroup.O_POSITIVE,
                today.minusDays(17), today.plusDays(25), BloodUnitStatus.AVAILABLE);

        // Unit Near Expiry for O+ (expires in 3 days: <= 7 days window, FEFO MUST SKIP THIS)
        BloodUnit uFEFO_NE = createUnit("UNT-DEMO-O-FEFO-NE", donFEFO_NE, BloodGroup.O_POSITIVE,
                today.minusDays(39), today.plusDays(3), BloodUnitStatus.NEAR_EXPIRY);

        // --- Near-Expiry Units (< 7 days) ---
        // A+ expiring in 2 days
        BloodUnit uNE_A = createUnit("UNT-DEMO-NE-A", don02, BloodGroup.A_POSITIVE,
                today.minusDays(40), today.plusDays(2), BloodUnitStatus.NEAR_EXPIRY);
        // B+ expiring in 4 days
        BloodUnit uNE_B = createUnit("UNT-DEMO-NE-B", don11, BloodGroup.B_POSITIVE,
                today.minusDays(38), today.plusDays(4), BloodUnitStatus.NEAR_EXPIRY);

        // --- Expired Units (< today) ---
        BloodUnit uEXP_A = createUnit("UNT-DEMO-EXP-A", donHist1, BloodGroup.A_POSITIVE,
                today.minusDays(50), today.minusDays(8), BloodUnitStatus.EXPIRED);
        BloodUnit uEXP_B = createUnit("UNT-DEMO-EXP-B", donHist2, BloodGroup.B_POSITIVE,
                today.minusDays(60), today.minusDays(18), BloodUnitStatus.EXPIRED);
        BloodUnit uEXP_O = createUnit("UNT-DEMO-EXP-O", donHist3, BloodGroup.O_NEGATIVE,
                today.minusDays(45), today.minusDays(3), BloodUnitStatus.EXPIRED);

        // --- Safe AVAILABLE Units Across All 8 Groups ---
        // A+ (3 additional units from don10 and don13)
        BloodUnit uA_01 = createUnit("UNT-DEMO-A-01", don10, BloodGroup.A_POSITIVE,
                today.minusDays(24), today.plusDays(18), BloodUnitStatus.AVAILABLE);
        BloodUnit uA_02 = createUnit("UNT-DEMO-A-02", don10, BloodGroup.A_POSITIVE,
                today.minusDays(24), today.plusDays(18), BloodUnitStatus.AVAILABLE);
        BloodUnit uA_03 = createUnit("UNT-DEMO-A-03", don13, BloodGroup.A_POSITIVE,
                today.minusDays(12), today.plusDays(30), BloodUnitStatus.AVAILABLE);
        BloodUnit uA_04 = createUnit("UNT-DEMO-A-04", don13, BloodGroup.A_POSITIVE,
                today.minusDays(12), today.plusDays(30), BloodUnitStatus.AVAILABLE);
        BloodUnit uA_05 = createUnit("UNT-DEMO-A-05", don22, BloodGroup.A_POSITIVE,
                today.minusDays(18), today.plusDays(24), BloodUnitStatus.AVAILABLE);

        // A- (1 unit from don17)
        BloodUnit uAN_01 = createUnit("UNT-DEMO-AN-01", don17, BloodGroup.A_NEGATIVE,
                today.minusDays(22), today.plusDays(20), BloodUnitStatus.AVAILABLE);

        // B+ (2 units from don14 and don23)
        BloodUnit uB_01 = createUnit("UNT-DEMO-B-01", don14, BloodGroup.B_POSITIVE,
                today.minusDays(7), today.plusDays(35), BloodUnitStatus.AVAILABLE);
        BloodUnit uB_02 = createUnit("UNT-DEMO-B-02", don23, BloodGroup.B_POSITIVE,
                today.minusDays(20), today.plusDays(22), BloodUnitStatus.AVAILABLE);

        // B- (2 units from don08 and don19)
        BloodUnit uBN_01 = createUnit("UNT-DEMO-BN-01", don08, BloodGroup.B_NEGATIVE,
                today.minusDays(21), today.plusDays(21), BloodUnitStatus.AVAILABLE);
        BloodUnit uBN_02 = createUnit("UNT-DEMO-BN-02", don19, BloodGroup.B_NEGATIVE,
                today.minusDays(14), today.plusDays(28), BloodUnitStatus.AVAILABLE);

        // AB+ (2 units from don05 and don16)
        BloodUnit uABP_01 = createUnit("UNT-DEMO-ABP-01", don05, BloodGroup.AB_POSITIVE,
                today.minusDays(26), today.plusDays(16), BloodUnitStatus.AVAILABLE);
        BloodUnit uABP_02 = createUnit("UNT-DEMO-ABP-02", don16, BloodGroup.AB_POSITIVE,
                today.minusDays(16), today.plusDays(26), BloodUnitStatus.AVAILABLE);

        // AB- (2 units from don09 and don20)
        BloodUnit uABN_01 = createUnit("UNT-DEMO-ABN-01", don09, BloodGroup.AB_NEGATIVE,
                today.minusDays(13), today.plusDays(29), BloodUnitStatus.AVAILABLE);
        BloodUnit uABN_02 = createUnit("UNT-DEMO-ABN-02", don20, BloodGroup.AB_NEGATIVE,
                today.minusDays(13), today.plusDays(29), BloodUnitStatus.AVAILABLE);

        // O+ (1 additional unit from don24)
        BloodUnit uO_01 = createUnit("UNT-DEMO-O-01", don24, BloodGroup.O_POSITIVE,
                today.minusDays(11), today.plusDays(31), BloodUnitStatus.AVAILABLE);

        // O- (1 unit from don18)
        BloodUnit uON_01 = createUnit("UNT-DEMO-ON-01", don18, BloodGroup.O_NEGATIVE,
                today.minusDays(23), today.plusDays(19), BloodUnitStatus.AVAILABLE);

        // --- Issued Units ---
        BloodUnit uISS_1 = createUnit("UNT-DEMO-ISS-01", donIss1, BloodGroup.O_POSITIVE,
                today.minusDays(20), today.plusDays(22), BloodUnitStatus.ISSUED);
        BloodUnit uISS_2 = createUnit("UNT-DEMO-ISS-02", donIss2, BloodGroup.B_POSITIVE,
                today.minusDays(25), today.plusDays(17), BloodUnitStatus.ISSUED);
        BloodUnit uISS_3 = createUnit("UNT-DEMO-ISS-03", donIss3, BloodGroup.A_POSITIVE,
                today.minusDays(15), today.plusDays(27), BloodUnitStatus.ISSUED);

        // --- Discarded Unit ---
        BloodUnit uDISC_1 = createUnit("UNT-DEMO-DIS-01", donDisc, BloodGroup.A_POSITIVE,
                today.minusDays(10), today.plusDays(32), BloodUnitStatus.DISCARDED);

        units.addAll(List.of(
                uFEFO_A, uFEFO_B, uFEFO_C, uFEFO_NE,
                uNE_A, uNE_B,
                uEXP_A, uEXP_B, uEXP_O,
                uA_01, uA_02, uA_03, uA_04, uA_05,
                uAN_01,
                uB_01, uB_02,
                uBN_01, uBN_02,
                uABP_01, uABP_02,
                uABN_01, uABN_02,
                uO_01,
                uON_01,
                uISS_1, uISS_2, uISS_3,
                uDISC_1
        ));
        bloodUnitRepository.saveAll(units);

        // ==========================================
        // 4. HISTORICAL ISSUE RECORDS
        // ==========================================
        IssueRecord rec1 = IssueRecord.builder()
                .issueCode("ISS-DEMO-001")
                .bloodUnit(uISS_1)
                .patientName("Ravi Demo")
                .hospitalName("City Care Demo Hospital")
                .requestedBloodGroup(BloodGroup.O_POSITIVE)
                .issueDate(today.minusDays(2).atTime(11, 30))
                .notes("Emergency trauma transfusion demo record")
                .build();

        IssueRecord rec2 = IssueRecord.builder()
                .issueCode("ISS-DEMO-002")
                .bloodUnit(uISS_2)
                .patientName("Meena Demo")
                .hospitalName("Regional Medical Demo Centre")
                .requestedBloodGroup(BloodGroup.B_POSITIVE)
                .issueDate(today.minusDays(5).atTime(15, 45))
                .notes("Scheduled surgical procedure demo record")
                .build();

        IssueRecord rec3 = IssueRecord.builder()
                .issueCode("ISS-DEMO-003")
                .bloodUnit(uISS_3)
                .patientName("Kavita Demo")
                .hospitalName("Apex Specialty Hospital")
                .requestedBloodGroup(BloodGroup.A_POSITIVE)
                .issueDate(today.minusDays(1).atTime(9, 15))
                .notes("ICU patient replacement unit")
                .build();

        issueRecordRepository.saveAll(List.of(rec1, rec2, rec3));

        log.info("Synthetic demo dataset successfully initialized: {} donors, {} donations, {} blood units, {} issue records.",
                donors.size(), allDonations.size(), units.size(), 3);
    }

    private Donor createDonor(String code, String name, String email, String phone,
                              LocalDate dob, Gender gender, BloodGroup bloodGroup, String address, boolean active) {
        return Donor.builder()
                .donorCode(code)
                .name(name)
                .email(email)
                .phone(phone)
                .dateOfBirth(dob)
                .gender(gender)
                .bloodGroup(bloodGroup)
                .address(address)
                .active(active)
                .build();
    }

    private Donation createDonation(String code, Donor donor, LocalDate donationDate, int units, String notes) {
        return Donation.builder()
                .donationCode(code)
                .donor(donor)
                .donationDate(donationDate)
                .numberOfUnits(units)
                .notes(notes)
                .build();
    }

    private BloodUnit createUnit(String code, Donation donation, BloodGroup bloodGroup,
                                 LocalDate collectionDate, LocalDate expiryDate, BloodUnitStatus status) {
        return BloodUnit.builder()
                .unitCode(code)
                .donation(donation)
                .bloodGroup(bloodGroup)
                .collectionDate(collectionDate)
                .expiryDate(expiryDate)
                .status(status)
                .build();
    }
}
