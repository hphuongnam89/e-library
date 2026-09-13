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
import vn.edu.phuxuan.elib.organization.dto.CampusDto;
import vn.edu.phuxuan.elib.organization.dto.CreateCampusRequest;
import vn.edu.phuxuan.elib.organization.dto.UpdateCampusRequest;

@RestController
@RequestMapping("/api/v1/campuses")
public class CampusController {

    private final OrganizationService organizationService;

    public CampusController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping
    public Page<CampusDto> getCampuses(@RequestParam(required = false) Long institutionId, Pageable pageable) {
        return organizationService.getCampuses(institutionId, pageable);
    }

    @GetMapping("/{id}")
    public CampusDto getCampusById(@PathVariable Long id) {
        return organizationService.getCampusById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CampusDto createCampus(@Valid @RequestBody CreateCampusRequest req) {
        return organizationService.createCampus(req);
    }

    @PatchMapping("/{id}")
    public CampusDto updateCampus(@PathVariable Long id, @Valid @RequestBody UpdateCampusRequest req) {
        return organizationService.updateCampus(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCampus(@PathVariable Long id) {
        organizationService.deleteCampus(id);
    }
}
