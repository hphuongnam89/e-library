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
import vn.edu.phuxuan.elib.organization.dto.CreateLibraryRequest;
import vn.edu.phuxuan.elib.organization.dto.LibraryDto;
import vn.edu.phuxuan.elib.organization.dto.UpdateLibraryRequest;

@RestController
@RequestMapping("/api/v1/libraries")
public class LibraryController {

    private final OrganizationService organizationService;

    public LibraryController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping
    public Page<LibraryDto> getLibraries(@RequestParam(required = false) Long campusId, Pageable pageable) {
        return organizationService.getLibraries(campusId, pageable);
    }

    @GetMapping("/{id}")
    public LibraryDto getLibraryById(@PathVariable Long id) {
        return organizationService.getLibraryById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LibraryDto createLibrary(@Valid @RequestBody CreateLibraryRequest req) {
        return organizationService.createLibrary(req);
    }

    @PatchMapping("/{id}")
    public LibraryDto updateLibrary(@PathVariable Long id, @Valid @RequestBody UpdateLibraryRequest req) {
        return organizationService.updateLibrary(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLibrary(@PathVariable Long id) {
        organizationService.deleteLibrary(id);
    }
}
