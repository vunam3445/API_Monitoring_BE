package com.example.demo.modules.uptimeLogs.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogExportRow {
    private String logId;
    private String monitorName;
    private String monitorUrl;
    private String monitorMethod;
    private String checkedAt;
    private String status;
    private String statusCode;
    private String responseTime;
    private String errorType;
    private String errorMessage;
    private String responseSnippet;
    private String assertionStatus;
    private String assertionMessage;

    public String[] toStringArray() {
        return new String[] {
            logId,
            monitorName,
            monitorUrl,
            monitorMethod,
            checkedAt,
            status,
            statusCode,
            responseTime,
            errorType,
            errorMessage,
            responseSnippet,
            assertionStatus,
            assertionMessage
        };
    }
}
