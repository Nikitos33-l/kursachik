package org.example.kursach.service;


import org.example.kursach.dto.AddressCoordinate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class YandexApiGeocoderServiceTest {
    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private YandexApiGeocoderService geocoderService;

    private final String MOCK_KEY = "test-key";
    private final String MOCK_URL = "https://test.api";

    @BeforeEach
    void setUp() {

        geocoderService = new YandexApiGeocoderService(
                MOCK_KEY,
                MOCK_URL,
                httpClient,
                new com.fasterxml.jackson.databind.ObjectMapper()
        );
    }

    @Test
    void shouldReturnCoordinatesWhenAddressIsValid() throws Exception {
        String mockJson = """
                {
                  "response": {
                    "GeoObjectCollection": {
                      "featureMember": [{
                        "GeoObject": {
                          "Point": { "pos": "27.5925 53.9163" }
                        }
                      }]
                    }
                  }
                }
                """;

        when(httpResponse.body()).thenReturn(mockJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        AddressCoordinate result = geocoderService.getCoordinate("Минск Гикало 9");

        assertNotNull(result);
        assertEquals(new BigDecimal("27.5925"), result.longitude());
        assertEquals(new BigDecimal("53.9163"), result.latitude());
    }

    @Test
    void shouldThrowExceptionWhenAddressNotFound() throws Exception {
        String emptyJson = "{\"response\":{\"GeoObjectCollection\":{\"featureMember\":[]}}}";

        when(httpResponse.body()).thenReturn(emptyJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            geocoderService.getCoordinate("Несуществующее место");
        });

        assertEquals("Координаты адресса не были найдены", exception.getMessage());
    }
}
