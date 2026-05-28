package org.example.station.service.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.securitycommon.UserPrincipal;
import org.example.station.service.api.common.dto.response.ServiceDetailDto;
import org.example.station.service.api.common.dto.response.StationServicesResponse;
import org.example.station.service.dto.request.RequestServiceDto;
import org.example.station.service.dto.response.ResponseServiceDto;
import org.example.station.service.entity.Service;
import org.example.station.service.entity.Station;
import org.example.station.service.mapper.ServiceMapper;
import org.example.station.service.producer.StationEventProducer;
import org.example.station.service.repository.ServiceRepository;
import org.example.station.service.repository.StationRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceManagementService {
    private final ServiceRepository serviceRepository;
    private final ServiceMapper serviceMapper;
    private final StationService stationService;
    private final StationRepository stationRepository;
    private final StationEventProducer stationEventProducer;
    private final ServiceCacheRepository serviceCacheRepository;

    @Transactional(readOnly = true)
    public List<ResponseServiceDto> findAll(Long stationId, UserPrincipal userPrincipal) {
        Long targetStationId = userPrincipal.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_USER"))
                ? stationId
                : userPrincipal.stationId();

        return serviceCacheRepository.getServicesByStationId(targetStationId);
    }

    @Transactional(readOnly = true)
    public ResponseServiceDto findById(Long id) {
        return serviceMapper.toDto(getServiceById(id));
    }

    @Transactional
    public void update(Long id, @Valid RequestServiceDto serviceDto) {
        Service service = getServiceById(id);
        service.setName(serviceDto.name());
        service.setPrice(serviceDto.price());

        Long stationId = service.getStation().getId();

        serviceCacheRepository.evictCache(stationId);
        stationEventProducer.sendStationServicesUpdatedEvent(stationId);
    }

    private Service getServiceById(Long id){
        return serviceRepository.findById(id)
                .orElseThrow(()->new EntityNotFoundException("Услуга с таким id не существует"));
    }

    @Transactional
    public void delete(Long id) {
        Service service = getServiceById(id);
        Long stationId = service.getStation().getId();

        serviceRepository.delete(service);

        serviceCacheRepository.evictCache(stationId);
        stationEventProducer.sendStationServicesUpdatedEvent(stationId);
    }

    @Transactional
    public void add(RequestServiceDto serviceDto, UserPrincipal userPrincipal) {
        Station station = stationService.getStationById(userPrincipal.stationId());
        Service service = serviceMapper.toEntity(serviceDto,station);
        serviceRepository.save(service);

        serviceCacheRepository.evictCache(station.getId());
    }

    @Transactional(readOnly = true)
    public StationServicesResponse findByIdsAndValidateService(List<Long> serviceIds, Long stationId) {
        List<ServiceDetailDto> services = serviceMapper.toDtoDetailsList
                (serviceRepository.findAllByIdIn(serviceIds));
        boolean stationExists = stationRepository.existsById(stationId);

        return new StationServicesResponse(stationExists,services);
    }
}