package com.example.demo.modules.user.services;

import com.example.demo.common.base.BaseService;
import com.example.demo.common.base.CloudinaryService;
import com.example.demo.common.cache.ICacheService;
import com.example.demo.common.exceptions.ForbidenException;
import com.example.demo.modules.auth.dto.RegisterRequest;
import com.example.demo.modules.user.dto.UpdateUserRequest;
import com.example.demo.modules.user.dto.UserResponse;
import com.example.demo.modules.user.entities.User;
import com.example.demo.modules.user.mappers.UserMapper;
import com.example.demo.modules.user.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class UserService
        extends BaseService<User, UUID, RegisterRequest, UpdateUserRequest, UserResponse>
        implements IUserService {

    private final CloudinaryService cloudinaryService;

    // Định nghĩa ID mặc định để kiểm tra
    private static final String DEFAULT_AVATAR_PUBLIC_ID = "API_Monitoring/default/default-avatar_uftwz0";
    public UserService(UserRepository repository, UserMapper mapper, ICacheService cacheService, CloudinaryService cloudinaryService) {
        super(repository, mapper, cacheService);
        this.cloudinaryService = cloudinaryService;
    }

    @Override
    protected Class<User> getEntityClass() {
        return User.class;
    }

    @Override
    public UserResponse update(UUID id,UpdateUserRequest request) {

        if (!id.equals(request.getId())) {
            throw new ForbidenException("Bạn không có quyền cập nhật thông tin của người dùng này!");
        }        // 1. Tìm user hiện tại trong DB
        User user = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Xử lý logic Upload Avatar nếu có file mới
        if (request.getAvatarFile() != null && !request.getAvatarFile().isEmpty()) {
            try {

                // Trong Service, hãy tách việc xóa ra một Thread khác hoặc dùng @Async
                CompletableFuture.runAsync(() -> {
                    if (user.getAvatarPublicId() != null && !user.getAvatarPublicId().equals(DEFAULT_AVATAR_PUBLIC_ID)) {
                        try {
                            cloudinaryService.delete(user.getAvatarPublicId());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });

// Tiếp tục thực hiện upload ảnh mới ngay lập tức
                Map uploadResult = cloudinaryService.upload(request.getAvatarFile(), "API_Monitoring/avatars");
                // Cập nhật thông tin ảnh mới vào entity
                user.setAvatarUrl(uploadResult.get("secure_url").toString());
                user.setAvatarPublicId(uploadResult.get("public_id").toString());
            } catch (Exception e) {
                throw new RuntimeException("Lỗi xử lý ảnh: " + e.getMessage());
            }
        }

        // 3. Cập nhật các thông tin khác
        user.setFullName(request.getFullName());
        user.setCompany(request.getCompany());

        // 4. Lưu vào DB và trả về Response
        User updatedUser = repository.save(user);
        evictObjectCache(id);
        evictListCache();
        return mapper.toResponse(updatedUser);
    }
}