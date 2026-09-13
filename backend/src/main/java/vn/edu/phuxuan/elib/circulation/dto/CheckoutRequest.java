package vn.edu.phuxuan.elib.circulation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CheckoutRequest(
        @NotBlank String studentCode,
        @NotEmpty List<@NotBlank String> barcodes
) {
}
