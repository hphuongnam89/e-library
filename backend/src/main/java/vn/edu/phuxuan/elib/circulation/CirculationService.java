package vn.edu.phuxuan.elib.circulation;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.phuxuan.elib.catalog.BookCopy;
import vn.edu.phuxuan.elib.catalog.BookCopyRepository;
import vn.edu.phuxuan.elib.catalog.BookCopyStatus;
import vn.edu.phuxuan.elib.circulation.dto.BatchCheckoutResponse;
import vn.edu.phuxuan.elib.circulation.dto.BorrowDto;
import vn.edu.phuxuan.elib.circulation.dto.BorrowingPolicyDto;
import vn.edu.phuxuan.elib.circulation.dto.CheckoutRequest;
import vn.edu.phuxuan.elib.circulation.dto.CreateBorrowingPolicyRequest;
import vn.edu.phuxuan.elib.identity.AppUser;
import vn.edu.phuxuan.elib.identity.AppUserRepository;
import vn.edu.phuxuan.elib.identity.UserStatus;
import vn.edu.phuxuan.elib.organization.Library;
import vn.edu.phuxuan.elib.organization.LibraryRepository;

@Service
@Transactional
public class CirculationService {

    private final BorrowRepository borrowRepository;
    private final BorrowingPolicyRepository borrowingPolicyRepository;
    private final BookCopyRepository bookCopyRepository;
    private final AppUserRepository appUserRepository;
    private final LibraryRepository libraryRepository;

    public CirculationService(
            BorrowRepository borrowRepository,
            BorrowingPolicyRepository borrowingPolicyRepository,
            BookCopyRepository bookCopyRepository,
            AppUserRepository appUserRepository,
            LibraryRepository libraryRepository
    ) {
        this.borrowRepository = borrowRepository;
        this.borrowingPolicyRepository = borrowingPolicyRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.appUserRepository = appUserRepository;
        this.libraryRepository = libraryRepository;
    }

    // ==================== CHECKOUT ====================

    @CacheEvict(value = "dashboardSummary", allEntries = true)
    public BatchCheckoutResponse checkout(CheckoutRequest req) {
        AppUser user = appUserRepository.findByStudentCode(req.studentCode().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found with student code: " + req.studentCode()));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User account is not active");
        }

        // Check if user has unpaid fines
        if (borrowRepository.existsByUserIdAndFineAmountGreaterThanAndFinePaidAtIsNull(user.getId(), BigDecimal.ZERO)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User has unpaid overdue fines. All fines must be settled before borrowing.");
        }

        long activeLoans = borrowRepository.countByUserIdAndStatus(user.getId(), BorrowStatus.BORROWED);

        List<BorrowDto> resultItems = new ArrayList<>();
        Instant now = Instant.now();
        Map<Long, BorrowingPolicy> policyCache = new HashMap<>();

        for (String barcode : req.barcodes()) {
            BookCopy copy = bookCopyRepository.findByBarcodeForUpdate(barcode.trim())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book copy not found with barcode: " + barcode));

            if (copy.getStatus() != BookCopyStatus.AVAILABLE) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Book copy with barcode '" + barcode + "' is not available for borrowing (status: " + copy.getStatus() + ")");
            }

            Long libraryId = copy.getLibrary().getId();
            BorrowingPolicy policy = policyCache.computeIfAbsent(libraryId, id -> borrowingPolicyRepository
                    .findFirstByLibraryIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(id, now)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active borrowing policy found for library: " + copy.getLibrary().getName())));

            if (policy.getMaxActiveLoans() != null && (activeLoans + resultItems.size() + 1) > policy.getMaxActiveLoans()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Borrowing limit exceeded. Maximum active loans permitted: " + policy.getMaxActiveLoans());
            }

            copy.setStatus(BookCopyStatus.BORROWED);
            bookCopyRepository.save(copy);

            Instant dueAt = now.plus(policy.getLoanDays(), ChronoUnit.DAYS);
            Borrow borrow = new Borrow(user, copy, policy, now, dueAt, policy.getDailyFine());
            Borrow saved = borrowRepository.save(borrow);
            resultItems.add(BorrowDto.from(saved));
        }

        return new BatchCheckoutResponse(resultItems);
    }

    // ==================== RETURN ====================

    @CacheEvict(value = "dashboardSummary", allEntries = true)
    public BorrowDto returnBorrow(Long borrowId) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Borrow record not found with id: " + borrowId));

        // Idempotent return: if already returned, return current state without modifying timestamps or fines
        if (borrow.getStatus() == BorrowStatus.RETURNED) {
            return BorrowDto.from(borrow);
        }

        Instant now = Instant.now();
        borrow.setReturnedAt(now);
        borrow.setStatus(BorrowStatus.RETURNED);

        // Fine calculation: daily_fine * max(0, days_overdue)
        if (now.isAfter(borrow.getDueAt())) {
            long overdueDays = ChronoUnit.DAYS.between(
                    borrow.getDueAt().atZone(ZoneOffset.UTC).toLocalDate(),
                    now.atZone(ZoneOffset.UTC).toLocalDate()
            );
            if (overdueDays <= 0) {
                overdueDays = 1;
            }
            BigDecimal fine = borrow.getDailyFine().multiply(BigDecimal.valueOf(overdueDays));
            borrow.setFineAmount(fine);
        } else {
            borrow.setFineAmount(BigDecimal.ZERO);
        }

        // Restore copy status
        BookCopy copy = borrow.getBookCopy();
        copy.setStatus(BookCopyStatus.AVAILABLE);
        bookCopyRepository.save(copy);

        return BorrowDto.from(borrowRepository.save(borrow));
    }

    // ==================== FINE PAYMENT ====================

    @CacheEvict(value = "dashboardSummary", allEntries = true)
    public BorrowDto payFine(Long borrowId) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Borrow record not found with id: " + borrowId));

        if (borrow.getFineAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No fine is due for this borrow record");
        }

        // Idempotent: already paid
        if (borrow.getFinePaidAt() != null) {
            return BorrowDto.from(borrow);
        }

        borrow.setFinePaidAt(Instant.now());
        return BorrowDto.from(borrowRepository.save(borrow));
    }

    // ==================== POLICY MANAGEMENT ====================

    public BorrowingPolicyDto createPolicy(Long libraryId, CreateBorrowingPolicyRequest req) {
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Library not found with id: " + libraryId));

        if (borrowingPolicyRepository.existsByLibraryIdAndEffectiveFrom(libraryId, req.effectiveFrom())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Policy for this library already exists for effective date: " + req.effectiveFrom());
        }

        BorrowingPolicy policy = new BorrowingPolicy(
                library,
                req.loanDays(),
                req.dailyFine(),
                req.maxActiveLoans(),
                req.effectiveFrom()
        );
        return BorrowingPolicyDto.from(borrowingPolicyRepository.save(policy));
    }

    @Transactional(readOnly = true)
    public Page<BorrowingPolicyDto> getPolicies(Long libraryId, Pageable pageable) {
        return borrowingPolicyRepository.findByLibraryIdOrderByEffectiveFromDesc(libraryId, pageable).map(BorrowingPolicyDto::from);
    }

    // ==================== QUERY BORROWS ====================

    @Transactional(readOnly = true)
    public Page<BorrowDto> getBorrows(
            Long userId,
            BorrowStatus status,
            Boolean overdue,
            Instant from,
            Instant to,
            Pageable pageable
    ) {
        return borrowRepository.findBorrowsWithFilters(userId, status, overdue, Instant.now(), from, to, pageable).map(BorrowDto::from);
    }

    @Transactional(readOnly = true)
    public Page<BorrowDto> getMyBorrows(Long currentUserId, Pageable pageable) {
        return borrowRepository.findByUserId(currentUserId, pageable).map(BorrowDto::from);
    }

    @Transactional(readOnly = true)
    public BorrowDto getBorrowById(Long id) {
        return borrowRepository.findById(id)
                .map(BorrowDto::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Borrow record not found with id: " + id));
    }
}
