package org.example.kursach.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.kursach.dto.AddressCoordinate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@Service
public class YandexApiGeocoderService implements GeocoderService{
    @Value("${geocoder.api.key}")
    private String apiKey;

    @Value("${geocoder.api.url}")
    private String apiUrl;

    @Override
    public CompletableFuture<AddressCoordinate> getCoordinate(String address) {
        String url = buildUrl(address);

        try(HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(url)).build();
            CompletableFuture<HttpResponse<String>> response = httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString());
            return response.thenApply(result -> parseJson(result.body()));
        }
    }

    private String buildUrl(String clientAddress){
        String fullAddress = "Беларусь, " + clientAddress;
        String encodedAddress = URLEncoder.encode(fullAddress, StandardCharsets.UTF_8);
        return String.format("%s?apikey=%s&geocode=%s&format=json",apiUrl,apiKey,encodedAddress);
    }

    private AddressCoordinate parseJson(String json){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode root = objectMapper.readTree(json);
            String path = "/response/GeoObjectCollection/featureMember/0/GeoObject/Point/pos";
            JsonNode posNode = root.at(path);
            if(posNode.isMissingNode()){
                throw new RuntimeException("Координаты адресса не были найдены");
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
