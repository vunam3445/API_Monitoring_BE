package com.example.demo.common.base;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interface chỉ đọc - dành cho các service không cần thao tác ghi.
 * Tuân thủ nguyên lý ISP: client không bị ép phụ thuộc vào method không dùng.
 *
 * @param <RES> Kiểu Response DTO
 * @param <ID>  Kiểu khóa chính
 */
public interface IReadOnlyService<RES, ID> {

    /**
     * Tìm entity theo ID
     */
    RES findById(ID id);

    /**
     * Lấy danh sách có phân trang
     */
    Page<RES> findAll(Pageable pageable);
}
