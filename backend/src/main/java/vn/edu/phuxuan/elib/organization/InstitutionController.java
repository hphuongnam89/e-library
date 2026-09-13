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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.phuxuan.elib.organization.dto.CreateInstitutionRequest;
import vn.edu.phuxuan.elib.organization.dto.InstitutionDto;
import vn.edu.phuxuan.elib.organization.dto.UpdateInstitutionRequest;

@RestController
@RequestMapping("/api/v1/institutions")
public class InstitutionController {

    private final OrganizationService organizationService;

    public InstitutionController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping
    public Page<InstitutionDto> getInstitutions(Pageable pageable) {
        return organizationService.getInstitutions(pageable);
    }

    @GetMapping("/{id}")
    public InstitutionDto getInstitutionById(@PathVariable Long id) {
        return organizationService.getInstitutionById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InstitutionDto createInstitution(@Valid @RequestBody CreateInstitutionRequest req) {
        return organizationService.createInstitution(req);
    }

    @PatchMapping("/{id}")
    public InstitutionDto updateInstitution(@PathVariable Long id, @Valid @RequestBody UpdateInstitutionRequest req) {
        return organizationService.updateInstitution(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInstitution(@PathVariable Long id) {
        organizationService.deleteInstitution(id);
    }
}
