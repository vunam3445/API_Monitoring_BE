# Task: Tích hợp API cho Revenue Analytics Dashboard

## 1. API Tổng quan doanh thu (Stats)
**Route:** `GET /api/admin/revenue/stats`  
**Mô tả:** Trả về các chỉ số tài chính và trạng thái gói cước hiện tại.  
**Đầu ra:**
```json
{
  "totalRevenue": 1240000.0,
  "revenueGrowth": 12.5,
  "mrr": 98500.0,
  "mrrGrowth": 8.2,
  "activeSubscriptions": 3842,
  "subsGrowth": 15.0,
  "expiringSoon": 142,
  "arpu": 27.9,
  "ltv": 1450.0
}
```

## 2. API Biểu đồ doanh thu (Charts)
**Route:** `GET /api/admin/revenue/charts?period=last_30_days`  
**Mô tả:** Trả về dữ liệu chuỗi thời gian để vẽ biểu đồ doanh thu.  
**Đầu ra:**
```json
{
  "labels": ["01 May", "02 May", ...],
  "datasets": [
    {
      "label": "Revenue",
      "data": [1200, 1500, 1100, ...]
    }
  ]
}
```

## 3. API Phân tích người dùng & Đăng ký (Analytics)
**Route:** `GET /api/admin/revenue/subscription-analytics`  
**Mô tả:** Trả về dữ liệu so sánh các nhóm người dùng và xu hướng.  
**Đầu ra:**
```json
{
  "usersComparison": {
    "free": 12450,
    "paid": 3520
  },
  "upgradeTrends": {
    "count": 245,
    "growth": 18.4
  },
  "churnMetrics": {
    "rate": 2.1,
    "status": "Good"
  }
}
```

## 4. API Chi tiết hiệu quả gói cước (Plan Breakdown)
**Route:** `GET /api/admin/revenue/plan-breakdown`  
**Mô tả:** Thống kê chi tiết từng loại gói cước (thay thế cho quản lý subscription cũ).  
**Đầu ra:**
```json
[
  {
    "id": "free",
    "name": "Free Plan",
    "activeSubscribers": 12450,
    "monthlyRevenue": 0.0,
    "churned30d": 452,
    "retention": 92.0,
    "growth": 0
  },
  {
    "id": "pro",
    "name": "Pro Plan",
    "activeSubscribers": 3200,
    "monthlyRevenue": 156800.0,
    "churned30d": 12,
    "retention": 99.2,
    "growth": 14.5
  }
]
```

## 5. API Danh sách giao dịch (Transactions)
**Route:** `GET /api/admin/revenue/recent-transactions?page=0&size=10`  
**Mô tả:** Lấy danh sách các giao dịch thanh toán mới nhất.  
**Đầu ra:**
```json
{
  "content": [
    {
      "id": "TXN-8821",
      "userName": "Alex Rivera",
      "userEmail": "alex@rivera.com",
      "amount": 490.0,
      "plan": "Pro Yearly",
      "date": "2023-05-12T10:00:00Z",
      "status": "SUCCESS"
    }
  ],
  "totalElements": 1284
}
```
