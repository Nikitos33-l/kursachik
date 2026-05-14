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

    @Mock
    private ServiceRepository serviceRepository;
    @Mock
    private ServiceMapper serviceMapper;
    @Mock
    private StationService stationService;
    @Mock
    private StationRepository stationRepository;

    @InjectMocks
    private ServiceManagementService serviceManagementService;

    @Test
    @DisplayName("findAll для ROLE_USER: берет stationId из аргумента")
    void findAll_RoleUser_UsesProvidedId() {
        Long providedId = 1L;
        UserPrincipal principal = mock(UserPrincipal.class);
        var authority = new SimpleGrantedAuthority("ROLE_USER");

        doReturn(List.of(authority)).when(principal).getAuthorities();
        when(serviceRepository.findAllByStation_id(providedId)).thenReturn(Collections.emptyList());

        serviceManagementService.findAll(providedId, principal);

        verify(serviceRepository).findAllByStation_id(providedId);
    }

    @Test
    @DisplayName("findAll для других ролей: берет stationId из токена")
    void findAll_OtherRole_UsesPrincipalId() {
        Long providedId = 1L;
        Long principalStationId = 99L;
        UserPrincipal principal = mock(UserPrincipal.class);

        when(principal.getAuthorities()).thenReturn(Collections.emptyList());
        when(principal.stationId()).thenReturn(principalStationId);
        when(serviceRepository.findAllByStation_id(principalStationId)).thenReturn(Collections.emptyList());

        serviceManagementService.findAll(providedId, principal);

        verify(serviceRepository).findAllByStation_id(principalStationId);
        verify(serviceRepository, never()).findAllByStation_id(providedId);
    }

    @Test
    @DisplayName("Успешное получение услуги по ID")
    void findById_Success() {
        Long id = 1L;
        Service service = new Service();
        when(serviceRepository.findById(id)).thenReturn(Optional.of(service));
        when(serviceMapper.toDto(service)).thenReturn(new ResponseServiceDto(id, "Test", BigDecimal.TEN));

        ResponseServiceDto result = serviceManagementService.findById(id);

        assertNotNull(result);
        assertEquals(id, result.id());
    }

    @Test
    @DisplayName("Ошибка, если услуга не найдена")
    void findById_NotFound() {
        when(serviceRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> serviceManagementService.findById(1L));
    }

    @Test
    @DisplayName("Обновление существующей услуги")
    void update_Success() {
        Long id = 1L;
        RequestServiceDto dto = new RequestServiceDto("New Name", BigDecimal.valueOf(500));
        Service existingService = new Service();

        when(serviceRepository.findById(id)).thenReturn(Optional.of(existingService));

        serviceManagementService.update(id, dto);

        assertEquals("New Name", existingService.getName());
        assertEquals(BigDecimal.valueOf(500), existingService.getPrice());
    }

    @Test
    @DisplayName("Успешное добавление новой услуги")
    void add_Success() {
        RequestServiceDto dto = new RequestServiceDto("Wash", BigDecimal.valueOf(100));
        UserPrincipal principal = mock(UserPrincipal.class);
        Station station = new Station();
        Service service = new Service();

        when(principal.stationId()).thenReturn(5L);
        when(stationService.getStationById(5L)).thenReturn(station);
        when(serviceMapper.toEntity(dto, station)).thenReturn(service);

        serviceManagementService.add(dto, principal);

        verify(serviceRepository).save(service);
    }

    @Test
    @DisplayName("Удаление услуги по ID")
    void delete_Success() {
        serviceManagementService.delete(1L);
        verify(serviceRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Массовый поиск услуг и проверка существования станции")
    void findByIdsAndValidateService_Success() {
        List<Long> ids = List.of(1L, 2L);
        Long stationId = 10L;
        ServiceDetailDto detail = new ServiceDetailDto(1L, "Service", BigDecimal.ONE);

        when(serviceRepository.findAllByIdIn(ids)).thenReturn(Collections.emptyList());
        when(serviceMapper.toDtoDetailsList(any())).thenReturn(List.of(detail));
        when(stationRepository.existsById(stationId)).thenReturn(true);

        StationServicesResponse response = serviceManagementService.findByIdsAndValidateService(ids, stationId);

        assertTrue(response.stationExists());
        assertEquals(1, response.services().size());
        verify(stationRepository).existsById(stationId);
    }
}