package com.aqa.api.tests;

import com.aqa.api.config.APIConfig;
import com.aqa.api.validators.ResponseValidator;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * OpenMeteoAPITest
 * ----------------
 * Automated API tests for Open-Meteo Forecast API (https://open-meteo.com/)
 * No API key required — free, production-grade, publicly accessible.
 *
 * Test case table:
 * ┌────┬──────────────────────────────────────────────────────┬──────────────────────────────────────────────────────┐
 * │ TC │ Test Method                                          │ Validation + Why                                     │
 * ├────┼──────────────────────────────────────────────────────┼──────────────────────────────────────────────────────┤
 * │ 01 │ testForecastReturns200ForValidCities                 │ HTTP 200 + JSON CT — smoke test for all 5 cities     │
 * │ 02 │ testResponseContainsRequiredTopLevelKeys             │ Schema: 6 mandatory keys always present              │
 * │ 03 │ testHourlyVariablesReturnedInResponse                │ hourly block + equal-length time & variable arrays   │
 * │ 04 │ testDailyVariablesReturnedInResponse                 │ daily block + 7-element arrays by default            │
 * │ 05 │ testCurrentWeatherReturnedInResponse                 │ current block, ISO-8601 time, numeric variable value │
 * │ 06 │ testTemperatureUnitSwitching                         │ Unit symbol in metadata, plausible temp range        │
 * │ 07 │ testWindSpeedUnitParameterised                       │ 4 wind unit codes accepted, echoed in response       │
 * │ 08 │ testCoordinateEchoAccuracy                           │ lat/lon in response within ±1° of requested          │
 * │ 09 │ testInvalidCoordinatesReturnError                    │ HTTP 400/422 + error:true for 4 bad coord pairs      │
 * │ 10 │ testMissingRequiredParamReturnsError                 │ HTTP 400/422 when latitude param omitted             │
 * │ 11 │ testForecastDaysParameterRespectsArrayLength         │ hourly.time.length == forecastDays × 24              │
 * │ 12 │ testTimezoneFieldIsValidString                       │ timezone is non-empty string for all cities          │
 * └────┴──────────────────────────────────────────────────────┴──────────────────────────────────────────────────────┘
 */
public class OpenMeteoAPITest extends BaseAPITest {

    // ─────────────────────────────────────────────────────────────────────────
    // DataProviders
    // ─────────────────────────────────────────────────────────────────────────

    @DataProvider(name = "cities")
    public Object[][] citiesProvider() {
        Object[][] data = new Object[APIConfig.CITY_NAMES.length][3];
        for (int i = 0; i < APIConfig.CITY_NAMES.length; i++) {
            data[i][0] = APIConfig.CITY_NAMES[i];
            data[i][1] = APIConfig.CITY_COORDS[i][0];
            data[i][2] = APIConfig.CITY_COORDS[i][1];
        }
        return data;
    }

    @DataProvider(name = "hourlyVars")
    public Object[][] hourlyVarsProvider() {
        Object[][] d = new Object[APIConfig.HOURLY_VARS.length][1];
        for (int i = 0; i < APIConfig.HOURLY_VARS.length; i++) d[i][0] = APIConfig.HOURLY_VARS[i];
        return d;
    }

    @DataProvider(name = "dailyVars")
    public Object[][] dailyVarsProvider() {
        Object[][] d = new Object[APIConfig.DAILY_VARS.length][1];
        for (int i = 0; i < APIConfig.DAILY_VARS.length; i++) d[i][0] = APIConfig.DAILY_VARS[i];
        return d;
    }

    @DataProvider(name = "currentVars")
    public Object[][] currentVarsProvider() {
        Object[][] d = new Object[APIConfig.CURRENT_VARS.length][1];
        for (int i = 0; i < APIConfig.CURRENT_VARS.length; i++) d[i][0] = APIConfig.CURRENT_VARS[i];
        return d;
    }

    @DataProvider(name = "temperatureUnits")
    public Object[][] temperatureUnitsProvider() {
        Object[][] d = new Object[APIConfig.TEMPERATURE_UNITS.length][1];
        for (int i = 0; i < APIConfig.TEMPERATURE_UNITS.length; i++) d[i][0] = APIConfig.TEMPERATURE_UNITS[i];
        return d;
    }

    @DataProvider(name = "windSpeedUnits")
    public Object[][] windSpeedUnitsProvider() {
        Object[][] d = new Object[APIConfig.WIND_SPEED_UNITS.length][1];
        for (int i = 0; i < APIConfig.WIND_SPEED_UNITS.length; i++) d[i][0] = APIConfig.WIND_SPEED_UNITS[i];
        return d;
    }

    @DataProvider(name = "invalidCoords")
    public Object[][] invalidCoordsProvider() {
        return new Object[][] {
                { 999.0,   999.0,  "out-of-range both"  },
                {  91.0,     0.0,  "latitude > 90"      },
                {   0.0,   181.0,  "longitude > 180"    },
                { -91.0,     0.0,  "latitude < -90"     },
        };
    }

    @DataProvider(name = "forecastDays")
    public Object[][] forecastDaysProvider() {
        Object[][] d = new Object[APIConfig.FORECAST_DAYS.length][1];
        for (int i = 0; i < APIConfig.FORECAST_DAYS.length; i++) d[i][0] = APIConfig.FORECAST_DAYS[i];
        return d;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TC-01  HTTP 200 for all cities
    // ─────────────────────────────────────────────────────────────────────────
    @Test(dataProvider = "cities",
          description = "TC-01: HTTP 200 + JSON content-type for valid city coordinates")
    public void testForecastReturns200ForValidCities(String city, double lat, double lon) {
        getTest().info("TC-01 | City: " + city);
        Response response = client.getForecast(lat, lon);
        ResponseValidator.assertStatus200(response);
        ResponseValidator.assertJsonContentType(response);
        ResponseValidator.assertNoErrorInResponse(response);
        getTest().pass(city + " → HTTP 200 ✓");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TC-02  Required top-level schema keys
    // ─────────────────────────────────────────────────────────────────────────
    @Test(dataProvider = "cities",
          description = "TC-02: Response contains all mandatory top-level keys")
    public void testResponseContainsRequiredTopLevelKeys(String city, double lat, double lon) {
        getTest().info("TC-02 | City: " + city);
        Response response = client.getForecast(lat, lon);
        ResponseValidator.assertStatus200(response);
        ResponseValidator.assertTopLevelKeysPresent(response,
                "latitude", "longitude", "generationtime_ms",
                "utc_offset_seconds", "timezone", "timezone_abbreviation");
        getTest().pass(city + " → all schema keys present ✓");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TC-03  Hourly variables
    // ─────────────────────────────────────────────────────────────────────────
    @Test(dataProvider = "hourlyVars",
          description = "TC-03: Requested hourly variable returns array of correct length")
    public void testHourlyVariablesReturnedInResponse(String variable) {
        getTest().info("TC-03 | Variable: " + variable);
        double[] coords = APIConfig.CITY_COORDS[0]; // Bengaluru
        Response response = client.getForecast(coords[0], coords[1],
                Map.of("hourly", variable));

        ResponseValidator.assertStatus200(response);
        ResponseValidator.assertKeyExistsInBlock(response, "hourly", "time");
        ResponseValidator.assertKeyExistsInBlock(response, "hourly", variable);
        ResponseValidator.assertArraysEqualLength(response, "hourly", "time", variable);
        getTest().pass(variable + " → hourly arrays equal length ✓");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TC-04  Daily variables
    // ─────────────────────────────────────────────────────────────────────────
    @Test(dataProvider = "dailyVars",
          description = "TC-04: Daily variable returns 7-element arrays (default forecast window)")
    public void testDailyVariablesReturnedInResponse(String variable) {
        getTest().info("TC-04 | Variable: " + variable);
        double[] coords = APIConfig.CITY_COORDS[1]; // London
        Response response = client.getForecast(coords[0], coords[1],
                Map.of("daily", variable, "timezone", "Europe/London"));

        ResponseValidator.assertStatus200(response);
        ResponseValidator.assertKeyExistsInBlock(response, "daily", "time");
        ResponseValidator.assertKeyExistsInBlock(response, "daily", variable);
        ResponseValidator.assertArraysEqualLength(response, "daily", "time", variable);
        ResponseValidator.assertArrayLength(response, "daily.time", 7);
        getTest().pass(variable + " → 7-day daily arrays ✓");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TC-05  Current weather fields
    // ─────────────────────────────────────────────────────────────────────────
    @Test(dataProvider = "currentVars",
          description = "TC-05: Current weather block contains ISO-8601 time and numeric variable")
    public void testCurrentWeatherReturnedInResponse(String variable) {
        getTest().info("TC-05 | Variable: " + variable);
        double[] coords = APIConfig.CITY_COORDS[3]; // Tokyo
        Response response = client.getForecast(coords[0], coords[1],
                Map.of("current", variable));

        ResponseValidator.assertStatus200(response);
        ResponseValidator.assertKeyExistsInBlock(response, "current", "time");
        ResponseValidator.assertKeyExistsInBlock(response, "current", variable);

        String time = response.jsonPath().getString("current.time");
        Assert.assertTrue(time != null && time.contains("T"),
                "current.time is not ISO-8601: " + time);

        Object value = response.jsonPath().get("current." + variable);
        Assert.assertTrue(value instanceof Number,
                "Expected numeric value for '" + variable + "', got: " + value);
        getTest().pass(variable + " → current block valid ✓");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TC-06  Temperature unit switching
    // ─────────────────────────────────────────────────────────────────────────
    @Test(dataProvider = "temperatureUnits",
          description = "TC-06: Temperature unit reflected in metadata; value in plausible range")
    public void testTemperatureUnitSwitching(String unit) {
        getTest().info("TC-06 | Unit: " + unit);
        double[] coords = APIConfig.CITY_COORDS[2]; // New York
        Response response = client.getForecast(coords[0], coords[1],
                Map.of("current", "temperature_2m", "temperature_unit", unit));

        ResponseValidator.assertStatus200(response);
        String expectedSymbol = "celsius".equals(unit) ? "°C" : "°F";
        ResponseValidator.assertUnitSymbolInMetadata(response,
                "current_units.temperature_2m", expectedSymbol);

        double temp = response.jsonPath().getDouble("current.temperature_2m");
        ResponseValidator.assertTemperatureInRange(temp, unit);
        getTest().pass(unit + " → " + temp + expectedSymbol + " in plausible range ✓");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TC-07  Wind speed unit parametrised
    // ─────────────────────────────────────────────────────────────────────────
    @Test(dataProvider = "windSpeedUnits",
          description = "TC-07: All 4 wind speed unit codes accepted; echoed in response metadata")
    public void testWindSpeedUnitParameterised(String unit) {
        getTest().info("TC-07 | Wind unit: " + unit);
        double[] coords = APIConfig.CITY_COORDS[4]; // Sydney
        Response response = client.getForecast(coords[0], coords[1],
                Map.of("current", "wind_speed_10m", "wind_speed_unit", unit));

        ResponseValidator.assertStatus200(response);
        ResponseValidator.assertKeyExistsInBlock(response, "current", "wind_speed_10m");
        String reportedUnit = response.jsonPath().getString("current_units.wind_speed_10m");
        Assert.assertNotNull(reportedUnit,
                "No unit reported for wind_speed_10m with unit='" + unit + "'");
        getTest().pass(unit + " → reported as '" + reportedUnit + "' ✓");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TC-08  Coordinate echo accuracy
    // ─────────────────────────────────────────────────────────────────────────
    @Test(dataProvider = "cities",
          description = "TC-08: API echoes lat/lon within ±1° of requested coordinates")
    public void testCoordinateEchoAccuracy(String city, double lat, double lon) {
        getTest().info("TC-08 | City: " + city);
        Response response = client.getForecast(lat, lon);
        ResponseValidator.assertStatus200(response);
        ResponseValidator.assertCoordinateAccuracy(response, lat, lon, 1.0);
        getTest().pass(city + " → coordinates within ±1° ✓");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TC-09  Invalid coordinates → 4xx
    // ─────────────────────────────────────────────────────────────────────────
    @Test(dataProvider = "invalidCoords",
          description = "TC-09: Invalid coordinates return HTTP 4xx + error:true")
    public void testInvalidCoordinatesReturnError(double lat, double lon, String desc) {
        getTest().info("TC-09 | " + desc + " (" + lat + ", " + lon + ")");
        Response response = client.getForecast(lat, lon);
        ResponseValidator.assertStatusIsClientError(response);
        ResponseValidator.assertErrorInResponse(response);
        getTest().pass(desc + " → 4xx + error:true ✓");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TC-10  Missing required parameter
    // ─────────────────────────────────────────────────────────────────────────
    @Test(description = "TC-10: Omitting required 'latitude' parameter returns HTTP 4xx")
    public void testMissingRequiredParamReturnsError() {
        getTest().info("TC-10 | Missing latitude");
        // longitude provided but latitude deliberately omitted
        Response response = client.get(Map.of("longitude", 77.59, "current", "temperature_2m"));
        ResponseValidator.assertStatusIsClientError(response);
        getTest().pass("Missing latitude → HTTP 4xx ✓");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TC-11  forecast_days controls array length
    // ─────────────────────────────────────────────────────────────────────────
    @Test(dataProvider = "forecastDays",
          description = "TC-11: hourly.time array length == forecast_days × 24")
    public void testForecastDaysParameterRespectsArrayLength(int forecastDays) {
        getTest().info("TC-11 | forecast_days=" + forecastDays);
        double[] coords = APIConfig.CITY_COORDS[0]; // Bengaluru
        Response response = client.getForecast(coords[0], coords[1], Map.of(
                "hourly",        "temperature_2m",
                "forecast_days", forecastDays,
                "timezone",      "Asia/Kolkata"
        ));
        ResponseValidator.assertStatus200(response);
        ResponseValidator.assertArrayLength(response, "hourly.time", forecastDays * 24);
        getTest().pass("forecast_days=" + forecastDays + " → " + (forecastDays * 24) + " hourly points ✓");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TC-12  Timezone is valid string
    // ─────────────────────────────────────────────────────────────────────────
    @Test(dataProvider = "cities",
          description = "TC-12: 'timezone' field is a non-empty string for all cities")
    public void testTimezoneFieldIsValidString(String city, double lat, double lon) {
        getTest().info("TC-12 | City: " + city);
        Response response = client.getForecast(lat, lon);
        ResponseValidator.assertStatus200(response);
        ResponseValidator.assertTimezoneIsValidString(response);
        getTest().pass(city + " → timezone: " + response.jsonPath().getString("timezone") + " ✓");
    }
}
