package com.example.demo.common.base;

/**
 * Interface đầy đủ CRUD - kế thừa IReadOnlyService và thêm các thao tác ghi.
 * Tuân thủ ISP: service nào cần CRUD đầy đủ thì implement interface này,
 * service nào chỉ cần đọc thì implement IReadOnlyService.
 *
 * @param <CREQ> Kiểu Create Request DTO
 * @param <UREQ> Kiểu Update Request DTO
 * @param <RES>  Kiểu Response DTO
 * @param <ID>   Kiểu khóa chính
 */
public interface ICrudService<CREQ, UREQ, RES, ID> extends IReadOnlyService<RES, ID> {

    /**
     * Tạo mới entity
     */
    RES create(CREQ requestDto);

    /**
     * Cập nhật entity theo ID
     */
    RES update(ID id, UREQ requestDto);

    /**
     * Xóa entity theo ID
     */
    void delete(ID id);
}
