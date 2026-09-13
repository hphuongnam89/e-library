# E-LIB — Phase 0/1

Thư viện Đại học Phú Xuân. Hiện có nền tảng chạy được và trang trạng thái kết nối thật. Chức năng đăng nhập, sách, mượn trả và PDF chưa thuộc phase 1.

## Chạy toàn bộ bằng Docker

Cần Docker Engine/Compose đang hoạt động và Python 3. Không cần Java/Node trên host để chạy bản container.

```sh
python3 scripts/init-env.py
docker compose --profile app up -d --build --wait
```

Mở [E-LIB local](http://127.0.0.1:5173), [trạng thái](http://127.0.0.1:5173/system). Backend trực tiếp: `http://127.0.0.1:18080/api/system/health/readiness`.

`init-env.py` sinh mật khẩu riêng, chỉ thêm biến thiếu và giữ `.env` hiện có; không in secret. Có thể tham khảo `.env.example`. Không commit `.env`. Nếu tự dùng file mẫu, thay password mẫu trước khi chạy. Đổi mật khẩu PostgreSQL sau khi đã tạo volume cần cập nhật role trong DB; sửa env đơn thuần không đổi password dữ liệu cũ.

| Dịch vụ | Host port mặc định | Ghi chú |
|---|---|---|
| Frontend | 5173 | Nginx, SPA routing, `/api` proxy |
| Backend | 18080 | Spring Boot Java 21 |
| PostgreSQL | 55432 | PostgreSQL 17, volume riêng |
| Redis | 56379 | Password + AOF |

Cổng đều bind `127.0.0.1`. Đổi các biến port trong `.env` khi bị chiếm; URL kiểm tra phải đổi tương ứng. Compose dùng project `elib`, không tác động project Docker khác. Chỉ một frontend được dùng port 5173 tại một thời điểm.

## Phát triển bằng hot reload

Cần JDK **21**, Node **22.12+**, Python 3, Docker. Maven Wrapper tự tải Maven 3.9.15; không cần cài Maven toàn cục. macOS: `export JAVA_HOME="$(/usr/libexec/java_home -v 21)"` khi chạy wrapper/test trực tiếp.

```sh
python3 scripts/init-env.py
docker compose --profile app stop frontend backend
docker compose up -d --wait db redis
python3 scripts/dev-backend.py
```

Ở terminal khác:

```sh
cd frontend
npm ci
npm run dev
```

Vite proxy mặc định tới `127.0.0.1:18080`. Nếu đổi BACKEND_PORT, đặt `API_PROXY_TARGET` theo `frontend/.env.example`. Chỉ biến `VITE_*` mới đi vào bundle; không đưa secret vào biến frontend.

## Kiểm tra

```sh
# JDK 21 + Docker đang chạy; Testcontainers dùng DB/Redis riêng, không dùng DB local.
cd backend
./mvnw verify

cd ../frontend
npm ci
npm run lint
npm run build
npm test
npm audit --audit-level=moderate
npx playwright install chromium
# Cần stack đầy đủ đang chạy tại port 5173.
npm run test:e2e

cd ..
python3 scripts/smoke.py
# Chủ động tạm dừng DB/Redis của project elib, kiểm tra lỗi và phục hồi trong finally.
python3 scripts/smoke.py --check-outages
```

E2E dùng `E2E_BASE_URL` nếu đổi port. `smoke.py` nhận `--base-url`. Build không thay thế kiểm tra API/UI. Npm audit chỉ kiểm tra npm, không chứng minh Maven/container image không có lỗ hổng.

## Dữ liệu, migrations và dừng

- Runtime phase 1 chỉ có namespace `elib` + Flyway history. Không seed tài khoản, dữ liệu nghiệp vụ hoặc policy giả.
- `docs/schema.sql` là schema đích phase 0, được test riêng; không chạy file này thủ công lên DB ứng dụng.
- Không sửa migration đã áp dụng. Thêm `V2__...sql` khi phase tương ứng bắt đầu; JPA chỉ validate.
- `docker compose --profile app down` dừng/xóa container, giữ volume. **Không dùng `down -v`** nếu cần giữ dữ liệu.
- Volume PostgreSQL major 17 tách khỏi volume `postgres:latest` cũ. Chưa migrate dữ liệu cũ; tuyệt đối không gắn volume khác major để thử chạy.

## Tài liệu

[Tiến độ có bằng chứng](docs/development-status.md) · [Yêu cầu](docs/requirements.md) · [Kiến trúc](docs/architecture.md) · [Database](docs/database-design.md) · [API](docs/api-design.md) · [Kế hoạch](docs/development-plan.md) · [Quyết định](docs/decisions.md) · [Kiểm thử](docs/testing-strategy.md) · [Bảo mật](docs/security-checklist.md).
