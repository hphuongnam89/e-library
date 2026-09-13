package vn.edu.phuxuan.elib.catalog.dto;

import vn.edu.phuxuan.elib.catalog.Category;

public record CategoryDto(Long id, Long parentId, String parentName, String name) {
    public static CategoryDto from(Category entity) {
        return new CategoryDto(
                entity.getId(),
                entity.getParent() != null ? entity.getParent().getId() : null,
                entity.getParent() != null ? entity.getParent().getName() : null,
                entity.getName()
        );
    }
}
