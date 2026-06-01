package com.example.demo.common.security;

import com.example.demo.common.exceptions.UrlSecurityValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

/**
 * Validator trung tâm chịu trách nhiệm kiểm tra an toàn bảo mật của URL.
 *
 * Bảo vệ chống lại:
 * 1. SSRF (Server-Side Request Forgery): Chặn URL trỏ tới IP nội bộ/loopback/link-local/multicast.
 * 2. DNS Rebinding: Phân giải DNS động ngay tại thời điểm kiểm tra để phát hiện IP nội bộ ẩn sau domain.
 * 3. CRLF / HTTP Response Splitting: Chặn các ký tự điều khiển \r, \n trong URL.
 * 4. Protocol Injection: Chỉ cho phép http và https.
 */
@Component
@Slf4j
public class UrlSecurityValidator {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    /**
     * Kiểm tra URL có an toàn hay không.
     * Ném ra UrlSecurityValidationException nếu URL vi phạm chính sách bảo mật.
     *
     * @param urlString URL cần kiểm tra
     * @throws UrlSecurityValidationException khi URL không an toàn
     */
    public void validateUrl(String urlString) {
        if (urlString == null || urlString.isBlank()) {
            throw new UrlSecurityValidationException("URL không được để trống.");
        }

        // 1. Kiểm tra CRLF injection trong URL thô (bao gồm cả encoded %0d%0a)
        validateNoCrlfInjection(urlString);

        // 2. Parse và kiểm tra giao thức (scheme)
        URI uri = parseUri(urlString);
        validateScheme(uri);

        // 3. Trích xuất host và kiểm tra
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new UrlSecurityValidationException("URL không có host hợp lệ.");
        }

        // 4. Phân giải DNS và kiểm tra từng IP address
        validateResolvedAddresses(host);
    }

    /**
     * Kiểm tra chuỗi (URL, header value, query param value) không chứa ký tự CRLF.
     *
     * @param value chuỗi cần kiểm tra
     * @throws UrlSecurityValidationException nếu phát hiện ký tự điều khiển
     */
    public void validateNoCrlfInjection(String value) {
        if (value == null) return;

        // Kiểm tra ký tự thô
        if (value.contains("\r") || value.contains("\n")) {
            throw new UrlSecurityValidationException(
                    "URL hoặc tham số chứa ký tự điều khiển không hợp lệ (CRLF Injection).");
        }

        // Kiểm tra encoded: %0d, %0a (case-insensitive)
        String lower = value.toLowerCase();
        if (lower.contains("%0d") || lower.contains("%0a")) {
            throw new UrlSecurityValidationException(
                    "URL hoặc tham số chứa ký tự điều khiển được mã hóa không hợp lệ (%0D/%0A Injection).");
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private URI parseUri(String urlString) {
        try {
            return new URI(urlString);
        } catch (URISyntaxException e) {
            throw new UrlSecurityValidationException("URL không đúng định dạng: " + e.getMessage());
        }
    }

    private void validateScheme(URI uri) {
        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            throw new UrlSecurityValidationException(
                    "Giao thức URL không hợp lệ. Chỉ chấp nhận http hoặc https. Được cung cấp: " + scheme);
        }
    }

    private void validateResolvedAddresses(String host) {
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (!isSafeAddress(address)) {
                    log.warn("[UrlSecurityValidator] Chặn URL trỏ tới địa chỉ không an toàn: host={}, ip={}",
                            host, address.getHostAddress());
                    throw new UrlSecurityValidationException(
                            "URL trỏ tới dải địa chỉ mạng nội bộ hoặc không an toàn (SSRF): "
                                    + address.getHostAddress());
                }
            }
        } catch (UrlSecurityValidationException e) {
            throw e; // Re-throw security exception
        } catch (Exception e) {
            // Không thể phân giải DNS — coi là không an toàn
            log.warn("[UrlSecurityValidator] Không thể phân giải DNS cho host: {}. Lý do: {}", host, e.getMessage());
            throw new UrlSecurityValidationException(
                    "Không thể phân giải tên miền của URL. Vui lòng kiểm tra lại: " + host);
        }
    }

    /**
     * Kiểm tra xem một địa chỉ IP có an toàn không.
     * Trả về false nếu IP thuộc các dải nguy hiểm.
     */
    public boolean isSafeAddress(InetAddress address) {
        if (address.isLoopbackAddress()) return false;      // 127.0.0.0/8, ::1
        if (address.isAnyLocalAddress()) return false;      // 0.0.0.0, ::
        if (address.isLinkLocalAddress()) return false;     // 169.254.0.0/16, fe80::/10 (AWS metadata)
        if (address.isSiteLocalAddress()) return false;     // 10.x, 172.16-31.x, 192.168.x (RFC 1918)
        if (address.isMulticastAddress()) return false;     // 224.0.0.0/4

        // Kiểm tra chính xác địa chỉ AWS/GCP Cloud Metadata
        if ("169.254.169.254".equals(address.getHostAddress())) return false;

        return true;
    }
}
