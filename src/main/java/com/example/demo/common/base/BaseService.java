package com.example.demo.common.base;

import com.example.demo.common.cache.ICacheService;
import com.example.demo.common.exceptions.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
public abstract class BaseService<T, ID, CREQ, UREQ, RES> implements ICrudService<CREQ, UREQ, RES, ID> {

    protected final JpaRepository<T, ID> repository;
    protected final BaseMapper<T, CREQ, UREQ, RES> mapper;
    protected final ICacheService cacheService;

    private static final String CACHE_OBJECT = "api-monitoring:api:object";
    private static final String CACHE_LIST = "api-monitoring:api:list";

    protected BaseService(JpaRepository<T, ID> repository,
                          BaseMapper<T, CREQ, UREQ, RES> mapper,
                          ICacheService cacheService) {
        this.repository = repository;
        this.mapper = mapper;
        this.cacheService = cacheService;
    }

    protected abstract Class<T> getEntityClass();

    public String getObjectName() {
        return getEntityClass().getSimpleName().toLowerCase();
    }

    /**
     * Xóa cache object theo key cụ thể
     */
    protected void evictObjectCache(ID id) {
        String key = CACHE_OBJECT + "::" + getObjectName() + ":" + id;
        cacheService.evict(key);
    }

    /**
     * Xóa toàn bộ cache danh sách
     */
    protected void evictListCache() {
        cacheService.evictByPrefix(CACHE_LIST + "::");
    }

    // 1. CREATE - Xóa toàn bộ cache danh sách
    @Override
    @Transactional
    public RES create(CREQ requestDto) {
        T entity = mapper.toEntity(requestDto);
        T savedEntity = repository.save(entity);

        // Xóa cache danh sách sau khi tạo mới
        evictListCache();

        return mapper.toResponse(savedEntity);
    }

    // 2. UPDATE - Xóa cache object và cache danh sách
    @Override
    @Transactional
    public RES update(ID id, UREQ requestDto) {
        T existingEntity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dữ liệu: " + id));

        // MapStruct: cập nhật entity từ DTO, tự bỏ qua field null nhờ NullValuePropertyMappingStrategy.IGNORE
        mapper.updateEntityFromDto(requestDto, existingEntity);

        T updatedEntity = repository.save(existingEntity);

        // Xóa cache object và danh sách sau khi update
        evictObjectCache(id);
        evictListCache();

        return mapper.toResponse(updatedEntity);
    }

    // 3. FIND BY ID
    @Override
    @Cacheable(value = CACHE_OBJECT, key = "#root.target.getObjectName() + ':' + #id", unless = "#result == null")
    public RES findById(ID id) {
        T entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dữ liệu: " + id));
        return mapper.toResponse(entity);
    }

    // 4. FIND ALL (Phân trang)
    @Override
    @Cacheable(value = CACHE_LIST,
            key = "#root.target.getObjectName() + ':page_' + #pageable.pageNumber + '_size_' + #pageable.pageSize",
            unless = "#result == null")
    public Page<RES> findAll(Pageable pageable) {
        Page<T> entityPage = repository.findAll(pageable);
        List<RES> dtos = mapper.toResponseList(entityPage.getContent());

        return new RestPageImpl<>(
                dtos,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                null, false, 0, null, false, 0
        );
    }

    // 5. DELETE
    @Override
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