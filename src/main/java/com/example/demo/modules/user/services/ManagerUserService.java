package com.example.demo.modules.user.services;

import com.example.demo.modules.user.dto.UserAdminResponse;
import com.example.demo.modules.user.entities.User;
import com.example.demo.modules.user.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ManagerUserService implements IManagerUserService{
    private final UserRepository userRepository;


    @Override
    public Page<UserAdminResponse> getAllUser(Pageable pageable) {
        return null;
    }

    @Override
    public void blockUser(UUID userId) {

    }
}
