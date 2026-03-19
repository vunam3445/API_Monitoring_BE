package com.example.demo.modules.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

@Data
public class UpdateUserRequest {
    // Nên để UUID cho đồng nhất với Entity
    private UUID id;

    @Size(min = 2, max = 100, message = "Họ tên phải từ 2 đến 100 ký tự")
    private String fullName;

    private String company;

    // Đổi thành chữ thường ở đầu (avatarFile)
    private MultipartFile avatarFile;
}