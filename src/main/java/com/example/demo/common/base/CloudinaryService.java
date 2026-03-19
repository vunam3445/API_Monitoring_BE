package com.example.demo.common.base;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    public Map upload(MultipartFile file, String folderName) {
        try {
            Map params = ObjectUtils.asMap(
                    "folder", folderName,
                    "resource_type", "auto"
            );
            return cloudinary.uploader().upload(file.getBytes(), params);
        } catch (IOException e) {
            throw new RuntimeException("Upload thất bại: " + e.getMessage());
        }
    }

    // Hàm xóa ảnh (rất cần cho đồ án khi user xóa bài đăng)
    public Map delete(String publicId) throws IOException {
        return cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }
}