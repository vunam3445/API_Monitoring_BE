package com.example.demo.modules.user.services;

import com.example.demo.modules.user.dto.UserAdminResponse;
import com.example.demo.modules.user.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IManagerUserService {
    Page<UserAdminResponse> getAllUser(Pageable pageable);
    public void blockUser(UUID userId);
}
