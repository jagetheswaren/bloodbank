package com.bloodbank.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BloodGroup {
    A_POSITIVE("A+"),
    A_NEGATIVE("A-"),
    B_POSITIVE("B+"),
    B_NEGATIVE("B-"),
    AB_POSITIVE("AB+"),
    AB_NEGATIVE("AB-"),
    O_POSITIVE("O+"),
    O_NEGATIVE("O-");

    private final String displayValue;

    BloodGroup(String displayValue) {
        this.displayValue = displayValue;
    }

    @JsonValue
    public String getDisplayValue() {
        return displayValue;
    }

    @JsonCreator
    public static BloodGroup fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Blood group cannot be null or empty");
        }
        String normalized = value.trim().toUpperCase();
        for (BloodGroup bg : values()) {
            if (bg.displayValue.equalsIgnoreCase(normalized) 
                    || bg.name().equalsIgnoreCase(normalized)
                    || bg.displayValue.equalsIgnoreCase(normalized.replace(" ", "+"))) {
                return bg;
            }
        }
        throw new IllegalArgumentException("Invalid blood group: " + value + ". Valid values are: A+, A-, B+, B-, AB+, AB-, O+, O-");
    }

    @Override
    public String toString() {
        return displayValue;
    }
}
