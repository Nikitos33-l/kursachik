package org.example.station.service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.station.service.entity.SagaEvent;
import org.example.station.service.entity.SagaStatus;
import org.example.station.service.entity.TypeSagaEvent;
import org.example.station.service.repository.SagaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SagaEventService {

    private final SagaRepository sagaRepository;

    @Transactional
    public void startStationDeleteSaga(Long stationId){
        Map<String,Object> payload = new HashMap<>();
        payload.put("orderServiceDelete",false);
        payload.put("userServiceDelete",false);
        startSaga(stationId,TypeSagaEvent.STATION_DELETE,payload);
    }

    private void startSaga(Long stationId, TypeSagaEvent sagaEvent, Map<String,Object> payload){
        SagaEvent saga = SagaEvent.builder()
                .targetId(stationId)
                .eventType(sagaEvent)
                .sagaStatus(SagaStatus.IN_PENDING)
                .payload(payload)
                .createdAt(LocalDateTime.now())
                .build();

        sagaRepository.save(saga);
        log.info("Событие саги успешно сохранено в БД stationId:{}",stationId);
    }

}
