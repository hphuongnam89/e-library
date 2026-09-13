package vn.edu.phuxuan.elib.organization.dto;

import java.time.Instant;
import vn.edu.phuxuan.elib.organization.Institution;

public record InstitutionDto(Long id, String name, Instant createdAt) {
    public static InstitutionDto from(Institution entity) {
        return new InstitutionDto(entity.getId(), entity.getName(), entity.getCreatedAt());
    }
}
