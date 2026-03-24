package com.example.demo.modules.user.repositories;

import com.example.demo.modules.user.entities.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
@Repository
public interface UserSettingRepository extends JpaRepository<UserSetting, UUID> {
    @Query("SELECT us FROM UserSetting us " +
            "JOIN FETCH us.user u " +
            "LEFT JOIN FETCH u.subscriptionPlan p " +
            "WHERE us.userId = :id")
    Optional<UserSetting> findByIdWithUserAndPlan(@Param("id") UUID id);
}
