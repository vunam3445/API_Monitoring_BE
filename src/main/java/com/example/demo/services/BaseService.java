package com.example.demo.services;

import com.example.demo.exceptions.ResourceNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * T: Entity
 * ID: Kiểu dữ liệu ID (UUID, Long,...)
 * CREQ: DTO dùng cho tạo mới
 * UREQ: DTO dùng cho cập nhật
 * RES: DTO dùng cho dữ liệu trả về
 */
public abstract class BaseService<T, ID, CREQ, UREQ, RES> {

    protected final JpaRepository<T, ID> repository;

    @Autowired
    protected ModelMapper modelMapper;

    protected BaseService(JpaRepository<T, ID> repository) {
        this.repository = repository;
    }

    // --- CÁC HÀM TRỪU TƯỢNG ĐỂ LỚP CON CUNG CẤP CLASS TYPE ---
    protected abstract Class<T> getEntityClass();
    protected abstract Class<RES> getResponseClass();

    // 1. CREATE
    @Transactional
    public RES create(CREQ requestDto) {
        T entity = modelMapper.map(requestDto, getEntityClass());
        T savedEntity = repository.save(entity);
        return modelMapper.map(savedEntity, getResponseClass());
    }

    // 2. UPDATE
    @Transactional
    public RES update(ID id, UREQ requestDto) {
        T existingEntity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dữ liệu với ID: " + id));

        // Cấu hình ModelMapper bỏ qua null để không ghi đè dữ liệu cũ bằng null
        modelMapper.getConfiguration().setSkipNullEnabled(true);
        modelMapper.map(requestDto, existingEntity);

        T updatedEntity = repository.save(existingEntity);
        return modelMapper.map(updatedEntity, getResponseClass());
    }

    // 3. FIND BY ID
    public RES findById(ID id) {
        T entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dữ liệu với ID: " + id));
        return modelMapper.map(entity, getResponseClass());
    }
    // 4. FIND ALL (Không phân trang)
    public List<RES> findAll() {
        return repository.findAll().stream()
                .map(entity -> modelMapper.map(entity, getResponseClass()))
                .collect(Collectors.toList());
    }

    // 5. FIND ALL (Phân trang)
    public Page<RES> findAll(Pageable pageable) {
        return repository.findAll(pageable)
                .map(entity -> modelMapper.map(entity, getResponseClass()));
    }

    // 6. DELETE
    @Transactional
    public void delete(ID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Không thể xóa. Không tìm thấy ID: " + id);
        }
        repository.deleteById(id);
    }
}