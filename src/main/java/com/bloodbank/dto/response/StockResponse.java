package com.bloodbank.dto.response;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.*;

import java.util.Collections;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockResponse {

    private Map<String, Long> stock;

    @JsonValue
    public Map<String, Long> getStock() {
        return stock != null ? stock : Collections.emptyMap();
    }
}
