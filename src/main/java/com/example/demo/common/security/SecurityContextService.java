package com.example.demo.common.security;

import com.example.demo.modules.user.entities.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class SecurityContextService implements ISecurityContextService {

    @Override
    public Optional<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    @Override
    public Optional<UUID> getCurrentUserId() {
        return getCurrentUser().map(User::getId);
    }

    @Override
    public boolean isAuthenticated() {
        return getCurrentUser().isPresent();
    }
}
