package com.example.demo.modules.user.services;

import com.example.demo.modules.user.dto.UserAdminResponse;
import com.example.demo.modules.user.dto.UserFilterCriteria;
import com.example.demo.modules.user.entities.User;
import com.example.demo.modules.user.repositories.UserSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IManagerUserService {
    Page<UserAdminResponse> getAllUser(UserFilterCriteria userFilterCriteria, Pageable pageable);
    void blockUser(UUID userId);
}
