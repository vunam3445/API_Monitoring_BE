package com.example.demo.modules.user.mappers;


import com.example.demo.common.base.BaseMapper;
import com.example.demo.modules.user.dto.CreateUserSettingRequest;
import com.example.demo.modules.user.dto.UpdateUserSettingRequest;
import com.example.demo.modules.user.dto.UserSettingResponse;
import com.example.demo.modules.user.entities.UserSetting;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserSettingMapper extends BaseMapper<UserSetting, CreateUserSettingRequest, UpdateUserSettingRequest, UserSettingResponse> {
}
