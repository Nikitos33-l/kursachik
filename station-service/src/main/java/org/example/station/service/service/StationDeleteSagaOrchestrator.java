package org.example.station.service.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.station.service.entity.SagaEvent;
import org.example.station.service.entity.SagaStatus;
import org.example.station.service.entity.TypeSagaEvent;
import org.example.station.service.repository.SagaRepository;
import org.example.station.service.repository.StationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StationDeleteSagaOrchestrator {
    //TODO написать обработчики для провала от user service и обработчики от order service

    private final SagaRepository sagaRepository;
    private final StationOutboxEventService outboxEventService;
    private final StationRepository stationRepository;

    @Transactional
    public void handleUserSuccessStationDelete(Long stationId, String correlationId) {
        SagaEvent sagaEvent = getSagaOrThrow(correlationId);

        if (sagaEvent.getSagaStatus() == SagaStatus.FAILED) {
            log.warn("UserService вернул УСПЕХ, но сага {} уже FAILED. Отправляем откат в UserService", correlationId);
            outboxEventService.saveUserCompensateDeleteEvent(stationId, correlationId);
            return;
        }

        Map<String, Object> payload = sagaEvent.getPayload();
        payload.put("userServiceDelete", true);
        sagaEvent.setPayload(payload);

        checkAndCompleteOrCompensate(sagaEvent, correlationId);
        sagaRepository.save(sagaEvent);
    }

    private void checkAndCompleteOrCompensate(SagaEvent sagaEvent,String correlationId){
        Map<String,Object> payload = sagaEvent.getPayload();
        boolean isOrderServiceDelete = (boolean) payload.getOrDefault("orderServiceDelete",false);
        boolean isUserServiceDelete = (boolean) payload.getOrDefault("userServiceDelete",false);
        if(isOrderServiceDelete && isUserServiceDelete){
            sagaEvent.setSagaStatus(SagaStatus.COMPLETED);
            stationRepository.deleteById(sagaEvent.getTargetId());
            outboxEventService.saveStationDeleteSagaCompletedEvent(sagaEvent.getTargetId(),correlationId);
        }
    }

    private SagaEvent getSagaOrThrow(String correlationId){
        return sagaRepository.findByIdWithLock(UUID.fromString(correlationId)).orElseThrow(()->new EntityNotFoundException("Такого события саги не было обнаружено"));
    }

    @Transactional
    public String startStationDeleteSaga(Long stationId){
        Map<String,Object> payload = new HashMap<>();
        payload.put("orderServiceDelete",false);
        payload.put("userServiceDelete",false);
        return startSaga(stationId,TypeSagaEvent.STATION_DELETE,payload);
    }


    private String startSaga(Long stationId, TypeSagaEvent sagaEvent, Map<String,Object> payload){
        SagaEvent saga = SagaEvent.builder()
                .targetId(stationId)
                .eventType(sagaEvent)
                .sagaStatus(SagaStatus.IN_PENDING)
                .payload(payload)
                .createdAt(LocalDateTime.now())
                .build();

        SagaEvent savedSaga = sagaRepository.save(saga);
        log.info("Событие саги успешно сохранено в БД stationId:{}",stationId);
        return savedSaga.getId().toString();
    }

}
