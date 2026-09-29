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
    private Demo demo = new Demo();
    private Mail mail = new Mail();

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

    @Getter
    @Setter
    public static class Demo {
        /**
         * Whether to seed synthetic demo records on application startup.
         */
        private boolean seedEnabled = true;
    }

    @Getter
    @Setter
    public static class Mail {
        /**
         * Whether email notifications are enabled.
         */
        private boolean enabled = false;

        /**
         * Sender email address.
         */
        private String from = "no-reply@bloodbank.org";

        /**
         * Operational/Admin alert recipient email address.
         */
        private String adminEmail = "admin@bloodbank.org";

        /**
         * Whether daily scheduled donor eligibility reminders are enabled.
         */
        private boolean eligibilityRemindersEnabled = false;
    }
}
