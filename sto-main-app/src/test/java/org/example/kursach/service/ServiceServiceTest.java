package org.example.kursach.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.kursach.dto.ServiceRequestDto;
import org.example.kursach.dto.UserPrincipal;
import org.example.kursach.entity.Service;
import org.example.kursach.entity.Stations;
import org.example.kursach.mapping.ServiceMap;
import org.example.kursach.repository.ServiceRepository;
import org.example.kursach.repository.StationsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ServiceServiceTest {
    @Mock
    ServiceRepository serviceRepository;

    @Mock
    ServiceMap serviceMap;

    @Mock
    StationsRepository stationsRepository;

    @InjectMocks
    ServiceService service;

    @Test
    @DisplayName("Успешное удаление услуги с существующим id")
    public void successfulDelete(){
        Long serviceId = 1L;
        when(serviceRepository.existsById(serviceId)).thenReturn(true);

        service.delete(serviceId);

        verify(serviceRepository).existsById(serviceId);
        verify(serviceRepository).deleteById(serviceId);
    }

    @Test
    @DisplayName("Попытка удаления услуги с несуществующим id")
    public void shouldThrowExceptionDelete(){
        Long serviceId = 1L;
        when(serviceRepository.existsById(serviceId)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(serviceId)).isInstanceOf(EntityNotFoundException.class);

        verify(serviceRepository).existsById(serviceId);
        verify(serviceRepository,times(0)).deleteById(serviceId);
    }

    @Test
    @DisplayName("Успешное обновление услуги")
    public void successfulUpdate() {
        Long serviceId = 1L;
        ServiceRequestDto requestDto = new ServiceRequestDto("New Name", 100.0);
        Service existingService = new Service();
        existingService.setId(serviceId);
        existingService.setName("Old Name");
        existingService.setPrice(50.0);

        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(existingService));

        service.update(serviceId, requestDto);

        assertThat(existingService.getName()).isEqualTo("New Name");
        assertThat(existingService.getPrice()).isEqualTo(100.0);
        verify(serviceRepository).findById(serviceId);
    }

    @Test
    @DisplayName("Попытка обновления услуги с несуществующим id")
    public void shouldThrowExceptionWhenUpdateNonExistentService() {
        Long nonExistentId = 99L;
        ServiceRequestDto requestDto = new ServiceRequestDto("New Name", 100.0);

        when(serviceRepository.findById(nonExistentId)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.update(nonExistentId, requestDto))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Объект не найден");

        verify(serviceRepository).findById(nonExistentId);
    }

    @Test
    @DisplayName("Успешное добавление услуги к конкретной станции")
    public void successfulAdd() {
        Long targetStationId = 10L;
        UserPrincipal userPrincipal = mock(UserPrincipal.class);
        when(userPrincipal.stationId()).thenReturn(targetStationId);

        ServiceRequestDto requestDto = new ServiceRequestDto("Wheel Alignment", 80.0);

        Stations mockStation = new Stations();
        mockStation.setId(targetStationId);

        Service mappedService = new Service();
        mappedService.setName(requestDto.name());
        mappedService.setPrice(requestDto.price());

        when(stationsRepository.findById(targetStationId)).thenReturn(Optional.of(mockStation));
        when(serviceMap.mapToService(requestDto)).thenReturn(mappedService);

        service.add(requestDto, userPrincipal);

        ArgumentCaptor<Service> serviceCaptor = ArgumentCaptor.forClass(Service.class);
        verify(serviceRepository).save(serviceCaptor.capture());

        Service savedService = serviceCaptor.getValue();
        assertThat(savedService.getStation()).isEqualTo(mockStation);
        assertThat(savedService.getName()).isEqualTo("Wheel Alignment");

        verify(stationsRepository).findById(targetStationId);
        verify(serviceMap).mapToService(requestDto);
    }

    @Test
    @DisplayName("Ошибка добавления услуги, если станция не найдена")
    public void shouldThrowExceptionWhenStationNotFound() {
        Long nonExistentStationId = 404L;
        UserPrincipal userPrincipal = mock(UserPrincipal.class);
        when(userPrincipal.stationId()).thenReturn(nonExistentStationId);

        ServiceRequestDto requestDto = new ServiceRequestDto("Any Service", 50.0);

        when(stationsRepository.findById(nonExistentStationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.add(requestDto, userPrincipal))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Станции с таким id не существует");

        verify(serviceRepository, times(0)).save(any());
    }


}
