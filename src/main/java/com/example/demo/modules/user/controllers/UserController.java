package com.example.demo.modules.user.controllers;


import com.example.demo.common.base.BaseController;
import com.example.demo.modules.auth.dto.RegisterRequest;
import com.example.demo.modules.user.dto.UpdateUserRequest;
import com.example.demo.modules.user.dto.UserResponse;
import com.example.demo.modules.user.services.IUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    @Override
    public ResponseEntity<Void> delete(UUID id) {
        return ResponseEntity.noContent().build();
    }
    @Override
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<UserResponse> update(
            @PathVariable UUID id,
            @ModelAttribute UpdateUserRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

}
