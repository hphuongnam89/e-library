package vn.edu.phuxuan.elib.circulation.dto;

import java.util.List;

public record BatchCheckoutResponse(
        List<BorrowDto> items
) {
}
