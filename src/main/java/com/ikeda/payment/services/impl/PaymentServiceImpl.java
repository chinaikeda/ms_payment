package com.ikeda.payment.services.impl;

import com.ikeda.payment.dtos.PaymentCommandRecordDto;
import com.ikeda.payment.dtos.PaymentRequestRecordDto;
import com.ikeda.payment.enums.PaymentControl;
import com.ikeda.payment.enums.PaymentStatus;
import com.ikeda.payment.exceptions.NotFoundException;
import com.ikeda.payment.models.CreditCardModel;
import com.ikeda.payment.models.PaymentModel;
import com.ikeda.payment.models.UserModel;
import com.ikeda.payment.publishers.PaymentCommandPublisher;
import com.ikeda.payment.publishers.PaymentEventPublisher;
import com.ikeda.payment.repositories.CreditCardRepository;
import com.ikeda.payment.repositories.PaymentRepository;
import com.ikeda.payment.repositories.UserRepository;
import com.ikeda.payment.services.PaymentService;
import com.ikeda.payment.services.PaymentStripeService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    Logger logger = LogManager.getLogger(PaymentServiceImpl.class);

    final PaymentRepository paymentRepository;
    final UserRepository userRepository;
    final CreditCardRepository creditCardRepository;
    final PaymentCommandPublisher paymentCommandPublisher;
    final PaymentStripeService paymentStripeService;
    final PaymentEventPublisher paymentEventPublisher;

    public PaymentServiceImpl(PaymentRepository paymentRepository, UserRepository userRepository, CreditCardRepository creditCardRepository, PaymentCommandPublisher paymentCommandPublisher, PaymentStripeService paymentStripeService, PaymentEventPublisher paymentEventPublisher) {
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.creditCardRepository = creditCardRepository;
        this.paymentCommandPublisher = paymentCommandPublisher;
        this.paymentStripeService = paymentStripeService;
        this.paymentEventPublisher = paymentEventPublisher;
    }

    @Transactional
    @Override
    public PaymentModel requestPayment(PaymentRequestRecordDto paymentRequestRecordDto, UserModel userModel) {
//        AI - Primeira vesrão
//        var creditCardModel = new CreditCardModel();
//        var creditCardModelOptional = creditCardRepository.findByUser(userModel);
//
//        if (creditCardModelOptional.isPresent()){
//            creditCardModel = creditCardModelOptional.get();
//        }

//      AI - Segunda vesrão com a mesma finalidade
        var creditCardModel = creditCardRepository
                .findByUser(userModel)
                .orElseGet(CreditCardModel:: new);

        BeanUtils.copyProperties(paymentRequestRecordDto, creditCardModel);
        creditCardModel.setUser(userModel);
        creditCardRepository.save(creditCardModel);

        var paymentModel = new PaymentModel();
        paymentModel.setPaymentControl(PaymentControl.REQUESTED);
        paymentModel.setPaymentRequestDate(LocalDateTime.now(ZoneId.of("UTC")));
        paymentModel.setPaymentExpirationDate(LocalDateTime.now(ZoneId.of("UTC")).plusMonths(12));
        paymentModel.setLastDigitisCreditCard(paymentRequestRecordDto.creditCardNumber().substring(paymentRequestRecordDto.creditCardNumber().length()-4));
        paymentModel.setValuePaid(paymentRequestRecordDto.valuePaid());
        paymentModel.setUser(userModel);
        paymentRepository.save(paymentModel);

        try {
            var paymentCommandRecordDto = new PaymentCommandRecordDto(userModel.getUserId(), paymentModel.getPaymentId(), creditCardModel.getCardId());
            paymentCommandPublisher.publishPaymentCommand(paymentCommandRecordDto);
        } catch (Exception e){
            logger.error("Error sending payment command message with cause: {} ", e.getMessage());
        }


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

    @Transactional
    @Override
    public void makePayment(PaymentCommandRecordDto paymentCommandRecordDto) {
        var paymentModel = paymentRepository.findById(paymentCommandRecordDto.paymentId()).get();
        var userModel = userRepository.findById(paymentCommandRecordDto.userId()).get();
        var crediCardModel = creditCardRepository.findById(paymentCommandRecordDto.cardID()).get();

        paymentModel = paymentStripeService.processStripePayment(paymentModel, crediCardModel);
        paymentRepository.save(paymentModel);

        if (paymentModel.getPaymentControl().equals(PaymentControl.EFFECTED)){
            userModel.setPaymentStatus(PaymentStatus.PAYING);
            userModel.setLastPaymentDate(LocalDateTime.now(ZoneId.of("UTC")));
            userModel.setPaymentExpirationDate(LocalDateTime.now(ZoneId.of("UTC")).plusMonths(12));
            if (userModel.getFirstPaymentDate() == null){
                userModel.setFirstPaymentDate(LocalDateTime.now(ZoneId.of("UTC")));
            }
        } else {
            userModel.setPaymentStatus(PaymentStatus.DEBTOR);
        }
        userRepository.save(userModel);

        if (paymentModel.getPaymentControl().equals(PaymentControl.EFFECTED) ||
                paymentModel.getPaymentControl().equals(PaymentControl.REFUSED)){
            paymentEventPublisher.publishPaymentEvent(paymentModel.convertToPaymentEventDto());
        }
    }
}
