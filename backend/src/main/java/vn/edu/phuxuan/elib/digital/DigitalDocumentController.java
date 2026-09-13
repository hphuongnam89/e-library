package vn.edu.phuxuan.elib.digital;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.phuxuan.elib.digital.dto.ApproveDocumentRequest;
import vn.edu.phuxuan.elib.digital.dto.CreateDigitalDocumentRequest;
import vn.edu.phuxuan.elib.digital.dto.DigitalDocumentDto;
import vn.edu.phuxuan.elib.digital.dto.DocumentGrantDto;
import vn.edu.phuxuan.elib.digital.dto.UpdateDigitalDocumentRequest;
import vn.edu.phuxuan.elib.digital.dto.UpdateDocumentGrantsRequest;
import vn.edu.phuxuan.elib.identity.AppUser;
import vn.edu.phuxuan.elib.identity.AppUserRepository;
import vn.edu.phuxuan.elib.identity.CustomOidcUser;

@RestController
@RequestMapping("/api/v1/digital-documents")
public class DigitalDocumentController {

    private final DigitalDocumentService digitalDocumentService;
    private final AppUserRepository appUserRepository;

    public DigitalDocumentController(DigitalDocumentService digitalDocumentService,
                                     AppUserRepository appUserRepository) {
        this.digitalDocumentService = digitalDocumentService;
        this.appUserRepository = appUserRepository;
    }

    @GetMapping
    public Page<DigitalDocumentDto> getDocuments(
            @RequestParam(required = false) Long libraryId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) DigitalDocumentStatus status,
            @RequestParam(required = false) String query,
            Pageable pageable,
            Authentication authentication,
            @AuthenticationPrincipal Object principal
    ) {
        AppUser currentUser = resolveUser(authentication, principal);
        return digitalDocumentService.getDocuments(libraryId, categoryId, status, query, currentUser, pageable);
    }

    @GetMapping("/{id}")
    public DigitalDocumentDto getDocumentById(
            @PathVariable Long id,
            Authentication authentication,
            @AuthenticationPrincipal Object principal
    ) {
        AppUser currentUser = resolveUser(authentication, principal);
        return digitalDocumentService.getDocumentById(id, currentUser);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DigitalDocumentDto createDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("libraryId") Long libraryId,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "publisher", required = false) String publisher,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "permission", required = false) DigitalDocumentPermission permission,
            Authentication authentication,
            @AuthenticationPrincipal Object principal
    ) {
        AppUser currentUser = resolveUser(authentication, principal);
        CreateDigitalDocumentRequest req = new CreateDigitalDocumentRequest(
                libraryId, title, description, publisher, categoryId, permission);
        return digitalDocumentService.createDocument(file, req, currentUser);
    }

    @PatchMapping("/{id}")
    public DigitalDocumentDto updateDocument(
            @PathVariable Long id,
            @RequestBody @Valid UpdateDigitalDocumentRequest request,
            Authentication authentication,
            @AuthenticationPrincipal Object principal
    ) {
        AppUser currentUser = resolveUser(authentication, principal);
        return digitalDocumentService.updateDocument(id, request, currentUser);
    }

    @PostMapping("/{id}/submit")
    public DigitalDocumentDto submitForApproval(
            @PathVariable Long id,
            Authentication authentication,
            @AuthenticationPrincipal Object principal
    ) {
        AppUser currentUser = resolveUser(authentication, principal);
        return digitalDocumentService.submitForApproval(id, currentUser);
    }

    @PostMapping("/{id}/approve")
    public DigitalDocumentDto approveOrReject(
            @PathVariable Long id,
            @RequestBody @Valid ApproveDocumentRequest request,
            Authentication authentication,
            @AuthenticationPrincipal Object principal
    ) {
        AppUser currentUser = resolveUser(authentication, principal);
        return digitalDocumentService.approveOrReject(id, request, currentUser);
    }

    @PostMapping("/{id}/publish")
    public DigitalDocumentDto publish(
            @PathVariable Long id,
            Authentication authentication,
            @AuthenticationPrincipal Object principal
    ) {
        AppUser currentUser = resolveUser(authentication, principal);
        return digitalDocumentService.publish(id, currentUser);
    }

    @GetMapping("/{id}/grants")
    public List<DocumentGrantDto> getGrants(
            @PathVariable Long id,
            Authentication authentication,
            @AuthenticationPrincipal Object principal
    ) {
        AppUser currentUser = resolveUser(authentication, principal);
        return digitalDocumentService.getGrants(id, currentUser);
    }

    @PutMapping("/{id}/grants")
    public List<DocumentGrantDto> updateGrants(
            @PathVariable Long id,
            @RequestBody @Valid UpdateDocumentGrantsRequest request,
            Authentication authentication,
            @AuthenticationPrincipal Object principal
    ) {
        AppUser currentUser = resolveUser(authentication, principal);
        return digitalDocumentService.updateGrants(id, request, currentUser);
    }

    @GetMapping("/{id}/stream")
    public ResponseEntity<InputStreamResource> streamDocument(
            @PathVariable Long id,
            @RequestHeader(value = "Range", required = false) String rangeHeader,
            Authentication authentication,
            @AuthenticationPrincipal Object principal
    ) {
        AppUser currentUser = resolveUser(authentication, principal);
        return digitalDocumentService.streamDocument(id, rangeHeader, currentUser);
    }

    private AppUser resolveUser(Authentication authentication, Object principal) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        if (principal instanceof CustomOidcUser customUser) {
            return appUserRepository.findById(customUser.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        }
        if (principal instanceof OidcUser oidcUser) {
            return appUserRepository.findByGoogleSubject(oidcUser.getSubject())
                    .or(() -> appUserRepository.findByEmailIgnoreCase(oidcUser.getEmail()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        }
        if (principal instanceof UserDetails userDetails) {
            return appUserRepository.findByEmailIgnoreCase(userDetails.getUsername())
                    .or(() -> appUserRepository.findByGoogleSubject(userDetails.getUsername()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        }
        String name = authentication.getName();
        return appUserRepository.findByEmailIgnoreCase(name)
                .or(() -> appUserRepository.findByGoogleSubject(name))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
