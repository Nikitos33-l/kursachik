package org.example.station.service.service;

import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.station.service.api.common.dto.request.RequestOrderMappingStationDto;
import org.example.station.service.api.common.dto.response.SummaryResponseStationDto;
import org.example.station.service.constants.CacheNames;
import org.example.station.service.dto.AddressCoordinate;
import org.example.station.service.dto.request.RequestStationDto;
import org.example.station.service.dto.response.ResponseStationDto;
import org.example.station.service.entity.Station;
import org.example.station.service.mapper.StationMapper;
import org.example.station.service.producer.StationEventProducer;
import org.example.station.service.repository.StationRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StationService {
    private final GeocoderService geocoderService;
    private final StationMapper stationMapper;
    private final StationRepository stationRepository;
    private final StationEventProducer stationEventProducer;

    @Transactional
    @CacheEvict(value = CacheNames.STATION_CACHE, key = "'all'")
    public void addStation(RequestStationDto requestStationDto) {
        log.info("Регистрация новой СТО. Название: '{}', Адрес: '{}'", requestStationDto.name(), requestStationDto.address());
        AddressCoordinate addressCoordinate = geocoderService.getCoordinate(requestStationDto.address());
        Station station = stationMapper.toEntity(requestStationDto, addressCoordinate);
        stationRepository.save(station);
        log.info("СТО успешно создана. Присвоен ID: {}, Координаты: [Lat={}, Lon={}]",
                station.getId(), addressCoordinate.latitude(), addressCoordinate.longitude());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.STATION_CACHE, key = "'all'")
    public List<ResponseStationDto> findAll() {
        log.debug("[CACHE MISS] Считывание полного списка станций напрямую из БД");
        return stationMapper.toDtoList(stationRepository.findAll());
    }

    @Transactional(readOnly = true)
    public ResponseStationDto findById(Long id) {
        log.debug("Поиск СТО по ID: {}", id);
        return stationMapper.toDto(getStationById(id));
    }

    @Transactional
    @CacheEvict(value = CacheNames.STATION_CACHE, key = "'all'")
    public void update(Long id, RequestStationDto stationDto) {
        log.info("Запрос на изменение СТО ID: {}. Проверка изменений...", id);
        Station updateStation = getStationById(id);

        if (!updateStation.getAddressText().equalsIgnoreCase(stationDto.address())) {
            log.info("Адрес СТО изменился с '{}' на '{}'. Запрос новых координат у геокодера",
                    updateStation.getAddressText(), stationDto.address());
            AddressCoordinate coordinate = geocoderService.getCoordinate(stationDto.address());
            updateStation.setAddressText(stationDto.address());
            updateStation.setLatitude(coordinate.latitude());
            updateStation.setLongitude(coordinate.longitude());
        }

        updateStation.setName(stationDto.name());
        log.info("Профиль СТО ID: {} успешно сохранен", id);
    }

    Station getStationById(Long id) {
        return stationRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Станция с ID {} не найдена", id);
                    return new EntityNotFoundException("Станция с таким id не была найдена");
                });
    }

    @Transactional
    @CacheEvict(value = CacheNames.STATION_CACHE, key = "'all'")
    public void delete(Long id) {
        log.warn("Инициация удаления СТО ID: {}", id);
        try {
            stationRepository.deleteById(id);
            runAfterCommit(() -> {
                log.info("[POST-COMMIT] Транзакция удаления СТО ID: {} закоммичена. Публикация события в брокер", id);
                stationEventProducer.publishUserDeletedEvent(id);
            });
        } catch (FeignException e) {
            log.error("Каскадное удаление СТО ID: {} прервано. Ошибка вызова Feign-клиента user-service", id, e);
            throw new RuntimeException("Не удалось удалить связанные сущности");
        }
    }

    @Transactional(readOnly = true)
    public Map<Long, SummaryResponseStationDto> getStationsByOrders(List<RequestOrderMappingStationDto> request) {
        log.debug("Внутренний запрос агрегации данных СТО для списка из {} заказов", request.size());
        Set<Long> stationIds = getStationIds(request);
        Map<Long, Station> stations = stationRepository.findAllByIdIn(stationIds)
                .stream().collect(Collectors.toMap(Station::getId, s -> s));

        return request.stream().collect(Collectors.toMap(
                RequestOrderMappingStationDto::orderId,
                r -> stationMapper.toSummaryDto(stations.get(r.stationId()))
        ));
    }

    private Set<Long> getStationIds(List<RequestOrderMappingStationDto> dtoList) {
        return dtoList.stream().
                map(RequestOrderMappingStationDto::stationId).collect(Collectors.toSet());
    }

    private static void runAfterCommit(Runnable runnable) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                runnable.run();
            }
        });
    }
}