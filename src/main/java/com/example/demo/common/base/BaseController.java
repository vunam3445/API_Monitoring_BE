package com.example.demo.common.base;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * CREQ: Create Request DTO
 * UREQ: Update Request DTO
 * RES: Response DTO
 * ID: Kiểu ID
 */
public abstract class BaseController<CREQ, UREQ, RES, ID> {

    protected final ICrudService<CREQ, UREQ, RES, ID> service;

    protected BaseController(ICrudService<CREQ, UREQ, RES, ID> service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<RES> create(@RequestBody CREQ request) {
        return ResponseEntity.ok(service.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RES> getById(@PathVariable ID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/paging")
    public ResponseEntity<Page<RES>> getAll(Pageable pageable) {
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RES> update(@PathVariable ID id, @RequestBody UREQ request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable ID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}