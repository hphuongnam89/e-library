package vn.edu.phuxuan.elib.organization;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.phuxuan.elib.organization.dto.CreateDepartmentRequest;
import vn.edu.phuxuan.elib.organization.dto.DepartmentDto;
import vn.edu.phuxuan.elib.organization.dto.UpdateDepartmentRequest;

@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentController {

    private final OrganizationService organizationService;

    public DepartmentController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping
    public Page<DepartmentDto> getDepartments(@RequestParam(required = false) Long libraryId, Pageable pageable) {
        return organizationService.getDepartments(libraryId, pageable);
    }

    @GetMapping("/{id}")
    public DepartmentDto getDepartmentById(@PathVariable Long id) {
        return organizationService.getDepartmentById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DepartmentDto createDepartment(@Valid @RequestBody CreateDepartmentRequest req) {
        return organizationService.createDepartment(req);
    }

    @PatchMapping("/{id}")
    public DepartmentDto updateDepartment(@PathVariable Long id, @Valid @RequestBody UpdateDepartmentRequest req) {
        return organizationService.updateDepartment(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDepartment(@PathVariable Long id) {
        organizationService.deleteDepartment(id);
    }
}
