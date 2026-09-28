package com.bloodbank.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "bloodbank")
public class BloodBankProperties {

    private Donation donation = new Donation();
    private Inventory inventory = new Inventory();
    private BloodUnit bloodUnit = new BloodUnit();

    @Getter
    @Setter
    public static class Donation {
        /**
         * Minimum gap in days between two donations for the same donor.
         */
        private int minimumGapDays = 90;
    }

    @Getter
    @Setter
    public static class Inventory {
        /**
         * Days threshold before expiry to flag a unit as NEAR_EXPIRY.
         */
        private int nearExpiryDays = 7;
    }

    @Getter
    @Setter
    public static class BloodUnit {
        /**
         * Standard shelf life of a collected blood unit in days.
         */
        private int shelfLifeDays = 42;
    }
}
