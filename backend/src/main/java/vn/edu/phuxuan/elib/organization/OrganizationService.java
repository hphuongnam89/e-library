package vn.edu.phuxuan.elib.organization;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.phuxuan.elib.identity.AppUser;
import vn.edu.phuxuan.elib.identity.AppUserRepository;
import vn.edu.phuxuan.elib.identity.UserDto;
import vn.edu.phuxuan.elib.organization.dto.CampusDto;
import vn.edu.phuxuan.elib.organization.dto.CreateCampusRequest;
import vn.edu.phuxuan.elib.organization.dto.CreateDepartmentRequest;
import vn.edu.phuxuan.elib.organization.dto.CreateInstitutionRequest;
import vn.edu.phuxuan.elib.organization.dto.CreateLibraryRequest;
import vn.edu.phuxuan.elib.organization.dto.DepartmentDto;
import vn.edu.phuxuan.elib.organization.dto.InstitutionDto;
import vn.edu.phuxuan.elib.organization.dto.LibraryDto;
import vn.edu.phuxuan.elib.organization.dto.UpdateCampusRequest;
import vn.edu.phuxuan.elib.organization.dto.UpdateDepartmentRequest;
import vn.edu.phuxuan.elib.organization.dto.UpdateInstitutionRequest;
import vn.edu.phuxuan.elib.organization.dto.UpdateLibraryRequest;

@Service
@Transactional
public class OrganizationService {

    private final InstitutionRepository institutionRepository;
    private final CampusRepository campusRepository;
    private final LibraryRepository libraryRepository;
    private final DepartmentRepository departmentRepository;
    private final AppUserRepository appUserRepository;

    public OrganizationService(
            InstitutionRepository institutionRepository,
            CampusRepository campusRepository,
            LibraryRepository libraryRepository,
            DepartmentRepository departmentRepository,
            AppUserRepository appUserRepository
    ) {
        this.institutionRepository = institutionRepository;
        this.campusRepository = campusRepository;
        this.libraryRepository = libraryRepository;
        this.departmentRepository = departmentRepository;
        this.appUserRepository = appUserRepository;
    }

    // --- Institution ---
    @Transactional(readOnly = true)
    public Page<InstitutionDto> getInstitutions(Pageable pageable) {
        return institutionRepository.findAll(pageable).map(InstitutionDto::from);
    }

    @Transactional(readOnly = true)
    public InstitutionDto getInstitutionById(Long id) {
        return institutionRepository.findById(id)
                .map(InstitutionDto::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Institution not found with id: " + id));
    }

    public InstitutionDto createInstitution(CreateInstitutionRequest req) {
        if (institutionRepository.existsByNameIgnoreCase(req.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Institution with name '" + req.name() + "' already exists");
        }
        Institution entity = new Institution(req.name());
        return InstitutionDto.from(institutionRepository.save(entity));
    }

    public InstitutionDto updateInstitution(Long id, UpdateInstitutionRequest req) {
        Institution entity = institutionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Institution not found with id: " + id));

        if (req.name() != null && !req.name().isBlank()) {
            if (institutionRepository.existsByNameIgnoreCaseAndIdNot(req.name(), id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Institution with name '" + req.name() + "' already exists");
            }
            entity.setName(req.name());
        }
        return InstitutionDto.from(institutionRepository.save(entity));
    }

    public void deleteInstitution(Long id) {
        if (!institutionRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Institution not found with id: " + id);
        }
        if (campusRepository.existsByInstitutionId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete institution because it has associated campuses");
        }
        institutionRepository.deleteById(id);
    }

    // --- Campus ---
    @Transactional(readOnly = true)
    public Page<CampusDto> getCampuses(Long institutionId, Pageable pageable) {
        if (institutionId != null) {
            return campusRepository.findByInstitutionId(institutionId, pageable).map(CampusDto::from);
        }
        return campusRepository.findAll(pageable).map(CampusDto::from);
    }

    @Transactional(readOnly = true)
    public CampusDto getCampusById(Long id) {
        return campusRepository.findById(id)
                .map(CampusDto::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campus not found with id: " + id));
    }

    public CampusDto createCampus(CreateCampusRequest req) {
        Institution institution = institutionRepository.findById(req.institutionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Institution not found with id: " + req.institutionId()));
        Campus entity = new Campus(institution, req.name());
        return CampusDto.from(campusRepository.save(entity));
    }

    public CampusDto updateCampus(Long id, UpdateCampusRequest req) {
        Campus entity = campusRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campus not found with id: " + id));

        if (req.institutionId() != null) {
            Institution institution = institutionRepository.findById(req.institutionId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Institution not found with id: " + req.institutionId()));
            entity.setInstitution(institution);
        }
        if (req.name() != null && !req.name().isBlank()) {
            entity.setName(req.name());
        }
        return CampusDto.from(campusRepository.save(entity));
    }

    public void deleteCampus(Long id) {
        if (!campusRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Campus not found with id: " + id);
        }
        if (libraryRepository.existsByCampusId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete campus because it has associated libraries");
        }
        campusRepository.deleteById(id);
    }

    // --- Library ---
    @Transactional(readOnly = true)
    public Page<LibraryDto> getLibraries(Long campusId, Pageable pageable) {
        if (campusId != null) {
            return libraryRepository.findByCampusId(campusId, pageable).map(LibraryDto::from);
        }
        return libraryRepository.findAll(pageable).map(LibraryDto::from);
    }

    @Transactional(readOnly = true)
    public LibraryDto getLibraryById(Long id) {
        return libraryRepository.findById(id)
                .map(LibraryDto::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Library not found with id: " + id));
    }

    public LibraryDto createLibrary(CreateLibraryRequest req) {
        Campus campus = campusRepository.findById(req.campusId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campus not found with id: " + req.campusId()));
        Library entity = new Library(campus, req.name(), req.address());
        return LibraryDto.from(libraryRepository.save(entity));
    }

    public LibraryDto updateLibrary(Long id, UpdateLibraryRequest req) {
        Library entity = libraryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Library not found with id: " + id));

        if (req.campusId() != null) {
            Campus campus = campusRepository.findById(req.campusId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campus not found with id: " + req.campusId()));
            entity.setCampus(campus);
        }
        if (req.name() != null && !req.name().isBlank()) {
            entity.setName(req.name());
        }
        if (req.address() != null) {
            entity.setAddress(req.address());
        }
        return LibraryDto.from(libraryRepository.save(entity));
    }

    public void deleteLibrary(Long id) {
        if (!libraryRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Library not found with id: " + id);
        }
        if (departmentRepository.existsByLibraryId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete library because it has associated departments");
        }
        libraryRepository.deleteById(id);
    }

    // --- Department ---
    @Transactional(readOnly = true)
    public Page<DepartmentDto> getDepartments(Long libraryId, Pageable pageable) {
        if (libraryId != null) {
            return departmentRepository.findByLibraryId(libraryId, pageable).map(DepartmentDto::from);
        }
        return departmentRepository.findAll(pageable).map(DepartmentDto::from);
    }

    @Transactional(readOnly = true)
    public DepartmentDto getDepartmentById(Long id) {
        return departmentRepository.findById(id)
                .map(DepartmentDto::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found with id: " + id));
    }

    public DepartmentDto createDepartment(CreateDepartmentRequest req) {
        Library library = libraryRepository.findById(req.libraryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Library not found with id: " + req.libraryId()));
        Department entity = new Department(library, req.name());
        return DepartmentDto.from(departmentRepository.save(entity));
    }

    public DepartmentDto updateDepartment(Long id, UpdateDepartmentRequest req) {
        Department entity = departmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found with id: " + id));

        if (req.libraryId() != null) {
            Library library = libraryRepository.findById(req.libraryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Library not found with id: " + req.libraryId()));
            entity.setLibrary(library);
        }
        if (req.name() != null && !req.name().isBlank()) {
            entity.setName(req.name());
        }
        return DepartmentDto.from(departmentRepository.save(entity));
    }

    public void deleteDepartment(Long id) {
        if (!departmentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found with id: " + id);
        }
        if (appUserRepository.existsByDepartmentId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete department because users are assigned to it");
        }
        departmentRepository.deleteById(id);
    }

    // --- User Department Assignment ---
    public UserDto assignDepartment(Long userId, Long departmentId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + userId));

        if (departmentId != null) {
            Department department = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department not found with id: " + departmentId));
            user.setDepartment(department);
        } else {
            user.setDepartment(null);
        }

        return UserDto.from(appUserRepository.save(user));
    }
}
