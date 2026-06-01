package com.example.demo.common.security;

import com.example.demo.common.exceptions.UrlSecurityValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UrlSecurityValidator.
 * Kiểm tra toàn bộ các trường hợp SSRF, DNS Rebinding và CRLF Injection.
 */
class UrlSecurityValidatorTest {

    private UrlSecurityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new UrlSecurityValidator();
    }

    // =========================================================================
    // Kiểm tra CRLF Injection
    // =========================================================================

    @Test
    @DisplayName("Chặn URL chứa ký tự CRLF thô")
    void validateNoCrlfInjection_rawCRLF_shouldThrow() {
        assertThrows(UrlSecurityValidationException.class,
                () -> validator.validateNoCrlfInjection("http://google.com\r\n/path"));
    }

    @Test
    @DisplayName("Chặn URL chứa ký tự LF thô")
    void validateNoCrlfInjection_rawLF_shouldThrow() {
        assertThrows(UrlSecurityValidationException.class,
                () -> validator.validateNoCrlfInjection("http://google.com\n/path"));
    }

    @Test
    @DisplayName("Chặn URL chứa %0d encoded")
    void validateNoCrlfInjection_encoded0d_shouldThrow() {
        assertThrows(UrlSecurityValidationException.class,
                () -> validator.validateNoCrlfInjection("http://google.com%0d%0a/path"));
    }

    @Test
    @DisplayName("Chặn URL chứa %0a encoded (case insensitive)")
    void validateNoCrlfInjection_encoded0aUppercase_shouldThrow() {
        assertThrows(UrlSecurityValidationException.class,
                () -> validator.validateNoCrlfInjection("http://google.com%0D%0A/path"));
    }

    @Test
    @DisplayName("Cho phép URL sạch không có CRLF")
    void validateNoCrlfInjection_cleanUrl_shouldPass() {
        assertDoesNotThrow(() -> validator.validateNoCrlfInjection("https://api.example.com/v1/health"));
    }

    // =========================================================================
    // Kiểm tra Protocol/Scheme
    // =========================================================================

    @ParameterizedTest
    @ValueSource(strings = {"file:///etc/passwd", "gopher://evil.com", "ftp://internal.server", "jar://something"})
    @DisplayName("Chặn các giao thức không hợp lệ ngoài http/https")
    void validateUrl_invalidScheme_shouldThrow(String url) {
        assertThrows(UrlSecurityValidationException.class, () -> validator.validateUrl(url));
    }

    // =========================================================================
    // Kiểm tra IP nội bộ (SSRF Protection)
    // =========================================================================

    @ParameterizedTest
    @ValueSource(strings = {
            "http://127.0.0.1",
            "http://127.0.0.1/admin",
            "http://localhost",
    })
    @DisplayName("Chặn URL trỏ tới loopback address (localhost / 127.x)")
    void validateUrl_loopbackAddress_shouldThrow(String url) {
        assertThrows(UrlSecurityValidationException.class, () -> validator.validateUrl(url));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://192.168.1.1",
            "http://10.0.0.1",
            "http://10.255.255.255",
            "http://172.16.0.1",
            "http://172.31.255.255",
    })
    @DisplayName("Chặn URL trỏ tới dải Private IP (RFC 1918)")
    void validateUrl_privateIp_shouldThrow(String url) {
        assertThrows(UrlSecurityValidationException.class, () -> validator.validateUrl(url));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://169.254.169.254",
            "http://169.254.169.254/latest/meta-data",
    })
    @DisplayName("Chặn URL trỏ tới AWS/GCP Metadata IP (169.254.169.254)")
    void validateUrl_cloudMetadataIp_shouldThrow(String url) {
        assertThrows(UrlSecurityValidationException.class, () -> validator.validateUrl(url));
    }

    // =========================================================================
    // Kiểm tra isSafeAddress helper
    // =========================================================================

    @Test
    @DisplayName("isSafeAddress trả về false cho loopback address")
    void isSafeAddress_loopback_returnsFalse() throws Exception {
        var loopback = java.net.InetAddress.getByName("127.0.0.1");
        assertFalse(validator.isSafeAddress(loopback));
    }

    @Test
    @DisplayName("isSafeAddress trả về false cho private address 192.168.x.x")
    void isSafeAddress_privateRange_returnsFalse() throws Exception {
        var privateAddr = java.net.InetAddress.getByName("192.168.1.100");
        assertFalse(validator.isSafeAddress(privateAddr));
    }

    @Test
    @DisplayName("isSafeAddress trả về false cho link-local 169.254.169.254")
    void isSafeAddress_linkLocal_returnsFalse() throws Exception {
        var linkLocal = java.net.InetAddress.getByName("169.254.169.254");
        assertFalse(validator.isSafeAddress(linkLocal));
    }

    // =========================================================================
    // Kiểm tra URL hợp lệ (không bị chặn)
    // =========================================================================

    @Test
    @DisplayName("Cho phép URL hợp lệ trỏ tới public domain")
    void validateUrl_publicDomain_shouldPass() {
        // google.com là domain công khai - luôn resolves ra public IP
        assertDoesNotThrow(() -> validator.validateUrl("https://google.com"));
    }

    @Test
    @DisplayName("Từ chối URL null hoặc trống")
    void validateUrl_null_shouldThrow() {
        assertThrows(UrlSecurityValidationException.class, () -> validator.validateUrl(null));
        assertThrows(UrlSecurityValidationException.class, () -> validator.validateUrl(""));
        assertThrows(UrlSecurityValidationException.class, () -> validator.validateUrl("  "));
    }
}
