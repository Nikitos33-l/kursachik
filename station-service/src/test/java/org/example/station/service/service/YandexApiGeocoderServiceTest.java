package org.example.station.service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.station.service.dto.AddressCoordinate;
import org.example.station.service.exceptions.GetAddressCoordinateExceptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class YandexApiGeocoderServiceTest {

    private YandexApiGeocoderService geocoderService;
    private MockRestServiceServer mockServer;
    private final String apiUrl = "https://geocode-maps.yandex.ru/1.x/";
    private final String apiKey = "test-key";

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        geocoderService = new YandexApiGeocoderService(restTemplate, apiUrl, apiKey, new ObjectMapper());
    }

    @Test
    @DisplayName("Успешное получение координат из JSON ответа")
    void getCoordinate_Success() {
        String address = "Гикало 9";
        String mockJsonResponse = """
            {
              "response": {
                "GeoObjectCollection": {
                  "featureMember": [
                    {
                      "GeoObject": {
                        "Point": {
                          "pos": "27.586234 53.916892"
                        }
                      }
                    }
                  ]
                }
              }
            }
            """;

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("geocode=%D0%91%D0%B5%D0%BB%D0%B0%D1%80%D1%83%D1%81%D1%8C")))
                .andRespond(withSuccess(mockJsonResponse, MediaType.APPLICATION_JSON));

        AddressCoordinate result = geocoderService.getCoordinate(address);

        assertNotNull(result);
        assertEquals(new BigDecimal("27.586234"), result.longitude());
        assertEquals(new BigDecimal("53.916892"), result.latitude());
    }

    @Test
    @DisplayName("Ошибка, если адрес не найден (pos отсутствует)")
    void getCoordinate_NotFound() {
        String mockEmptyResponse = """
            {
              "response": {
                "GeoObjectCollection": {
                  "featureMember": []
                }
              }
            }
            """;

        mockServer.expect(requestTo(org.hamcrest.Matchers.any(String.class)))
                .andRespond(withSuccess(mockEmptyResponse, MediaType.APPLICATION_JSON));

        assertThrows(GetAddressCoordinateExceptions.class, () -> geocoderService.getCoordinate("Несуществующее место"));
    }

    @Test
    @DisplayName("Ошибка, если Яндекс API вернул 500 или 400")
    void getCoordinate_ApiError() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.any(String.class)))
                .andRespond(withServerError());

        assertThrows(GetAddressCoordinateExceptions.class, () -> geocoderService.getCoordinate("Гикало 9"));
    }
}