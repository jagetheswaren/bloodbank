package com.bloodbank.controller;

import com.bloodbank.dto.response.BloodUnitResponse;
import com.bloodbank.enums.BloodGroup;
import com.bloodbank.enums.BloodUnitStatus;
import com.bloodbank.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory Management", description = "APIs for tracking blood inventory, current safe stock levels, and expiry auto-flagging")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @Operation(summary = "Get paginated inventory units", description = "Retrieves all blood units with optional filtering by status and blood group")
    public ResponseEntity<Page<BloodUnitResponse>> getAllInventory(
            @RequestParam(required = false) BloodUnitStatus status,
            @RequestParam(required = false) BloodGroup bloodGroup,
            @PageableDefault(size = 10, sort = "expiryDate", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<BloodUnitResponse> units = inventoryService.getAllInventory(pageable, status, bloodGroup);
        return ResponseEntity.ok(units);
    }

    @GetMapping("/stock")
    @Operation(
        summary = "Get real-time safe stock count by blood group",
        description = "Returns count of genuinely safe, issueable AVAILABLE units across all 8 blood groups. " +
                      "Excludes EXPIRED, NEAR_EXPIRY, ISSUED, and DISCARDED units."
    )
    public ResponseEntity<Map<String, Long>> getStockLevels() {
        Map<String, Long> stock = inventoryService.getStockLevels();
        return ResponseEntity.ok(stock);
    }

    @GetMapping("/near-expiry")
    @Operation(summary = "Get blood units nearing expiry", description = "Retrieves units expiring within the configured window (e.g., 7 days)")
    public ResponseEntity<Page<BloodUnitResponse>> getNearExpiryUnits(
            @PageableDefault(size = 10, sort = "expiryDate", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<BloodUnitResponse> units = inventoryService.getNearExpiryUnits(pageable);
        return ResponseEntity.ok(units);
    }

    @GetMapping("/expired")
    @Operation(summary = "Get expired blood units", description = "Retrieves all units past their expiry date")
    public ResponseEntity<Page<BloodUnitResponse>> getExpiredUnits(
            @PageableDefault(size = 10, sort = "expiryDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<BloodUnitResponse> units = inventoryService.getExpiredUnits(pageable);
        return ResponseEntity.ok(units);
    }

    @GetMapping("/blood-group/{bloodGroup}")
    @Operation(summary = "Get inventory units by blood group", description = "Retrieves all blood units for a specific blood group")
    public ResponseEntity<Page<BloodUnitResponse>> getUnitsByBloodGroup(
            @PathVariable BloodGroup bloodGroup,
            @PageableDefault(size = 10, sort = "expiryDate", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<BloodUnitResponse> units = inventoryService.getUnitsByBloodGroup(bloodGroup, pageable);
        return ResponseEntity.ok(units);
    }

    @GetMapping("/unit/{unitCode}")
    @Operation(summary = "Get blood unit details by unit code", description = "Fetches a specific unit's status and tracking history")
    public ResponseEntity<BloodUnitResponse> getUnitByCode(@PathVariable String unitCode) {
        BloodUnitResponse response = inventoryService.getUnitByCode(unitCode);
        return ResponseEntity.ok(response);
    }
}
