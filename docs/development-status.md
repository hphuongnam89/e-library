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

## Status: Waiting to start Phase 3 — ORGANIZATION & DEPARTMENTS

### Next: Phase 3 — ORGANIZATION & DEPARTMENTS
- Campus, Library, Department hierarchy
- Migration V3 adding `department_id` FK to `app_user` (per D017)
- Department admin endpoints
- User-to-department assignment
- Institution scoping & tests
