package org.example.notification.service.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderNotificationType {
    NEW(
            "Заказ принят",
            "Здравствуйте! Ваш заказ №%s успешно принят. Мы сообщим, когда мастер приступит к работе."
    ),
    IN_PROGRESS(
            "Заказ в работе",
            "Хорошие новости! Ваш автомобиль по заказу №%s передан мастеру и уже находится в работе."
    ),
    READY(
            "Заказ готов",
            "Ваш заказ №%s готов! Работы завершены, вы можете забрать свой автомобиль в любое удобное время."
    ),
    CANCELLED(
            "Заказ отменен",
            "Информируем вас, что ваш заказ №%s был отменен. Для уточнения деталей вы можете связаться с менеджером."
    ),
    NEW_ORDER_FOR_WORKER(
            "Новое задание на СТО",
            "Назначен новый заказ №%s. Пожалуйста, ознакомьтесь с деталями в системе."
    );

    private final String subject;
    private final String template;

    @JsonCreator
    public static OrderNotificationType fromString(String value){
        return OrderNotificationType.valueOf(value.toUpperCase().trim());
    }
}
