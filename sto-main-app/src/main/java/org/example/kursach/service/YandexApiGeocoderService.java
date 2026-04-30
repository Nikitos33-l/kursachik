package org.example.kursach.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.kursach.Exceptions.AddressNotFoundException;
import org.example.kursach.dto.AddressCoordinate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class YandexApiGeocoderService implements GeocoderService{
    private final String apiKey;
    private final String apiUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public YandexApiGeocoderService(@Value("${geocoder.api.key}") String apiKey, @Value("${geocoder.api.url}") String apiUrl, HttpClient httpClient, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public AddressCoordinate getCoordinate(String address) {
        String url = buildUrl(address);
        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(url)).build();
        HttpResponse<String> response;
        try {
            response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return parseJson(response.body());
    }

    private String buildUrl(String clientAddress){
        String fullAddress = "Беларусь, " + clientAddress;
        String encodedAddress = URLEncoder.encode(fullAddress, StandardCharsets.UTF_8);
        return String.format("%s?apikey=%s&geocode=%s&format=json",apiUrl,apiKey,encodedAddress);
    }

    private AddressCoordinate parseJson(String json){
        try {
            JsonNode root = objectMapper.readTree(json);
            String path = "/response/GeoObjectCollection/featureMember/0/GeoObject/Point/pos";
            JsonNode posNode = root.at(path);
            if(posNode.isMissingNode()){
                throw new AddressNotFoundException("Координаты адреса не были найдены");
            }
            String[] coordinate = posNode.asText().split(" ");
            int LONGITUDE_INDEX = 0;
            int LATITUDE_INDEX = 1;
            BigDecimal longitude = new BigDecimal(coordinate[LONGITUDE_INDEX]);
            BigDecimal latitude = new BigDecimal(coordinate[LATITUDE_INDEX]);
            return new AddressCoordinate(longitude,latitude);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
