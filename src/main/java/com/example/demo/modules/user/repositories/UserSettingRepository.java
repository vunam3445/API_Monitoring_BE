package com.example.demo.modules.user.repositories;

import com.example.demo.modules.user.entities.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserSettingRepository extends JpaRepository<UserSetting, UUID> {

}
