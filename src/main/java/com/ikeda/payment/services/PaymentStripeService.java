package com.ikeda.payment.services;

import com.ikeda.payment.models.CreditCardModel;
import com.ikeda.payment.models.PaymentModel;

public interface PaymentStripeService {

    PaymentModel processStripePayment(PaymentModel paymentModel, CreditCardModel creditCardModel);
}
