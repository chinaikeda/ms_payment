package com.ikeda.payment.publishers;

import com.ikeda.payment.dtos.NotificationRecordCommandDto;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NotificationCommandPublisher {

    final RabbitTemplate rabbitTemplate;

    public NotificationCommandPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Value(value = "${ikeda.broker.exchange.notificationCommandExchange}")
    private String notificationCommandExchange;

    @Value(value = "${ikeda.broker.key.notificationCommandKey}")
    private String notificationCommandKey;

    public void publishNotificationCommand(NotificationRecordCommandDto notificationRecordCommandDto){
        rabbitTemplate.convertAndSend(notificationCommandExchange, notificationCommandKey, notificationRecordCommandDto);
    }
}
