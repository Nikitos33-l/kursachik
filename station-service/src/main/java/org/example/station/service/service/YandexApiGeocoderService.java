package org.example.station.service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@Service
public class YandexApiGeocoderService implements GeocoderService{
    private final RestTemplate restTemplate;
    private final String url;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    public YandexApiGeocoderService(RestTemplate restTemplate, @Value("${geocoder.api.url}") String url, @Value("${geocoder.api.key}") String apiKey, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.url = url;
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
    }

    @Override
    public AddressCoordinate getCoordinate(String address) {
        URI requestUrl = URI.create(buildUrl(address));

        ResponseEntity<String> response = restTemplate.getForEntity(requestUrl, String.class);

        if(response.getStatusCode().isError()){
            throw new GetAddressCoordinateExceptions("Ошибка при получении адреса");
        }

        return parseToAddressCoordinate(response.getBody());
    }

    private AddressCoordinate parseToAddressCoordinate(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode posNode = root.path("response")
                    .path("GeoObjectCollection")
                    .path("featureMember")
                    .get(0)
                    .path("GeoObject")
                    .path("Point")
                    .path("pos");

            if (posNode.isMissingNode()) {
                throw new GetAddressCoordinateExceptions("Адрес не найден");
            }

            String[] coords = posNode.asText().split(" ");
            BigDecimal lon = BigDecimal.valueOf(Double.parseDouble(coords[0]));
            BigDecimal lat = BigDecimal.valueOf(Double.parseDouble(coords[1]));

            return new AddressCoordinate(lon,lat);

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String buildUrl(String clientAddress){
        String fullAddress = "Беларусь, " + clientAddress;
        String encodedAddress = URLEncoder.encode(fullAddress, StandardCharsets.UTF_8);
        return String.format("%s?apikey=%s&geocode=%s&format=json",url,apiKey,encodedAddress);
    }


}
