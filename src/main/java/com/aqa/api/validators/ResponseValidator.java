package com.aqa.api.validators;

import io.restassured.response.Response;
import org.testng.Assert;

import java.util.List;

/**
 * ResponseValidator
 * -----------------
 * Reusable assertion helpers for Open-Meteo API responses.
 * Composing these in tests keeps individual test methods concise.
 */
public final class ResponseValidator {

    private ResponseValidator() {}

    // ── HTTP-level assertions ─────────────────────────────────────────────────

    public static void assertStatus200(Response response) {
        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected HTTP 200, got: " + response.getStatusCode()
                + " | Body: " + response.getBody().asString().substring(0, Math.min(300, response.getBody().asString().length())));
    }

    public static void assertStatusIsClientError(Response response) {
        int status = response.getStatusCode();
        Assert.assertTrue(status == 400 || status == 422,
                "Expected HTTP 400 or 422, got: " + status);
    }

    public static void assertJsonContentType(Response response) {
        String ct = response.getContentType();
        Assert.assertTrue(ct != null && ct.contains("application/json"),
                "Expected JSON content-type, got: " + ct);
    }

    // ── Schema / key assertions ───────────────────────────────────────────────

    public static void assertTopLevelKeysPresent(Response response, String... keys) {
        for (String key : keys) {
            Object value = response.jsonPath().get(key);
            Assert.assertNotNull(value, "Missing top-level key in response: '" + key + "'");
        }
    }

    public static void assertKeyExistsInBlock(Response response, String block, String key) {
        Object blockObj = response.jsonPath().get(block);
        Assert.assertNotNull(blockObj, "Missing block '" + block + "' in response.");
        Object value = response.jsonPath().get(block + "." + key);
        Assert.assertNotNull(value,
                "Missing key '" + key + "' inside block '" + block + "'");
    }

    // ── Data integrity assertions ─────────────────────────────────────────────

    public static void assertArraysEqualLength(Response response,
                                               String block,
                                               String arrayA,
                                               String arrayB) {
        List<?> a = response.jsonPath().getList(block + "." + arrayA);
        List<?> b = response.jsonPath().getList(block + "." + arrayB);
        Assert.assertNotNull(a, "Array '" + arrayA + "' missing from block '" + block + "'");
        Assert.assertNotNull(b, "Array '" + arrayB + "' missing from block '" + block + "'");
        Assert.assertEquals(a.size(), b.size(),
                "Array length mismatch: '" + arrayA + "'=" + a.size()
                + " vs '" + arrayB + "'=" + b.size());
        Assert.assertTrue(a.size() > 0, "Array '" + arrayA + "' is empty.");
    }

    public static void assertArrayLength(Response response,
                                         String jsonPath,
                                         int expectedLength) {
        List<?> list = response.jsonPath().getList(jsonPath);
        Assert.assertNotNull(list, "Array at '" + jsonPath + "' is null.");
        Assert.assertEquals(list.size(), expectedLength,
                "Expected array length " + expectedLength
                + " at '" + jsonPath + "', got: " + list.size());
    }

    public static void assertCoordinateAccuracy(Response response,
                                                double expectedLat,
                                                double expectedLon,
                                                double tolerance) {
        Double actualLat = response.jsonPath().getDouble("latitude");
        Double actualLon = response.jsonPath().getDouble("longitude");
        Assert.assertNotNull(actualLat, "Missing 'latitude' in response.");
        Assert.assertNotNull(actualLon, "Missing 'longitude' in response.");
        Assert.assertTrue(Math.abs(actualLat - expectedLat) <= tolerance,
                "Latitude mismatch: expected ~" + expectedLat + ", got " + actualLat);
        Assert.assertTrue(Math.abs(actualLon - expectedLon) <= tolerance,
                "Longitude mismatch: expected ~" + expectedLon + ", got " + actualLon);
    }

    public static void assertTemperatureInRange(double value, String unit) {
        double low, high;
        if ("celsius".equals(unit)) {
            low = -90; high = 60;
        } else {
            low = -130; high = 140;
        }
        Assert.assertTrue(value >= low && value <= high,
                "Temperature " + value + " is outside plausible range [" + low + ", " + high + "]");
    }

    public static void assertNoErrorInResponse(Response response) {
        Object error = response.jsonPath().get("error");
        Assert.assertNull(error,
                "API returned an error: " + response.jsonPath().get("reason"));
    }

    public static void assertErrorInResponse(Response response) {
        Object error = response.jsonPath().get("error");
        Assert.assertNotNull(error, "Expected 'error' field in response body.");
        Assert.assertEquals(error.toString(), "true",
                "Expected error=true, got: " + error);
    }

    public static void assertTimezoneIsValidString(Response response) {
        String tz = response.jsonPath().getString("timezone");
        Assert.assertNotNull(tz,  "Timezone is null.");
        Assert.assertFalse(tz.isBlank(), "Timezone is an empty string.");
    }

    public static void assertUnitSymbolInMetadata(Response response,
                                                  String unitPath,
                                                  String expectedSymbol) {
        String reportedUnit = response.jsonPath().getString(unitPath);
        Assert.assertNotNull(reportedUnit, "Unit metadata missing at: " + unitPath);
        Assert.assertTrue(reportedUnit.contains(expectedSymbol),
                "Expected unit symbol '" + expectedSymbol
                + "' in metadata, got: '" + reportedUnit + "'");
    }
}
