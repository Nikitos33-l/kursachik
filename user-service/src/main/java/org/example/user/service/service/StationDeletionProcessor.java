package org.example.user.service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.user.contracts.UserStationFailedDeleteEvent;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StationDeletionProcessor {
    private final UserService userService;
    private final UserOutboxService userOutboxService;

    public void deleteStationUsers(Long stationId){
        try {
            userService.deleteByWorkplace(stationId);
        }
        catch (Exception e){
            log.error("Сбой удаления пользователей для СТО ID: {}. Пишем FAILED в Outbox", stationId);
            UserStationFailedDeleteEvent event = new UserStationFailedDeleteEvent(stationId,e.getMessage());
            userOutboxService.saveUserStationFailedDeleteEvent(event);
        }
    }
}
