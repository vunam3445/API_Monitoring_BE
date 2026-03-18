package com.example.demo.common.base;

import java.util.List;

/**
 * Interface mapper base sử dụng MapStruct.
 * Tất cả mapper cụ thể sẽ kế thừa interface này.
 * MapStruct tự động generate implementation tại compile-time,
 * đảm bảo type-safe và hiệu năng cao hơn ModelMapper (runtime reflection).
 *
 * @param <E>    Entity class
 * @param <CREQ> Create Request DTO
 * @param <UREQ> Update Request DTO  
 * @param <RES>  Response DTO
 */
public interface BaseMapper<E, CREQ, UREQ, RES> {

    /**
     * Chuyển đổi Create Request DTO thành Entity
     */
    E toEntity(CREQ createRequest);

    /**
     * Chuyển đổi Entity thành Response DTO
     */
    RES toResponse(E entity);

    /**
     * Chuyển đổi danh sách Entity thành danh sách Response DTO
     */
    List<RES> toResponseList(List<E> entities);

    /**
     * Cập nhật entity hiện tại từ Update Request DTO.
     * Các field null trong DTO sẽ KHÔNG ghi đè giá trị hiện tại.
     */
    void updateEntityFromDto(UREQ updateRequest, @org.mapstruct.MappingTarget E entity);
}
