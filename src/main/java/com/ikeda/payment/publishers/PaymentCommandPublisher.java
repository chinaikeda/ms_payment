package com.ikeda.payment.publishers;

import com.ikeda.payment.dtos.PaymentCommandRecordDto;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentCommandPublisher {

    final RabbitTemplate rabbitTemplate;

    public PaymentCommandPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Value(value = "${ikeda.broker.exchange.paymentCommandExchange}")
    private String paymentCommandExchange;

    @Value(value = "${ikeda.broker.key.paymentCommandKey}")
    private String paymentCommandKey;

    public void publishPaymentCommand(PaymentCommandRecordDto paymentCommandRecordDto){
        rabbitTemplate.convertAndSend(paymentCommandExchange, paymentCommandKey, paymentCommandRecordDto);
    }

}
