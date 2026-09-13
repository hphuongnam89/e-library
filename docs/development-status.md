# E-LIB — Development Status

Mỗi session/agent bắt đầu với file này. Cập nhật sau mỗi task hoàn thành.

## Phase 0 — ANALYSIS (Complete) ✅
- [x] Đọc requirements E-LIB đầy đủ
- [x] Phân tích repository: hiện tại rỗng
- [x] Xác định module và dependency
- [x] Thiết kế ERD và API ở mức đủ
- [x] Xác định rủi ro
- [x] Tạo /docs/architecture.md
- [x] Tạo /docs/database-design.md
- [x] Tạo /docs/api-design.md
- [x] Tạo /docs/development-plan.md
- [x] Tạo /docs/development-status.md (file này)
- [x] Tạo /docs/decisions.md
- [x] Report findings

## Phase 1 — FOUNDATION (Complete) ✅

### 1.1 Maven/Java 21 skeleton + dependency versions
- [x] Spring Boot 3.5.16, Java 21, Maven wrapper
- [x] Dependencies: web, JPA, validation, security, actuator, data-redis, flyway, postgresql, testcontainers
- [x] Multi-stage Dockerfile (eclipse-temurin-21, non-root user `elib`)

### 1.2 React/TS/Tailwind/router + lint/tests
- [x] React 18.3.1, TypeScript 5.9.3, Vite 6.4.3, Tailwind 3.4.17
- [x] React Router 7.18.3 — routes: `/` (Home), `/system` (SystemStatus), `*` (NotFound 404)
- [x] ESLint flat config, strict TypeScript
- [x] Vitest 4.1.11 + Testing Library — 8 unit tests
- [x] Playwright 1.63 E2E — desktop/mobile, deep-link, 404, outage mock/retry, API passthrough
- [x] ErrorBoundary, skip-link accessibility, aria-live status
- [x] Multi-stage Dockerfile (node:22-alpine build → nginxinc/nginx-unprivileged:1.28-alpine)

### 1.3 PostgreSQL/Redis/env/volumes
- [x] Docker Compose: db (postgres:17-alpine), redis (redis:7.4-alpine), backend, frontend
- [x] Profile `app` cho backend/frontend; db/redis chạy mặc định
- [x] Volumes riêng biệt: postgres17-data, redis-data (AOF)
- [x] Port chỉ bind 127.0.0.1
- [x] Health checks cho tất cả services
- [x] `scripts/init-env.py` — sinh password an toàn (secrets.token_urlsafe), chmod 600

### 1.4 Flyway/schema validation
- [x] V1__foundation.sql — tạo schema `elib` với comment
- [x] `hibernate.ddl-auto=validate` — Hibernate chỉ validate, không sửa schema
- [x] `flyway.clean-disabled=true` — cấm xóa schema
- [x] `flyway.create-schemas=true`

### 1.5 Security deny-default, error response/request log, probes
- [x] SecurityConfig: denyAll(), chỉ GET health public, không form/basic/default admin
- [x] CSRF giữ bật; CORS không mở wildcard; session STATELESS
- [x] Custom 401/403 responses qua ProblemResponses (RFC 7807 application/problem+json)
- [x] GlobalExceptionHandler: 400 (validation/malformed), 405 (+Allow header), 500 (che stacktrace)
- [x] RequestIdFilter: UUID per request, MDC correlation, access log (method/status/duration), không log sensitive data
- [x] Actuator probes tại /api/system/health: liveness (process), readiness (db + redis); ẩn details/components
- [x] Graceful shutdown 20s; HikariCP pool 10; server.error suppressed
- [x] Nginx security headers: CSP, X-Frame-Options DENY, X-Content-Type-Options nosniff, Referrer-Policy

### 1.6 Docker build + proxy/deep-link
- [x] Frontend Nginx proxy: /api/ → backend:8080
- [x] SPA routing: try_files fallback /index.html
- [x] Vite dev proxy: /api → 127.0.0.1:18080
- [x] Asset caching (1y cho /assets/)
- [x] `scripts/dev-backend.py` — hot reload trên host, auto JAVA_HOME trên macOS

### 1.7 Test, README/status, Git checkpoint
- [x] FoundationIntegrationTest (6 tests): health probes, deny-default 401 + request ID, CSRF 403, CORS blocked, Flyway validate, schema design verification
- [x] GlobalExceptionHandlerTest (4 tests): validation 400, malformed JSON 400, method 405 + Allow, exception 500 che details
- [x] SystemStatus.test.tsx (8 tests): loading, UP, HTTP error, bad payload, null, network error/retry, timeout, unmount cancel
- [x] E2E foundation.spec.ts (4 tests × 2 devices): navigation + deep-link, outage/retry, 404, API passthrough + RFC 7807
- [x] smoke.py: readiness, SPA deep-link, deny-default 401, CSRF 403
- [x] smoke.py --check-outages: pause DB/Redis → 503, liveness 200, recovery
- [x] README.md: hướng dẫn Docker, hot reload, kiểm tra, data/migrations

---

## Phase 2 — AUTH (Complete) ✅

### 2.1 Database migration & Entity
- [x] Flyway migration `V2__auth.sql`: tạo bảng `app_user` (id, google_subject UNIQUE, email, full_name, student_code UNIQUE, role CHECK, status CHECK, created_at). Chưa có `department_id` theo quyết định D017.
- [x] Index `uq_user_email` UNIQUE trên `lower(email)`.
- [x] JPA Entity `AppUser`, enums `UserRole` (STUDENT, LECTURER, LIBRARIAN, ADMIN), `UserStatus` (ACTIVE, INACTIVE).
- [x] Spring Data JPA `AppUserRepository` với `findByGoogleSubject`, `findByEmailIgnoreCase`.

### 2.2 Dependencies & Configuration
- [x] Thêm `spring-boot-starter-oauth2-client` và `spring-session-data-redis` vào `backend/pom.xml`.
- [x] Cấu hình Spring Session với Redis (`store-type: redis`, namespace `elib:session`, timeout 30m).
- [x] Cấu hình CookieSerializer: tên cookie `ELIB_SESSION`, HttpOnly=true, SameSite=Lax, Secure khi production.
- [x] Cấu hình Google OAuth2 client registration và domain allowlist qua `elib.auth.allowed-domains`.

### 2.3 Spring Security OIDC & Session
- [x] Chuyển `SecurityConfig` từ `STATELESS` sang `SessionCreationPolicy.IF_REQUIRED`.
- [x] `CustomOidcUserService`: load Google OIDC user, kiểm tra `email_verified`, kiểm tra `DomainValidator`, tự động provision user mới với role `STUDENT` hoặc cập nhật profile, kiểm tra trạng thái tài khoản `ACTIVE`.
- [x] `CustomOidcUser`: Serializable OIDC principal chứa id, email, fullName, studentCode, role, status và Spring Security authorities (`ROLE_STUDENT`, `ROLE_ADMIN`, ...).
- [x] `DomainValidator`: kiểm tra domain allowlist đối với email trường (hỗ trợ nhiều domain phân tách bằng dấu phẩy).

### 2.4 SPA CSRF Protection
- [x] `CookieCsrfTokenRepository.withHttpOnlyFalse()` phát hành cookie `XSRF-TOKEN` cho frontend.
- [x] `SpaCsrfTokenRequestHandler`: xử lý cả header `X-XSRF-TOKEN` (raw) và form token (XOR mask) chống BREACH attack.
- [x] `CsrfCookieFilter`: đặt sau `CsrfFilter` để force sinh deferred token cookie cho SPA requests.
- [x] Endpoint `GET /api/v1/auth/csrf`: trả về csrf token payload và kích hoạt phát hành cookie.

### 2.5 Auth API & Logout
- [x] `GET /api/v1/auth/me`: trả về `UserDto` (id, email, fullName, studentCode, role, status) của người dùng hiện tại; 401 nếu chưa đăng nhập.
- [x] `POST /api/v1/auth/logout`: yêu cầu CSRF token, invalidate session trên Redis, xóa cookies `ELIB_SESSION` và `XSRF-TOKEN`, trả về HTTP 204 No Content.
- [x] RBAC enforcement: `/api/v1/admin/**` yêu cầu role `ADMIN`; `/api/v1/librarian/**` yêu cầu `LIBRARIAN` hoặc `ADMIN`.

### 2.6 Frontend Auth UI
- [x] Hook `useAuth`: quản lý auth state (`loading`, `authenticated`, `unauthenticated`), hàm `login()` chuyển hướng tới `/oauth2/authorization/google`, hàm `logout()` gửi `POST /api/v1/auth/logout` kèm CSRF token.
- [x] Component `UserMenu`: hiển thị tên người dùng, huy hiệu vai trò tiếng Việt (Sinh viên, Giảng viên, Thủ thư, Quản trị viên) và nút đăng xuất.
- [x] Tích hợp `UserMenu` và nút "Đăng nhập Google" vào header điều hướng trong `App.tsx`.
- [x] Unit tests: `UserMenu.test.tsx` (3 tests), `useAuth.test.ts` (3 tests).

### 2.7 Infrastructure & Scripts
- [x] `.env.example` và `scripts/init-env.py`: thêm `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `ALLOWED_DOMAINS`, `SESSION_COOKIE_SECURE`.
- [x] `scripts/dev-backend.py`: thêm các keys mới vào environment loader.
- [x] `docker-compose.yml`: truyền các biến OAuth2/OIDC vào backend service container.
- [x] `frontend/nginx.conf` và `frontend/vite.config.ts`: thêm reverse proxy cho `/oauth2/` và `/login/oauth2/`.
- [x] `scripts/smoke.py`: bổ sung kiểm tra live readiness cho `/api/v1/auth/csrf` (200) và `/api/v1/auth/me` (401).

### Evidence — Gate verification 2026-09-13

```
Backend:  ./mvnw verify → Tests run: 29, Failures: 0, Errors: 0 — BUILD SUCCESS (17.8s)
          - AuthIntegrationTest: 11 tests (unauthenticated 401, CSRF cookie & header, logout 204,
            OIDC login profile, student forbidden from admin, admin allowed, librarian allowed/forbidden,
            unique constraints, case-insensitive email index)
          - CustomOidcUserServiceTest: 5 tests (unverified email, disallowed domain, inactive user,
            auto-provision, update full name)
          - DomainValidatorTest: 3 tests (empty allowlist, configured domains, invalid email format)
          - FoundationIntegrationTest: 6 tests (health probes, deny-default, CSRF reject, CORS,
            Flyway validate, schema design verification)
          - GlobalExceptionHandlerTest: 4 tests (validation, malformed json, 405, 500 sanitized)

Frontend: npm run lint   → 0 warnings, exit 0
          npm run build  → tsc --noEmit && vite build — 43 modules, 767ms, exit 0
          npm test       → 3 files, 14 tests passed (752ms)
          npm audit      → found 0 vulnerabilities
```

---

## Phase 3 — ORGANIZATION & DEPARTMENTS (Complete) ✅

### 3.1 Database Migration & Schema
- [x] `V3__organization.sql`:
  - Tạo bảng `institution` (`id`, `name`, `created_at`).
  - Tạo bảng `campus` (`id`, `institution_id` FK, `name`).
  - Tạo bảng `library` (`id`, `campus_id` FK, `name`, `address`).
  - Tạo bảng `department` (`id`, `library_id` FK, `name`).
  - Bổ sung cột `department_id BIGINT REFERENCES department(id)` vào bảng `app_user` (theo Decision D017).
  - Tạo foreign key indexes: `idx_campus_institution`, `idx_library_campus`, `idx_department_library`, `idx_user_department`.

### 3.2 JPA Entities & Repositories
- [x] Entity `Institution`: quản lý cấp cơ quan/trường học.
- [x] Entity `Campus`: thuộc về `Institution` (`@ManyToOne(fetch = FetchType.LAZY)`).
- [x] Entity `Library`: thuộc về `Campus` (`@ManyToOne(fetch = FetchType.LAZY)`).
- [x] Entity `Department`: thuộc về `Library` (`@ManyToOne(fetch = FetchType.LAZY)`).
- [x] Cập nhật `AppUser`: quan hệ `@ManyToOne(fetch = FetchType.LAZY)` với `Department` qua `department_id`.
- [x] Repositories:
  - `InstitutionRepository`: `existsByNameIgnoreCase`, `existsByNameIgnoreCaseAndIdNot`.
  - `CampusRepository`: `findByInstitutionId`, `existsByInstitutionId`.
  - `LibraryRepository`: `findByCampusId`, `existsByCampusId`.
  - `DepartmentRepository`: `findByLibraryId`, `existsByLibraryId`.
  - `AppUserRepository`: bổ sung `existsByDepartmentId(Long departmentId)`.

### 3.3 DTO Records & Service Layer
- [x] DTOs phân tầng:
  - `InstitutionDto`, `CreateInstitutionRequest`, `UpdateInstitutionRequest`.
  - `CampusDto`, `CreateCampusRequest`, `UpdateCampusRequest`.
  - `LibraryDto`, `CreateLibraryRequest`, `UpdateLibraryRequest`.
  - `DepartmentDto`, `CreateDepartmentRequest`, `UpdateDepartmentRequest`.
  - `AssignDepartmentRequest`.
  - Cập nhật `UserDto`: bổ sung `Long departmentId` (tự động phản ánh trong `/api/v1/auth/me`).
- [x] `OrganizationService`:
  - Đầy đủ CRUD cho cả 4 cấp tổ chức hỗ trợ phân trang (`Pageable`) và lọc theo cấp cha.
  - Validation ràng buộc cha: trả về `HTTP 400 Bad Request` nếu ID cấp cha không tồn tại.
  - Bảo toàn toàn vẹn dữ liệu (Referential Integrity): xóa đối tượng cha khi còn đối tượng con hoặc còn người dùng được gán sẽ trả về `HTTP 409 Conflict`.
  - Gán/hủy phòng ban cho người dùng: `assignDepartment(userId, departmentId)`.

### 3.4 REST Controllers & Security RBAC
- [x] `InstitutionController`: `/api/v1/institutions`.
- [x] `CampusController`: `/api/v1/campuses`.
- [x] `LibraryController`: `/api/v1/libraries`.
- [x] `DepartmentController`: `/api/v1/departments`.
- [x] `AdminUserController`: `PATCH /api/v1/admin/users/{id}/department`.
- [x] Phân quyền trong `SecurityConfig`:
  - Đọc (`GET` trên các cấp tổ chức): người dùng đã xác thực (`authenticated()`).
  - Ghi (`POST`, `PATCH`, `DELETE` trên các cấp tổ chức): yêu cầu quyền `hasRole('ADMIN')`.
  - Gán người dùng vào phòng ban: thuộc prefix `/api/v1/admin/**` được bảo vệ bởi `hasRole('ADMIN')`.
  - Người dùng không có quyền hoặc chưa đăng nhập bị chặn với HTTP 401 / HTTP 403 chuẩn RFC 9457 `application/problem+json`.

### Evidence — Gate verification 2026-09-13

```
Backend:  ./mvnw verify → Tests run: 35, Failures: 0, Errors: 0 — BUILD SUCCESS (15.6s)
          - OrganizationIntegrationTest: 6 tests (unauthenticated 401, non-admin write forbidden 403,
            full hierarchy CRUD lifecycle & relationship mapping, parent existence validation 400,
            referential integrity 409 conflict on delete when children/users exist, user department
            assignment & /api/v1/auth/me reflection & reverse cleanup)
          - AuthIntegrationTest: 11 tests (unauthenticated 401, CSRF cookie & header, logout 204,
            OIDC login profile, student forbidden from admin, admin allowed, librarian allowed/forbidden,
            unique constraints, case-insensitive email index)
          - CustomOidcUserServiceTest: 5 tests (unverified email, disallowed domain, inactive user,
            auto-provision, update full name)
          - DomainValidatorTest: 3 tests (empty allowlist, configured domains, invalid email format)
          - FoundationIntegrationTest: 6 tests (health probes, deny-default, CSRF reject, CORS,
            Flyway validate, schema design verification)
          - GlobalExceptionHandlerTest: 4 tests (validation, malformed json, 405, 500 sanitized)

Frontend: npm run lint   → 0 warnings, exit 0
          npm run build  → tsc --noEmit && vite build — 43 modules, 724ms, exit 0
          npm test       → 3 files, 14 tests passed (685ms)
          npm audit      → found 0 vulnerabilities

Smoke:    scripts/smoke.py → live readiness, deep link, unauthorized JSON, CSRF, Phase 2 auth, and Phase 3 organization endpoints
```

---

## Phase 4 — CATALOG & METADATA (Complete) ✅

### 4.1 Database Migration & PostgreSQL 17 FTS
- [x] Flyway migration `V4__catalog.sql`:
  - Tạo bảng phân cấp `category` (hỗ trợ danh mục đa cấp, `UNIQUE NULLS NOT DISTINCT (parent_id, name)`).
  - Tạo sequence nguyên tử `book_barcode_sequence START WITH 100001 INCREMENT BY 1` để cấp phát barcode không xung đột.
  - Tạo bảng `book_title` với cột lưu trữ `search_vector tsvector GENERATED ALWAYS AS (to_tsvector('simple', ...)) STORED` và chỉ mục GIN `idx_book_title_search`.
  - Tạo bảng bản sao sách vật lý `book_copy` với optimistic locking (`version BIGINT`), mã vạch độc nhất `barcode VARCHAR(100) UNIQUE`, trạng thái sách `status CHECK (status IN ('AVAILABLE','BORROWED','LOST','DAMAGED','MAINTENANCE'))`.
  - Các chỉ mục ngoại khóa và truy vấn: `idx_category_parent`, `idx_book_title_category`, `idx_copy_title`, `idx_copy_library_status`.

### 4.2 JPA Entities & Repositories
- [x] Enums & Entities:
  - `BookCopyStatus`: `AVAILABLE`, `BORROWED`, `LOST`, `DAMAGED`, `MAINTENANCE`.
  - `Category`: quan hệ cha con tự tham chiếu (`@ManyToOne(fetch = FetchType.LAZY)`).
  - `BookTitle`: thông tin nhan đề sách, tác giả, nhà xuất bản, ISBN, năm xuất bản, danh mục (`@ManyToOne(fetch = FetchType.LAZY)`).
  - `BookCopy`: bản sao vật lý gắn với thư viện cụ thể (`Library`) và nhan đề (`BookTitle`), tích hợp `@Version` chống race condition khi mượn/trả.
- [x] Repositories:
  - `CategoryRepository`: duyệt cây danh mục, kiểm tra trùng tên cùng cấp cha (`existsByNameIgnoreCaseAndParentId`, `existsByNameIgnoreCaseAndParentIsNull`).
  - `BookTitleRepository`: tìm kiếm toàn văn PostgreSQL native query `searchFts` kết hợp lọc theo `categoryId` trên schema `elib.book_title`.
  - `BookCopyRepository`: truy vấn bản sao theo nhan đề, thư viện, trạng thái, mã vạch và cấp phát barcode tuần tự qua `getNextBarcodeSequence()`.

### 4.3 DTO Records & Service Layer
- [x] DTOs chuẩn hóa:
  - `CategoryDto`, `CreateCategoryRequest`, `UpdateCategoryRequest`.
  - `BookTitleDto`, `CreateBookTitleRequest`, `UpdateBookTitleRequest`.
  - `BookCopyDto`, `CreateBookCopyRequest`, `UpdateBookCopyRequest`.
- [x] `CatalogService`:
  - Phân trang chuẩn Spring Data `Pageable` cho toàn bộ danh mục, nhan đề, bản sao.
  - Thuật toán phát hiện và ngăn chặn vòng lặp danh mục (Category Hierarchy Cycle Detection): kiểm tra phả hệ tổ tiên, cấm một danh mục trở thành cha của chính nó hoặc là con của hậu duệ nó (trả về HTTP 400 Bad Request).
  - Tự động sinh mã barcode `PXU-BC-<sequence>` từ PostgreSQL sequence khi không nhập mã thủ công, đảm bảo tính duy nhất tuyệt đối.
  - Hỗ trợ endpoint tra cứu nhanh bản sao theo mã vạch `/api/v1/book-copies/barcode/{barcode}` cho máy quét mã vạch USB HID.
  - Kiểm soát toàn vẹn dữ liệu (Referential Integrity): cấm xóa danh mục còn danh mục con hoặc còn sách (409), cấm xóa nhan đề còn bản sao (409), cấm xóa bản sao khi đang ở trạng thái `BORROWED` (409).

### 4.4 REST Controllers & Security RBAC
- [x] `CategoryController`: `/api/v1/categories` (hỗ trợ `/roots`, `/{id}/children`, CRUD).
- [x] `BookTitleController`: `/api/v1/book-titles` (hỗ trợ tìm kiếm FTS với `query`, lọc theo `categoryId`, CRUD).
- [x] `BookCopyController`: `/api/v1/book-copies` (hỗ trợ lọc theo `bookTitleId`, `libraryId`, `status`, tra cứu barcode `/barcode/{barcode}`, CRUD).
- [x] Phân quyền Spring Security trong `SecurityConfig`:
  - Đọc catalog (`GET /api/v1/categories/**`, `/api/v1/book-titles/**`, `/api/v1/book-copies/**`): người dùng đã xác thực (`authenticated()`).
  - Quản lý catalog (`POST`, `PATCH`, `DELETE`): yêu cầu quyền Thủ thư hoặc Quản trị viên (`hasAnyRole("LIBRARIAN", "ADMIN")`).
  - Sinh viên/người dùng thông thường cố gắng thao tác ghi bị chặn với HTTP 403 Forbidden RFC 9457 `application/problem+json`.

### Evidence — Gate verification 2026-09-13

```
Backend:  ./mvnw verify → Tests run: 41, Failures: 0, Errors: 0 — BUILD SUCCESS (19.5s)
          - CatalogIntegrationTest: 6 tests (unauthenticated 401, student forbidden 403,
            category hierarchy & cycle detection 400 & duplicate prevention 409,
            book title CRUD & PostgreSQL 17 FTS search by title/author/publisher,
            book copy sequence generation & USB HID barcode scan lookup,
            referential integrity 409 conflict rules on borrowed copies/titles/categories)
          - OrganizationIntegrationTest: 6 tests
          - AuthIntegrationTest: 11 tests
          - CustomOidcUserServiceTest: 5 tests
          - DomainValidatorTest: 3 tests
          - FoundationIntegrationTest: 6 tests
          - GlobalExceptionHandlerTest: 4 tests

Frontend: npm run lint   → 0 warnings, exit 0
          npm run build  → tsc --noEmit && vite build — 43 modules, 774ms, exit 0
          npm test       → 3 files, 14 tests passed (847ms)
          npm audit      → found 0 vulnerabilities

Smoke:    scripts/smoke.py → live readiness, deep link, unauthorized JSON, CSRF, Phase 2 auth, Phase 3 org, and Phase 4 catalog endpoints
```

---

## Phase 5 — CIRCULATION & LOANS (Complete) ✅

### 5.1 Database Migration & Concurrency Constraints
- [x] Flyway migration `V5__circulation.sql`:
  - Tạo bảng `borrowing_policy` (chính sách mượn sách có hiệu lực theo thời điểm `effective_from`, ràng buộc `UNIQUE (library_id, effective_from)`).
  - Tạo bảng `borrow` (ghi nhận mượn/trả, `due_at`, `returned_at`, snapshot `daily_fine`, `fine_amount`, `fine_paid_at`, `status`).
  - Partial unique index `uq_active_borrow_copy ON borrow(book_copy_id) WHERE returned_at IS NULL` chống mượn đè 1 cuốn sách đồng thời ở mức DB.
  - Các chỉ mục ngoại khóa và truy vấn: `idx_borrow_user`, `idx_borrow_due`, `idx_borrow_policy`, `idx_borrow_status`.

### 5.2 JPA Entities & Repositories
- [x] Enums & Entities:
  - `BorrowStatus`: `BORROWED`, `RETURNED`.
  - `BorrowingPolicy`: chính sách mượn sách của thư viện.
  - `Borrow`: khoản mượn liên kết `AppUser`, `BookCopy`, `BorrowingPolicy`.
- [x] Repositories:
  - `BorrowingPolicyRepository`: tìm chính sách có hiệu lực gần nhất `findFirstByLibraryIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc`, phân trang theo thư viện.
  - `BorrowRepository`: đếm số sách đang mượn `countByUserIdAndStatus`, kiểm tra nợ phạt chưa trả `existsByUserIdAndFineAmountGreaterThanAndFinePaidAtIsNull`, lọc đa tiêu chí `findBorrowsWithFilters`.
  - Cập nhật `BookCopyRepository`: bổ sung `findByBarcodeForUpdate` với khóa bi quan `LockModeType.PESSIMISTIC_WRITE`.
  - Cập nhật `AppUserRepository`: bổ sung `findByStudentCode`.

### 5.3 DTO Records & Service Layer
- [x] DTOs chuẩn hóa:
  - `BorrowingPolicyDto`, `CreateBorrowingPolicyRequest`.
  - `BorrowDto`, `CheckoutRequest`, `BatchCheckoutResponse`.
- [x] `CirculationService`:
  - **Mượn sách theo lô (Batch Checkout)**: Quét danh sách mã vạch USB HID, kiểm tra tài khoản `ACTIVE`, kiểm tra nợ phạt chưa thanh toán (chặn mượn nếu nợ phạt), khóa dòng bi quan `PESSIMISTIC_WRITE` trên từng `BookCopy`, kiểm tra trạng thái `AVAILABLE` (trả về 409 Conflict nếu đã mượn hoặc hư hỏng), kiểm tra hạn mức `maxActiveLoans` theo chính sách hiệu lực, cập nhật `BookCopy.status = BORROWED`, lưu `Borrow` với snapshot `daily_fine`.
  - **Trả sách (Idempotent Return)**: Ghi nhận `returnedAt`, cập nhật trạng thái `BookCopy.status = AVAILABLE`, tự động tính tiền phạt trễ hạn theo số ngày trễ $\times$ `daily_fine`, xử lý idempotent khi gọi lại.
  - **Xác nhận nộp phạt (Idempotent Fine Payment)**: Xác nhận thu phạt, ghi nhận `finePaidAt = now()`.
  - **Quản lý chính sách & Lịch sử**: Quản lý chính sách mượn theo phiên bản, tra cứu lịch sử mượn cá nhân `/me/borrows` và lọc danh sách mượn cho thủ thư.

### 5.4 REST Controllers & Security RBAC
- [x] `CirculationController`:
  - `POST /api/v1/borrows`: Check-out theo lô (`hasAnyRole("LIBRARIAN", "ADMIN")`).
  - `GET /api/v1/borrows`: Danh sách mượn lọc theo user, status, overdue, date range (`hasAnyRole("LIBRARIAN", "ADMIN")`).
  - `GET /api/v1/borrows/{id}`: Chi tiết khoản mượn (`hasAnyRole("LIBRARIAN", "ADMIN")`).
  - `POST /api/v1/borrows/{id}/return`: Trả sách (`hasAnyRole("LIBRARIAN", "ADMIN")`).
  - `POST /api/v1/borrows/{id}/fine-payment`: Nộp tiền phạt (`hasAnyRole("LIBRARIAN", "ADMIN")`).
  - `GET /api/v1/me/borrows`: Lịch sử mượn cá nhân (`authenticated()`).
- [x] `BorrowingPolicyController`:
  - `GET /api/v1/libraries/{libraryId}/borrowing-policies`: Xem chính sách (`authenticated()`).
  - `POST /api/v1/libraries/{libraryId}/borrowing-policies`: Tạo chính sách (`hasRole("ADMIN")`).
- [x] Cập nhật `SecurityConfig.java`: Phân quyền RBAC chặt chẽ, sinh viên không thể tự check-out hoặc xác nhận trả/thu tiền phạt (HTTP 403).

### Evidence — Gate verification 2026-09-13

```
Backend:  ./mvnw verify → Tests run: 47, Failures: 0, Errors: 0 — BUILD SUCCESS (24.2s)
          - CirculationIntegrationTest: 6 tests (unauthenticated 401, student forbidden from librarian
            actions 403, full checkout & return happy path with 0 fine & idempotent return, overdue fine
            calculation & payment lifecycle & unpaid fine borrowing block, policy max active loans limit
            enforcement & unavailable copy 409 conflict, database partial unique index enforcement
            uq_active_borrow_copy against simultaneous active loans)
          - CatalogIntegrationTest: 6 tests
          - OrganizationIntegrationTest: 6 tests
          - AuthIntegrationTest: 11 tests
          - CustomOidcUserServiceTest: 5 tests
          - DomainValidatorTest: 3 tests
          - FoundationIntegrationTest: 6 tests
          - GlobalExceptionHandlerTest: 4 tests

Frontend: npm run lint   → 0 warnings, exit 0
          npm run build  → tsc --noEmit && vite build — 43 modules, 858ms, exit 0
          npm test       → 3 files, 14 tests passed (914ms)
          npm audit      → found 0 vulnerabilities

Smoke:    scripts/smoke.py → live readiness, deep link, unauthorized JSON, CSRF, Phase 2 auth, Phase 3 org, Phase 4 catalog, and Phase 5 circulation endpoints
```

---

## Status: Waiting to start Phase 6 — DIGITAL ASSETS & STORAGE

### Next: Phase 6 — DIGITAL ASSETS & STORAGE
- `digital_document`, `document_grant`, `document_version` migrations (V6)
- File storage service (local filesystem abstraction, anti path traversal, SHA-256 integrity)
- PDF upload validation (50MB max, `application/pdf` MIME magic bytes check)
- Document lifecycle state machine (`DRAFT` → `PENDING` → `APPROVED` → `PUBLISHED`)
- Secure HTTP Range streaming (`/api/v1/digital-documents/{id}/stream` with HTTP 206 Partial Content, no direct file URL exposed)
- Department & role-based document access grants

