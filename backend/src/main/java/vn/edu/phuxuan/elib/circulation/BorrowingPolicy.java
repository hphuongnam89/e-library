package vn.edu.phuxuan.elib.circulation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import vn.edu.phuxuan.elib.organization.Library;

@Entity
@Table(name = "borrowing_policy")
public class BorrowingPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "library_id", nullable = false)
    private Library library;

    @Column(name = "loan_days", nullable = false)
    private int loanDays;

    @Column(name = "daily_fine", nullable = false, precision = 12, scale = 2)
    private BigDecimal dailyFine;

    @Column(name = "max_active_loans")
    private Integer maxActiveLoans;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    public BorrowingPolicy() {
    }

    public BorrowingPolicy(Library library, int loanDays, BigDecimal dailyFine, Integer maxActiveLoans, Instant effectiveFrom) {
        this.library = library;
        this.loanDays = loanDays;
        this.dailyFine = dailyFine;
        this.maxActiveLoans = maxActiveLoans;
        this.effectiveFrom = effectiveFrom;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Library getLibrary() {
        return library;
    }

    public void setLibrary(Library library) {
        this.library = library;
    }

    public int getLoanDays() {
        return loanDays;
    }

    public void setLoanDays(int loanDays) {
        this.loanDays = loanDays;
    }

    public BigDecimal getDailyFine() {
        return dailyFine;
    }

    public void setDailyFine(BigDecimal dailyFine) {
        this.dailyFine = dailyFine;
    }

    public Integer getMaxActiveLoans() {
        return maxActiveLoans;
    }

    public void setMaxActiveLoans(Integer maxActiveLoans) {
        this.maxActiveLoans = maxActiveLoans;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Instant effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BorrowingPolicy other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
