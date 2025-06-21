package com.ikeda.payment.services.impl;

import com.ikeda.payment.dtos.PaymentRequestRecordDto;
import com.ikeda.payment.enums.PaymentControl;
import com.ikeda.payment.exceptions.NotFoundException;
import com.ikeda.payment.models.CreditCardModel;
import com.ikeda.payment.models.PaymentModel;
import com.ikeda.payment.models.UserModel;
import com.ikeda.payment.repositories.CreditCardRepository;
import com.ikeda.payment.repositories.PaymentRepository;
import com.ikeda.payment.repositories.UserRepository;
import com.ikeda.payment.services.PaymentService;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    final PaymentRepository paymentRepository;
    final UserRepository userRepository;
    final CreditCardRepository creditCardRepository;

    public PaymentServiceImpl(PaymentRepository paymentRepository, UserRepository userRepository, CreditCardRepository creditCardRepository) {
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.creditCardRepository = creditCardRepository;
    }

    @Transactional
    @Override
    public PaymentModel requestPayment(PaymentRequestRecordDto paymentRequestRecordDto, UserModel userModel) {
//        TODO - Primeira vesrão
//        var creditCardModel = new CreditCardModel();
//        var creditCardModelOptional = creditCardRepository.findByUser(userModel);
//
//        if (creditCardModelOptional.isPresent()){
//            creditCardModel = creditCardModelOptional.get();
//        }

//      Segunda vesrão com a mesma finalidade
        var creditCardModel = creditCardRepository
                .findByUser(userModel)
                .orElseGet(CreditCardModel:: new);

        BeanUtils.copyProperties(paymentRequestRecordDto, creditCardModel);
        creditCardModel.setUser(userModel);
        creditCardRepository.save(creditCardModel);

        var paymentModel = new PaymentModel();
        paymentModel.setPaymentControl(PaymentControl.REQUESTED);
        paymentModel.setPaymentExpirationDate(LocalDateTime.now(ZoneId.of("UTC")));
        paymentModel.setPaymentExpirationDate(LocalDateTime.now(ZoneId.of("UTC")).plusMonths(12));
        paymentModel.setLastDigitisCreditCard(paymentRequestRecordDto.creditCardNumber().substring(paymentRequestRecordDto.creditCardNumber().length()-4));
        paymentModel.setValuePaid(paymentRequestRecordDto.valuePaid());
        paymentModel.setUser(userModel);
        paymentRepository.save(paymentModel);

        // send requet to queue

        return paymentModel;
    }

    @Override
    public Optional<PaymentModel> findLastPaymentByUser(UserModel userModel) {
        return paymentRepository.findTopByUserOrderByPaymentRequestDateDesc(userModel);
    }

    @Override
    public Page<PaymentModel> findAllByUser(Specification<PaymentModel> spec, Pageable pageable) {
        return paymentRepository.findAll(spec, pageable);
    }

    @Override
    public Optional<PaymentModel> findPaymentByUser(UUID userId, UUID paymentId) {
        Optional<PaymentModel> paymentModelOptional = paymentRepository.findPaymentByUser(userId, paymentId);
        if (paymentModelOptional.isEmpty()){
            throw new NotFoundException("Error: Payment not found for this user.");
        }
        return paymentModelOptional;
    }
}
