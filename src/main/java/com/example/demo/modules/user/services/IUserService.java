package com.example.demo.modules.user.services;

import com.example.demo.common.base.ICrudService;
import com.example.demo.modules.auth.dto.RegisterRequest;
import com.example.demo.modules.user.dto.UpdateUserRequest;
import com.example.demo.modules.user.dto.UserResponse;

import java.util.UUID;

public interface IUserService extends ICrudService<RegisterRequest, UpdateUserRequest, UserResponse, UUID> {

}
