# E-LIB — Decision Log

## D001 — Stack Technology Selection
- **Issue**: Chọn framework cho frontend và backend
- **Decision**: React + TypeScript + Tailwind CSS (frontend), Java 21 + Spring Boot (backend)
- **Reason**: Theo stack bắt buộc trong E-LIB_AI_AGENT_MASTER_GUIDE.pdf trang 1; tài liệu nghiệp vụ gốc liệt kê nhiều lựa chọn backend
- **Date**: 2026-09-13
- **Blocked**: Không

## D002 — Architecture Pattern
- **Issue**: Monolith vs Microservices
- **Decision**: Modular Monolith
- **Reason**: Dễ deploy, ít phức tạp cho MVP, dễ scale về sau nếu cần
- **Date**: 2026-09-13
- **Blocked**: Không

## D003 — Database Schema Design
- **Issue**: Quan hệ giữa BookTitle, BookCopy, Borrow
- **Decision**: 
  - Institution → Campus → Library → Department → User (tổ chức)
  - BookTitle → BookCopy (1:many, 1 BookTitle có nhiều bản sao)
  - Borrow tham chiếu BookCopy, không phải chỉ BookTitle
  - Migration versioned, có thể chạy lại trên môi trường sạch
- **Reason**: Phù hợp với mô hình thực tế, dễ quản lý
- **Date**: 2026-09-13
- **Blocked**: Không

## D004 — Digital Library Security
- **Issue**: Làm sao bảo vệ tài liệu số
- **Decision**: Private S3 storage + permission check + streaming API + watermark + rate limit
- **Not**: Public PDF URL, trả storage URL trực tiếp, coi chặn Ctrl+S làm DRM tuyệt đối
- **Reason**: Phân quyền đúng, streaming qua API an toàn hơn
- **Date**: 2026-09-13
- **Blocked**: Không

## D005 — Authentication
- **Issue**: Auth flow
- **Decision**: Google OAuth 2.0 / OIDC. Không cần mật khẩu E-LIB riêng.
- **Reason**: SSO tiện, ít user phải nhớ mật khẩu, tích hợp Google Workspace
- **Date**: 2026-09-13
- **Blocked**: Không

## D006 — Sửa mô hình dữ liệu và policy (2026-09-13)
- Vấn đề: reserved name user, hai reading_log, cột returned_at trùng, CHECK mâu thuẫn, barcode cấp đầu sách và policy hard-coded.
- Quyết định: schema thiết kế duy nhất tại docs/schema.sql; app_user, copy barcode/status, CHECK kết hợp + unique active loan; tách reading; policy versioned; session auth ở Redis.
- Xác minh: PostgreSQL Testcontainers thực thi DDL và các ca loan/return/duplicate/grants. Schema thiết kế chưa là runtime migration nghiệp vụ.

## D007 — Giữ ranh giới Foundation (2026-09-13)
- Chỉ triển khai app shell, health, cấu hình DB/Redis, Flyway namespace, security/error/logging và tests.
- Bỏ dependency chưa dùng (PDF.js/axios/charts/store) khỏi foundation; bổ sung bản đã kiểm tra khi phase tương ứng bắt đầu.
- Flyway V1 chỉ khởi tạo namespace/lịch sử; không seed admin, sách, domain hay policy giả.

## D008 — Browser authentication (2026-09-13)
- Phase 2 dùng Spring Security OIDC + server session Redis, HttpOnly cookie; thay contract JWT/refresh-token trả frontend trong bản nháp cũ.
- Lý do: không tự viết OAuth/token flow, giữ credential ngoài browser JS. CSRF và same-origin bắt buộc. Domain/client ID/secret chờ cấu hình thực tế.
- Phase 1 vẫn deny-default, stateless và chưa có login hoạt động.

## D009 — Môi trường local và nguồn tiến độ (2026-09-13)
- Compose có profile app, port loopback, Postgres major 17 volume riêng, Redis authenticated; không tự dùng volume postgres:latest cũ.
- Một status chuẩn tại docs/development-status.md, file gốc chỉ dẫn tới đó. Thứ tự phase khớp Guide trang 3.
- Chưa có bằng chứng triển khai production; không tạo domain giả.

## D010 — Dependency patches (2026-09-13)
- Spring Boot 3.5.16/Java 21; React 18 giữ nguyên. Router nâng 7.18.3, Vitest 4.1.11 để sửa advisory xuất hiện trong npm audit; Vite 6.4.3 tương thích.
- [Router advisory](https://github.com/remix-run/react-router/security/advisories/GHSA-wrjc-x8rr-h8h6); [Vitest advisory](https://github.com/vitest-dev/vitest/security/advisories/GHSA-82fw-gwwq-j7x9).
- Lockfile được lưu và Docker dùng npm ci. Kết quả audit là snapshot, không phải bảo đảm không có CVE tương lai.

## D011 — Full-text search dùng PostgreSQL FTS (2026-09-13)
- Vấn đề: tìm kiếm sách theo tiêu đề, tác giả, nhà xuất bản cần nhanh và chính xác hơn `LIKE '%query%'`.
- Quyết định: dùng PostgreSQL built-in FTS (`tsvector` generated column + GIN index + `plainto_tsquery('simple', :q)`). Cân nhắc Elasticsearch ở Phase 13 nếu PG FTS không đủ hiệu năng dưới tải.
- Lý do: không thêm infrastructure dependency sớm; PG FTS đủ tốt cho 10k–100k titles; simple config phù hợp tiếng Việt không dấu + dấu.

## D012 — API versioning strategy (2026-09-13)
- Vấn đề: cần chiến lược rõ ràng khi API breaking change.
- Quyết định: URL-based versioning `/api/v1` → `/api/v2`. Duy trì v1 song song ít nhất 1 release. Non-breaking evolution không cần bump.
- Không dùng header-based (`Accept-Version`) để giữ đơn giản.

## D013 — Mở rộng book_copy.status (2026-09-13)
- Vấn đề: chỉ AVAILABLE/BORROWED/LOST không đủ cho thực tế thư viện.
- Quyết định: thêm DAMAGED (hư hỏng) và MAINTENANCE (đang bảo trì/đóng dấu/dán mã). Implement ở Phase 4 migration.
- Catalog service đổi LOST/DAMAGED/MAINTENANCE/AVAILABLE; circulation service đổi BORROWED. Không cho mượn bản đang DAMAGED/MAINTENANCE.

## D014 — Thêm ISBN và publication_year cho book_title (2026-09-13)
- Vấn đề: hệ thống thư viện chuẩn cần ISBN để tra cứu và đối soát.
- Quyết định: thêm `isbn VARCHAR(20)` (nullable — sách nội bộ có thể không có) và `publication_year SMALLINT` (nullable) vào `book_title`. Implement ở Phase 4 migration.
- ISBN hỗ trợ cả ISBN-10 và ISBN-13; không enforce format ở DB, validate ở service layer.

## D015 — Rate limiting hai lớp (2026-09-13)
- Vấn đề: cần chống abuse cho streaming, upload, và API endpoints.
- Quyết định: Nginx `limit_req_zone` cho upload/static + `bucket4j-spring-boot-starter` + Redis cho per-user API rate limiting.
- Lý do: defense-in-depth; Nginx chặn tầng transport, bucket4j chặn tầng application với per-user granularity.
- Cấu hình Phase 6–7; enforce Phase 12.

## D016 — Load testing bằng k6 (2026-09-13)
- Vấn đề: Phase 13 yêu cầu đo hiệu năng nhưng chưa chọn tool.
- Quyết định: k6 (JavaScript, nhẹ, output dễ đọc, tích hợp CI).
- Không chọn: Gatling (Scala verbose), JMeter (XML heavy, khó maintain scripts).

## D017 — Chiến lược FK giữa Phase 2 và 3 (2026-09-13)
- Vấn đề: `app_user.department_id` FK tới `department`, nhưng Phase 2 tạo user trước Phase 3 tạo department.
- Quyết định: Phase 2 tạo `app_user` không có `department_id`. Phase 3 thêm column nullable + FK, backfill, rồi tùy yêu cầu thêm NOT NULL constraint.
- Code Phase 2 xử lý user chưa có department (department = null). Không tạo dummy department.

## D018 — Notification type và channel (2026-09-13)
- Vấn đề: bảng notification chỉ có message text, không phân loại.
- Quyết định: Phase 9 migration thêm `notification_type VARCHAR(50) NOT NULL` (DUE_REMINDER/OVERDUE/SYSTEM) và `channel VARCHAR(20) NOT NULL DEFAULT 'EMAIL'` (EMAIL/IN_APP).
- Mở rộng channel sang push notification nếu có nhu cầu sau.

## D019 — Document status state machine (2026-09-13)
- Vấn đề: digital_document có 4 trạng thái nhưng chưa rõ luồng chuyển đổi.
- Quyết định: DRAFT → PENDING → APPROVED → PUBLISHED. Cho phép PENDING → DRAFT (reject) và APPROVED → DRAFT (rework). Không cho quay về từ PUBLISHED — dùng `is_active=false` để ẩn.
- Librarian upload → DRAFT; submit → PENDING; approve → APPROVED; publish → PUBLISHED.

## D020 — Coverage targets và contract testing (2026-09-13)
- Backend ≥80% line (service ≥90%); frontend ≥70%. Enforce bằng JaCoCo + Vitest v8 từ Phase 2.
- Phase 12: contract testing bằng Spring Cloud Contract hoặc Pact đảm bảo API changes không break frontend.

