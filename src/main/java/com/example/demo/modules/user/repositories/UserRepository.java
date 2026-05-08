package com.example.demo.modules.user.repositories;

import com.example.demo.modules.user.dto.UserAdminResponse;
import com.example.demo.modules.user.entities.User;
import com.example.demo.modules.user.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"subscriptions"})
    Page<User> findAll(Specification<User> spec, Pageable pageable);

    Optional<User> findByEmail(String email);

    Optional<User> findByRefreshToken(String refreshToken);

    @Query("SELECT u.status, COUNT(u) FROM User u " +
           "WHERE u.role != :excludeRole " +
           "AND u.planType IS NOT NULL AND u.planType != '' " +
           "GROUP BY u.status")
    List<Object[]> countUsersByStatus(UserRole excludeRole);

    @Query("SELECT u.planType, COUNT(u) FROM User u " +
           "WHERE u.role != :excludeRole " +
           "AND u.planType IS NOT NULL AND u.planType != '' " +
           "GROUP BY u.planType")
    List<Object[]> countUsersByPlan(UserRole excludeRole);

    
}
