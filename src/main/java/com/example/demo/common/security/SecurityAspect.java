package com.example.demo.common.security;

import com.example.demo.common.security.annotations.IsOwnerOrAdmin;
import com.example.demo.modules.user.entities.User;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
public class SecurityAspect {

    private final SecurityService securityService;

    @Before("@annotation(isOwnerOrAdmin)")
    public void checkOwnership(JoinPoint joinPoint, IsOwnerOrAdmin isOwnerOrAdmin) {
        // 1. Kiểm tra nếu là ADMIN thì cho qua luôn
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return;
        }

        // 2. Lấy ID từ tham số phương thức (giả định tham số tên là 'id' hoặc @PathVariable)
        Object targetId = getParameterByName(joinPoint, "id");
        if (targetId == null) {
            // Thử lấy tham số đầu tiên nếu không thấy tham số tên 'id'
            Object[] args = joinPoint.getArgs();
            if (args.length > 0) targetId = args[0];
        }

        if (targetId == null) {
            throw new AccessDeniedException("Target ID not found for ownership check");
        }

        // 3. Gọi SecurityService để check
        String entityName = isOwnerOrAdmin.entityName();
        if (!securityService.isOwner(targetId, entityName)) {
            throw new AccessDeniedException("Bạn không có quyền thực hiện hành động này trên entity: " + entityName);
        }
    }

    private Object getParameterByName(JoinPoint joinPoint, String name) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                if (parameterNames[i].equals(name)) {
                    return args[i];
                }
            }
        }
        return null;
    }
}
