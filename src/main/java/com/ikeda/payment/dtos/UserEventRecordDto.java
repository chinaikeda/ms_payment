package com.ikeda.payment.dtos;

import com.ikeda.payment.models.UserModel;
import org.springframework.beans.BeanUtils;

import java.util.UUID;

public record UserEventRecordDto(UUID userId,
                                 String login,
                                 String email,
                                 String password,
                                 String oldPassword,
                                 String name,
                                 String phoneNumber,
                                 String imageUrl,

                                 String actionType) {

    public UserModel convertToUserModel(){
        var userModel = new UserModel();
        BeanUtils.copyProperties(this, userModel);
        return userModel;
    }
}