package com.example.demo.controllers;

import com.example.demo.services.BaseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * T: Entity
 * ID: Kiểu ID
 * CREQ: Create Request DTO
 * UREQ: Update Request DTO
 * RES: Response DTO
 */
public abstract class BaseController<T, ID, CREQ, UREQ, RES> {

    protected final BaseService<T, ID, CREQ, UREQ, RES> service;

    protected BaseController(BaseService<T, ID, CREQ, UREQ, RES> service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<RES> create(@RequestBody CREQ request) {
        // BaseService mới đã tự biết EntityClass thông qua getEntityClass() bên Service
        // nên ở đây ta không cần truyền vào nữa
        return ResponseEntity.ok(service.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RES> getById(@PathVariable ID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<RES>> getAll() {
        return ResponseEntity.ok(service.findAll());
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