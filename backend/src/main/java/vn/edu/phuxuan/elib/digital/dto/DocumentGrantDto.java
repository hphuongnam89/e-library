package vn.edu.phuxuan.elib.digital.dto;

import vn.edu.phuxuan.elib.digital.DocumentGrant;

public record DocumentGrantDto(
        Long id,
        Long documentId,
        Long institutionId,
        String institutionName,
        Long campusId,
        String campusName,
        Long departmentId,
        String departmentName,
        Long userId,
        String userFullName,
        String userEmail
) {
    public static DocumentGrantDto from(DocumentGrant grant) {
        return new DocumentGrantDto(
                grant.getId(),
                grant.getDocument().getId(),
                grant.getInstitution() != null ? grant.getInstitution().getId() : null,
                grant.getInstitution() != null ? grant.getInstitution().getName() : null,
                grant.getCampus() != null ? grant.getCampus().getId() : null,
                grant.getCampus() != null ? grant.getCampus().getName() : null,
                grant.getDepartment() != null ? grant.getDepartment().getId() : null,
                grant.getDepartment() != null ? grant.getDepartment().getName() : null,
                grant.getUser() != null ? grant.getUser().getId() : null,
                grant.getUser() != null ? grant.getUser().getFullName() : null,
                grant.getUser() != null ? grant.getUser().getEmail() : null
        );
    }
}
