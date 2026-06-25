package org.example.station.service.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.securitycommon.UserPrincipal;
import org.example.station.service.api.common.dto.response.ServiceDetailDto;
import org.example.station.service.api.common.dto.response.StationServicesResponse;
import org.example.station.service.dto.request.RequestServiceDto;
import org.example.station.service.dto.response.ResponseServiceDto;
import org.example.station.service.entity.Service;
import org.example.station.service.entity.Station;
import org.example.station.service.mapper.ServiceMapper;
import org.example.station.service.repository.ServiceRepository;
import org.example.station.service.repository.StationRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceManagementService {
    private final ServiceRepository serviceRepository;
    private final ServiceMapper serviceMapper;
    private final StationService stationService;
    private final StationRepository stationRepository;
    private final ServiceCacheRepository serviceCacheRepository;
    private final StationOutboxEventService outboxEventService;


    @Transactional(readOnly = true)
    public List<ResponseServiceDto> findAll(Long stationId, UserPrincipal userPrincipal) {
        boolean isUser = userPrincipal.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_USER"));

        Long targetStationId = isUser ? stationId : userPrincipal.stationId();
        if (targetStationId == null) {
            throw new IllegalArgumentException("Необходимо указать ID станции для получения списка услуг");
        }
        log.debug("Запрос списка услуг. Инициатор: {}, Выбранный СТО ID: {}", userPrincipal.email(), targetStationId);

        return serviceCacheRepository.getServicesByStationId(targetStationId);
    }

    @Transactional(readOnly = true)
    public ResponseServiceDto findById(Long id) {
        log.debug("Поиск услуги по ID: {}", id);
        return serviceMapper.toDto(getServiceById(id));
    }

    @Transactional
    public void update(Long id, @Valid RequestServiceDto serviceDto) {
        log.info("Запрос на обновление услуги ID: {}. Новые данные: name='{}', price={}", id, serviceDto.name(), serviceDto.price());
        Service service = getServiceById(id);
        service.setName(serviceDto.name());
        service.setPrice(serviceDto.price());

        Long stationId = service.getStation().getId();

        serviceCacheRepository.evictCache(stationId);

        outboxEventService.saveStationServicesUpdatedEvent(stationId);

        log.info("Услуга ID: {} успешно обновлена. Кэш сброшен, событие изменения прайс-листа сохранено в Outbox", id);
    }

    private Service getServiceById(Long id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Ошибка бизнес-логики: Услуга с ID {} не найдена в БД", id);
                    return new EntityNotFoundException("Услуга с таким id не существует");
                });
    }

    @Transactional
    public void delete(Long id) {
        log.info("Запрос на удаление услуги ID: {}", id);
        Service service = getServiceById(id);
        Long stationId = service.getStation().getId();

        serviceRepository.delete(service);

        serviceCacheRepository.evictCache(stationId);

        outboxEventService.saveStationServicesUpdatedEvent(stationId);

        log.info("Услуга ID: {} успешно удалена. Кэш СТО {} очищен, событие изменения сохранено в Outbox", id, stationId);
    }

    @Transactional
    public void add(RequestServiceDto serviceDto, UserPrincipal userPrincipal) {
        log.info("Добавление новой услуги СТО администратором [{}]. Название: '{}'", userPrincipal.email(), serviceDto.name());
        Station station = stationService.getStationById(userPrincipal.stationId());
        Service service = serviceMapper.toEntity(serviceDto, station);
        serviceRepository.save(service);

        serviceCacheRepository.evictCache(station.getId());
        log.info("Новая услуга успешно сохранена с ID: {}. Кэш СТО ID: {} сброшен", service.getId(), station.getId());
    }

    @Transactional(readOnly = true)
    public StationServicesResponse findByIdsAndValidateService(List<Long> serviceIds, Long stationId) {
        log.debug("Внутренний запрос валидации. Проверка услуг {} для СТО ID: {}", serviceIds, stationId);
        List<ServiceDetailDto> services = serviceMapper.toDtoDetailsList(serviceRepository.findAllByIdIn(serviceIds));
        boolean stationExists = stationRepository.existsById(stationId);

        return new StationServicesResponse(stationExists, services);
    }
}