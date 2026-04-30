package org.example.kursach;

import org.example.kursach.dto.OrderNotificationDto;
import org.example.kursach.producer.NotificationRabbitMQProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Testcontainers
public class RabbitMQIntegrationTest {

    @Container
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.12-management");

    @DynamicPropertySource
    public static void configureProperty(DynamicPropertyRegistry propertyRegistry){
        propertyRegistry.add("spring.rabbitmq.host",rabbitMQContainer::getHost);
        propertyRegistry.add("spring.rabbitmq.port",rabbitMQContainer::getAmqpPort);
        propertyRegistry.add("spring.rabbitmq.username",rabbitMQContainer::getAdminUsername);
        propertyRegistry.add("spring.rabbitmq.password",rabbitMQContainer::getAdminPassword);

        propertyRegistry.add("notification.exchange",()-> "test.exchange");
        propertyRegistry.add("notification.routing-key",() -> "test.key");
    }

    @Autowired
    NotificationRabbitMQProducer producer;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    AmqpAdmin amqpAdmin;

    @BeforeEach
    public void setup(){
        String queueName = "test.queue";
        String exchangeName = "test.exchange";
        String routingKey = "test.key";

        Queue queue = new Queue(queueName);
        TopicExchange exchange = new TopicExchange(exchangeName);
        Binding binding = BindingBuilder.bind(queue).to(exchange).with(routingKey);

        amqpAdmin.declareExchange(exchange);
        amqpAdmin.declareQueue(queue);
        amqpAdmin.declareBinding(binding);
    }

    @Test
    @DisplayName("Успешный тест с отправкай и изыманием сообщения из брокера")
    public void shouldSendAndReceiveMessageSuccessfully()
    {
        OrderNotificationDto notification = new OrderNotificationDto(1L,"test@gmail.com","READY");

        producer.sendNotification(notification);

        OrderNotificationDto receivedDto =(OrderNotificationDto) rabbitTemplate.receiveAndConvert("test.queue");

        assertNotNull(receivedDto);
        assertEquals(notification.orderId(), receivedDto.orderId());
        assertEquals(notification.email(), receivedDto.email());
        assertEquals(notification.type(), receivedDto.type());
    }



}
