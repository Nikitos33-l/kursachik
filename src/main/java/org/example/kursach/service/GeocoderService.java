package org.example.kursach.service;

import org.example.kursach.dto.AddressCoordinate;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

public interface GeocoderService {
    public AddressCoordinate getCoordinate(String address);
}
