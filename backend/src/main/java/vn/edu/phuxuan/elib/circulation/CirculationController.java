package vn.edu.phuxuan.elib.circulation;

import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.phuxuan.elib.circulation.dto.BatchCheckoutResponse;
import vn.edu.phuxuan.elib.circulation.dto.BorrowDto;
import vn.edu.phuxuan.elib.circulation.dto.CheckoutRequest;
import vn.edu.phuxuan.elib.identity.AppUser;
import vn.edu.phuxuan.elib.identity.AppUserRepository;
import vn.edu.phuxuan.elib.identity.CustomOidcUser;

@RestController
@RequestMapping("/api/v1")
public class CirculationController {

    private final CirculationService circulationService;
    private final AppUserRepository appUserRepository;

    public CirculationController(CirculationService circulationService, AppUserRepository appUserRepository) {
        this.circulationService = circulationService;
        this.appUserRepository = appUserRepository;
    }

    @PostMapping("/borrows")
    @ResponseStatus(HttpStatus.CREATED)
    public BatchCheckoutResponse checkout(@Valid @RequestBody CheckoutRequest req) {
        return circulationService.checkout(req);
    }

    @GetMapping("/borrows")
    public Page<BorrowDto> getBorrows(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) BorrowStatus status,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            Pageable pageable
    ) {
        return circulationService.getBorrows(userId, status, overdue, from, to, pageable);
    }

    @GetMapping("/borrows/{id}")
    public BorrowDto getBorrowById(@PathVariable Long id) {
        return circulationService.getBorrowById(id);
    }

    @PostMapping("/borrows/{id}/return")
    public BorrowDto returnBorrow(@PathVariable Long id) {
        return circulationService.returnBorrow(id);
    }

    @PostMapping("/borrows/{id}/fine-payment")
    public BorrowDto payFine(@PathVariable Long id) {
        return circulationService.payFine(id);
    }

    @GetMapping("/me/borrows")
    public Page<BorrowDto> getMyBorrows(
            Authentication authentication,
            @AuthenticationPrincipal Object principal,
            Pageable pageable
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }
        Long currentUserId = resolveUserId(authentication, principal);
        return circulationService.getMyBorrows(currentUserId, pageable);
    }

    private Long resolveUserId(Authentication authentication, Object principal) {
        if (principal instanceof CustomOidcUser customUser) {
            return customUser.getUserId();
        }
        if (principal instanceof OidcUser oidcUser) {
            return appUserRepository.findByGoogleSubject(oidcUser.getSubject())
                    .or(() -> appUserRepository.findByEmailIgnoreCase(oidcUser.getEmail()))
                    .map(AppUser::getId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        }
        if (principal instanceof UserDetails userDetails) {
            return appUserRepository.findByEmailIgnoreCase(userDetails.getUsername())
                    .or(() -> appUserRepository.findByGoogleSubject(userDetails.getUsername()))
                    .map(AppUser::getId)
                    .orElse(0L);
        }
        String name = authentication.getName();
        return appUserRepository.findByEmailIgnoreCase(name)
                .or(() -> appUserRepository.findByGoogleSubject(name))
                .map(AppUser::getId)
                .orElse(0L);
    }
}
