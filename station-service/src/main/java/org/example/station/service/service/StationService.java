package org.example.station.service.service;

import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.order.service.api.common.client.OrderServiceClient;
import org.example.station.service.api.common.dto.request.RequestOrderMappingStationDto;
import org.example.station.service.api.common.dto.response.SummaryResponseStationDto;
import org.example.station.service.dto.AddressCoordinate;
import org.example.station.service.dto.request.RequestStationDto;
import org.example.station.service.dto.response.ResponseStationDto;
import org.example.station.service.entity.Station;
import org.example.station.service.mapper.StationMapper;
import org.example.station.service.repository.StationRepository;
import org.example.user.api.client.UserServiceFeignClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StationService {
    private final GeocoderService geocoderService;
    private final StationMapper stationMapper;
    private final StationRepository stationRepository;
    private final OrderServiceClient orderServiceClient;
    private final UserServiceFeignClient userServiceClient;

    @Transactional
    public void addStation(RequestStationDto requestStationDto) {
        AddressCoordinate addressCoordinate = geocoderService.getCoordinate(requestStationDto.address());
        Station station = stationMapper.toEntity(requestStationDto,addressCoordinate);
        stationRepository.save(station);
    }

    @Transactional(readOnly = true)
    public List<ResponseStationDto> findAll() {
        return stationMapper.toDtoList(stationRepository.findAll());
    }

    @Transactional(readOnly = true)
    public ResponseStationDto findById(Long id) {
        Station station = getStationById(id);
        return stationMapper.toDto(station);
    }

    @Transactional
    public void update(Long id,RequestStationDto stationDto) {
        Station updateStation = getStationById(id);
        if(!updateStation.getAddressText().equalsIgnoreCase(stationDto.address())){
            AddressCoordinate coordinate = geocoderService.getCoordinate(stationDto.address());
            updateStation.setAddressText(stationDto.address());
            updateStation.setLatitude(coordinate.latitude());
            updateStation.setLongitude(coordinate.longitude());
        }
        updateStation.setName(stationDto.name());
    }

    Station getStationById(Long id){
        return stationRepository.findById(id)
                .orElseThrow(()->new EntityNotFoundException("Станция с таким id не была найдена"));
    }

    @Transactional
    public void delete(Long id) {
        try {
            stationRepository.deleteById(id);
            orderServiceClient.deleteByStation(id);
            userServiceClient.deleteWorkersByWorkplace(id);
        }
        catch (FeignException e){
            throw new RuntimeException("Не удалось удалить связанные сущности");
        }
    }

    @Transactional(readOnly = true)
    public Map<Long, SummaryResponseStationDto> getStationsByOrders(List<RequestOrderMappingStationDto> request) {
        Set<Long> stationIds = getStationIds(request);
        Map<Long,Station> stations = stationRepository.findAllByIdIn(stationIds)
                .stream().collect(Collectors.toMap(Station::getId,s->s));

        return request.stream().collect(Collectors.toMap(
                RequestOrderMappingStationDto::orderId,
                r-> stationMapper.toSummaryDto(stations.get(r.stationId()))
        ));
    }

    private Set<Long> getStationIds(List<RequestOrderMappingStationDto> dtoList){
        return dtoList.stream().
                map(RequestOrderMappingStationDto::stationId).collect(Collectors.toSet());
    }

}
