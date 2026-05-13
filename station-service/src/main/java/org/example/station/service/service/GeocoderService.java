package org.example.station.service.service;

import org.example.station.service.dto.AddressCoordinate;

public interface GeocoderService {
    AddressCoordinate getCoordinate(String address);
}
