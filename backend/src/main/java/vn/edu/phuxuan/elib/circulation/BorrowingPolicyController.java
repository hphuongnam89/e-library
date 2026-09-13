package vn.edu.phuxuan.elib.circulation;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.phuxuan.elib.circulation.dto.BorrowingPolicyDto;
import vn.edu.phuxuan.elib.circulation.dto.CreateBorrowingPolicyRequest;

@RestController
@RequestMapping("/api/v1/libraries/{libraryId}/borrowing-policies")
public class BorrowingPolicyController {

    private final CirculationService circulationService;

    public BorrowingPolicyController(CirculationService circulationService) {
        this.circulationService = circulationService;
    }

    @GetMapping
    public Page<BorrowingPolicyDto> getPolicies(@PathVariable Long libraryId, Pageable pageable) {
        return circulationService.getPolicies(libraryId, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BorrowingPolicyDto createPolicy(
            @PathVariable Long libraryId,
            @Valid @RequestBody CreateBorrowingPolicyRequest req
    ) {
        return circulationService.createPolicy(libraryId, req);
    }
}
