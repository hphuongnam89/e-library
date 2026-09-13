# Security gates

## Nền tảng phase 1

- Deny-default; chỉ GET health public; không Basic/form/default admin; CSRF giữ bật.
- Không CORS wildcard; frontend/API cùng origin; port Docker chỉ loopback.
- Health không lộ components/host/secret; errors dùng ProblemDetail với request ID; không stacktrace phía client.
- Credentials từ env, `.env` bị ignore, password local sinh riêng; không VITE secret.
- Java container chạy non-root; frontend Nginx unprivileged, CSP/nosniff/frame policy.
- Lockfile npm + bản vá; Flyway validate/clean disabled; không thay đổi volume cũ.
- Các kiểm tra runtime, dependency và giới hạn được ghi tại development-status.

## Rate limiting strategy

Hai lớp defense-in-depth:
1. **Nginx** `limit_req_zone`: giới hạn request rate cho upload endpoints và static assets. Cấu hình ở Phase 6–7.
2. **Application** `bucket4j-spring-boot-starter` + Redis backend: giới hạn per-user rate cho streaming, API business endpoints. Cấu hình ở Phase 7, enforce từ Phase 12.

Rate limit defaults (cấu hình, có thể điều chỉnh):
- Streaming: 60 requests/phút per user.
- Upload: 10 requests/phút per user.
- General API: 300 requests/phút per user.
- Trả HTTP 429 Too Many Requests với `Retry-After` header.

## Secret rotation

- `scripts/init-env.py` sinh password ban đầu an toàn (`secrets.token_urlsafe`).
- Đổi POSTGRES_PASSWORD trong .env **không** tự đổi password trong DB. Quy trình rotation:
  1. `ALTER ROLE elib WITH PASSWORD 'new-password';` trong psql.
  2. Cập nhật `POSTGRES_PASSWORD` trong `.env`.
  3. Restart backend container.
- Phase 14: tạo `scripts/rotate-secrets.py` tự động hóa quy trình trên cho cả PostgreSQL và Redis.
- Redis: `CONFIG SET requirepass "new-password"` + update `.env` + restart consumers.

## Gate trước các phase liên quan / production

- Phase 2: Google issuer/audience/state/nonce, verified email/domain, role provisioning; cookie HttpOnly/Secure/SameSite, CSRF/session invalidation và giới hạn đăng nhập.
- Phase 3–5: scope tổ chức, IDOR, transaction/locks, unique constraints, policy và audit.
- Phase 6–8: private bucket, quyền trên mọi stream/range, upload bytes/type/size (mặc định 50MB, chỉ PDF), rate limit (bucket4j + Nginx), watermark, session ownership, bounded tracking; không xem UI chặn save là DRM.
- Phase 9–11: email retry limit/dedup, redacted logs, report scope, XLSX formula injection; không sửa audit qua API.
- Phase 12–14: scan Maven + container OS, HTTPS/trusted proxy/cookie production, least-privilege DB roles, network isolation, secret rotation script, backup/restore và retention/RPO/RTO được xác nhận.

Chạy local thành công và npm audit sạch không đồng nghĩa đủ bảo mật production. Không claim các gate phase sau đã xong.
