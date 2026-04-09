package com.example.demo.modules.user.controllers;

import com.example.demo.modules.user.dto.UserAdminResponse;
import com.example.demo.modules.user.dto.UserFilterCriteria;
import com.example.demo.modules.user.dto.UserStatisticsResponse;
import com.example.demo.modules.user.services.IManagerUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ManagerUserController {

    private final IManagerUserService managerUserService;

    /**
     * Lấy danh sách người dùng dành cho admin (có phân trang và lọc)
     * Dữ liệu trả về bao gồm các thông tin quản trị và phân trang chi tiết.
     */
    @GetMapping
    public ResponseEntity<Page<UserAdminResponse>> getAllUsers(
            UserFilterCriteria criteria,
            Pageable pageable) {
        return ResponseEntity.ok(managerUserService.getAllUser(criteria, pageable));
    }

    /**
     * Khóa người dùng
     */
    @PatchMapping("/{userId}/block")
    public ResponseEntity<UserAdminResponse> blockUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(managerUserService.blockUser(userId));
    }

    /**
     * Kích hoạt người dùng
     */
    @PatchMapping("/{userId}/active")
    public ResponseEntity<UserAdminResponse> activeUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(managerUserService.activeUser(userId));
    }

    /**
     * Lấy thống kê tổng hợp người dùng và các gói Plan (Dành cho Dashboard Admin)
     */
    @GetMapping("/stats")
    public ResponseEntity<UserStatisticsResponse> getUserStatistics() {
        return ResponseEntity.ok(managerUserService.countUserAndPlanUser());
    }
}
