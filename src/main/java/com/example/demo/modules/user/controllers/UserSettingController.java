package com.example.demo.modules.user.controllers;

import com.example.demo.common.base.BaseController;
import com.example.demo.common.base.ICrudService;
import com.example.demo.common.security.annotations.IsOwnerOrAdmin;
import com.example.demo.modules.user.dto.CreateUserSettingRequest;
import com.example.demo.modules.user.dto.UpdateUserSettingRequest;
import com.example.demo.modules.user.dto.UserSettingResponse;
import com.example.demo.modules.user.services.IUserSettingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
@RestController
@RequestMapping("/api/user-settings")

public class UserSettingController extends BaseController<CreateUserSettingRequest, UpdateUserSettingRequest, UserSettingResponse, UUID> {

    protected UserSettingController(IUserSettingService service) {
        super(service);
    }
    @Override
    @IsOwnerOrAdmin(entityName = "UserSetting")
    public ResponseEntity<UserSettingResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateUserSettingRequest request) {
        return super.update(id, request);
    }

    @Override
    @IsOwnerOrAdmin(entityName = "UserSetting")
    public ResponseEntity<UserSettingResponse> getById(@PathVariable UUID id) {
        return super.getById(id);
    }
}
