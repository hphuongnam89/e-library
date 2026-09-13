package vn.edu.phuxuan.elib.organization.dto;

import vn.edu.phuxuan.elib.organization.Campus;

public record CampusDto(Long id, Long institutionId, String institutionName, String name) {
    public static CampusDto from(Campus entity) {
        return new CampusDto(
                entity.getId(),
                entity.getInstitution().getId(),
                entity.getInstitution().getName(),
                entity.getName()
        );
    }
}
