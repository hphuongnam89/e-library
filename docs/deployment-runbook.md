# E-LIB — Sổ tay Vận hành & Triển khai Production (Deployment Runbook)

Tài liệu hướng dẫn toàn diện từ A-Z dành cho Kỹ sư DevOps / Quản trị viên hệ thống để cài đặt, bảo mật, vận hành và xử lý sự cố nền tảng E-LIB trên môi trường Production.

---

## 1. Yêu cầu Hạ tầng & Môi trường (Prerequisites)

### Cấu hình Phần cứng Tối thiểu (Minimum Specs)
- **CPU**: 4 vCPU (khuyến nghị x86_64 hoặc ARM64).
- **RAM**: 8 GB RAM (Tối thiểu: 4 GB cho DB/Redis/JVM).
- **Ổ đĩa**: 50 GB SSD (NVMe khuyến nghị cho PostgreSQL 17 và PDF asset storage).
- **Hệ điều hành**: Ubuntu 22.04 LTS / 24.04 LTS, Debian 12, hoặc RHEL 9.

### Phần mềm Bắt buộc
- **Docker Engine**: Version ≥ 24.0.
- **Docker Compose**: Version ≥ 2.20 (sử dụng lệnh `docker compose`).
- **Python**: Version ≥ 3.10 (phục vụ các script bảo trì và sao lưu).
- **OpenSSL**: Version ≥ 3.0.

---

## 2. Quy trình Triển khai Lần đầu (Cold Start Procedure)

### Bước 1: Sao chép Mã nguồn & Phân quyền
```bash
git clone https://github.com/phuxuan-university/e-library.git /opt/elib
cd /opt/elib
```

### Bước 2: Khởi tạo Tệp tin Môi trường An toàn
Chạy script sinh mật khẩu mật mã học (cryptographically strong tokens):
```bash
python3 scripts/init-env.py
```
Tệp `.env` sẽ được tạo với phân quyền `0600` (chỉ người dùng hiện tại có quyền đọc/ghi).

Mở tệp `.env` và điền các thông tin của đơn vị trường:
```ini
# Google Workspace OIDC Client
GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-client-secret
ALLOWED_DOMAINS=pxu.edu.vn,phuxuan.edu.vn

# Security Configuration
SESSION_COOKIE_SECURE=true
BACKEND_PORT=18080
FRONTEND_PORT=8080
```

### Bước 3: Cấu hình Chứng chỉ SSL/TLS
1. **Sử dụng Certbot / Let's Encrypt**:
   ```bash
   certbot certonly --standalone -d elib.pxu.edu.vn --agree-tos -m admin@pxu.edu.vn
   cp /etc/letsencrypt/live/elib.pxu.edu.vn/fullchain.pem nginx/ssl/elib.crt
   cp /etc/letsencrypt/live/elib.pxu.edu.vn/privkey.pem nginx/ssl/elib.key
   chmod 600 nginx/ssl/elib.key
   ```
2. **Hoặc sử dụng Chứng chỉ nội bộ / Cloudflare Origin Cert**:
   Lưu chứng chỉ vào `nginx/ssl/elib.crt` và khóa bí mật vào `nginx/ssl/elib.key`.

### Bước 4: Khởi động Cơ sở Dữ liệu & Thiết lập Phân quyền Tối thiểu
```bash
# 1. Khởi động DB và Redis trước
docker compose -f docker-compose.prod.yml up -d db redis

# 2. Đợi dịch vụ sẵn sàng
docker compose -f docker-compose.prod.yml ps

# 3. Áp dụng bảng phân quyền tối thiểu (Least-Privilege Roles)
docker compose -f docker-compose.prod.yml exec -T db psql -U elib -d elib < scripts/db/least-privilege-roles.sql
```

### Bước 5: Khởi động Toàn bộ Hệ thống
```bash
docker compose -f docker-compose.prod.yml up -d --build
```

### Bước 6: Kiểm tra Hoạt động & Smoke Tests
Chạy script kiểm tra khói tự động:
```bash
python3 scripts/smoke.py
```
Xác nhận phản hồi từ Probes:
- Liveness: `curl -I https://elib.pxu.edu.vn/api/system/health/liveness` → HTTP 200
- Readiness: `curl -I https://elib.pxu.edu.vn/api/system/health/readiness` → HTTP 200

---

## 3. Lịch trình Sao lưu & Khôi phục Tự động

### Cấu hình Cron Job Sao lưu hàng ngày
Mở bảng lịch trình cron:
```bash
crontab -e
```
Thêm dòng sao lưu tự động vào 02:00 sáng mỗi ngày:
```cron
0 2 * * * /opt/elib/scripts/backup.sh >> /var/log/elib-backup.log 2>&1
```

### Kiểm thử Phục hồi Thảm họa Định kỳ (Drill)
Mỗi tháng thực hiện chạy kiểm thử drill tự động để xác nhận tính toàn vẹn của tệp sao lưu:
```bash
python3 scripts/backup-drill.py
```

---

## 4. Quy trình Cập nhật Không Gián đoạn (Zero-Downtime Rolling Update)

Khi có bản phát hành mã nguồn mới:

```bash
# 1. Kéo mã nguồn mới nhất
git pull origin main

# 2. Build image container mới trong chế độ nền
docker compose -f docker-compose.prod.yml build --no-cache backend frontend

# 3. Khởi động lại dịch vụ backend (Spring Boot tự động chạy migration Flyway an toàn)
docker compose -f docker-compose.prod.yml up -d --no-deps backend

# 4. Kiểm tra sức khỏe backend
until curl -s http://localhost:18080/api/system/health/readiness | grep -q "UP"; do
    echo "Đang đợi backend sẵn sàng..."
    sleep 3
done

# 5. Khởi động lại frontend và Nginx Proxy
docker compose -f docker-compose.prod.yml up -d --no-deps frontend nginx-proxy
echo "Cập nhật hoàn tất thành công!"
```

---

## 5. Quy trình Đổi Khóa Bí mật An toàn (Secret Rotation)

Khi cần xoay vòng mật khẩu DB, Redis hoặc session token:

```bash
# 1. Chạy mô phỏng trước
python3 scripts/rotate-secrets.py --dry-run

# 2. Thực hiện đổi khóa thực tế
python3 scripts/rotate-secrets.py --target all

# 3. Làm theo các câu lệnh hướng dẫn hiển thị trên màn hình để đồng bộ mật khẩu mới vào DB/Redis
# 4. Khởi động lại backend
docker compose -f docker-compose.prod.yml restart backend
```

---

## 6. Kế hoạch Khôi phục Sự cố Khẩn cấp (Rollback & Disaster Recovery)

Nếu xảy ra sự cố nghiêm trọng sau khi cập nhật:
1. **Khôi phục Image Cũ**:
   ```bash
   git checkout <PREVIOUS_COMMIT_TAG>
   docker compose -f docker-compose.prod.yml up -d --build
   ```
2. **Khôi phục Dữ liệu từ Bản Snapshot Gần nhất**:
   ```bash
   # Lấy tệp dump mới nhất
   LATEST_DUMP=$(ls -t /var/backups/elib/elib_*.dump | head -n 1)
   
   # Chạy script phục hồi
   /opt/elib/scripts/restore.sh "$LATEST_DUMP"
   ```
3. **Khởi động lại toàn bộ stack**:
   ```bash
   docker compose -f docker-compose.prod.yml restart
   ```
