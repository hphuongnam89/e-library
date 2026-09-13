package vn.edu.phuxuan.elib.digital;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.phuxuan.elib.catalog.Category;
import vn.edu.phuxuan.elib.catalog.CategoryRepository;
import vn.edu.phuxuan.elib.digital.dto.ApproveDocumentRequest;
import vn.edu.phuxuan.elib.digital.dto.CreateDigitalDocumentRequest;
import vn.edu.phuxuan.elib.digital.dto.DigitalDocumentDto;
import vn.edu.phuxuan.elib.digital.dto.GrantTargetRequest;
import vn.edu.phuxuan.elib.digital.dto.UpdateDocumentGrantsRequest;
import vn.edu.phuxuan.elib.digital.storage.StorageService;
import vn.edu.phuxuan.elib.identity.AppUser;
import vn.edu.phuxuan.elib.identity.AppUserRepository;
import vn.edu.phuxuan.elib.identity.UserRole;
import vn.edu.phuxuan.elib.identity.UserStatus;
import vn.edu.phuxuan.elib.organization.Campus;
import vn.edu.phuxuan.elib.organization.CampusRepository;
import vn.edu.phuxuan.elib.organization.Department;
import vn.edu.phuxuan.elib.organization.DepartmentRepository;
import vn.edu.phuxuan.elib.organization.Institution;
import vn.edu.phuxuan.elib.organization.InstitutionRepository;
import vn.edu.phuxuan.elib.organization.Library;
import vn.edu.phuxuan.elib.organization.LibraryRepository;

@ExtendWith(MockitoExtension.class)
class DigitalDocumentServiceTest {

    @Mock
    private DigitalDocumentRepository documentRepository;
    @Mock
    private DocumentGrantRepository grantRepository;
    @Mock
    private LibraryRepository libraryRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private InstitutionRepository institutionRepository;
    @Mock
    private CampusRepository campusRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private StorageService storageService;

    @InjectMocks
    private DigitalDocumentService service;

    private Library library;
    private AppUser librarian;
    private AppUser student;
    private DigitalDocument document;

    @BeforeEach
    void setUp() {
        Institution institution = new Institution("Đại học Phú Xuân");
        Campus campus = new Campus(institution, "Cơ sở 1");
        library = new Library(campus, "Thư viện 1", "Huế");

        librarian = new AppUser("sub-lib", "lib@pxu.edu.vn", "Thủ Thư", UserRole.LIBRARIAN);
        librarian.setStatus(UserStatus.ACTIVE);

        student = new AppUser("sub-stu", "stu@pxu.edu.vn", "Sinh Viên", UserRole.STUDENT);
        student.setStatus(UserStatus.ACTIVE);

        document = new DigitalDocument(
                library, "Giáo trình", "Mô tả", "NXB", null,
                "key-123.pdf", "application/pdf", 1024L, DigitalDocumentPermission.AUTHENTICATED);
        ReflectionTestUtils.setField(document, "id", 1L);
    }

    @Test
    void createDocumentStoresFileAndInitializesWithDraftStatus() {
        byte[] pdfBytes = "%PDF-1.4 test".getBytes(StandardCharsets.US_ASCII);
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", pdfBytes);

        when(libraryRepository.findById(1L)).thenReturn(Optional.of(library));
        when(storageService.store(any(), anyString(), anyString(), anyLong())).thenReturn("generated-key.pdf");
        when(documentRepository.save(any(DigitalDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateDigitalDocumentRequest req = new CreateDigitalDocumentRequest(
                1L, "Giáo trình Java", "Mô tả", "NXB", null, DigitalDocumentPermission.RESTRICTED);

        DigitalDocumentDto result = service.createDocument(file, req, librarian);

        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("Giáo trình Java");
        assertThat(result.status()).isEqualTo(DigitalDocumentStatus.DRAFT);
        assertThat(result.permission()).isEqualTo(DigitalDocumentPermission.RESTRICTED);
    }

    @Test
    void submitForApprovalTransitionsDraftToPending() {
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DigitalDocumentDto result = service.submitForApproval(1L, librarian);

        assertThat(result.status()).isEqualTo(DigitalDocumentStatus.PENDING);
    }

    @Test
    void approveAndRejectTransitionsCorrectly() {
        document.setStatus(DigitalDocumentStatus.PENDING);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // 1. Approve
        DigitalDocumentDto approved = service.approveOrReject(1L, new ApproveDocumentRequest(true, "OK"), librarian);
        assertThat(approved.status()).isEqualTo(DigitalDocumentStatus.APPROVED);

        // 2. Reject/rework back to DRAFT from APPROVED
        DigitalDocumentDto reworked = service.approveOrReject(1L, new ApproveDocumentRequest(false, "Sửa lại"), librarian);
        assertThat(reworked.status()).isEqualTo(DigitalDocumentStatus.DRAFT);
    }

    @Test
    void publishTransitionsApprovedToPublished() {
        document.setStatus(DigitalDocumentStatus.APPROVED);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DigitalDocumentDto published = service.publish(1L, librarian);
        assertThat(published.status()).isEqualTo(DigitalDocumentStatus.PUBLISHED);
    }

    @Test
    void blocksIllegalTransitions() {
        // Cannot publish directly from DRAFT
        document.setStatus(DigitalDocumentStatus.DRAFT);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> service.publish(1L, librarian))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Chỉ tài liệu ở trạng thái APPROVED mới có thể xuất bản");

        // Cannot rework from PUBLISHED
        document.setStatus(DigitalDocumentStatus.PUBLISHED);
        assertThatThrownBy(() -> service.approveOrReject(1L, new ApproveDocumentRequest(false, "Cancel"), librarian))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Không thể từ chối tài liệu ở trạng thái: PUBLISHED");
    }

    @Test
    void updatesDocumentGrantsWithValidation() {
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        Department dept = new Department(library, "Khoa CNTT");
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(dept));

        UpdateDocumentGrantsRequest req = new UpdateDocumentGrantsRequest(
                List.of(new GrantTargetRequest(null, null, 10L, null)));

        service.updateGrants(1L, req, librarian);

        verify(grantRepository).deleteByDocumentId(1L);
        verify(grantRepository).save(any(DocumentGrant.class));
    }

    @Test
    void streamDocumentSupportsFullAndRangeRequests() {
        document.setStatus(DigitalDocumentStatus.PUBLISHED);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(storageService.getFileSize("key-123.pdf")).thenReturn(1024L);
        when(storageService.loadRange(eq("key-123.pdf"), anyLong(), anyLong()))
                .thenReturn(new ByteArrayInputStream(new byte[500]));

        // Full stream (Range == null)
        ResponseEntity<InputStreamResource> fullRes = service.streamDocument(1L, null, student);
        assertThat(fullRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fullRes.getHeaders().getContentLength()).isEqualTo(1024L);

        // Byte range stream
        ResponseEntity<InputStreamResource> rangeRes = service.streamDocument(1L, "bytes=0-499", student);
        assertThat(rangeRes.getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
        assertThat(rangeRes.getHeaders().getFirst("Content-Range")).isEqualTo("bytes 0-499/1024");
        assertThat(rangeRes.getHeaders().getContentLength()).isEqualTo(500L);

        // Invalid range
        ResponseEntity<InputStreamResource> badRangeRes = service.streamDocument(1L, "bytes=2000-3000", student);
        assertThat(badRangeRes.getStatusCode()).isEqualTo(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
    }

    @Test
    void streamDocumentBlocksUnauthorizedUserOnRestrictedDocument() {
        document.setStatus(DigitalDocumentStatus.PUBLISHED);
        document.setPermission(DigitalDocumentPermission.RESTRICTED);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(grantRepository.hasGrantAccess(anyLong(), any(), any(), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.streamDocument(1L, null, student))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Bạn không có quyền truy cập tài liệu này");
    }
}
