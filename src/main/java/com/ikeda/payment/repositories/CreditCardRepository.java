package com.ikeda.payment.repositories;

import com.ikeda.payment.models.CreditCardModel;
import com.ikeda.payment.models.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface CreditCardRepository extends JpaRepository <CreditCardModel, UUID>, JpaSpecificationExecutor<CreditCardModel> {

    Optional<CreditCardModel> findByUser(UserModel userModel);
}
