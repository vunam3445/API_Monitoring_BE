package com.example.demo.modules.monitor.execution;

import com.example.demo.modules.monitor.entities.Monitor;
import com.example.demo.modules.uptimeLogs.entities.UptimeLogs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation gọi API bằng Spring WebClient (non-blocking I/O).
 *
 * Luồng thực thi:
 * 1. Xây dựng request từ config của Monitor (URL, method, headers, auth, body, queryParams).
 * 2. Gửi request và đo thời gian phản hồi.
 * 3. Phân tích kết quả: so sánh status code, response time với assertion rules.
 * 4. Trả về UptimeLogs entity (chưa persist) chứa đầy đủ kết quả.
 */
@Service
@Slf4j
public class WebClientApiExecutionService implements ApiExecutionService {

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final int RESPONSE_SNIPPET_MAX_LENGTH = 500;

    private final WebClient webClient;

    public WebClientApiExecutionService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .codecs(config -> config.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB
                .build();
    }

    @Override
    public UptimeLogs execute(Monitor monitor) {
        UptimeLogs uptimeLogs = new UptimeLogs();
        uptimeLogs.setMonitor(monitor); // Gán entity để MapStruct lấy được name/url/method
        uptimeLogs.setMonitorId(monitor.getId());

        long startTime = System.currentTimeMillis();

        try {
            // Xây dựng và gửi request
            String responseBody = buildAndSendRequest(monitor);
            long responseTimeMs = System.currentTimeMillis() - startTime;

            // Populate kết quả thành công (status code sẽ không có ở đây vì WebClient
            // ném exception cho non-2xx, nên nếu đến đây = request thành công)
            uptimeLogs.setResponseTimeMs((int) responseTimeMs);
            uptimeLogs.setStatusCode(200); // Default cho response thành công
            uptimeLogs.setResponseSnippet(truncate(responseBody));

            // Kiểm tra assertions
            evaluateAssertions(monitor, uptimeLogs);

        } catch (WebClientResponseException ex) {
            // Server trả về HTTP error (4xx, 5xx)
            long responseTimeMs = System.currentTimeMillis() - startTime;
            uptimeLogs.setStatusCode(ex.getStatusCode().value());
            uptimeLogs.setResponseTimeMs((int) responseTimeMs);
            uptimeLogs.setResponseSnippet(truncate(ex.getResponseBodyAsString()));

            // Vẫn đánh giá assertion (có thể expected 404...)
            evaluateAssertions(monitor, uptimeLogs);

        } catch (Exception ex) {
            // Lỗi kết nối: timeout, DNS, SSL...
            long responseTimeMs = System.currentTimeMillis() - startTime;
            uptimeLogs.setResponseTimeMs((int) responseTimeMs);
            uptimeLogs.setIsUp(false);
            uptimeLogs.setErrorType(classifyError(ex));
            uptimeLogs.setErrorMessage(ex.getMessage());
            uptimeLogs.setAssertionStatus("FAILED");
            uptimeLogs.setAssertionMessage("Request failed: " + ex.getMessage());
        }

        return uptimeLogs;
    }

    /**
     * Xây dựng WebClient request từ cấu hình Monitor và gửi đi.
     */
    private String buildAndSendRequest(Monitor monitor) {
        HttpMethod httpMethod = HttpMethod.valueOf(monitor.getMethod().toUpperCase());

        WebClient.RequestBodySpec requestSpec = webClient
                .method(httpMethod)
                .uri(buildUri(monitor))
                .headers(headers -> applyHeaders(headers, monitor));

        // Thêm body nếu có (POST, PUT, PATCH)
        Mono<String> responseMono;
        if (monitor.getBody() != null && !monitor.getBody().isBlank()) {
            responseMono = requestSpec
                    .bodyValue(monitor.getBody())
                    .retrieve()
                    .bodyToMono(String.class);
        } else {
            responseMono = requestSpec
                    .retrieve()
                    .bodyToMono(String.class);
        }

        // Block với timeout
        return responseMono
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
                .block();
    }

    /**
     * Xây dựng URI với query parameters nếu có.
     */
    private String buildUri(Monitor monitor) {
        String baseUrl = monitor.getUrl();
        Map<String, String> queryParams = monitor.getQueryParams();

        if (queryParams == null || queryParams.isEmpty()) {
            return baseUrl;
        }

        StringBuilder sb = new StringBuilder(baseUrl);
        sb.append(baseUrl.contains("?") ? "&" : "?");

        String queryString = queryParams.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        sb.append(queryString);
        return sb.toString();
    }

    /**
     * Áp dụng headers và authentication vào request.
     */
    private void applyHeaders(HttpHeaders headers, Monitor monitor) {
        // Custom headers
        if (monitor.getHeaders() != null) {
            monitor.getHeaders().forEach(headers::set);
        }

        // Authentication
        Map<String, Object> auth = monitor.getAuth();
        if (auth != null && auth.containsKey("type")) {
            String authType = auth.get("type").toString().toLowerCase();
            switch (authType) {
                case "bearer" -> {
                    String token = auth.getOrDefault("token", "").toString();
                    headers.setBearerAuth(token);
                }
                case "basic" -> {
                    String username = auth.getOrDefault("username", "").toString();
                    String password = auth.getOrDefault("password", "").toString();
                    headers.setBasicAuth(username, password);
                }
                case "api_key" -> {
                    String headerName = auth.getOrDefault("headerName", "X-API-Key").toString();
                    String apiKey = auth.getOrDefault("apiKey", "").toString();
                    headers.set(headerName, apiKey);
                }
            }
        }
    }

    /**
     * Đánh giá kết quả response dựa trên assertion rules của Monitor.
     * Kiểm tra:
     * 1. Status code có nằm trong danh sách expected không.
     * 2. Response time có vượt quá max cho phép không.
     */
    private void evaluateAssertions(Monitor monitor, UptimeLogs uptimeLogs) {
        boolean isUp = true;
        StringBuilder assertionMessage = new StringBuilder();

        // 1. Kiểm tra status code
        if (monitor.getExpectedStatusCodes() != null && !monitor.getExpectedStatusCodes().isBlank()) {
            Set<Integer> expectedCodes = Arrays.stream(monitor.getExpectedStatusCodes().split(","))
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .collect(Collectors.toSet());

            if (!expectedCodes.contains(uptimeLogs.getStatusCode())) {
                isUp = false;
                assertionMessage.append(String.format(
                        "Status code %d is not in expected codes %s. ",
                        uptimeLogs.getStatusCode(), expectedCodes));
            }
        }

        // 2. Kiểm tra response time
        if (monitor.getMaxResponseTimeMs() != null && uptimeLogs.getResponseTimeMs() != null) {
            if (uptimeLogs.getResponseTimeMs() > monitor.getMaxResponseTimeMs()) {
                isUp = false;
                assertionMessage.append(String.format(
                        "Response time %dms exceeded max %dms. ",
                        uptimeLogs.getResponseTimeMs(), monitor.getMaxResponseTimeMs()));
            }
        }

        uptimeLogs.setIsUp(isUp);
        uptimeLogs.setAssertionStatus(isUp ? "PASSED" : "FAILED");
        uptimeLogs.setAssertionMessage(
                assertionMessage.isEmpty() ? "All assertions passed" : assertionMessage.toString().trim());
    }

    /**
     * Phân loại lỗi dựa trên exception type.
     */
    private String classifyError(Exception ex) {
        String className = ex.getClass().getSimpleName();
        String message = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";

        if (message.contains("timeout") || className.contains("Timeout")) {
            return "TIMEOUT";
        }
        if (message.contains("dns") || message.contains("unknown host")) {
            return "DNS_ERROR";
        }
        if (message.contains("connection refused")) {
            return "CONNECTION_REFUSED";
        }
        if (message.contains("ssl") || message.contains("certificate")) {
            return "SSL_ERROR";
        }
        return "CONNECTION_ERROR";
    }

    /**
     * Cắt ngắn response body để tránh lưu quá nhiều dữ liệu vào DB.
     */
    private String truncate(String text) {
        if (text == null) return null;
        return text.length() > RESPONSE_SNIPPET_MAX_LENGTH
                ? text.substring(0, RESPONSE_SNIPPET_MAX_LENGTH) + "..."
                : text;
    }
}
