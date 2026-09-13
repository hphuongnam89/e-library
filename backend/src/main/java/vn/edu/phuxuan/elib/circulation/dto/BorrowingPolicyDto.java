package vn.edu.phuxuan.elib.circulation.dto;

import java.math.BigDecimal;
import java.time.Instant;
import vn.edu.phuxuan.elib.circulation.BorrowingPolicy;

public record BorrowingPolicyDto(
        Long id,
        Long libraryId,
        String libraryName,
        int loanDays,
        BigDecimal dailyFine,
        Integer maxActiveLoans,
        Instant effectiveFrom
) {
    public static BorrowingPolicyDto from(BorrowingPolicy policy) {
        return new BorrowingPolicyDto(
                policy.getId(),
                policy.getLibrary() != null ? policy.getLibrary().getId() : null,
                policy.getLibrary() != null ? policy.getLibrary().getName() : null,
                policy.getLoanDays(),
                policy.getDailyFine(),
                policy.getMaxActiveLoans(),
                policy.getEffectiveFrom()
        );
    }
}
