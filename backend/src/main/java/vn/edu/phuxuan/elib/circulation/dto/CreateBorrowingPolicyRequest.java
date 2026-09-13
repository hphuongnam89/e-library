package vn.edu.phuxuan.elib.circulation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;

public record CreateBorrowingPolicyRequest(
        @NotNull @Positive Integer loanDays,
        @NotNull @DecimalMin("0.00") BigDecimal dailyFine,
        @Positive Integer maxActiveLoans,
        @NotNull Instant effectiveFrom
) {
}
