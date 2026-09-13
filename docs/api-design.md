# E-LIB — API contract

## Phạm vi và quy ước

Base URL cùng origin, prefix `/api/v1` cho nghiệp vụ. Chỉ ba health endpoint dưới đây được triển khai ở phase 1. Mọi route nghiệp vụ trong tài liệu là **contract cho phase sau**, hiện bị chặn. Chưa có domain production được xác nhận.

- JSON dùng camelCase; ID số trừ readingSessionId UUID. Thời gian ISO-8601 UTC, tiền VND dạng decimal string; không tính tiền bằng float.
- Trả object trực tiếp hoặc page `{items,page,size,totalElements,totalPages}`. Page bắt đầu 0, mặc định 20, tối đa 100; sort dùng allowlist của từng endpoint.
- 200 đọc/cập nhật; 201 tạo + Location; 204 xóa/logout; 400 input; 401 chưa xác thực; 403 thiếu quyền/CSRF; 404 không có resource; 409 conflict trạng thái/unique; 413 quá dung lượng; 415 sai loại; 429 giới hạn; 500 lỗi nội bộ; 503 chưa sẵn sàng.
- Lỗi dùng `application/problem+json`: `{type,title,status,detail,requestId}`; instance có thể do framework thêm. Validation không phản chiếu token/input nhạy cảm. Không trả stacktrace hoặc tên lớp.
- Client gửi ID không đồng nghĩa có quyền; scope tổ chức phải lấy từ người dùng phía server. DTO không trả storageKey hoặc entity JPA.

### API versioning

Prefix `/api/v1` cho tất cả business endpoints. Khi cần breaking change:
- Tạo endpoint mới `/api/v2/resource` thay vì sửa v1.
- Duy trì v1 song song trong ít nhất 1 release cycle để client chuyển đổi.
- Non-breaking evolution (thêm field, thêm optional param) không cần bump version.
- Không dùng header-based versioning (`Accept-Version`) để giữ đơn giản cho university-scale.

## Phase 1 — đang triển khai

| Method / path | Auth | Response |
|---|---|---|
| GET `/api/system/health` | Public | 200 UP hoặc 503 DOWN; có thể có `groups:[liveness,readiness]` |
| GET `/api/system/health/liveness` | Public | Trạng thái tiến trình; không phụ thuộc DB/Redis |
| GET `/api/system/health/readiness` | Public | 200 `{"status":"UP"}` khi app + PostgreSQL + Redis sẵn sàng, 503 khi không sẵn sàng |

GET khác: 401 ở security boundary. Unsafe requests không CSRF: 403. Health không công khai component/host/config. `X-Request-ID` UUID do server tạo. UI chỉ báo ready khi readiness response HTTP thành công và status UP.

## Phase 2 — authentication và quyền

Dùng Spring Security OIDC Authorization Code flow, không tự xác thực Google bằng JSON chứa token.

| Method / path | Hợp đồng |
|---|---|
| GET `/oauth2/authorization/google` | Bắt đầu redirect; state/nonce do framework quản lý |
| GET `/login/oauth2/code/google` | Callback; verify issuer/audience/state/nonce và verified email/domain allowlist |
| GET `/api/v1/auth/me` | User DTO `{id,email,fullName,studentCode,role,status,departmentId}`; 401 nếu chưa login |
| GET `/api/v1/auth/csrf` | Cấp CSRF token cho browser cùng origin; không cache |
| POST `/api/v1/auth/logout` | CSRF bắt buộc, hủy server session và cookie; 204 |

Redis giữ session; cookie HttpOnly, SameSite=Lax, Secure dưới HTTPS. Không trả JWT/refresh token cho localStorage. Role lấy từ hồ sơ được cấp quyền, không suy ra admin từ domain. Google credentials/domain là cấu hình cần cung cấp trước phase 2. Người dùng INACTIVE bị từ chối; đổi role/khóa account phải vô hiệu hóa session phù hợp.

Quyền: STUDENT/LECTURER đọc catalog, tài liệu được cấp và lịch sử của mình; LIBRARIAN quản lý nghiệp vụ trong scope được giao; ADMIN quản trị hệ thống. Không mặc định admin được vượt quyền đọc PDF hạn chế.

## Phase 3 — tổ chức

GET/POST `/api/v1/institutions`, `/campuses`, `/libraries`, `/departments`; GET/PATCH `/{resource}/{id}`. Admin ghi; người dùng chỉ đọc scope cho phép. Parent ID phải tồn tại, không cho tạo quan hệ chéo tenant; không hard-delete tổ chức đang được tham chiếu (409). Danh sách có pagination.

## Phase 4 — catalog và barcode

| Method / path | Quyền và payload |
|---|---|
| GET `/api/v1/book-titles` | Auth; filters q (PostgreSQL FTS, `plainto_tsquery`),author,publisher,departmentId,categoryId,status,libraryId,page,size; availability tổng hợp từ copies |
| POST `/api/v1/book-titles` | Thủ thư; `{title,author,publisher,isbn,publicationYear,categoryId}` → 201 Title DTO |
| GET/PATCH `/api/v1/book-titles/{id}` | Đọc auth, sửa thủ thư; metadata, không barcode/status bản sách |
| GET `/api/v1/book-titles/{id}/copies` | Page Copy DTO `{id,bookTitleId,libraryId,barcode,location,status,version}` |
| POST `/api/v1/book-titles/{id}/copies` | Thủ thư; `{libraryId,quantity,location}`; quantity bounded; sinh một mã UNIQUE cho mỗi bản, trả danh sách copy; 201 |
| GET `/api/v1/book-copies/by-barcode/{barcode}` | Tra bản sách chính xác; 404 nếu không có |
| PATCH `/api/v1/book-copies/{id}` | Thủ thư; vị trí/trạng thái hợp lệ + version; conflict →409; không đổi BORROWED qua catalog |
| POST `/api/v1/book-copies/barcode-pdf` | Thủ thư; `{copyIds:[...]}` bounded; PDF A4 theo cấu hình kích thước tem |
| POST `/api/v1/book-titles/import` | Thủ thư; file đã validate, giới hạn dòng/dung lượng; trả kết quả từng dòng, không partial success mơ hồ |
| GET/POST `/api/v1/categories`; PATCH `/categories/{id}` | Đọc auth; ghi thủ thư; kiểm tra chu trình và tên trùng |

USB HID gửi chuỗi + Enter cho frontend; frontend gọi API bình thường. Backend không thể tin header để kết luận đang dùng máy quét vật lý. Sequence + UNIQUE sinh barcode; không dùng số hàng hiện tại hoặc MAX+1.

## Phase 5 — circulation

| Method / path | Hợp đồng |
|---|---|
| POST `/api/v1/borrows` | Thủ thư; `{studentCode,barcodes:["ELIB-00001"]}`; transaction toàn bộ lô, trả `{items:[BorrowDTO]}`; 409 nếu có bản không AVAILABLE; giới hạn số bản theo policy |
| GET `/api/v1/borrows` | Thủ thư scoped, filters userId,status,overdue,from,to,page,size |
| GET `/api/v1/me/borrows` | Lịch sử/các khoản mượn của người đang login |
| POST `/api/v1/borrows/{id}/return` | Thủ thư; confirm trả, tính phạt server, trả BorrowDTO; retry không tạo phạt lần hai |
| POST `/api/v1/borrows/{id}/fine-payment` | Thủ thư; xác nhận thu phạt; audit, retry không ghi nhận hai lần |
| GET/POST `/api/v1/libraries/{id}/borrowing-policies` | Quyền quản lý policy; versioned `{loanDays,dailyFine,maxActiveLoans,effectiveFrom}` |

BorrowDTO: `{id,userId,bookCopyId,barcode,borrowedAt,dueAt,returnedAt,status,fineAmount,fineStatus}`. Không đặt luật “người dùng chỉ được một khoản mượn” khi chưa có policy. Không xóa history; transaction khóa copy và kiểm tra scope policy. Duplicate request sau timeout không tạo thêm loan khi bản đang được mượn.

## Phase 6–7 — tài liệu và reader

| Method / path | Hợp đồng |
|---|---|
| GET `/api/v1/digital-documents` | Auth + scope; page metadata các tài liệu được phép |
| POST `/api/v1/digital-documents` | Thủ thư; multipart PDF + metadata; validate bytes/type/size; 201 `{id,title,status}`; không trả file_path/storage URL |
| GET/PATCH `/api/v1/digital-documents/{id}` | Metadata được phép; sửa/xuất bản cần quyền thủ thư |
| POST `/api/v1/digital-documents/{id}/submit` | Librarian submit: DRAFT → PENDING |
| POST `/api/v1/digital-documents/{id}/approve` | Admin/Librarian approve: PENDING → APPROVED; hoặc reject: PENDING → DRAFT |
| POST `/api/v1/digital-documents/{id}/publish` | Librarian publish: APPROVED → PUBLISHED |
| PUT `/api/v1/digital-documents/{id}/grants` | Thủ thư có scope; danh sách grant đích, validation và audit |
| GET `/api/v1/digital-documents/{id}/stream` | Auth + quyền + PUBLISHED/active; 200 hoặc 206 cho Range hợp lệ, 416 sai Range; PDF bytes, private/no-store, không redirect ra S3 |

Upload limits (cấu hình, giá trị mặc định):
- Max file size: 50MB (có thể tăng, cần xác nhận với stakeholder).
- Accepted MIME types: `application/pdf` only. Epub/docx chưa hỗ trợ trong scope hiện tại.
- Concurrent upload limit: 3 per user (prevent abuse).
- Tổng storage quota per library: chưa giới hạn Phase 6, cân nhắc Phase 11.

Document status state machine: `DRAFT → PENDING → APPROVED → PUBLISHED`. Cho phép `PENDING → DRAFT` (reject/rework) và `APPROVED → DRAFT` (rework). Không cho phép quay về từ PUBLISHED (dùng `is_active=false` để ẩn).

Watermark lấy user hiện tại tại server/session; không có API cho client tự gửi email/MSSV để giả watermark. PDF.js không có download/print UI; không tuyên bố DRM tuyệt đối. Rate limiting: `bucket4j-spring-boot-starter` + Redis cho streaming/API endpoints; Nginx `limit_req_zone` cho upload. Cấu hình tại phase sở hữu.

## Phase 8 — reading

| Method / path | Hợp đồng |
|---|---|
| POST `/api/v1/reading/sessions` | `{documentId}` → `{sessionId,heartbeatIntervalSeconds:15,idleTimeoutSeconds:60}`; server kiểm tra quyền |
| POST `/api/v1/reading/sessions/{id}/heartbeat` | `{sequenceNumber,active,visible}`; session thuộc user; trả `{accepted,activeSeconds}`; dedup sequence, không tin duration client |
| POST `/api/v1/reading/sessions/{id}/end` | Kết thúc idempotent; không cộng thời gian sau khi hết session |
| GET `/api/v1/me/reading-history` | Page theo user từ session, không nhận userId để xem người khác |

Server chỉ cộng delta bị giới hạn khi active + visible; xử lý idle 60s, hidden tab, mạng gián đoạn, reorder/replay, nhiều tab. Thiếu document/session ID trong heartbeat cũ đã được sửa.

## Phase 9–11 — thông báo, báo cáo, quản trị

| Endpoints | Scope |
|---|---|
| GET `/api/v1/me/notifications` | User hiện tại, page; filter theo `type` (DUE_REMINDER/OVERDUE/SYSTEM) và `channel` (EMAIL/IN_APP) |
| GET `/api/v1/dashboard/summary` | Thủ thư/admin đúng scope |
| GET `/api/v1/reports/{books,borrows,reading,users}` | Filters from,to,departmentId,categoryId,status,page,size; whitelist report |
| GET `/api/v1/reports/{report}/export` | XLSX; cùng quyền/bộ lọc; giới hạn kích thước, chống formula injection |
| GET `/api/v1/admin/users`; PATCH `/api/v1/admin/users/{id}` | Admin, page; role/status allowlist, audit |
| POST `/api/v1/admin/users/import` | Admin; bulk import users từ Excel (validate, trả kết quả từng dòng) |
| GET `/api/v1/admin/audit`; GET/PATCH `/api/v1/admin/settings` | Admin; không trả secret, không sửa audit |

Jobs gửi email dùng deduplication key + retry bounded; chưa triển khai/sending trong phase 1. Các contract phải được test tại phase tương ứng trước đánh dấu hoàn thành.
