package com.bloodbank.config;

import com.bloodbank.enums.BloodGroup;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToBloodGroupConverter implements Converter<String, BloodGroup> {

    @Override
    public BloodGroup convert(String source) {
        if (source == null || source.trim().isEmpty()) {
            return null;
        }
        return BloodGroup.fromValue(source);
    }
}
