package com.example.demo.modules.user.mappers;

import com.example.demo.common.base.BaseMapper;
import com.example.demo.modules.auth.dto.RegisterRequest;
import com.example.demo.modules.user.dto.UpdateUserRequest;
import com.example.demo.modules.user.dto.UserResponse;
import com.example.demo.modules.user.entities.User;

public interface UserMapper extends BaseMapper<User, RegisterRequest, UpdateUserRequest, UserResponse> {

}
