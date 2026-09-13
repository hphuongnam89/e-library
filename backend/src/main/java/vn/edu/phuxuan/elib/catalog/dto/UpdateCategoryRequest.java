package vn.edu.phuxuan.elib.catalog.dto;

import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        Long parentId,
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name
) {}
