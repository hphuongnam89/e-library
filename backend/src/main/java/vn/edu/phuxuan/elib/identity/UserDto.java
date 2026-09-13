package vn.edu.phuxuan.elib.identity;

public record UserDto(
        Long id,
        String email,
        String fullName,
        String studentCode,
        UserRole role,
        UserStatus status,
        Long departmentId
) {
    public static UserDto from(AppUser user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getStudentCode(),
                user.getRole(),
                user.getStatus(),
                user.getDepartment() != null ? user.getDepartment().getId() : null
        );
    }
}
