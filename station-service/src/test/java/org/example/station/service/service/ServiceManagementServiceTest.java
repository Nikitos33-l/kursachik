package org.example.station.service.service;

import jakarta.persistence.EntityNotFoundException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceManagementServiceTest {

    @Mock private ServiceRepository serviceRepository;
    @Mock private ServiceMapper serviceMapper;
    @Mock private StationService stationService;
    @Mock private StationRepository stationRepository;
    @Mock private ServiceCacheRepository serviceCacheRepository;
    @Mock private StationOutboxEventService outboxEventService;

    @InjectMocks
    private ServiceManagementService serviceManagementService;

    private Station sampleStation;
    private Service sampleService;
    private final Long stationId = 10L;
    private final Long serviceId = 1L;

    @BeforeEach
    void setUp() {
        sampleStation = new Station();
        sampleStation.setId(stationId);

        sampleService = new Service();
        sampleService.setId(serviceId);
        sampleService.setName("Standard Wash");
        sampleService.setPrice(BigDecimal.valueOf(100));
        sampleService.setStation(sampleStation);
    }

    @Test
    @DisplayName("findAll для ROLE_USER: берет stationId из переданного аргумента")
    void findAll_RoleUser_UsesProvidedId() {
        Long providedId = 1L;
        UserPrincipal principal = mock(UserPrincipal.class);
        var authority = new SimpleGrantedAuthority("ROLE_USER");

        doReturn(List.of(authority)).when(principal).getAuthorities();

        List<ResponseServiceDto> expectedResult = List.of(new ResponseServiceDto(serviceId, "Wash", BigDecimal.TEN));
        when(serviceCacheRepository.getServicesByStationId(providedId)).thenReturn(expectedResult);

        List<ResponseServiceDto> result = serviceManagementService.findAll(providedId, principal);

        assertNotNull(result);
        assertEquals(expectedResult, result);
        verify(serviceCacheRepository, times(1)).getServicesByStationId(providedId);
    }

    @Test
    @DisplayName("findAll для других ролей: берет stationId из principal")
    void findAll_OtherRole_UsesPrincipalId() {
        Long providedId = 1L;
        Long principalStationId = 99L;
        UserPrincipal principal = mock(UserPrincipal.class);

        when(principal.getAuthorities()).thenReturn(Collections.emptyList());
        when(principal.stationId()).thenReturn(principalStationId);
        when(serviceCacheRepository.getServicesByStationId(principalStationId)).thenReturn(Collections.emptyList());

        List<ResponseServiceDto> result = serviceManagementService.findAll(providedId, principal);

        assertNotNull(result);
        verify(serviceCacheRepository).getServicesByStationId(principalStationId);
        verify(serviceCacheRepository, never()).getServicesByStationId(providedId);
    }

    @Test
    @DisplayName("Успешное получение услуги по ID")
    void findById_Success() {
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(sampleService));

        ResponseServiceDto expectedDto = new ResponseServiceDto(serviceId, "Standard Wash", BigDecimal.valueOf(100));
        when(serviceMapper.toDto(sampleService)).thenReturn(expectedDto);

        ResponseServiceDto result = serviceManagementService.findById(serviceId);

        assertNotNull(result);
        assertEquals("Standard Wash", result.name());
        verify(serviceRepository, times(1)).findById(serviceId);
    }

    @Test
    @DisplayName("Ошибка EntityNotFoundException, если услуга не найдена")
    void findById_NotFound_ThrowsException() {
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> serviceManagementService.findById(serviceId));
    }

    @Test
    @DisplayName("Успешное обновление услуги с инвалидацией кэша и сохранением события в Outbox")
    void update_Success() {
        RequestServiceDto dto = new RequestServiceDto("Premium Wash", BigDecimal.valueOf(150));
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(sampleService));

        serviceManagementService.update(serviceId, dto);

        assertEquals("Premium Wash", sampleService.getName());
        assertEquals(BigDecimal.valueOf(150), sampleService.getPrice());

        // Проверяем сброс кэша и фиксацию в Outbox таблицу
        verify(serviceCacheRepository, times(1)).evictCache(stationId);
        verify(outboxEventService, times(1)).saveStationServicesUpdatedEvent(stationId);
    }

    @Test
    @DisplayName("Успешное добавление услуги и сброс кэша станции (без Outbox, так как в сервисе его нет)")
    void add_Success() {
        RequestServiceDto dto = new RequestServiceDto("Dry Cleaning", BigDecimal.valueOf(300));
        UserPrincipal principal = mock(UserPrincipal.class);
        Service newService = new Service();

        when(principal.stationId()).thenReturn(stationId);
        when(stationService.getStationById(stationId)).thenReturn(sampleStation);
        when(serviceMapper.toEntity(dto, sampleStation)).thenReturn(newService);

        serviceManagementService.add(dto, principal);

        verify(serviceRepository, times(1)).save(newService);
        verify(serviceCacheRepository, times(1)).evictCache(stationId);
        // Метод add не вызывает outboxEventService по текущей логике сервиса
        verifyNoInteractions(outboxEventService);
    }

    @Test
    @DisplayName("Успешное удаление услуги с вызовом репозитория и сохранением события в Outbox")
    void delete_Success() {
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(sampleService));

        serviceManagementService.delete(serviceId);

        verify(serviceRepository, times(1)).delete(sampleService);
        verify(serviceCacheRepository, times(1)).evictCache(stationId);
        verify(outboxEventService, times(1)).saveStationServicesUpdatedEvent(stationId);
    }

    @Test
    @DisplayName("Валидация списка услуг и проверка существования автостанции")
    void findByIdsAndValidateService_Success() {
        List<Long> ids = List.of(1L, 2L);
        ServiceDetailDto detailDto = new ServiceDetailDto(serviceId, "Standard Wash", BigDecimal.valueOf(100));

        when(serviceRepository.findAllByIdIn(ids)).thenReturn(List.of(sampleService));
        when(serviceMapper.toDtoDetailsList(any())).thenReturn(List.of(detailDto));
        when(stationRepository.existsById(stationId)).thenReturn(true);

        StationServicesResponse response = serviceManagementService.findByIdsAndValidateService(ids, stationId);

        assertNotNull(response);
        assertTrue(response.stationExists());
        assertEquals(1, response.services().size());
        assertEquals("Standard Wash", response.services().get(0).name());

        verify(stationRepository, times(1)).existsById(stationId);
        verify(serviceRepository, times(1)).findAllByIdIn(ids);
    }
}