package com.aqa.api.config;

/**
 * Central configuration for API Automation.
 * API chosen: Open-Meteo (https://open-meteo.com/)
 * - Free, no API key required
 * - Stable schema, rich parametrize surface
 */
public final class APIConfig {

    private APIConfig() {}

    // ── Endpoints ─────────────────────────────────────────────────────────────
    public static final String BASE_URL          = "https://api.open-meteo.com";
    public static final String FORECAST_ENDPOINT = "/v1/forecast";

    // ── Timeouts ──────────────────────────────────────────────────────────────
    public static final int CONNECTION_TIMEOUT = 10_000;  // ms
    public static final int READ_TIMEOUT       = 15_000;  // ms

    // ── Test data: cities with known lat/lon ──────────────────────────────────
    public static final double[][] CITY_COORDS = {
            // { latitude, longitude }
            {12.9716,  77.5946},   // Bengaluru
            {51.5074,  -0.1278},   // London
            {40.7128, -74.0060},   // New York
            {35.6762, 139.6503},   // Tokyo
            {-33.8688, 151.2093},  // Sydney
    };

    public static final String[] CITY_NAMES = {
            "Bengaluru", "London", "New York", "Tokyo", "Sydney"
    };

    // ── Valid variable sets ───────────────────────────────────────────────────
    public static final String[] HOURLY_VARS  = {
            "temperature_2m", "relative_humidity_2m", "wind_speed_10m"
    };
    public static final String[] DAILY_VARS   = {
            "temperature_2m_max", "temperature_2m_min", "precipitation_sum"
    };
    public static final String[] CURRENT_VARS = {
            "temperature_2m", "wind_speed_10m", "weather_code"
    };

    // ── Unit options ──────────────────────────────────────────────────────────
    public static final String[] TEMPERATURE_UNITS = { "celsius", "fahrenheit" };
    public static final String[] WIND_SPEED_UNITS  = { "kmh", "mph", "ms", "kn" };

    // ── Forecast day counts to test ───────────────────────────────────────────
    public static final int[] FORECAST_DAYS = { 1, 3, 7, 14 };

    // ── Reports ───────────────────────────────────────────────────────────────
    public static final String REPORTS_DIR = "test-output/reports";
}
