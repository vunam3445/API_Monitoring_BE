package com.example.demo.modules.system.services;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe registry để đo đếm chính xác số lượng Worker Threads đang
 * THỰC SỰ BẬN RỘN xử lý tin nhắn từ RabbitMQ ở bất kỳ thời điểm nào.
 *
 * Lý do tồn tại:
 * Spring AMQP's getActiveConsumerCount() chỉ đếm số luồng đang "sống" và
 * giữ kết nối với broker (Alive Threads), không phân biệt luồng đang
 * nhàn rỗi chờ tin hay đang tích cực xử lý. Registry này lấp đầy khoảng
 * trống đó bằng cách đếm chính xác số Busy Threads tại thời điểm thực.
 *
 * Cơ chế hoạt động:
 * - Mỗi Consumer gọi increment() ngay khi nhận được tin nhắn để xử lý.
 * - Mỗi Consumer gọi decrement() trong khối finally sau khi xử lý xong.
 * - Điều này đảm bảo biến đếm luôn phản ánh đúng số lượng công việc đang
 *   được thực thi song song ở bất kỳ thời điểm nào.
 */
@Component
public class ActiveWorkerRegistry {

    private final AtomicInteger busyCount = new AtomicInteger(0);

    /**
     * Gọi khi một Consumer bắt đầu xử lý một tin nhắn.
     * Thread-safe, không cần synchronized.
     */
    public void increment() {
        busyCount.incrementAndGet();
    }

    /**
     * Gọi khi một Consumer hoàn thành xử lý một tin nhắn (trong khối finally).
     * Thread-safe, bao gồm cơ chế phòng ngừa trường hợp đếm âm do exception bất ngờ.
     */
    public void decrement() {
        int result = busyCount.decrementAndGet();
        if (result < 0) {
            busyCount.set(0); // Bảo vệ khỏi trường hợp lệch đếm cực đoan
        }
    }

    /**
     * Trả về số lượng Worker Threads đang THỰC SỰ BẬN RỘN tại thời điểm gọi.
     */
    public int getActiveCount() {
        return busyCount.get();
    }
}
