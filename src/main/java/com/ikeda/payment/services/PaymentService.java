package com.ikeda.payment.services;

import com.ikeda.payment.dtos.PaymentRequestRecordDto;
import com.ikeda.payment.models.PaymentModel;
import com.ikeda.payment.models.UserModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface PaymentService {

    PaymentModel requestPayment(PaymentRequestRecordDto paymentRequestRecordDto, UserModel userModel);

    Optional<PaymentModel> findLastPaymentByUser(UserModel userModel);

    Page<PaymentModel> findAllByUser(Specification<PaymentModel> and, Pageable pageable);

    Optional<PaymentModel> findPaymentByUser(UUID userId, UUID paymentId);
}
