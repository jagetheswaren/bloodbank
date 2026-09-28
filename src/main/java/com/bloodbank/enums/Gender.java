package com.bloodbank.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Gender {
    MALE,
    FEMALE,
    OTHER;

    @JsonCreator
    public static Gender fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Gender cannot be null or empty");
        }
        for (Gender g : values()) {
            if (g.name().equalsIgnoreCase(value.trim())) {
                return g;
            }
        }
        throw new IllegalArgumentException("Invalid gender: " + value + ". Valid values are: MALE, FEMALE, OTHER");
    }
}
