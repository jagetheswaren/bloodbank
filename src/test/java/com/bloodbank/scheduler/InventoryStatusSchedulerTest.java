package com.bloodbank.scheduler;

import com.bloodbank.service.InventoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryStatusSchedulerTest {

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private InventoryStatusScheduler scheduler;

    @Test
    @DisplayName("Should invoke inventoryService.updateInventoryStatuses() during scheduled run")
    void testRunInventoryStatusEvaluation() {
        when(inventoryService.updateInventoryStatuses()).thenReturn(3);

        scheduler.runInventoryStatusEvaluation();

        verify(inventoryService, times(1)).updateInventoryStatuses();
    }
}
