package com.bloodbank;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BloodBankIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private static Long createdDonorId;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @Order(1)
    @DisplayName("1. POST /api/donors - Should register a new donor successfully")
    void test1_RegisterDonor() throws Exception {
        Map<String, Object> donorRequest = Map.of(
                "name", "Evelyn Reed",
                "email", "evelyn.reed@example.com",
                "phone", "+15551234567",
                "dateOfBirth", "1992-04-10",
                "gender", "FEMALE",
                "bloodGroup", "O+",
                "address", "456 Elm Avenue"
        );

        String responseBody = mockMvc.perform(post("/api/donors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(donorRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.donorCode").value(startsWith("DNR-")))
                .andExpect(jsonPath("$.name").value("Evelyn Reed"))
                .andExpect(jsonPath("$.email").value("evelyn.reed@example.com"))
                .andExpect(jsonPath("$.bloodGroup").value("O+"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> responseMap = objectMapper.readValue(responseBody, Map.class);
        createdDonorId = Long.valueOf(responseMap.get("id").toString());
    }

    @Test
    @Order(2)
    @DisplayName("2. POST /api/donors - Should return 400 when invalid input is provided")
    void test2_RegisterDonor_ValidationError() throws Exception {
        Map<String, Object> invalidRequest = Map.of(
                "name", "",
                "email", "not-an-email",
                "phone", "",
                "dateOfBirth", "2099-01-01" // future date
        );

        mockMvc.perform(post("/api/donors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    @Order(3)
    @DisplayName("3. GET /api/donors/{id} - Should return 404 for nonexistent donor")
    void test3_GetDonor_NotFound() throws Exception {
        mockMvc.perform(get("/api/donors/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @Order(4)
    @DisplayName("4. GET /api/donors/{id}/eligibility - First-time donor must be eligible")
    void test4_CheckEligibility_FirstTimeEligible() throws Exception {
        mockMvc.perform(get("/api/donors/" + createdDonorId + "/eligibility"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donorId").value(createdDonorId))
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.remainingDays").value(0))
                .andExpect(jsonPath("$.message").value(containsString("First-time donor")));
    }

    @Test
    @Order(5)
    @DisplayName("5. POST /api/donations - Should register donation and generate 2 BloodUnits")
    void test5_RegisterDonation() throws Exception {
        Map<String, Object> donationRequest = Map.of(
                "donorId", createdDonorId,
                "donationDate", LocalDate.now().toString(),
                "numberOfUnits", 2,
                "notes", "Blood drive donation"
        );

        mockMvc.perform(post("/api/donations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(donationRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.donationCode").value(startsWith("DON-")))
                .andExpect(jsonPath("$.bloodGroup").value("O+"))
                .andExpect(jsonPath("$.numberOfUnits").value(2))
                .andExpect(jsonPath("$.bloodUnits", hasSize(2)))
                .andExpect(jsonPath("$.bloodUnits[0].unitCode").value(startsWith("UNT-")))
                .andExpect(jsonPath("$.bloodUnits[0].status").value("AVAILABLE"));
    }

    @Test
    @Order(6)
    @DisplayName("6. GET /api/inventory/stock - Stock must show 2 units of O+ and all 8 blood groups")
    void test6_CheckStockAfterDonation() throws Exception {
        mockMvc.perform(get("/api/inventory/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.['O+']").value(2))
                .andExpect(jsonPath("$.['A+']").value(0))
                .andExpect(jsonPath("$.['A-']").value(0))
                .andExpect(jsonPath("$.['B+']").value(0))
                .andExpect(jsonPath("$.['B-']").value(0))
                .andExpect(jsonPath("$.['AB+']").value(0))
                .andExpect(jsonPath("$.['AB-']").value(0))
                .andExpect(jsonPath("$.['O-']").value(0));
    }

    @Test
    @Order(7)
    @DisplayName("7. GET /api/donors/{id}/eligibility - Donor must now be ineligible (within 90-day gap)")
    void test7_CheckEligibilityAfterDonation_Ineligible() throws Exception {
        mockMvc.perform(get("/api/donors/" + createdDonorId + "/eligibility"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donorId").value(createdDonorId))
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.remainingDays").value(greaterThanOrEqualTo(89)))
                .andExpect(jsonPath("$.message").value(containsString("minimum donation gap")));
    }

    @Test
    @Order(8)
    @DisplayName("8. POST /api/donations - Should reject donation before 90-day gap completes")
    void test8_RegisterDonation_RejectedBeforeGap() throws Exception {
        Map<String, Object> donationRequest = Map.of(
                "donorId", createdDonorId,
                "donationDate", LocalDate.now().toString(),
                "numberOfUnits", 1,
                "notes", "Attempting immediate second donation"
        );

        mockMvc.perform(post("/api/donations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(donationRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("DONOR_NOT_ELIGIBLE"))
                .andExpect(jsonPath("$.message").value(containsString("minimum donation gap")));
    }

    @Test
    @Order(9)
    @DisplayName("9. POST /api/issues - Should issue 1 unit of O+ blood using FEFO")
    void test9_IssueBlood() throws Exception {
        Map<String, Object> issueRequest = Map.of(
                "bloodGroup", "O+",
                "numberOfUnits", 1,
                "patientName", "Marcus Vance",
                "hospitalName", "Memorial Hospital",
                "notes", "Emergency trauma patient"
        );

        mockMvc.perform(post("/api/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(issueRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientName").value("Marcus Vance"))
                .andExpect(jsonPath("$.hospitalName").value("Memorial Hospital"))
                .andExpect(jsonPath("$.requestedBloodGroup").value("O+"))
                .andExpect(jsonPath("$.numberOfUnitsIssued").value(1))
                .andExpect(jsonPath("$.issuedUnits", hasSize(1)))
                .andExpect(jsonPath("$.issuedUnits[0].issueCode").value(startsWith("ISS-")));
    }

    @Test
    @Order(10)
    @DisplayName("10. GET /api/inventory/stock - Stock must decrease to 1 unit of O+ after issue")
    void test10_StockDecreasedAfterIssue() throws Exception {
        mockMvc.perform(get("/api/inventory/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.['O+']").value(1));
    }

    @Test
    @Order(11)
    @DisplayName("11. POST /api/issues - Should return 400 when requested units exceed safe stock")
    void test11_IssueBlood_InsufficientStock() throws Exception {
        Map<String, Object> excessiveIssueRequest = Map.of(
                "bloodGroup", "O+",
                "numberOfUnits", 10, // only 1 unit remaining in inventory
                "patientName", "Sarah Connor",
                "hospitalName", "St. Jude Clinic"
        );

        mockMvc.perform(post("/api/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(excessiveIssueRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.message").value(containsString("Insufficient safe inventory")));
    }

    @Test
    @Order(12)
    @DisplayName("12. GET /api/issues - Should retrieve paginated audit log of issued units")
    void test12_GetAllIssues() throws Exception {
        mockMvc.perform(get("/api/issues?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].patientName").value("Marcus Vance"));
    }

    @Test
    @Order(13)
    @DisplayName("13. DELETE /api/donors/{id} - Should perform safe soft deactivation")
    void test13_DeactivateDonor() throws Exception {
        mockMvc.perform(delete("/api/donors/" + createdDonorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdDonorId))
                .andExpect(jsonPath("$.active").value(false));

        // Inactive donor should now be ineligible to donate
        mockMvc.perform(get("/api/donors/" + createdDonorId + "/eligibility"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.message").value(containsString("inactive")));
    }
}
