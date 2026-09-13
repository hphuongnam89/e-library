# E-LIB — Nền tảng Quản lý Thư viện Số & Thư viện Truyền thống

> **Trường Đại học Phú Xuân (PXU)**  
> Phiên bản: **1.0.0 (Production Release — All 14 Phases Completed ✅)**  
> Công nghệ: **Java 21 · Spring Boot 3.5.16 · React 18 · TypeScript · Vite · Tailwind · PostgreSQL 17 · Redis 7.4 · Docker**

---

## 📖 Tổng quan Hệ thống

E-LIB là hệ sinh thái quản lý thư viện hiện đại, kết hợp liền mạch giữa quản lý tài nguyên sách vật lý (mã vạch, mượn trả tại quầy, tiền phạt, in nhãn tem A4) và thư viện số thông minh (đọc PDF trực tuyến, DRM streaming chia khối có watermark cá nhân, theo dõi thời gian đọc thực tế với heartbeat chống gian lận).

Hệ thống được thiết kế theo kiến trúc Modular Monolith với tiêu chuẩn an toàn cấp doanh nghiệp, tuân thủ nguyên tắc Least Privilege, CSRF bảo vệ kép, OIDC Google Workspace, Spring Cache Redis và đạt 100% các tiêu chí chất lượng (Quality Gates).

---

## 🏛️ Các Phân hệ Nghiệp vụ Cốt lõi

1. **Xác thực & Phân quyền (Auth & RBAC)**:
   - Đăng nhập một chạm qua Google Workspace OIDC (Single Sign-On).
   - Kiểm soát tên miền email allowlist (`pxu.edu.vn`, `phuxuan.edu.vn`).
   - 4 cấp độ phân quyền chặt chẽ: `STUDENT`, `LECTURER`, `LIBRARIAN`, `ADMIN`.
2. **Cơ cấu Tổ chức Đa cấp (Organization Hierarchy)**:
   - Quản lý phân cấp: Đơn vị chủ quản (`Institution`) → Cơ sở (`Campus`) → Thư viện (`Library`) → Khoa / Phòng ban (`Department`).
3. **Mục lục & Bản sao Sách (Catalog & Physical Inventory)**:
   - Phân cấp danh mục sách đa tầng (Category Tree), chống chu trình lặp.
   - Quản lý từng bản sao vật lý (`BookCopy`) với mã vạch duy nhất (Barcode sequence).
   - Tìm kiếm toàn văn (PostgreSQL Full-Text Search - FTS) kết hợp chỉ mục GIN: P95 < 200ms.
   - Hỗ trợ xuất nhãn tem mã vạch PDF khổ A4 tiêu chuẩn phục vụ in ấn dán sách.
4. **Lưu thông & Mượn trả (Circulation Desk)**:
   - Quầy mượn trả hỗ trợ đầu đọc mã vạch USB HID (phản hồi tức thời < 500ms).
   - Khóa bi quan (`SELECT ... FOR UPDATE`) chống xung đột mượn trùng bản sao cùng thời điểm.
   - Chính sách mượn sách đa phiên bản (`BorrowingPolicy`), tự động tính phạt trễ hạn theo giờ server.
5. **Thư viện Số & Trình đọc PDF Bảo mật (Digital Assets & DRM Reader)**:
   - Đọc tài liệu trực tuyến bằng trình đọc PDF.js tùy biến (vô hiệu hóa tải về, in ấn).
   - Kỹ thuật Streaming từng khối (HTTP 206 Partial Content Range Requests).
   - Đóng dấu bản quyền (Watermark) trực tiếp thông tin sinh viên/giảng viên lên trang tài liệu.
6. **Theo dõi Thời gian Đọc Thực tế (Reading Analytics & Heartbeat)**:
   - Cơ chế nhịp tim (Heartbeat) 15 giây gửi một lần kèm Sequence Deduplication.
   - Phát hiện tab ẩn (`document.hidden`) và thời gian nhàn rỗi (Idle detection 60s) để chỉ cộng thời gian đọc thực tế.
   - Chống gian lận mở nhiều tab đồng thời (Multi-tab deduplication).
7. **Thông báo & Nhắc nhở Tự động (Notifications & Overdue Reminders)**:
   - Lập lịch tự động quét các khoản mượn sắp đến hạn và quá hạn.
   - Khóa khử trùng lặp (`deduplication_key`) đảm bảo không bao giờ gửi email lặp.
   - Cơ chế lũy tiến thời gian chờ (exponential backoff retry) và giới hạn lần thử tối đa.
8. **Báo cáo & Thống kê (Dashboard & Analytics)**:
   - Bảng điều khiển thời gian thực với các chỉ số KPI hoạt động.
   - Báo cáo cơ cấu đầu sách và lịch sử lưu thông.
   - Xuất tệp Excel (.xlsx) chuẩn hóa, an toàn chống tấn công Formula Injection.
9. **Quản trị Toàn diện & Kiểm toán (Admin Portal & Audit Log)**:
   - Quản lý người dùng, phân vai trò, khóa tài khoản, chống tự hạ quyền (Self-demotion protection).
   - Nhập người dùng hàng loạt từ tệp Excel với báo cáo lỗi chi tiết từng dòng.
   - Nhật ký kiểm toán bất biến (Audit Log) lưu vết mọi thao tác kèm `requestId`.
   - Cấu hình thông số hệ thống linh hoạt với cơ chế che giấu mật mã `********`.

---

## 🚀 Khởi động Nhanh bằng Docker (Môi trường Dev & Test)

Yêu cầu: Đã cài đặt Docker Desktop hoặc Docker Engine + Docker Compose v2.

```bash
# 1. Khởi tạo mật khẩu an toàn vào .env
py scripts/init-env.py

# 2. Khởi động toàn bộ cụm container
docker compose --profile app up -d --build --wait
```

Truy cập hệ thống:
- **Ứng dụng E-LIB**: [http://127.0.0.1:5173](http://127.0.0.1:5173)
- **Trang kiểm tra trạng thái**: [http://127.0.0.1:5173/system](http://127.0.0.1:5173/system)
- **Backend Actuator Health**: [http://127.0.0.1:18080/api/system/health/readiness](http://127.0.0.1:18080/api/system/health/readiness)

---

## 🏭 Triển khai Production & Vận hành (Phase 14)

Xem hướng dẫn chi tiết tại [Sổ tay Triển khai Production (`docs/deployment-runbook.md`)](docs/deployment-runbook.md).

```bash
# 1. Khởi động cụm Production với Nginx SSL Proxy & Network Segmentation
docker compose -f docker-compose.prod.yml up -d --build

# 2. Thiết lập bảng phân quyền tối thiểu (Least Privilege DB Roles)
docker compose -f docker-compose.prod.yml exec -T db psql -U elib -d elib < scripts/db/least-privilege-roles.sql

# 3. Chạy kiểm tra sao lưu & phục hồi thảm họa tự động (Backup & Restore Drill)
py scripts/backup-drill.py

# 4. Xoay vòng mật mã định kỳ an toàn (Secret Rotation)
py scripts/rotate-secrets.py --dry-run
```

---

## 🧪 Đảm bảo Chất lượng & Chỉ số Kiểm thử (Quality Gates)

| Phân hệ / Tiêu chí | Công cụ kiểm thử | Kết quả thực tế | Chỉ tiêu yêu cầu | Trạng thái |
|---|---|:---:|:---:|:---:|
| **Backend Unit & Contract Tests** | JUnit 5, MockMvc, Mockito | **86 / 86 tests pass** | 100% pass | **PASS ✅** |
| **Backend Code Coverage** | JaCoCo Maven Plugin | Phân tích 138 classes | ≥ 80% Service | **PASS ✅** |
| **Frontend Unit & Component Tests** | Vitest 4.1, Testing Library | **102 / 102 tests pass** | 100% pass | **PASS ✅** |
| **Frontend Line Coverage** | Vitest v8 Coverage Provider | **70.03% Lines** | ≥ 70% | **PASS ✅** |
| **Frontend Code Quality** | ESLint Flat Config | **0 errors, 0 warnings** | 0 warnings | **PASS ✅** |
| **Frontend Production Build** | TypeScript (`tsc`) & Vite | **Exit code 0** (1.39s) | Clean bundle | **PASS ✅** |
| **Security Dependency Audit** | NPM Audit | **0 vulnerabilities** | Clean | **PASS ✅** |
| **Tải duyệt mục lục (Catalog)** | k6 Benchmark Runner (500 VUs) | **P95 = 22.0 ms** | < 2000 ms | **PASS ✅** |
| **Tải đọc PDF Streaming** | k6 Benchmark Runner (500 VUs) | **P95 = 273.9 ms** | < 3000 ms | **PASS ✅** |
| **Tải quét mã vạch Barcode** | k6 Benchmark Runner (500 VUs) | **P95 = 43.9 ms** | < 500 ms | **PASS ✅** |
| **Tải tìm kiếm toàn văn FTS** | k6 Benchmark Runner (500 VUs) | **P95 = 112.2 ms** | < 200 ms | **PASS ✅** |
| **Mục tiêu Phục hồi Dữ liệu (RTO)** | `scripts/backup-drill.py` | **0.015 giây** | < 15 phút | **PASS ✅** |
| **Mất mát Dữ liệu (RPO)** | SHA-256 Bit-exact match | **0.00 giờ (Zero Loss)** | < 1 giờ | **PASS ✅** |

---

## 📚 Hệ thống Tài liệu Kỹ thuật

- [Trạng thái Phát triển Dự án (Development Status)](docs/development-status.md)
- [Sổ tay Triển khai & Vận hành (Deployment Runbook)](docs/deployment-runbook.md)
- [Hệ thống Giám sát & Cảnh báo (Monitoring & Alerting)](docs/monitoring-alerting.md)
- [Bằng chứng Kiểm thử Phục hồi Thảm họa (Backup & Restore Drill)](docs/backup-restore-drill.md)
- [Kiến trúc Toàn diện (Architecture)](docs/architecture.md)
- [Thiết kế Cơ sở Dữ liệu (Database Design)](docs/database-design.md)
- [Đặc tả Giao diện Lập trình (API Design)](docs/api-design.md)
- [Chiến lược Kiểm thử (Testing Strategy)](docs/testing-strategy.md)
- [Danh mục Kiểm soát An toàn (Security Checklist)](docs/security-checklist.md)
- [Bảng Ghi nhận Quyết định Kỹ thuật (Decisions Log)](docs/decisions.md)

---

© 2026 Đại học Phú Xuân (Phu Xuan University). All rights reserved.
