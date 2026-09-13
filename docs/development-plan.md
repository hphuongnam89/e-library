# E-LIB — Kế hoạch phát triển

Thứ tự chuẩn: `E-LIB_AI_AGENT_MASTER_GUIDE.pdf`, trang 3. Trạng thái thực tế chỉ ở [development-status.md](development-status.md). Không đánh dấu một phase hoàn thành chỉ vì đã tạo file.

| Phase | Nội dung | Gate |
|---|---|---|
| 0 | Phân tích requirements/repo, kiến trúc, ERD/API, rủi ro | Nguồn rõ, schema kiểm thử được, không mâu thuẫn chính |
| 1 | Frontend/backend/DB/Redis/Docker/config/log/error | Build, lint, test, migration, API/UI thật, hướng dẫn chạy |
| 2 | Google/OIDC + User + RBAC | Auth/domain/role/session/CSRF tests |
| 3 | Institution/Campus/Library/Department + gắn User | FK tổ chức, scope queries, backfill department cho user |
| 4 | BookTitle/BookCopy/Category/Barcode/physical UI | Từng bản một barcode; CRUD/import/print; FTS; chưa mượn trả |
| 5 | Borrow/Return/Due date/Fine/History | Transaction/concurrency/policy/scan |
| 6–7 | Digital upload/permission/reader/streaming/watermark | Metadata/private storage/stream/range auth; upload limits; rate limit; hai phase nhưng cho phép overlap vì phụ thuộc chặt |
| 8 | Heartbeat/active/idle/log/history | Hidden tab, dedup, multi-tab, server-counted time; phase phức tạp nhất — cần acceptance criteria rõ trước khi bắt đầu |
| 9 | Email/reminders/overdue/retry/job | Không gửi trùng, retry/error; notification type/channel |
| 10 | Dashboard/reports/filter/Excel | Đối soát dữ liệu, quyền xuất; phụ thuộc Phase 8 data |
| 11 | Admin/audit/settings/policies | Phân quyền, audit, bulk user import |
| 12 | Integration/security/regression/acceptance | Đủ kịch bản nghiệp vụ, contract testing, không thêm scope |
| 13 | Performance/indexes/cache/load | Đo yêu cầu hiệu năng bằng k6; FTS index tuning |
| 14 | Docker/CI/CD/env/backup/monitoring/docs | Restore drill, secret rotation, release gate |

## Task phase 0

0.1 Đối chiếu hai PDF; 0.2 xác định scope/khác biệt; 0.3 chuẩn hóa schema và contract; 0.4 kiểm tra DDL/constraints; 0.5 báo cáo và cập nhật status.

## Task phase 1

1.1 Maven/Java 21 skeleton và dependency versions. 1.2 React/TS/Tailwind/router + lint/tests. 1.3 PostgreSQL/Redis/env/volumes. 1.4 Flyway/schema validation. 1.5 Security deny-default, error response/request log, probes. 1.6 Docker build và proxy/deep-link. 1.7 test clean start, readiness failure/recovery, README/status và Git checkpoint.

## Task phase 2

2.1 Flyway V2 migration: tạo bảng `app_user` (chưa có `department_id` FK — thêm ở Phase 3). JPA entity + repository. 2.2 Spring Security OIDC config: Google client registration, issuer/audience/nonce/state verification, domain allowlist, verified email check. 2.3 Redis session config: chuyển `STATELESS` → `IF_REQUIRED`, Spring Session + Redis, cookie HttpOnly/SameSite=Lax/Secure. 2.4 CSRF token flow: `GET /api/v1/auth/csrf` cấp token, tất cả unsafe requests yêu cầu CSRF. 2.5 Auth endpoints: `/api/v1/auth/me` (user DTO), `POST /api/v1/auth/logout` (hủy session + cookie, 204). 2.6 Role provisioning: STUDENT/LECTURER/LIBRARIAN/ADMIN từ hồ sơ được cấp, không suy ra từ domain; INACTIVE bị từ chối; đổi role/lock → invalidate session. 2.7 Frontend auth: login redirect, auth state, protected routes, logout UI. 2.8 Tests: OIDC flow (mock Google), role check, session expiry, CSRF enforcement, deny unauthorized, domain reject. 2.9 Cập nhật docs/status.

> **Lưu ý Phase 2:** Google client ID/secret và domain phải được cung cấp trước khi bắt đầu. Nếu chưa có, chuẩn bị mock OIDC provider cho development.

## Task phase 3

3.1 Flyway V3 migration: tạo bảng `institution`, `campus`, `library`, `department`; thêm column `department_id` FK vào `app_user` (nullable ban đầu). 3.2 JPA entities + repositories cho organization hierarchy. 3.3 CRUD endpoints: GET/POST `/api/v1/institutions`, `/campuses`, `/libraries`, `/departments`; GET/PATCH `/{resource}/{id}`. 3.4 Scope enforcement: Admin ghi, user đọc scope cho phép; parent phải tồn tại; không cho quan hệ chéo. 3.5 Không hard-delete tổ chức đang được tham chiếu (409). 3.6 Backfill/assign department cho existing users. 3.7 Tests: FK constraints, scope queries, 409 on referenced delete, hierarchy validation. 3.8 Cập nhật docs/status.

## Task phase 4

4.1 Flyway V4 migration: `category`, `book_title` (bao gồm `isbn`, `publication_year`), `book_copy` (statuses: AVAILABLE/BORROWED/LOST/DAMAGED/MAINTENANCE), `book_barcode_sequence`. 4.2 PostgreSQL FTS: `tsvector` column + GIN index trên `book_title` cho tìm kiếm title/author/publisher. 4.3 CRUD endpoints cho book_title, book_copy, category. 4.4 Barcode generation: sequence + UNIQUE, không MAX+1. 4.5 Barcode PDF: `POST /api/v1/book-copies/barcode-pdf`. 4.6 Import: `POST /api/v1/book-titles/import` với file validation. 4.7 Category: kiểm tra chu trình, tên trùng. 4.8 Frontend: catalog UI, search, barcode scan input (USB HID). 4.9 Tests: FTS queries, barcode uniqueness, import edge cases, category cycles. 4.10 Cập nhật docs/status.

## Task phase 5

5.1 Flyway V5 migration: `borrow`, `borrowing_policy`; indexes. 5.2 Borrow service: transaction + row lock `book_copy FOR UPDATE`, check AVAILABLE, scope thủ thư/policy, batch borrow. 5.3 Return service: tính phạt server theo policy snapshot, idempotent retry. 5.4 Fine payment: audit, idempotent. 5.5 Policy CRUD: versioned `{loanDays,dailyFine,maxActiveLoans,effectiveFrom}`. 5.6 Frontend: borrow/return UI với barcode scan, lịch sử mượn. 5.7 Tests: concurrency (parallel borrow same copy), policy enforcement, fine calculation, partial unique index, retry idempotency. 5.8 Cập nhật docs/status.

## Task phase 6–7

6.1 Flyway V6 migration: `digital_document`, `document_grant`. 6.2 S3/R2 storage integration: private bucket, upload endpoint. 6.3 Upload: multipart PDF + metadata, validate content-type/size (giới hạn cấu hình, mặc định 50MB, chỉ application/pdf), storage key riêng. 6.4 Document status state machine: DRAFT → PENDING → APPROVED → PUBLISHED (Librarian submit → Admin/Librarian approve; có thể APPROVED → DRAFT cho rework). 6.5 Grant management: PUT grants, scope validation. 6.6 Streaming: `GET /stream` với Range support (206), auth + permission check mỗi request, `Cache-Control: private, no-store`. 6.7 Watermark: server inject user info, không tin client. 6.8 PDF.js reader: không download/print UI. 6.9 Rate limiting: `bucket4j` + Redis cho streaming endpoint; Nginx `limit_req_zone` cho upload. 6.10 Frontend: document list, upload form, reader UI. 6.11 Tests: permission check, Range request, rate limit, upload validation, grant scope. 6.12 Cập nhật docs/status.

> **Lưu ý Phase 6–7:** Hai phase này có dependency chặt — upload mà chưa có reader khó test e2e. Cho phép overlap hoặc chạy liên tiếp không nghỉ.

## Task phase 8

8.1 Flyway V7 migration: `digital_reading_session`, `reading_heartbeat`, `digital_reading_summary`, `physical_reading_session`. 8.2 Session management: POST create, POST end (idempotent). 8.3 Heartbeat processing: sequence dedup, server timestamp, chỉ cộng delta khi active + visible, cap interval. 8.4 Multi-tab handling: không cộng đúp thời gian giữa các tab. 8.5 Idle detection: 60s timeout server-side. 8.6 Summary aggregation: UNIQUE(user, document), cập nhật active_seconds/session_count. 8.7 Reading history: `GET /api/v1/me/reading-history`. 8.8 Frontend: heartbeat client 15s, visibility API, idle tracking. 8.9 Tests: replay attack, out-of-order sequence, hidden tab, idle timeout, multi-tab dedup, network interruption recovery. 8.10 Cập nhật docs/status.

> **Lưu ý Phase 8:** Đây là phase phức tạp nhất. Phải có acceptance criteria chi tiết VÀ comprehensive edge-case tests TRƯỚC khi bắt đầu code. Estimate thời gian gấp đôi so với phase thông thường.

## Task phase 9–11

9.1 Notification entity + dedup key + retry bounded + `notification_type` + `channel`. 9.2 Email job: Spring scheduler, SMTP integration, template. 9.3 Dedup enforcement: không gửi trùng cùng dedup key. 9.4 Phase 10: Dashboard summary, reports với filters, XLSX export (chống formula injection). 9.5 Phase 11: Admin user management (bao gồm bulk import từ Excel), audit log (append-only), settings CRUD. 9.6 Tests cho mỗi sub-phase. 9.7 Cập nhật docs/status.

## Task phase 12–14

12.1 Integration/regression test suite: full scenario coverage, contract testing (Spring Cloud Contract hoặc Pact). 12.2 Security scan: Maven + npm + container OS. 12.3 Phase 13: k6 load testing, FTS/index tuning, cache strategy, measure <2s/<3s targets. 12.4 Phase 14: CI/CD pipeline, Docker production config, HTTPS/trusted proxy, least-privilege DB roles, secret rotation script, backup/restore drill, monitoring, documentation.

## Definition of Done

Đúng phạm vi; build và test liên quan pass; migration chạy từ DB sạch và validate; API/UI chạy thật; error/validation/security phù hợp; không TODO chặn phase; tài liệu và status khớp kết quả. Không tính nghiệp vụ ở phase sau là đã xong. Không đánh đồng môi trường local với production.

## Coverage targets

- Backend: ≥80% line coverage (service layer ≥90%); enforce bằng JaCoCo Maven plugin.
- Frontend: ≥70% line coverage (business logic/API integration); enforce bằng Vitest v8 coverage.
- Đo từ Phase 2 trở đi; Phase 1 foundation đã có targeted tests.
