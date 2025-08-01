package com.ikeda.payment.services.impl;

import com.ikeda.payment.enums.PaymentControl;
import com.ikeda.payment.models.CreditCardModel;
import com.ikeda.payment.models.PaymentModel;
import com.ikeda.payment.services.PaymentService;
import com.ikeda.payment.services.PaymentStripeService;
import com.stripe.Stripe;
import com.stripe.exception.CardException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.PaymentIntentCreateParams;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Primary
@Service
public class PaymentStripeServiceImpl implements PaymentStripeService {

    Logger logger = LogManager.getLogger(PaymentStripeServiceImpl.class);

//    TODO - AI - Teste
//    @Value(value = "${ikeda.stripe.secretKey.test}")
    @Value(value = "${ikeda.stripe.secretKey}")
    private String secretKeyStripe;

    public PaymentModel processStripePayment(PaymentModel paymentModel, CreditCardModel creditCardModel){
        Stripe.apiKey = secretKeyStripe;
        String paymentIntentId = null;

        try {
//             Step 01: Payment Intent
            var paramsPaymentIntent = PaymentIntentCreateParams.builder()
                    .setAmount(paymentModel.getValuePaid().multiply(new BigDecimal("100")).longValue())
                    .setCurrency("brl")
                    .setPaymentMethod(getPaymentMethod(creditCardModel.getCreditCardNumber().replace(" ", "")))
                    .addPaymentMethodType("card")
                    .build();

            var paymentIntent = PaymentIntent.create(paramsPaymentIntent);
            paymentIntentId = paymentIntent.getId();

//             Step 02: Payment Confirm
            var paramsPaymentConfirm = PaymentIntentConfirmParams.builder().build();
            var confirmPaymentIntent = paymentIntent.confirm(paramsPaymentConfirm);

            if (confirmPaymentIntent.getStatus().equals("succeeded")) {
                paymentModel.setPaymentControl(PaymentControl.EFFECTED);
                paymentModel.setPaymentMessage("payment effected - paymentIntent: " + paymentIntentId);
                paymentModel.setPaymentCompletionDate(LocalDateTime.now(ZoneId.of("UTC")));
            } else {
                paymentModel.setPaymentControl(PaymentControl.ERROR);
                paymentModel.setPaymentMessage("payment error v1 - paymentIntent: " + paymentIntentId);
            }
        } catch (CardException cardException) {
            logger.error("A payment error occured: {} ", cardException.getMessage());
            try {
                paymentModel.setPaymentControl(PaymentControl.REFUSED);
                var paymentIntent = PaymentIntent.retrieve(paymentIntentId);
                paymentModel.setPaymentMessage("payment refused v1 - paymentIntent: " + paymentIntentId +
                        ", cause: " + paymentIntent.getLastPaymentError().getCode() +
                        ", message: " + paymentIntent.getLastPaymentError().getMessage());
            } catch (Exception exception) {
                logger.error("Another problem occured, maybe unrelated to Stripe, with cause: {} ", exception.getMessage());
                paymentModel.setPaymentMessage("payment refused v2 - paymentIntent: " + paymentIntentId);
            }
        } catch (Exception exception){
            logger.error("Another problem occured, maybe unrelated to Stripe, with cause: {} ", exception.getMessage());
            paymentModel.setPaymentControl(PaymentControl.ERROR);
            paymentModel.setPaymentMessage("payment error v2 - paymentIntent: " + paymentIntentId);
        }
        return paymentModel;
    }

    private String getPaymentMethod(String creditCardNumber){
        return switch (creditCardNumber){
            case "4242424242424242" -> "pm_card_visa";
            case "5555555555554444" -> "pm_card_mastercard";
            case "4000000000009995" -> "pm_card_visa_chargeDeclinedInsufficientFunds";
            case "4000000000000127" -> "pm_card_chargeDeclinedIncorrectCvc";
            default -> "pm_card_visa_chargeDeclined";
        };
    }
}
