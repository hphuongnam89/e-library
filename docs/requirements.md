# E-LIB — Yêu cầu và nguồn đối chiếu

Nguồn nghiệp vụ: `../E-LIB.pdf` (35 trang). Nguồn quy trình, stack và thứ tự phase: `../E-LIB_AI_AGENT_MASTER_GUIDE.pdf` (8 trang). Số trang dưới đây là trang PDF, tính từ 1.

| ID | Yêu cầu | Nguồn | Phase thực hiện |
|---|---|---|---|
| R01 | Web responsive; 4 vai trò: sinh viên, giảng viên, thủ thư, admin | E-LIB tr.1,3,23 | 1 khung; 2 phân quyền |
| R02 | Google Workspace/OIDC; kiểm tra email/domain; không mật khẩu riêng | tr.4–5 | 2 |
| R03 | Institution → Campus → Library → Department → User | Guide tr.3,5 | 3 |
| R04 | Tìm sách theo tên, tác giả, NXB, khoa, bộ môn, danh mục, trạng thái | tr.6–8 | 4 |
| R05 | Mỗi bản vật lý một barcode; nhập 100 bản tạo 100 mã; in tem A4 | tr.8–9; Guide tr.5 | 4 |
| R06 | Quét MSSV và sách, kiểm tra điều kiện, xác nhận mượn/trả; hạn, phạt, trạng thái thanh toán | tr.9–12 | 5 |
| R07 | Thư viện số, phân loại, upload, xuất bản, quyền truy cập | tr.12–14,31 | 6 |
| R08 | PDF.js, storage riêng, streaming có kiểm tra quyền, watermark cá nhân | tr.13–15; Guide tr.5 | 7 |
| R09 | Heartbeat khoảng 15 giây; idle khoảng 60 giây; chỉ cộng thời gian hoạt động | tr.15–17; Guide tr.5 | 8 |
| R10 | Lịch sử cá nhân; dashboard, lọc báo cáo và xuất XLSX | tr.17–20 | 8,10 |
| R11 | Email nhắc hạn/quá hạn; chống gửi trùng và xử lý lỗi | tr.20–21; Guide tr.3 | 9 |
| R12 | Quản trị người dùng/vai trò/trạng thái, audit, chính sách | tr.21–22; Guide tr.3 | 11 |
| R13 | RBAC, private storage, rate limit, logging | tr.26–27 | 2,7,11,12 |
| R14 | Trang <2s, reader <3s, barcode gần tức thời | tr.27–28 | 13 đo dưới tải xác định |
| R15 | Máy quét USB HID gửi chuỗi và Enter | tr.28 | 4–5 |
| R16 | Loading/empty/error, trang di động; bộ màn hình chức năng | tr.32–33; Guide tr.4–5 | từng phase |
| R17 | Docker, CI/CD, backup, giám sát, tài liệu vận hành | Guide tr.1,3,7 | 1 nền; 14 production |

## Xử lý khác biệt giữa hai nguồn

- E-LIB tr.29 liệt kê nhiều backend; Guide tr.1 chốt Java 21 + Spring Boot: thực hiện theo Guide.
- E-LIB tr.35 xếp quản lý nhiều bản copy vào mở rộng; Guide tr.5 bắt buộc BookTitle → BookCopy → Barcode: mô hình dữ liệu phải tách ngay, tính năng phát triển ở phase 4.
- 14 ngày và 5.000đ/ngày xuất hiện trong ví dụ nghiệp vụ; Guide tr.5 cấm hard-code: phải là policy được cấu hình, không tự seed thành chính sách chính thức.
- Biện pháp giao diện không thể bảo đảm chống sao chép 100%; quyền, private storage, streaming và audit là các lớp kiểm soát chính.

## Thông tin cần xác nhận tại phase sở hữu

| Thông tin chưa được cung cấp | Xử lý hiện tại | Cần trước |
|---|---|---|
| Google client ID/secret, domain được phép, nguồn gán vai trò | Không bịa domain, không tự tạo admin | Phase 2 |
| Danh sách trường/cơ sở/thư viện/khoa | Không seed dữ liệu tổ chức giả | Phase 3 |
| Policy hạn mượn/phạt/giới hạn, múi giờ chốt ngày phạt | Thiết kế bảng policy; chốt timezone/quy tắc làm tròn với thư viện | Phase 5 |
| Bucket, quyền tài liệu, giới hạn PDF, retention | Storage key riêng; quyền mặc định RESTRICTED | Phase 6–8 |
| SMTP, nội dung và lịch gửi | Không gửi thư trong phase 1 | Phase 9 |
| Domain production, host, tải dự kiến, RPO/RTO | Chỉ chạy local; chưa tuyên bố đạt hiệu năng/SLA | Phase 13–14 |

Phase 0 hoàn tất phân tích không đồng nghĩa các nghiệp vụ trên đã được lập trình.
