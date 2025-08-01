package com.ikeda.payment.dtos;

import com.ikeda.payment.models.UserModel;
import org.springframework.beans.BeanUtils;

import java.util.UUID;

public record UserEventRecordDto(UUID userId,
                                 String username,
                                 String email,
                                 String name,
                                 String userStatus,
                                 String userType,
                                 String phoneNumber,
                                 String imageUrl,
                                 String actionType) {

    public UserModel convertToUserModel(UserModel userModel){
        BeanUtils.copyProperties(this, userModel);
        return userModel;
    }
}