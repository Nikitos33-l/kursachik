package org.example.station.service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.station.service.dto.AddressCoordinate;
import org.example.station.service.exceptions.GetAddressCoordinateExceptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class YandexApiGeocoderService implements GeocoderService {
    private final RestTemplate restTemplate;
    private final String url;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    public YandexApiGeocoderService(RestTemplate restTemplate,
                                    @Value("${geocoder.api.url}") String url,
                                    @Value("${geocoder.api.key}") String apiKey,
                                    ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.url = url;
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
    }

    @Override
    public AddressCoordinate getCoordinate(String address) {
        String requestUrlString = buildUrl(address);
        log.info("Отправка HTTP-запроса к Яндекс.Геокодеру для адреса: '{}'", address);

        try {
            URI requestUrl = URI.create(requestUrlString);
            ResponseEntity<String> response = restTemplate.getForEntity(requestUrl, String.class);

            log.debug("Получен успешный ответ от Яндекс.Геокодера (Status: {})", response.getStatusCode());
            return parseToAddressCoordinate(response.getBody());

        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error("Яндекс.Геокодер вернул ошибку HTTP! Код ответа: {}, Тело ответа: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new GetAddressCoordinateExceptions("Ошибка при обращении к геокодеру: " + e.getStatusCode());
        } catch (GetAddressCoordinateExceptions e) {
            throw e;
        } catch (Exception e) {
            log.error("Непредвиденная системная ошибка при работе с Яндекс.Геокодером для адреса: '{}'", address, e);
            throw new GetAddressCoordinateExceptions("Непредвиденная ошибка при получении координат");
        }
    }

    private AddressCoordinate parseToAddressCoordinate(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);

            JsonNode featureMember = root.path("response")
                    .path("GeoObjectCollection")
                    .path("featureMember");

            if (!featureMember.has(0)) {
                log.warn("Яндекс.Геокодер не смог распознать адрес. Массив 'featureMember' пуст.");
                throw new GetAddressCoordinateExceptions("Адрес не найден");
            }

            String pos = featureMember.get(0)
                    .path("GeoObject")
                    .path("Point")
                    .path("pos")
                    .asText("");

            if (pos.isEmpty()) {
                log.warn("В ответе Геокодера отсутствует узел 'Point.pos'");
                throw new GetAddressCoordinateExceptions("Координаты не найдены");
            }

            String[] coords = pos.split(" ");
            BigDecimal lon = new BigDecimal(coords[0]);
            BigDecimal lat = new BigDecimal(coords[1]);

            log.info("Адрес успешно геокодирован: Lon={}, Lat={}", lon, lat);
            return new AddressCoordinate(lon, lat);

        } catch (JsonProcessingException e) {
            log.error("Критическая ошибка парсинга JSON-структуры ответа Яндекса. Raw Body: {}", body, e);
            throw new RuntimeException("Ошибка парсинга ответа геокодера", e);
        }
    }

    private String buildUrl(String clientAddress) {
        String fullAddress = "Беларусь, " + clientAddress;
        String encodedAddress = URLEncoder.encode(fullAddress, StandardCharsets.UTF_8);
        return String.format("%s?apikey=%s&geocode=%s&format=json", url, apiKey, encodedAddress);
    }
}