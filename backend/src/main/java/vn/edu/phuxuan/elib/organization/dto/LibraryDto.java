package vn.edu.phuxuan.elib.organization.dto;

import vn.edu.phuxuan.elib.organization.Library;

public record LibraryDto(Long id, Long campusId, String campusName, String name, String address) {
    public static LibraryDto from(Library entity) {
        return new LibraryDto(
                entity.getId(),
                entity.getCampus().getId(),
                entity.getCampus().getName(),
                entity.getName(),
                entity.getAddress()
        );
    }
}
