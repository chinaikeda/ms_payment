package com.ikeda.payment.consumers;

import com.ikeda.payment.dtos.UserEventRecordDto;
import com.ikeda.payment.enums.ActionType;
import com.ikeda.payment.enums.PaymentStatus;
import com.ikeda.payment.models.UserModel;
import com.ikeda.payment.services.UserService;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class UserConsumer {

    final UserService userService;

    public UserConsumer(UserService userService) {
        this.userService = userService;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "${ikeda.broker.queue.userEventQueue.name}", durable = "true"),
            exchange = @Exchange(value = "${ikeda.broker.exchange.userEventExchange}",
                    type = ExchangeTypes.FANOUT, ignoreDeclarationExceptions = "true"))
    )
    public void listenUserEvent(@Payload UserEventRecordDto userEventRecordDto){
        switch (ActionType.valueOf(userEventRecordDto.actionType())){
            case CREATE -> {
                var userModel = userEventRecordDto.convertToUserModel(new UserModel());

                userModel.setPaymentStatus(PaymentStatus.NOTSTARTED);
                userService.save(userModel);
            }
            case UPDATE -> userService.save(userEventRecordDto.convertToUserModel(userService.findById(userEventRecordDto.userId()).get()));
            case DELETE -> userService.delete(userEventRecordDto.userId());
        }
    }
}
