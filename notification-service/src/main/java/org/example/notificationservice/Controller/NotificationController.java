package org.example.notificationservice.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.notificationservice.Service.EmailPushService;
import org.example.notificationservice.dto.Request;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {
    EmailPushService service;

    @PostMapping("/send")
    public void sendMessage(@RequestBody @Valid Request request){
        service.sendMessage(request);
    }
}
