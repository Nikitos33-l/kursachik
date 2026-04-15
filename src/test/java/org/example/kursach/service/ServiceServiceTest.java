package org.example.kursach.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.kursach.mapping.ServiceMap;
import org.example.kursach.repository.ServiceRepository;
import org.example.kursach.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ServiceServiceTest {
    @Mock
    ServiceRepository serviceRepository;

    @Mock
    ServiceMap service_map;

    @Mock
    UserRepository userRepository;

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
}
