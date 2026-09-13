package vn.edu.phuxuan.elib.identity;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.phuxuan.elib.identity.UserDto;
import vn.edu.phuxuan.elib.organization.OrganizationService;
import vn.edu.phuxuan.elib.organization.dto.AssignDepartmentRequest;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final OrganizationService organizationService;

    public AdminUserController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PatchMapping("/{id}/department")
    public UserDto assignDepartment(@PathVariable Long id, @RequestBody AssignDepartmentRequest req) {
        return organizationService.assignDepartment(id, req.departmentId());
    }
}
