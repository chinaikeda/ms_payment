package com.ikeda.payment.services;

import com.ikeda.payment.models.UserModel;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface UserService {

    UserModel save(UserModel userModel);
    void delete(UUID userId);

    Optional<UserModel> findById(UUID userId);
}
