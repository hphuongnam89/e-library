package vn.edu.phuxuan.elib.organization.dto;

import vn.edu.phuxuan.elib.organization.Department;

public record DepartmentDto(Long id, Long libraryId, String libraryName, String name) {
    public static DepartmentDto from(Department entity) {
        return new DepartmentDto(
                entity.getId(),
                entity.getLibrary().getId(),
                entity.getLibrary().getName(),
                entity.getName()
        );
    }
}
