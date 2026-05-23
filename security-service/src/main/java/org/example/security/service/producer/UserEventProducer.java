package org.example.security.service.producer;

import org.example.user.contracts.UserRegisterEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UserEventProducer {
    private final String userRegisterRoutingKey;
    private final String userEventExchanger;
    private final RabbitTemplate rabbitTemplate;

    public UserEventProducer(@Value("${user.register.routing.key}") String userRegisterEventKey,@Value("${user.exchange.name}") String userEventExchanger, RabbitTemplate rabbitTemplate) {
        this.userRegisterRoutingKey = userRegisterEventKey;
        this.userEventExchanger = userEventExchanger;
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishUserRegisterEvent(UserRegisterEvent event){
        rabbitTemplate.convertAndSend(userEventExchanger,userRegisterRoutingKey,event);
    }

}
