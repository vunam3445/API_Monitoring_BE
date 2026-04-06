package com.example.demo.modules.uptimeLogs.services;

import com.example.demo.modules.uptimeLogs.entities.UptimeLogs;
import org.springframework.data.jpa.domain.Specification;

import java.io.IOException;
import java.io.Writer;
import java.util.UUID;

public interface ILogExportService {
    /**
     * Xuất dữ liệu logs (sau khi đã filter qua Specification) thành CSV và ghi thẳng ra Writer
     */
    void exportLogsToCsv(Specification<UptimeLogs> specification, Writer writer) throws IOException;
}
