package com.example.demo.modules.user.repositories;

import com.example.demo.modules.user.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByRefreshToken(String refreshToken);
}
