# Chiến lược kiểm thử

## Gate phase 0/1

| Lớp | Kiểm tra | Công cụ |
|---|---|---|
| Schema thiết kế | DDL PostgreSQL; barcode UNIQUE, active loan UNIQUE, return/reborrow, trạng thái null, root category, grants | `FoundationIntegrationTest` |
| Migration | Clean DB, Flyway validate, migrate lần hai không chạy lại | Testcontainers PostgreSQL 17 |
| API nền | Health dùng DB/Redis thật, private endpoint 401, CSRF 403, request ID, không lộ details | Spring Boot random port |
| Error handling | Validation/malformed JSON 400, method 405 + Allow, exception 500 được che | MockMvc |
| Frontend | Strict TypeScript, ESLint, Tailwind/Vite build | npm scripts |
| Trạng thái UI | Loading, UP, HTTP lỗi, sai payload, null, mạng lỗi/retry, timeout, unmount | Vitest + Testing Library |
| Browser | Desktop/mobile, deep-link refresh, 404, outage mock/retry, API thật | Playwright Chromium |
| Hạ tầng lỗi thật | Tạm pause PostgreSQL/Redis; readiness 503, liveness 200, hồi phục 200 | `scripts/smoke.py --check-outages` |

Các test nghiệp vụ sử dụng schema thiết kế chỉ xác minh DDL/constraints, không chứng minh circulation/auth/tracking đã triển khai. Testcontainers luôn bắt buộc Docker, không tự skip để tạo kết quả pass giả. Không thay PostgreSQL bằng H2.

## Coverage targets

- **Backend**: ≥80% line coverage tổng thể; service layer ≥90%. Enforce bằng JaCoCo Maven plugin từ Phase 2.
- **Frontend**: ≥70% line coverage tổng thể; business logic và API integration ≥80%. Enforce bằng Vitest v8 coverage provider từ Phase 2.
- Phase 1 foundation đã có targeted tests (10 backend + 8 frontend unit + 4×2 E2E); coverage target áp dụng tích lũy từ Phase 2.

## Gate các phase sau

Auth/domain/RBAC/CSRF/session expiry; organization scope; transaction race; policy/date/fine rounding; permission/range/upload validation; heartbeat replay/idle/hidden/multi-tab; retry/dedup email; report reconciliation/XLSX injection. Mỗi module có negative tests trước khi cập nhật status. Phase 12 tổng hợp regression/security/acceptance; phase 13 đo tải; phase 14 phục hồi backup và rollout.

## Contract testing (Phase 12)

Dùng Spring Cloud Contract hoặc Pact cho consumer-driven contract testing giữa frontend và backend. Đảm bảo API contract changes không break frontend mà chưa được cập nhật. Triển khai ở Phase 12 (integration testing).

## Load testing (Phase 13)

Công cụ: **k6** (JavaScript, nhẹ, scriptable, output dễ đọc, tích hợp CI tốt). Đo targets:
- Trang tải < 2s (P95) dưới 500 concurrent users.
- PDF reader mở < 3s (P95).
- Barcode scan phản hồi < 500ms (P95).
- PostgreSQL FTS query < 200ms (P95) với dataset ≥10k titles.

Kết quả phải có evidence cụ thể (k6 summary output) trước khi đánh dấu Phase 13 hoàn thành. Build thành công hoặc test pass trên máy dev không chứng minh đạt tải production.

Evidence cụ thể và giới hạn xác minh nằm trong `development-status.md`, không ghi pass trước khi chạy.
