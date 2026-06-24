# CLAUDE.md

Hướng dẫn hành vi để giảm thiểu các lỗi lập trình phổ biến của LLM. Tích hợp với triết lý tối giản Ponytail.

## 4 Quy Tắc Luôn Tuân Thủ:
1. **Think first (Suy nghĩ trước)** — Phát biểu giả định rõ ràng, hỏi lại nếu chưa rõ, trình bày các phương án đánh đổi trước khi hành động.
2. **Simplicity first (Tối giản là trên hết)** — Viết lượng code tối thiểu để giải quyết vấn đề. Tuyệt đối tuân thủ bậc thang tư duy **Ponytail Ladder** dưới đây.
3. **Surgical changes (Thay đổi chuẩn xác)** — Chỉ sửa đổi những gì yêu cầu cần chạm tới. Không cải tiến hay dọn dẹp các vùng code xung quanh không liên quan.
4. **Verify (Xác minh)** — Xác định tiêu chí thành công trước khi thực hiện. Luôn chạy kiểm thử/linting để xác nhận kết quả trước khi hoàn thành.

---

## 1. Think Before Coding
* Không tự ý giả định hay che giấu sự mơ hồ. Hãy trao đổi và làm rõ.
* Nếu có nhiều cách giải thích, hãy trình bày các phương án cho người dùng thay vì tự chọn một cách im lặng.
* Nếu có cách tiếp cận đơn giản hơn, hãy đề xuất.

---

## 2. Simplicity First & Bậc Thang Ponytail (Ponytail Ladder)
Chỉ viết lượng code tối thiểu để giải quyết vấn đề. Không xây dựng trước các tính năng dự phòng, không trừu tượng hóa (abstractions) cho code dùng một lần.

Trước khi viết bất kỳ dòng code mới nào, AI phải dừng lại ở bậc thang đầu tiên thỏa mãn:
1. **YAGNI (You Aren't Gonna Need It):** Tính năng này có thực sự cần thiết không? Nếu không cần thiết $\rightarrow$ Bỏ qua.
2. **Tái sử dụng (Reuse):** Codebase đã có helper, util hay pattern tương tự chưa? Nếu có $\rightarrow$ Tái sử dụng, không viết lại.
3. **Thư viện chuẩn (Stdlib):** Thư viện chuẩn Java/Spring đã hỗ trợ sẵn tính năng này chưa? Nếu có $\rightarrow$ Sử dụng nó.
4. **Tính năng gốc (Native Platform):** Nền tảng hoặc cấu trúc Spring Boot hiện tại đã có tính năng gốc hỗ trợ chưa? Nếu có $\rightarrow$ Sử dụng native.
5. **Thư viện đã cài (Dependency):** Có dependency nào đã được khai báo sẵn trong `pom.xml` giải quyết được vấn đề không? Nếu có $\rightarrow$ Sử dụng nó.
6. **Viết một dòng (One-liner):** Đoạn code này có thể rút gọn tối đa trên một dòng không? Nếu có $\rightarrow$ Viết một dòng.
7. **Lượng code tối thiểu:** Chỉ khi không còn cách nào khác mới viết code mới với lượng tối thiểu nhất để hệ thống hoạt động.

*Quy tắc bổ sung:*
* Không tự ý thêm thư viện mới (dependency) nếu có thể tránh được.
* Ưu tiên việc xóa bỏ code thừa (deletion) hơn là thêm code mới. Chọn giải pháp đơn giản thay vì phức tạp.
* Chú thích các phần tối giản có chủ đích bằng comment `// ponytail: [lý do và hướng nâng cấp sau này]`.

---

## 3. Surgical Changes (Thay đổi chuẩn xác)
* Không "cải tiến" code xung quanh, comment hoặc định dạng không thuộc phạm vi yêu cầu.
* Không refactor những phần đang hoạt động tốt.
* Giữ đúng style code hiện tại của dự án, tuân thủ mô hình Clean Architecture và nguyên lý SOLID.
* Loại bỏ các import, biến, hoặc hàm thừa do chính thay đổi của bạn tạo ra.

---

## 4. Goal-Driven Execution (Thực thi theo mục tiêu)
* Chuyển đổi các tác vụ thành mục tiêu cụ thể có thể xác minh được.
* Chạy các lệnh kiểm thử (Unit Test, Integration Test) hoặc Server để xác nhận tính năng chạy đúng trước khi kết thúc công việc.
