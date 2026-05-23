package com.example.demo.modules.uptimeLogs.services;

import com.example.demo.modules.uptimeLogs.dto.LogExportRow;
import com.example.demo.modules.uptimeLogs.entities.UptimeLogs;
import com.example.demo.modules.uptimeLogs.repositories.UptimeLogsRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Writer;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class LogExportService implements ILogExportService {

    private final UptimeLogsRepository uptimeLogsRepository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional(readOnly = true)
    public void exportLogsToCsv(Specification<UptimeLogs> specification, Writer writer) throws IOException {
        
        // 1. Cấu hình định dạng CSV thông qua Apache Commons CSV (tự handle ký tự đặc biệt, dấu phẩy)
        CSVFormat format = CSVFormat.Builder.create()
                .setHeader("ID", "Monitor Name", "Monitor URL", "Method", "Checked At", "Status", "Status Code", "Response Time (ms)", "Error Type", "Error Message", "Response Snippet", "Assertion Status", "Assertion Message")
                .build();
                
        // 2. Fetch dữ liệu stream từ Database qua Fluent Query
        // Task 9 - SortBy DESC và limit 20,000 bản ghi trên luồng để chặn quá tải DB
        try (CSVPrinter csvPrinter = new CSVPrinter(writer, format);
             Stream<UptimeLogs> logStream = uptimeLogsRepository.findBy(specification, q -> 
                     q.sortBy(Sort.by(Sort.Direction.DESC, "checkedAt")).stream())) {

            // 3. Map từ Entity -> LogExportRow (DTO) -> Ghi ra CSV (Giới hạn tối đa 20,000 bản ghi)
            logStream.limit(20000).map(this::mapToRow).forEach(row -> {
                try {
                    // Commons CSV tự động escape các dấu nháy, dấu phẩy bên trong mảng String
                    csvPrinter.printRecord((Object[]) row.toStringArray());
                } catch (IOException e) {
                    throw new RuntimeException("Lỗi khi ghi dòng CSV", e);
                }
            });
            
            // Xả đệm khi xong
            csvPrinter.flush();
        }
    }

    /**
     * Map Entity UptimeLogs sang DTO LogExportRow
     */
    private LogExportRow mapToRow(UptimeLogs log) {
        String monitorName = log.getMonitor() != null ? log.getMonitor().getName() : "Unknown";
        String monitorUrl = log.getMonitor() != null && log.getMonitor().getUrl() != null ? log.getMonitor().getUrl() : "";
        String monitorMethod = log.getMonitor() != null && log.getMonitor().getMethod() != null ? log.getMonitor().getMethod() : "";
        String checkedAt = log.getCheckedAt() != null ? log.getCheckedAt().format(FORMATTER) : "";
        String status = Boolean.TRUE.equals(log.getIsUp()) ? "UP" : "DOWN";
        String statusCode = log.getStatusCode() != null ? String.valueOf(log.getStatusCode()) : "";
        String responseTime = log.getResponseTimeMs() != null ? String.valueOf(log.getResponseTimeMs()) : "0";
        String errorType = log.getErrorType() != null ? log.getErrorType() : "";
        String errorMessage = log.getErrorMessage() != null ? log.getErrorMessage() : "";
        String responseSnippet = log.getResponseSnippet() != null ? log.getResponseSnippet() : "";
        String assertionStatus = log.getAssertionStatus() != null ? log.getAssertionStatus() : "N/A";
        String assertionMessage = log.getAssertionMessage() != null ? log.getAssertionMessage() : "";

        return LogExportRow.builder()
                .logId(log.getId() != null ? String.valueOf(log.getId()) : "")
                .monitorName(monitorName)
                .monitorUrl(monitorUrl)
                .monitorMethod(monitorMethod)
                .checkedAt(checkedAt)
                .status(status)
                .statusCode(statusCode)
                .responseTime(responseTime)
                .errorType(errorType)
                .errorMessage(errorMessage)
                .responseSnippet(responseSnippet)
                .assertionStatus(assertionStatus)
                .assertionMessage(assertionMessage)
                .build();
    }
}
