package vn.edu.phuxuan.elib.digital;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.phuxuan.elib.catalog.Category;
import vn.edu.phuxuan.elib.catalog.CategoryRepository;
import vn.edu.phuxuan.elib.digital.dto.ApproveDocumentRequest;
import vn.edu.phuxuan.elib.digital.dto.CreateDigitalDocumentRequest;
import vn.edu.phuxuan.elib.digital.dto.DigitalDocumentDto;
import vn.edu.phuxuan.elib.digital.dto.DocumentGrantDto;
import vn.edu.phuxuan.elib.digital.dto.GrantTargetRequest;
import vn.edu.phuxuan.elib.digital.dto.UpdateDigitalDocumentRequest;
import vn.edu.phuxuan.elib.digital.dto.UpdateDocumentGrantsRequest;
import vn.edu.phuxuan.elib.digital.storage.StorageService;
import vn.edu.phuxuan.elib.identity.AppUser;
import vn.edu.phuxuan.elib.identity.AppUserRepository;
import vn.edu.phuxuan.elib.identity.UserRole;
import vn.edu.phuxuan.elib.organization.Campus;
import vn.edu.phuxuan.elib.organization.CampusRepository;
import vn.edu.phuxuan.elib.organization.Department;
import vn.edu.phuxuan.elib.organization.DepartmentRepository;
import vn.edu.phuxuan.elib.organization.Institution;
import vn.edu.phuxuan.elib.organization.InstitutionRepository;
import vn.edu.phuxuan.elib.organization.Library;
import vn.edu.phuxuan.elib.organization.LibraryRepository;

@Service
@Transactional
public class DigitalDocumentService {

    private final DigitalDocumentRepository documentRepository;
    private final DocumentGrantRepository grantRepository;
    private final LibraryRepository libraryRepository;
    private final CategoryRepository categoryRepository;
    private final InstitutionRepository institutionRepository;
    private final CampusRepository campusRepository;
    private final DepartmentRepository departmentRepository;
    private final AppUserRepository appUserRepository;
    private final StorageService storageService;

    public DigitalDocumentService(DigitalDocumentRepository documentRepository,
                                  DocumentGrantRepository grantRepository,
                                  LibraryRepository libraryRepository,
                                  CategoryRepository categoryRepository,
                                  InstitutionRepository institutionRepository,
                                  CampusRepository campusRepository,
                                  DepartmentRepository departmentRepository,
                                  AppUserRepository appUserRepository,
                                  StorageService storageService) {
        this.documentRepository = documentRepository;
        this.grantRepository = grantRepository;
        this.libraryRepository = libraryRepository;
        this.categoryRepository = categoryRepository;
        this.institutionRepository = institutionRepository;
        this.campusRepository = campusRepository;
        this.departmentRepository = departmentRepository;
        this.appUserRepository = appUserRepository;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public Page<DigitalDocumentDto> getDocuments(Long libraryId, Long categoryId, DigitalDocumentStatus status,
                                                String query, AppUser currentUser, Pageable pageable) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        boolean isStaff = currentUser.getRole() == UserRole.ADMIN || currentUser.getRole() == UserRole.LIBRARIAN;
        if (isStaff) {
            return documentRepository.findWithFilters(libraryId, categoryId, status, null, query, pageable)
                    .map(DigitalDocumentDto::from);
        }

        Long departmentId = currentUser.getDepartment() != null ? currentUser.getDepartment().getId() : null;
        Long campusId = (currentUser.getDepartment() != null && currentUser.getDepartment().getLibrary() != null
                && currentUser.getDepartment().getLibrary().getCampus() != null)
                ? currentUser.getDepartment().getLibrary().getCampus().getId() : null;
        Long institutionId = (currentUser.getDepartment() != null && currentUser.getDepartment().getLibrary() != null
                && currentUser.getDepartment().getLibrary().getCampus() != null
                && currentUser.getDepartment().getLibrary().getCampus().getInstitution() != null)
                ? currentUser.getDepartment().getLibrary().getCampus().getInstitution().getId() : null;

        return documentRepository.findAccessibleForUser(currentUser.getId(), departmentId, campusId, institutionId,
                libraryId, categoryId, query, pageable)
                .map(DigitalDocumentDto::from);
    }

    @Transactional(readOnly = true)
    public DigitalDocumentDto getDocumentById(Long id, AppUser currentUser) {
        DigitalDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài liệu số"));

        checkAccess(doc, currentUser);
        return DigitalDocumentDto.from(doc);
    }

    public DigitalDocumentDto createDocument(MultipartFile file, CreateDigitalDocumentRequest req, AppUser librarian) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tệp PDF không được để trống");
        }

        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();
        if (contentType != null && !contentType.equalsIgnoreCase("application/pdf") && !contentType.contains("pdf")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ chấp nhận tệp định dạng PDF");
        }

        Library library = libraryRepository.findById(req.libraryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy thư viện"));

        Category category = null;
        if (req.categoryId() != null) {
            category = categoryRepository.findById(req.categoryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy danh mục"));
        }

        String storageKey;
        try (InputStream in = file.getInputStream()) {
            storageKey = storageService.store(in, originalFilename, "application/pdf", file.getSize());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi đọc tệp tải lên", e);
        }

        DigitalDocument doc = new DigitalDocument(
                library,
                req.title().trim(),
                req.description(),
                req.publisher(),
                category,
                storageKey,
                "application/pdf",
                file.getSize(),
                req.permission() != null ? req.permission() : DigitalDocumentPermission.RESTRICTED
        );

        DigitalDocument saved = documentRepository.save(doc);
        return DigitalDocumentDto.from(saved);
    }

    public DigitalDocumentDto updateDocument(Long id, UpdateDigitalDocumentRequest req, AppUser librarian) {
        DigitalDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài liệu số"));

        if (req.title() != null && !req.title().isBlank()) {
            doc.setTitle(req.title().trim());
        }
        if (req.description() != null) {
            doc.setDescription(req.description());
        }
        if (req.publisher() != null) {
            doc.setPublisher(req.publisher());
        }
        if (req.categoryId() != null) {
            Category category = categoryRepository.findById(req.categoryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy danh mục"));
            doc.setCategory(category);
        }
        if (req.permission() != null) {
            doc.setPermission(req.permission());
        }
        if (req.isActive() != null) {
            doc.setActive(req.isActive());
        }

        return DigitalDocumentDto.from(documentRepository.save(doc));
    }

    public DigitalDocumentDto submitForApproval(Long id, AppUser librarian) {
        DigitalDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài liệu số"));

        if (doc.getStatus() != DigitalDocumentStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chỉ tài liệu ở trạng thái DRAFT mới có thể gửi phê duyệt (trạng thái hiện tại: " + doc.getStatus() + ")");
        }

        doc.setStatus(DigitalDocumentStatus.PENDING);
        return DigitalDocumentDto.from(documentRepository.save(doc));
    }

    public DigitalDocumentDto approveOrReject(Long id, ApproveDocumentRequest req, AppUser approver) {
        DigitalDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài liệu số"));

        if (req.approved()) {
            if (doc.getStatus() != DigitalDocumentStatus.PENDING) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Chỉ tài liệu ở trạng thái PENDING mới có thể phê duyệt (trạng thái hiện tại: " + doc.getStatus() + ")");
            }
            doc.setStatus(DigitalDocumentStatus.APPROVED);
        } else {
            if (doc.getStatus() != DigitalDocumentStatus.PENDING && doc.getStatus() != DigitalDocumentStatus.APPROVED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Không thể từ chối tài liệu ở trạng thái: " + doc.getStatus());
            }
            doc.setStatus(DigitalDocumentStatus.DRAFT);
        }

        return DigitalDocumentDto.from(documentRepository.save(doc));
    }

    public DigitalDocumentDto publish(Long id, AppUser librarian) {
        DigitalDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài liệu số"));

        if (doc.getStatus() != DigitalDocumentStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chỉ tài liệu ở trạng thái APPROVED mới có thể xuất bản (trạng thái hiện tại: " + doc.getStatus() + ")");
        }

        doc.setStatus(DigitalDocumentStatus.PUBLISHED);
        return DigitalDocumentDto.from(documentRepository.save(doc));
    }

    @Transactional(readOnly = true)
    public List<DocumentGrantDto> getGrants(Long documentId, AppUser librarian) {
        if (!documentRepository.existsById(documentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài liệu số");
        }
        return grantRepository.findByDocumentId(documentId).stream()
                .map(DocumentGrantDto::from)
                .toList();
    }

    public List<DocumentGrantDto> updateGrants(Long documentId, UpdateDocumentGrantsRequest req, AppUser librarian) {
        DigitalDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài liệu số"));

        grantRepository.deleteByDocumentId(documentId);

        if (req.grants() != null) {
            for (GrantTargetRequest target : req.grants()) {
                int targetsSpecified = (target.institutionId() != null ? 1 : 0) +
                                       (target.campusId() != null ? 1 : 0) +
                                       (target.departmentId() != null ? 1 : 0) +
                                       (target.userId() != null ? 1 : 0);

                if (targetsSpecified != 1) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Mỗi grant phải chỉ định chính xác 1 đối tượng: institutionId, campusId, departmentId, hoặc userId");
                }

                Institution institution = target.institutionId() != null
                        ? institutionRepository.findById(target.institutionId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Institution not found"))
                        : null;

                Campus campus = target.campusId() != null
                        ? campusRepository.findById(target.campusId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campus not found"))
                        : null;

                Department department = target.departmentId() != null
                        ? departmentRepository.findById(target.departmentId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department not found"))
                        : null;

                AppUser user = target.userId() != null
                        ? appUserRepository.findById(target.userId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"))
                        : null;

                DocumentGrant grant = new DocumentGrant(doc, institution, campus, department, user);
                grantRepository.save(grant);
            }
        }

        return grantRepository.findByDocumentId(documentId).stream()
                .map(DocumentGrantDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResponseEntity<InputStreamResource> streamDocument(Long id, String rangeHeader, AppUser currentUser) {
        DigitalDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài liệu số"));

        checkAccess(doc, currentUser);

        long fileSize = storageService.getFileSize(doc.getStorageKey());

        if (rangeHeader == null || rangeHeader.isBlank()) {
            InputStream stream = storageService.loadRange(doc.getStorageKey(), 0, fileSize);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileSize))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CACHE_CONTROL, "private, no-store, must-revalidate")
                    .header("Content-Disposition", "inline; filename=\"document.pdf\"")
                    .body(new InputStreamResource(stream));
        }

        if (!rangeHeader.startsWith("bytes=")) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                    .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                    .build();
        }

        String rangeSpec = rangeHeader.substring(6).trim();
        long start;
        long end;

        try {
            if (rangeSpec.startsWith("-")) {
                long suffix = Long.parseLong(rangeSpec.substring(1));
                start = Math.max(0, fileSize - suffix);
                end = fileSize - 1;
            } else if (rangeSpec.endsWith("-")) {
                start = Long.parseLong(rangeSpec.substring(0, rangeSpec.length() - 1));
                end = fileSize - 1;
            } else {
                String[] parts = rangeSpec.split("-");
                if (parts.length != 2) {
                    return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                            .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                            .build();
                }
                start = Long.parseLong(parts[0]);
                end = Long.parseLong(parts[1]);
            }
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                    .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                    .build();
        }

        if (start < 0 || start >= fileSize || end < start) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                    .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                    .build();
        }

        if (end >= fileSize) {
            end = fileSize - 1;
        }

        long length = end - start + 1;
        InputStream rangeStream = storageService.loadRange(doc.getStorageKey(), start, length);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(length))
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, must-revalidate")
                .header("Content-Disposition", "inline; filename=\"document.pdf\"")
                .body(new InputStreamResource(rangeStream));
    }

    public void checkAccess(DigitalDocument doc, AppUser currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        boolean isStaff = currentUser.getRole() == UserRole.ADMIN || currentUser.getRole() == UserRole.LIBRARIAN;
        if (isStaff) {
            return;
        }

        if (!doc.isActive() || doc.getStatus() != DigitalDocumentStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài liệu này hiện không khả dụng");
        }

        if (doc.getPermission() == DigitalDocumentPermission.AUTHENTICATED) {
            return;
        }

        Long departmentId = currentUser.getDepartment() != null ? currentUser.getDepartment().getId() : null;
        Long campusId = (currentUser.getDepartment() != null && currentUser.getDepartment().getLibrary() != null
                && currentUser.getDepartment().getLibrary().getCampus() != null)
                ? currentUser.getDepartment().getLibrary().getCampus().getId() : null;
        Long institutionId = (currentUser.getDepartment() != null && currentUser.getDepartment().getLibrary() != null
                && currentUser.getDepartment().getLibrary().getCampus() != null
                && currentUser.getDepartment().getLibrary().getCampus().getInstitution() != null)
                ? currentUser.getDepartment().getLibrary().getCampus().getInstitution().getId() : null;

        boolean hasAccess = grantRepository.hasGrantAccess(doc.getId(), currentUser.getId(), departmentId, campusId, institutionId);
        if (!hasAccess) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập tài liệu này");
        }
    }
}
