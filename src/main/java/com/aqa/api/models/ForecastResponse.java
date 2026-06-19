package com.aqa.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;
import java.util.List;

/**
 * ForecastResponse
 * ----------------
 * POJO representing the top-level Open-Meteo forecast response.
 * Used when deserialising the full response body for structured assertions.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ForecastResponse {

    private Double  latitude;
    private Double  longitude;
    private Double  generationtimeMs;
    private Integer utcOffsetSeconds;
    private String  timezone;
    private String  timezoneAbbreviation;
    private Double  elevation;

    private Map<String, List<Object>> hourly;
    private Map<String, String>       hourlyUnits;
    private Map<String, List<Object>> daily;
    private Map<String, String>       dailyUnits;
    private Map<String, Object>       current;
    private Map<String, String>       currentUnits;

    // Error fields
    private Boolean error;
    private String  reason;
}
