Chào bạn, những nhận xét trên rất xác đáng để nâng cấp dự án từ mức "chạy được" lên mức "chuyên nghiệp và dễ bảo trì".

Để AI có thể thực hiện refactor (tái cấu trúc) một cách chính xác nhất, bạn có thể gửi cho AI các **Prompt (Yêu cầu)** được chia nhỏ theo từng vấn đề dưới đây. Tôi đã soạn sẵn các Task để bạn chỉ cần Copy-Paste:

---

### Task 1: Chuyển đổi sang Constructor Injection & Clean Dependency
**Mục tiêu:** Giải quyết vi phạm nguyên lý **DIP** (Dependency Inversion) và loại bỏ `@Autowired` trên field.

> **Prompt:** "Hãy refactor class `BaseService` và các class kế thừa nó (như `SubscriptionPlanService`). 
> 1. Thay thế tất cả `@Autowired` trên field bằng **Constructor Injection**. 
> 2. Sử dụng `@RequiredArgsConstructor` của Lombok để code gọn gàng hơn. 
> 3. Đảm bảo các field được đánh dấu là `private final` hoặc `protected final`. 
> 4. Cập nhật lại các constructor ở lớp con để gọi đúng `super(...)` từ lớp cha."

---

### Task 2: Tách biệt Logic Caching (Decoupling)
**Mục tiêu:** Giải quyết vi phạm nguyên lý **SRP** (Single Responsibility). Service chỉ nên lo logic nghiệp vụ, không nên lo chi tiết hạ tầng Redis.

> **Prompt:** "Tôi muốn tách logic Cache ra khỏi `BaseService` để tuân thủ nguyên lý Đơn nhiệm (SRP). 
> 1. Hãy tạo một Interface `ICacheService` với các phương thức như `get(key)`, `set(key, value, ttl)`, `evict(key)`, `evictByPrefix(prefix)`.
> 2. Triển khai `RedisCacheServiceImpl` thực thi interface đó bằng `RedisTemplate`.
> 3. Thay thế việc gọi trực tiếp `redisTemplate` trong `BaseService` bằng cách inject `ICacheService`.
> 4. Hoặc: Hãy hướng dẫn tôi cấu hình Spring Cache với `@Cacheable` và `@CacheEvict` để loại bỏ hoàn toàn code Redis thủ công trong Service."



---

### Task 3: Áp dụng Interface Segregation (ISP) cho Service
**Mục tiêu:** Tránh việc các Service đơn giản bị ép buộc phải có các phương thức Delete/Update nếu không cần thiết.

> **Prompt:** "Hiện tại `BaseService` đang chứa tất cả các phương thức CRUD. Hãy giúp tôi tách interface này theo nguyên lý ISP:
> 1. Tạo `IReadOnlyService<DTO, ID>` (chỉ có findById, findAll).
> 2. Tạo `ICrudService<DTO, ID>` kế thừa từ `IReadOnlyService` (thêm create, update, delete).
> 3. Refactor `BaseService` để thực thi các interface này. 
> 4. Chỉnh sửa `SubscriptionPlanController` để nó phụ thuộc vào interface `ISubscriptionPlanService` thay vì class thực thi."

---

### Task 4: Chuyển đổi từ ModelMapper sang MapStruct
**Mục tiêu:** Tăng hiệu năng và đảm bảo Type-safe khi mapping dữ liệu.

> **Prompt:** "Tôi muốn thay thế `ModelMapper` bằng `MapStruct` để tối ưu hiệu năng. 
> 1. Hãy tạo một interface `BaseMapper<Entity, DTO>` sử dụng MapStruct.
> 2. Tạo `SubscriptionPlanMapper` kế thừa từ `BaseMapper`.
> 3. Thay đổi `BaseService` để sử dụng `BaseMapper` thay vì `modelMapper.map()`. 
> 4. Cấu hình để MapStruct hoạt động tốt với Spring IoC (componentModel = 'spring')."

---
✅ **Task 1: Constructor Injection & Clean Dependency (DIP)** - HOÀN THÀNH
✅ **Task 2: Tách biệt Logic Caching (SRP & DIP)** - HOÀN THÀNH
✅ **Task 3: Interface Segregation (ISP)** - HOÀN THÀNH
✅ **Task 4: Chuyển đổi sang MapStruct** - HOÀN THÀNH
✅ **Task 5: Tổ chức lại thư mục theo Feature (Module-based)** - HOÀN THÀNH

---
🎉 **Dự án đã được refactor toàn diện theo chuẩn Clean Architecture và SOLID!**

### Task 5: Tổ chức lại thư mục theo Feature (Module-based)
**Mục tiêu:** Cải thiện khả năng mở rộng của dự án.

> **Prompt:** "Hãy giúp tôi lập kế hoạch chuyển đổi cấu trúc thư mục từ Layer-based (controllers, services, repositories riêng biệt) sang **Feature-based (Module-based)**. 
> Ví dụ: di chuyển các file liên quan đến Subscription vào gói `com.example.demo.modules.subscription`. 


---
