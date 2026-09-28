package com.bloodbank.scheduler;

import com.bloodbank.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "bloodbank.inventory.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class InventoryStatusScheduler {

    private final InventoryService inventoryService;

    /**
     * Periodically inspects inventory to auto-flag units nearing expiry within 7 days
     * and mark past-due units as EXPIRED.
     */
    @Scheduled(fixedRateString = "${bloodbank.inventory.scheduler-rate-ms:60000}")
    public void runInventoryStatusEvaluation() {
        log.debug("Running scheduled inventory status evaluation...");
        try {
            int updatedUnits = inventoryService.updateInventoryStatuses();
            if (updatedUnits > 0) {
                log.info("Scheduled task updated {} blood unit(s) based on expiry date rules.", updatedUnits);
            }
        } catch (Exception ex) {
            log.error("Error during scheduled inventory status evaluation: {}", ex.getMessage(), ex);
        }
    }
}
