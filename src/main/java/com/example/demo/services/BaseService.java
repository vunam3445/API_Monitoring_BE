package com.example.demo.services;

import com.example.demo.dto.response.RestPageImpl;
import com.example.demo.exceptions.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public abstract class BaseService<T, ID, CREQ, UREQ, RES> {

    protected final JpaRepository<T, ID> repository;

    @Autowired
    protected ModelMapper modelMapper;

    @Autowired
    protected RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_OBJECT = "api-monitoring:api:object";
    private static final String CACHE_LIST = "api-monitoring:api:list";

    protected BaseService(JpaRepository<T, ID> repository) {
        this.repository = repository;
    }

    protected abstract Class<T> getEntityClass();
    protected abstract Class<RES> getResponseClass();

    public String getObjectName() {
        return getEntityClass().getSimpleName().toLowerCase();
    }

    /**
     * Xóa tất cả cache key có prefix chỉ định bằng RedisTemplate.
     * Dùng KEYS command trực tiếp để đảm bảo tìm đúng key và xóa.
     */
    private void evictCacheByPrefix(String prefix) {
        try {
            Set<String> keys = redisTemplate.keys(prefix + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Đã xóa {} cache key với prefix: {}", keys.size(), prefix);
            } else {
                log.info("Không tìm thấy cache key nào với prefix: {}", prefix);
            }
        } catch (Exception e) {
            log.error("Lỗi khi xóa cache với prefix: {}", prefix, e);
        }
    }

    /**
     * Xóa cache object theo key cụ thể
     */
    private void evictObjectCache(ID id) {
        try {
            String key = CACHE_OBJECT + "::" + getObjectName() + ":" + id;
            Boolean deleted = redisTemplate.delete(key);
            log.info("Xóa cache object key: {} - kết quả: {}", key, deleted);
        } catch (Exception e) {
            log.error("Lỗi khi xóa cache object key cho id: {}", id, e);
        }
    }

    /**
     * Xóa toàn bộ cache danh sách
     */
    private void evictListCache() {
        evictCacheByPrefix(CACHE_LIST + "::");
    }

    // 1. CREATE - Xóa toàn bộ cache danh sách
    @Transactional
    public RES create(CREQ requestDto) {
        T entity = modelMapper.map(requestDto, getEntityClass());
        T savedEntity = repository.save(entity);

        // Xóa cache danh sách sau khi tạo mới
        evictListCache();

        return modelMapper.map(savedEntity, getResponseClass());
    }

    // 2. UPDATE - Xóa cache object và cache danh sách
    @Transactional
    public RES update(ID id, UREQ requestDto) {
        T existingEntity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dữ liệu: " + id));

        modelMapper.getConfiguration().setSkipNullEnabled(true);
        modelMapper.map(requestDto, existingEntity);

        T updatedEntity = repository.save(existingEntity);

        // Xóa cache object và danh sách sau khi update
        evictObjectCache(id);
        evictListCache();

        return modelMapper.map(updatedEntity, getResponseClass());
    }

    // 3. FIND BY ID
    @Cacheable(value = CACHE_OBJECT, key = "#root.target.getObjectName() + ':' + #id", unless = "#result == null")
    public RES findById(ID id) {
        T entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dữ liệu: " + id));
        return modelMapper.map(entity, getResponseClass());
    }

    // 4. FIND ALL (Phân trang)
    @Cacheable(value = CACHE_LIST,
            key = "#root.target.getObjectName() + ':page_' + #pageable.pageNumber + '_size_' + #pageable.pageSize",
            unless = "#result == null")
    public Page<RES> findAll(Pageable pageable) {
        Page<T> entityPage = repository.findAll(pageable);
        List<RES> dtos = entityPage.getContent().stream()
                .map(entity -> modelMapper.map(entity, getResponseClass()))
                .collect(Collectors.toList());

        return new RestPageImpl<>(
                dtos,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                null, false, 0, null, false, 0
        );
    }

    // 5. DELETE
    @Transactional
    public void delete(ID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Không thể xóa. ID: " + id);
        }
        repository.deleteById(id);

        // Xóa cache object và danh sách sau khi xóa
        evictObjectCache(id);
        evictListCache();
    }
}