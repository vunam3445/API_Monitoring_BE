package com.example.demo.modules.user.services;

import com.example.demo.common.base.ICrudService;
import com.example.demo.modules.user.dto.CreateUserSettingRequest;
import com.example.demo.modules.user.dto.UpdateUserSettingRequest;
import com.example.demo.modules.user.dto.UserSettingResponse;

import java.util.UUID;

public interface IUserSettingService extends ICrudService<CreateUserSettingRequest, UpdateUserSettingRequest, UserSettingResponse, UUID>
{
}
