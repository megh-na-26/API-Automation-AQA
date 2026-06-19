package com.aqa.api.client;

import com.aqa.api.config.APIConfig;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * APIClient
 * ---------
 * Thin REST Assured 5.x wrapper for the Open-Meteo Forecast API.
 * Uses RequestSpecBuilder (the correct 5.x approach — HttpClientConfig removed).
 */
public class APIClient {

    private static final Logger log = LoggerFactory.getLogger(APIClient.class);
    private final RequestSpecification spec;

    public APIClient() {
        RestAssured.baseURI = APIConfig.BASE_URL;

        this.spec = new RequestSpecBuilder()
                .setBaseUri(APIConfig.BASE_URL)
                .setContentType(ContentType.JSON)
                .addFilter(new RequestLoggingFilter())
                .addFilter(new ResponseLoggingFilter())
                .build();
    }

    /**
     * Generic GET to the forecast endpoint with arbitrary query params.
     */
    public Response get(Map<String, Object> params) {
        log.info("GET {} | params: {}", APIConfig.FORECAST_ENDPOINT, params);
        return given()
                .spec(spec)
                .queryParams(params)
                .when()
                .get(APIConfig.FORECAST_ENDPOINT)
                .then()
                .extract()
                .response();
    }

    /**
     * Convenience – always includes lat/lon; caller adds extra params.
     */
    public Response getForecast(double latitude, double longitude, Map<String, Object> extra) {
        Map<String, Object> params = new HashMap<>(extra);
        params.put("latitude",  latitude);
        params.put("longitude", longitude);
        return get(params);
    }

    /** Overload with default current=temperature_2m param. */
    public Response getForecast(double latitude, double longitude) {
        return getForecast(latitude, longitude, Map.of("current", "temperature_2m"));
    }
}
