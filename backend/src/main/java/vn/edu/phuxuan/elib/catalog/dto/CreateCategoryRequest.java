package vn.edu.phuxuan.elib.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        Long parentId,
        @NotBlank(message = "Category name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name
) {}
