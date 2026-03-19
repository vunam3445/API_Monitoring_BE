package com.example.demo.modules.user.services;

import com.example.demo.common.base.BaseService;
import com.example.demo.common.cache.ICacheService;
import com.example.demo.modules.auth.dto.RegisterRequest;
import com.example.demo.modules.user.dto.UpdateUserRequest;
import com.example.demo.modules.user.dto.UserResponse;
import com.example.demo.modules.user.entities.User;
import com.example.demo.modules.user.mappers.UserMapper;
import com.example.demo.modules.user.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;
@Service
public class UserService
        extends BaseService<User, UUID, RegisterRequest,  UpdateUserRequest, UserResponse>
        implements IUserService {

    public UserService(UserRepository repository, UserMapper mapper, ICacheService cacheService) {
        super(repository, mapper,cacheService);
    }
    @Override
    protected Class<User> getEntityClass() {
        return User.class;
    }

    @Override
    public UserResponse update(UpdateUserRequest request) {

    }

}
