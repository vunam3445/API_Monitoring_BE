package com.example.demo.modules.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.File;

@Data
public class UpdateUserRequest {

    @Size(min = 2, max = 100, message = "Họ tên phải từ 2 đến 100 ký tự")
    private String fullName;

    private String company;

    private File avatar;

    // Nếu bạn muốn cho phép user đổi mật khẩu tại đây (không khuyến khích, nên tách riêng)
    // private String oldPassword;
    // private String newPassword;
}