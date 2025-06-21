package com.ikeda.payment.services.impl;

import com.ikeda.payment.exceptions.NotFoundException;
import com.ikeda.payment.models.UserModel;
import com.ikeda.payment.repositories.UserRepository;
import com.ikeda.payment.services.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.text.html.Option;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    final UserRepository userRepository;


    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserModel save(UserModel userModel) {
        return userRepository.save(userModel);
    }

    @Transactional
    @Override
    public void delete(UUID userId) {
        userRepository.deleteById(userId);
    }

    @Override
    public Optional<UserModel> findById(UUID userId) {
        Optional<UserModel> userModelOptional = userRepository.findById(userId);
        if (userModelOptional.isEmpty()){
            throw new NotFoundException("Error: Usernot found.");
        }
        return userModelOptional;
    }
}
