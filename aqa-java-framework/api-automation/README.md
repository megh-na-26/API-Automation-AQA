# API Automation Framework
### Open-Meteo Forecast API — Java 17 + REST Assured + TestNG

---

## Overview

REST API automation framework for testing the **Open-Meteo Forecast API** (`https://api.open-meteo.com/v1/forecast`).

Covers 12 test cases across functional correctness, schema validation, parametrised input testing, and negative/error-path coverage. TestNG `@DataProvider` is used extensively to maximise coverage with minimal code duplication.

**Why Open-Meteo?**
- Free — no API key, no signup, zero setup friction for reviewer
- Production-grade, stable JSON schema — reliable for contract assertions
- Rich parametrize surface: 5 cities × 3 variable groups × 4 wind units × 4 forecast windows

---

## Tech Stack

| Tool | Version | Purpose |
|---|---|---|
| Java | 17 | Language |
| REST Assured | 5.4.0 | HTTP client + fluent assertions |
| TestNG | 7.9.0 | Test runner + `@DataProvider` |
| Jackson | 2.17.0 | JSON deserialisation into POJOs |
| ExtentReports | 5.1.1 | HTML test reports |
| Hamcrest | 2.2 | Matcher library |
| Lombok | 1.18.30 | Boilerplate reduction |
| Logback | 1.4.14 | Logging |
| Maven | 3.8+ | Build + dependency management |

---

## Project Structure

```
api-automation/
├── pom.xml
└── src/
    ├── main/java/com/aqa/api/
    │   ├── config/
    │   │   └── APIConfig.java              ← Base URL, cities, variable lists, unit options
    │   ├── client/
    │   │   └── APIClient.java              ← REST Assured wrapper (RequestSpecification + timeout)
    │   ├── validators/
    │   │   └── ResponseValidator.java      ← 10 reusable assertion helpers
    │   └── models/
    │       └── ForecastResponse.java       ← Jackson POJO for full response
    └── test/java/com/aqa/api/tests/
        ├── BaseAPITest.java                ← Session-scoped client + ExtentReports lifecycle
        └── OpenMeteoAPITest.java           ← 12 test cases + 8 DataProviders
    └── test/resources/
        ├── testng.xml                      ← Suite config (parallel=methods, 4 threads)
        └── logback-test.xml               ← Logging configuration
```

---

## Prerequisites

- Java 17+
- Maven 3.8+
- Internet connection (tests hit the live Open-Meteo API)

---

## Setup & Run

### Install dependencies
```bash
cd api-automation
mvn clean install -DskipTests
```

### Run all tests
```bash
mvn test
```

### Run a specific test class
```bash
mvn test -Dtest=OpenMeteoAPITest
```

### Run a specific test method
```bash
mvn test -Dtest=OpenMeteoAPITest#testForecastReturns200ForValidCities
```

### Run in parallel (default — 4 threads via testng.xml)
```bash
mvn test
# parallel="methods" thread-count="4" already set in testng.xml
```

---

## API Under Test

**Base URL:** `https://api.open-meteo.com`
**Endpoint:** `GET /v1/forecast`

### Key Parameters

| Parameter | Type | Description |
|---|---|---|
| `latitude` | float | WGS-84 latitude (required) |
| `longitude` | float | WGS-84 longitude (required) |
| `current` | string | Comma-separated current weather variables |
| `hourly` | string | Comma-separated hourly variables |
| `daily` | string | Comma-separated daily variables |
| `temperature_unit` | string | `celsius` (default) or `fahrenheit` |
| `wind_speed_unit` | string | `kmh`, `mph`, `ms`, `kn` |
| `forecast_days` | int | 1–16 (default: 7) |
| `timezone` | string | IANA timezone string (e.g. `Asia/Kolkata`) |

---

## Test Cases

| # | Test Method | DataProvider | Axis | Validation | Why |
|---|---|---|---|---|---|
| TC-01 | `testForecastReturns200ForValidCities` | `cities` | 5 cities | HTTP 200 + JSON content-type + no error field | Smoke — confirms endpoint reachable for all geographic inputs |
| TC-02 | `testResponseContainsRequiredTopLevelKeys` | `cities` | 5 cities | 6 mandatory keys: `latitude`, `longitude`, `generationtime_ms`, `utc_offset_seconds`, `timezone`, `timezone_abbreviation` | Schema contract — these keys must always exist regardless of requested variables |
| TC-03 | `testHourlyVariablesReturnedInResponse` | `hourlyVars` | 3 variables | `hourly` block present; `time.length == variable.length`; arrays non-empty | Array parity — mismatched lengths silently corrupt downstream data alignment |
| TC-04 | `testDailyVariablesReturnedInResponse` | `dailyVars` | 3 variables | `daily` block present; 7-element arrays (default forecast window) | Default 7-day window must produce exactly 7 elements per variable |
| TC-05 | `testCurrentWeatherReturnedInResponse` | `currentVars` | 3 variables | `current` block present; `time` is ISO-8601 string; variable value is numeric | Current-weather path uses different internal logic than forecast aggregations |
| TC-06 | `testTemperatureUnitSwitching` | `temperatureUnits` | celsius / fahrenheit | Unit symbol (`°C`/`°F`) in `current_units` metadata; value within plausible physical range | Unit-swap bugs produce wrong values without triggering HTTP errors — must be caught at the API layer |
| TC-07 | `testWindSpeedUnitParameterised` | `windSpeedUnits` | kmh / mph / ms / kn | HTTP 200; `wind_speed_10m` in current block; unit echoed in `current_units` metadata | All 4 accepted unit codes must be reflected in the response — silent fallback to a default would be a bug |
| TC-08 | `testCoordinateEchoAccuracy` | `cities` | 5 cities | `latitude` and `longitude` in response within ±1.0° of requested values | API snaps to nearest grid point — large deviations signal routing bugs |
| TC-09 | `testInvalidCoordinatesReturnError` | `invalidCoords` | 4 bad pairs | HTTP 400 or 422; `error: true` in response body | Input validation must be enforced server-side; invalid coords must not return plausible-looking data |
| TC-10 | `testMissingRequiredParamReturnsError` | — | — | HTTP 400 or 422 when `latitude` is omitted | Required-parameter contract — client code must handle this; API must signal it clearly |
| TC-11 | `testForecastDaysParameterRespectsArrayLength` | `forecastDays` | 1 / 3 / 7 / 14 days | `hourly.time.length == forecastDays × 24` | Consumers rely on exact array lengths for charting/alignment; off-by-one errors silently corrupt output |
| TC-12 | `testTimezoneFieldIsValidString` | `cities` | 5 cities | `timezone` is non-null, non-empty string | Null/missing timezone causes silent time-zone bugs in any consumer that converts UTC to local time |

---

## Test Data

### Cities (used in parametrised tests)

| City | Latitude | Longitude |
|---|---|---|
| Bengaluru | 12.9716 | 77.5946 |
| London | 51.5074 | -0.1278 |
| New York | 40.7128 | -74.0060 |
| Tokyo | 35.6762 | 139.6503 |
| Sydney | -33.8688 | 151.2093 |

### Variable Groups

| Group | Variables |
|---|---|
| Hourly | `temperature_2m`, `relative_humidity_2m`, `wind_speed_10m` |
| Daily | `temperature_2m_max`, `temperature_2m_min`, `precipitation_sum` |
| Current | `temperature_2m`, `wind_speed_10m`, `weather_code` |

### Invalid Coordinate Pairs (TC-09)

| Pair | Description |
|---|---|
| `(999, 999)` | Both lat and lon out of range |
| `(91, 0)` | Latitude > 90 |
| `(0, 181)` | Longitude > 180 |
| `(-91, 0)` | Latitude < -90 |

---

## Key Design Decisions

### Reusable Validators (`ResponseValidator.java`)
All assertions are extracted into named static methods. Tests compose them rather than inline asserting:

```java
// In test:
ResponseValidator.assertStatus200(response);
ResponseValidator.assertTopLevelKeysPresent(response, "latitude", "longitude", ...);
ResponseValidator.assertArraysEqualLength(response, "hourly", "time", variable);
```

This keeps each test method under ~10 lines and makes assertion intent immediately readable.

### DataProvider-driven parametrisation
TestNG `@DataProvider` maps directly to `APIConfig` constants — adding a new city, variable, or unit to the config automatically expands test coverage with zero test code changes.

### Session-scoped `APIClient`
`BaseAPITest` creates one `APIClient` per test class (`@BeforeClass`). The REST Assured `RequestSpecification` (with base URI and timeout config) is reused across all test methods, avoiding repeated setup overhead.

### POJO Model (`ForecastResponse.java`)
Jackson `@JsonIgnoreProperties(ignoreUnknown = true)` on the POJO means the test suite won't break if Open-Meteo adds new fields — a real-world concern for long-lived API test suites.

---

## Test Output

| Artifact | Location |
|---|---|
| HTML Report | `test-output/reports/APITestReport.html` |
| Log file | `test-output/logs/api-automation.log` |

---

## Total Test Execution Count

The `@DataProvider` axes expand the 12 test methods into **47 individual test executions**:

| Test Method | Executions |
|---|---|
| TC-01 | 5 (one per city) |
| TC-02 | 5 |
| TC-03 | 3 (one per hourly variable) |
| TC-04 | 3 |
| TC-05 | 3 |
| TC-06 | 2 (celsius + fahrenheit) |
| TC-07 | 4 (four wind units) |
| TC-08 | 5 |
| TC-09 | 4 (four invalid coord pairs) |
| TC-10 | 1 |
| TC-11 | 4 (four forecast day counts) |
| TC-12 | 5 |
| **Total** | **44** |

All 44 run in parallel (4 threads) via `testng.xml`.

---

## CI Integration

```yaml
# .github/workflows/ci.yml (excerpt)
api-tests:
  name: API Tests – Open-Meteo
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: maven
    - name: Run API Tests
      working-directory: api-automation
      run: mvn test -B
    - name: Upload API Test Report
      if: always()
      uses: actions/upload-artifact@v4
      with:
        name: api-test-report
        path: api-automation/test-output/reports/
```
