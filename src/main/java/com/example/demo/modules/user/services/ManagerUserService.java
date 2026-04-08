package com.example.demo.modules.user.services;

import com.example.demo.common.base.RestPageImpl;
import com.example.demo.modules.user.dto.UserAdminResponse;
import com.example.demo.modules.user.dto.UserFilterCriteria;
import com.example.demo.modules.user.entities.User;
import com.example.demo.modules.user.mappers.UserMapper;
import com.example.demo.modules.user.repositories.UserRepository;
import com.example.demo.modules.user.repositories.UserSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ManagerUserService implements IManagerUserService{
    private final UserRepository userRepository;
    private final UserMapper mapper;

    @Override
    public Page<UserAdminResponse> getAllUser(UserFilterCriteria userFilterCriteria, Pageable pageable) {
        Specification<User> spec = Specification.where(UserSpecification.hasEmail(userFilterCriteria.getEmail())
                                                .and(UserSpecification.hasFullName(userFilterCriteria.getFullName()))
                                                .and(UserSpecification.hasRole(userFilterCriteria.getRole()))
                                                .and(UserSpecification.hasPlanType(userFilterCriteria.getPlanType()))
                                                .and(UserSpecification.hasStatus(userFilterCriteria.getStatus())));
        Page<User> users = userRepository.findAll(spec, pageable);
        List<UserAdminResponse> dtos = mapper.toUserAdminResponseList(users.getContent());
        return new RestPageImpl<>(
                dtos,
                users.getNumber(),
                users.getSize(),
                users.getTotalElements(),
                null, 
                users.isLast(),
                users.getTotalPages(),
                null,
                users.isFirst(),
                users.getNumberOfElements()
        );
    }

    @Override
    public void blockUser(UUID userId) {
        // Method placeholder
    }
}
