package com.ikeda.payment.services.impl;

import com.ikeda.payment.enums.PaymentControl;
import com.ikeda.payment.models.CreditCardModel;
import com.ikeda.payment.models.PaymentModel;
import com.ikeda.payment.services.PaymentStripeService;
import com.stripe.Stripe;
import com.stripe.exception.CardException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentMethodCreateParams;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PaymentStripeProdServiceImpl implements PaymentStripeService {

    Logger logger = LogManager.getLogger(PaymentStripeServiceImpl.class);

//    TODO - AI - Teste
//    @Value(value = "${ikeda.stripe.secretKey.prod}")
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
//                    .setPaymentMethod(getPaymentMethod(creditCardModel.getCreditCardNumber().replace(" ", "")))
                    .addPaymentMethodType("card")
                    .build();

            var paymentIntent = PaymentIntent.create(paramsPaymentIntent);
            paymentIntentId = paymentIntent.getId();

//             Step 02: Payment Confirm
            var paramsPaymentMethod = PaymentMethodCreateParams.builder()
                    .setType(PaymentMethodCreateParams.Type.CARD)
                    .setCard(
                            PaymentMethodCreateParams.CardDetails.builder()
                                    .setNumber(creditCardModel.getCreditCardNumber().replaceAll(" ", ""))
                                    .setExpMonth(Long.parseLong(creditCardModel.getExpirationDate().split("/")[0]))
                                    .setExpYear(Long.parseLong(creditCardModel.getExpirationDate().split("/")[1]))
                                    .setCvc(creditCardModel.getCvvCode())
                                    .build()
                    )
                    .setBillingDetails(
                            PaymentMethodCreateParams.BillingDetails.builder().setName(creditCardModel.getCardHolderFullName()).build()
                    )
                    .build();
            var paymentMethod = PaymentMethod.create(paramsPaymentMethod);

            var paramsPaymentConfirm = PaymentIntentConfirmParams.builder()
                    .setPaymentMethod(paymentMethod.getId())
                    .build();

            var confirmPaymentIntent = paymentIntent.confirm(paramsPaymentConfirm);

            if (confirmPaymentIntent.getStatus().equals("succeeded")) {
                paymentModel.setPaymentControl(PaymentControl.EFFECTED);
                paymentModel.setPaymentMessage("payment effected - paymentIntent: " + paymentIntentId);
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
}
