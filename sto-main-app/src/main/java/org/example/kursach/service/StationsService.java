package org.example.kursach.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.kursach.dto.AddressCoordinate;
import org.example.kursach.dto.RequestStationDto;
import org.example.kursach.dto.ResponseStationDTO;
import org.example.kursach.entity.Order;
import org.example.kursach.entity.Stations;
import org.example.kursach.entity.User;
import org.example.kursach.mapping.StationMapper;
import org.example.kursach.repository.OrderRepository;
import org.example.kursach.repository.ServiceRepository;
import org.example.kursach.repository.StationsRepository;
import org.example.kursach.repository.UserRepository;
import org.example.kursach.entity.Service;

import java.util.List;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class StationsService {
   private final StationsRepository stationsRepository;
   private final GeocoderService geocoderService;
   private final StationMapper stationMapper;
   private final OrderRepository orderRepository;
   private final ServiceRepository serviceRepository;
   private final UserRepository userRepository;

   @Transactional
   public void addStations(RequestStationDto stationDto){
      AddressCoordinate addressCoordinate = geocoderService.getCoordinate(stationDto.address());
      Stations station = stationMapper.toEntity(stationDto,addressCoordinate);
      stationsRepository.save(station);
   }

   public ResponseStationDTO findById(Long id) {
        return stationsRepository.findById(id)
                .map(stationMapper::toDTO)
                .orElseThrow(() -> new EntityNotFoundException("Станция с id " + id + " не найдена"));
   }

   public List<ResponseStationDTO> findAll() {
       return stationsRepository.findAll().stream().map(stationMapper::toDTO).toList();
   }

   @Transactional
   public void update(Long id,RequestStationDto stationDto) {
        Stations updateStation = stationsRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Станция с таким id не была найдена"));
        if(!updateStation.getAddressText().equalsIgnoreCase(stationDto.address())){
            AddressCoordinate coordinate = geocoderService.getCoordinate(stationDto.address());
            updateStation.setAddressText(stationDto.address());
            updateStation.setLatitude(coordinate.latitude());
            updateStation.setLongitude(coordinate.longitude());
        }
        updateStation.setName(stationDto.name());
   }

   @Transactional
   public void delete(Long stationId) {
       Stations station = stationsRepository.findById(stationId).orElseThrow(() -> new EntityNotFoundException("Станция с таким id не была найдена"));

       clearStationOrders(station);
       clearStationsServices(station);
       clearStationsEmployee(station);

       stationsRepository.delete(station);
   }

   private void clearStationOrders(Stations station){
        List<Order> orders = station.getOrders();
        if (orders!=null && !orders.isEmpty()){
            orderRepository.deleteAll(orders);
        }
   }

   private void clearStationsServices(Stations station){
        List<Service> services = station.getServices();
        if (services!=null && !services.isEmpty()){
            serviceRepository.deleteAll(services);
        }
   }

   private void clearStationsEmployee(Stations station){
        List<User> employees = userRepository.findByWorkplace(station);
        if (employees!=null && !employees.isEmpty()){
            userRepository.deleteAll(employees);
        }
   }


}
