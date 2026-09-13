package vn.edu.phuxuan.elib.admin.dto;

import vn.edu.phuxuan.elib.identity.UserRole;
import vn.edu.phuxuan.elib.identity.UserStatus;

public record UpdateUserRequest(
        UserRole role,
        UserStatus status,
        Long departmentId
) {}
