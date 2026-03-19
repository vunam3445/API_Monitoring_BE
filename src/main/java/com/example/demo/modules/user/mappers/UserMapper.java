package com.example.demo.modules.user.mappers;

import com.example.demo.common.base.BaseMapper;
import com.example.demo.modules.auth.dto.RegisterRequest;
import com.example.demo.modules.user.dto.UpdateUserRequest;
import com.example.demo.modules.user.dto.UserResponse;
import com.example.demo.modules.user.entities.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper extends BaseMapper<User, RegisterRequest, UpdateUserRequest, UserResponse> {

    // Phải khớp chính xác tên method và kiểu tham số từ BaseMapper
    @Override
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "avatarPublicId", ignore = true)
    User toEntity(RegisterRequest registerRequest);

    @Override
    UserResponse toResponse(User entity);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(UpdateUserRequest updateRequest, @MappingTarget User entity);
}