# E-LIB — Kiến trúc

## Phạm vi

Modular monolith, Java 21 + Spring Boot 3.5.16, React 18 + TypeScript + Vite + Tailwind, PostgreSQL 17, Redis 7.4. PDF.js/S3/OIDC được bổ sung tại phase sở hữu. Chưa có tính năng nghiệp vụ hoặc kết nối production trong phase 1.

```text
Browser → frontend (Vite dev / Nginx build)
                  /api → Spring Boot → PostgreSQL
                                    → Redis
                                    → private S3/R2 (phase 6–7)
```

Frontend/API cùng origin: Vite proxy khi phát triển, Nginx proxy khi dùng Compose. Không mở CORS wildcard. Nginx trả SPA cho route giao diện; `/api/` luôn đi backend, không trả index.html giả thành công.

## Cấu trúc thực tế

```text
frontend/src/{components,pages,test}/
frontend/e2e/                  # Browser acceptance tests
backend/src/main/java/vn/edu/phuxuan/elib/
  ElibApplication.java
  config/                     # Deny-by-default security
  web/                        # Request ID, sanitized ProblemDetail
backend/src/main/resources/
  application.yml
  db/migration/               # Flyway only
backend/src/test/java/        # Error, API, migration and PostgreSQL design tests
scripts/                      # Local env setup and verification
frontend/{Dockerfile,nginx.conf}
backend/Dockerfile
docker-compose.yml
docs/                         # requirements, contracts, decisions, verified status
```

Business modules tương lai: identity, organization, catalog, circulation, digital, reading, notification, reporting, administration. Mỗi module Controller → Service → Repository; DTO không expose entity. Chỉ tạo module khi đến phase, không tạo hàng loạt lớp rỗng.

## Nền tảng runtime

- Host mặc định: frontend 5173, backend 18080, DB 55432, Redis 56379. Bind loopback; cổng nội bộ container lần lượt 8080/8080/5432/6379.
- PostgreSQL volume riêng cho major 17; không đụng volume postgres:latest cũ. Redis có password và AOF cho môi trường local.
- Compose mặc định chạy DB/Redis; profile `app` thêm backend/frontend với dependency healthcheck.
- Flyway tạo schema `elib`; V1 đặt comment, chưa tạo bảng nghiệp vụ. JDBC/JPA đã kết nối, không có seed admin hoặc dữ liệu giả.
- Graceful shutdown; timeout DB/Redis; pool JDBC tối đa 10 (giá trị phát triển, phải đo trước production).
- Readiness kiểm tra DB + Redis + trạng thái nhận request; liveness chỉ kiểm tra tiến trình. Lỗi phụ thuộc không gây vòng lặp restart liveness.
- Public health chỉ có status (root có thêm tên nhóm); không lộ host, disk, password, stack hoặc component detail.

## Security/error/logging

Phase 1 chỉ cho GET ba health URL, deny-all với mọi API khác, không tài khoản mặc định, không Basic/form login. CSRF được giữ. API bị chặn trả ProblemDetail 401/403; lỗi MVC giữ status 400/404/405/415 phù hợp và không lộ stacktrace. Mỗi request có UUID do server tạo, trả `X-Request-ID` và log status/duration; không ghi query/body/token.

Phase 2 sử dụng Spring Security OIDC Authorization Code; session phía server trong Redis, cookie HttpOnly/SameSite/Lax và Secure khi HTTPS. Không lưu bearer/refresh token trong localStorage. Lúc thêm session phải chuyển cấu hình STATELESS sang IF_REQUIRED và triển khai CSRF token flow; chưa giả lập OIDC trong phase 1.

## Tìm kiếm

Phase 4 dùng PostgreSQL FTS (`tsvector` + GIN index) cho tìm kiếm sách. Query dùng `plainto_tsquery('simple', :q)`. Nếu FTS không đủ hiệu năng dưới tải production (đo ở Phase 13 bằng k6), cân nhắc Elasticsearch.

## Rate limiting

Hai lớp: Nginx `limit_req_zone` (upload/static) và `bucket4j-spring-boot-starter` + Redis (per-user API/streaming). Chi tiết tại [security-checklist.md](security-checklist.md).

## Rủi ro và phase xử lý

| Rủi ro | Kiểm soát / gate |
|---|---|
| Đăng nhập sai domain/quyền | OIDC issuer/audience/nonce/state, verified email, domain allowlist, role server; phase 2 |
| Mượn đồng thời | Transaction + row lock + partial UNIQUE; phase 5 |
| Lộ PDF | Private bucket, auth/grant trên mọi stream/range, rate limit, watermark/audit; phase 7 |
| Gian lận tracking | Session ownership, sequence dedup, cap server time, idle/hidden/multi-tab; phase 8 |
| Email gửi trùng | Dedup key, retry bounded, transactional enqueue; phase 9 |
| Mất dữ liệu | Versioned migrations, backup + restore drill, chốt RPO/RTO; phase 14 |
| Dependency lỗi | Lockfile, bản vá, npm audit; Maven/OSV và image scan trước release |
| Tài liệu lệch code | Một status chuẩn, schema/API contract được kiểm thử, ghi phạm vi đã xác minh |

Mục tiêu <2s trang, <3s reader và barcode gần tức thời là yêu cầu phải đo phase 13; build thành công không chứng minh đạt tải production.

## Tài liệu kỹ thuật đối chiếu

- [Spring Boot 3.5 requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html)
- [Actuator health/probes](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html)
- [PostgreSQL official image and volume layout](https://hub.docker.com/_/postgres)
