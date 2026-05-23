package com.example.demo.modules.user.repositories;

import com.example.demo.modules.user.entities.User;
import org.springframework.data.jpa.domain.Specification;

import java.io.Serializable;

import com.example.demo.modules.user.enums.UserRole;
import com.example.demo.modules.user.enums.UserStatus;

public class UserSpecification {
    public static Specification<User> hasFullName(String fullName) {
        return (root, query, cb) -> 
            (fullName == null || fullName.isBlank()) ? null : 
                cb.like(cb.lower(root.get("fullName")), "%" + fullName.toLowerCase() + "%");
    }

    public static Specification<User> hasEmail(String email) {
        return (root, query, cb) -> 
            (email == null || email.isBlank()) ? null : 
                cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%");
    }

    public static Specification<User> hasRole(UserRole role) {
        return (root, query, cb) -> role == null ? null : cb.equal(root.get("role"), role);
    }

    public static Specification<User> hasPlanType(String planType) {
        return (root, query, cb) -> 
            (planType == null || planType.isBlank()) ? null : cb.equal(root.get("planType"), planType);
    }

    public static Specification<User> hasStatus(UserStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }
}
