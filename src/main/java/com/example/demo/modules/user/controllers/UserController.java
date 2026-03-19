package com.example.demo.modules.user.controllers;


import com.example.demo.common.base.BaseController;
import com.example.demo.modules.auth.dto.RegisterRequest;
import com.example.demo.modules.user.dto.UpdateUserRequest;
import com.example.demo.modules.user.dto.UserResponse;
import com.example.demo.modules.user.services.IUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController extends BaseController<RegisterRequest, UpdateUserRequest, UserResponse, UUID> {

    public UserController(IUserService service) {
        super(service);
    }
    @Override
    public ResponseEntity<UserResponse> create(@RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

}
