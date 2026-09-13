package vn.edu.phuxuan.elib;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import vn.edu.phuxuan.elib.identity.AppUser;
import vn.edu.phuxuan.elib.identity.AppUserRepository;
import vn.edu.phuxuan.elib.identity.UserRole;
import vn.edu.phuxuan.elib.organization.Campus;
import vn.edu.phuxuan.elib.organization.CampusRepository;
import vn.edu.phuxuan.elib.organization.Department;
import vn.edu.phuxuan.elib.organization.DepartmentRepository;
import vn.edu.phuxuan.elib.organization.Institution;
import vn.edu.phuxuan.elib.organization.InstitutionRepository;
import vn.edu.phuxuan.elib.organization.Library;
import vn.edu.phuxuan.elib.organization.LibraryRepository;
import vn.edu.phuxuan.elib.organization.dto.AssignDepartmentRequest;
import vn.edu.phuxuan.elib.organization.dto.CreateCampusRequest;
import vn.edu.phuxuan.elib.organization.dto.CreateDepartmentRequest;
import vn.edu.phuxuan.elib.organization.dto.CreateInstitutionRequest;
import vn.edu.phuxuan.elib.organization.dto.CreateLibraryRequest;
import vn.edu.phuxuan.elib.organization.dto.UpdateCampusRequest;
import vn.edu.phuxuan.elib.organization.dto.UpdateDepartmentRequest;
import vn.edu.phuxuan.elib.organization.dto.UpdateInstitutionRequest;
import vn.edu.phuxuan.elib.organization.dto.UpdateLibraryRequest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class OrganizationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379)
            .withCommand("redis-server", "--requirepass", "org-test-redis");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry props) {
        props.add("spring.datasource.url", postgres::getJdbcUrl);
        props.add("spring.datasource.username", postgres::getUsername);
        props.add("spring.datasource.password", postgres::getPassword);
        props.add("spring.data.redis.host", redis::getHost);
        props.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        props.add("spring.data.redis.password", () -> "org-test-redis");
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    InstitutionRepository institutionRepository;

    @Autowired
    CampusRepository campusRepository;

    @Autowired
    LibraryRepository libraryRepository;

    @Autowired
    DepartmentRepository departmentRepository;

    @Autowired
    AppUserRepository appUserRepository;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        appUserRepository.deleteAll();
        departmentRepository.deleteAll();
        libraryRepository.deleteAll();
        campusRepository.deleteAll();
        institutionRepository.deleteAll();
    }

    @Test
    void unauthenticatedAccessToOrganizationEndpointsIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/institutions"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/campuses"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/libraries"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/departments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonAdminCannotPerformWriteOperations() throws Exception {
        CreateInstitutionRequest req = new CreateInstitutionRequest("Test Univ");

        mockMvc.perform(post("/api/v1/institutions")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/institutions/1")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LECTURER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateInstitutionRequest("New"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/institutions/1")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void fullHierarchyCrudLifecycleAndRelationships() throws Exception {
        // 1. Admin creates Institution
        CreateInstitutionRequest instReq = new CreateInstitutionRequest("Trường Đại học Phú Xuân");
        String instJson = mockMvc.perform(post("/api/v1/institutions")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(instReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Trường Đại học Phú Xuân"))
                .andReturn().getResponse().getContentAsString();

        Long institutionId = objectMapper.readTree(instJson).get("id").asLong();

        // 2. Admin creates Campus
        CreateCampusRequest campusReq = new CreateCampusRequest(institutionId, "Cơ sở 1 - 28 Nguyễn Tri Phương");
        String campusJson = mockMvc.perform(post("/api/v1/campuses")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(campusReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.institutionId").value(institutionId))
                .andExpect(jsonPath("$.institutionName").value("Trường Đại học Phú Xuân"))
                .andExpect(jsonPath("$.name").value("Cơ sở 1 - 28 Nguyễn Tri Phương"))
                .andReturn().getResponse().getContentAsString();

        Long campusId = objectMapper.readTree(campusJson).get("id").asLong();

        // 3. Admin creates Library
        CreateLibraryRequest libReq = new CreateLibraryRequest(campusId, "Thư viện Cơ sở 1", "Tầng 2, Nhà A");
        String libJson = mockMvc.perform(post("/api/v1/libraries")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(libReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.campusId").value(campusId))
                .andExpect(jsonPath("$.campusName").value("Cơ sở 1 - 28 Nguyễn Tri Phương"))
                .andExpect(jsonPath("$.name").value("Thư viện Cơ sở 1"))
                .andExpect(jsonPath("$.address").value("Tầng 2, Nhà A"))
                .andReturn().getResponse().getContentAsString();

        Long libraryId = objectMapper.readTree(libJson).get("id").asLong();

        // 4. Admin creates Department
        CreateDepartmentRequest deptReq = new CreateDepartmentRequest(libraryId, "Khoa Công nghệ Thông tin");
        String deptJson = mockMvc.perform(post("/api/v1/departments")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deptReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.libraryId").value(libraryId))
                .andExpect(jsonPath("$.libraryName").value("Thư viện Cơ sở 1"))
                .andExpect(jsonPath("$.name").value("Khoa Công nghệ Thông tin"))
                .andReturn().getResponse().getContentAsString();

        Long departmentId = objectMapper.readTree(deptJson).get("id").asLong();

        // 5. Authenticated student can read all levels
        mockMvc.perform(get("/api/v1/institutions")
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Trường Đại học Phú Xuân"));

        mockMvc.perform(get("/api/v1/campuses")
                        .param("institutionId", institutionId.toString())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Cơ sở 1 - 28 Nguyễn Tri Phương"));

        mockMvc.perform(get("/api/v1/libraries")
                        .param("campusId", campusId.toString())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Thư viện Cơ sở 1"));

        mockMvc.perform(get("/api/v1/departments")
                        .param("libraryId", libraryId.toString())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Khoa Công nghệ Thông tin"));

        // 6. Admin updates entities
        mockMvc.perform(patch("/api/v1/institutions/" + institutionId)
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateInstitutionRequest("Đại học Phú Xuân - Huế"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Đại học Phú Xuân - Huế"));

        mockMvc.perform(patch("/api/v1/campuses/" + campusId)
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateCampusRequest(null, "Cơ sở 1 Đống Đa"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cơ sở 1 Đống Đa"));

        mockMvc.perform(patch("/api/v1/libraries/" + libraryId)
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateLibraryRequest(null, "Thư viện Trung tâm", "Tầng 1-3"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Thư viện Trung tâm"))
                .andExpect(jsonPath("$.address").value("Tầng 1-3"));

        mockMvc.perform(patch("/api/v1/departments/" + departmentId)
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateDepartmentRequest(null, "Khoa CNTT & Trí tuệ Nhân tạo"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Khoa CNTT & Trí tuệ Nhân tạo"));
    }

    @Test
    void parentValidationFailsWhenParentDoesNotExist() throws Exception {
        // Campus with invalid institution
        CreateCampusRequest campusReq = new CreateCampusRequest(9999L, "Invalid Campus");
        mockMvc.perform(post("/api/v1/campuses")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(campusReq)))
                .andExpect(status().isBadRequest());

        // Library with invalid campus
        CreateLibraryRequest libReq = new CreateLibraryRequest(9999L, "Invalid Lib", "Somewhere");
        mockMvc.perform(post("/api/v1/libraries")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(libReq)))
                .andExpect(status().isBadRequest());

        // Department with invalid library
        CreateDepartmentRequest deptReq = new CreateDepartmentRequest(9999L, "Invalid Dept");
        mockMvc.perform(post("/api/v1/departments")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deptReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteReturns409ConflictWhenChildEntitiesExist() throws Exception {
        Institution inst = institutionRepository.save(new Institution("Inst A"));
        Campus campus = campusRepository.save(new Campus(inst, "Campus A"));
        Library lib = libraryRepository.save(new Library(campus, "Lib A", "Addr A"));
        Department dept = departmentRepository.save(new Department(lib, "Dept A"));

        AppUser user = new AppUser("sub-assigned", "student@phuxuan.edu.vn", "Student Assigned", UserRole.STUDENT);
        user.setDepartment(dept);
        appUserRepository.save(user);

        // 1. Delete Institution -> 409 because campus exists
        mockMvc.perform(delete("/api/v1/institutions/" + inst.getId())
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isConflict());

        // 2. Delete Campus -> 409 because library exists
        mockMvc.perform(delete("/api/v1/campuses/" + campus.getId())
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isConflict());

        // 3. Delete Library -> 409 because department exists
        mockMvc.perform(delete("/api/v1/libraries/" + lib.getId())
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isConflict());

        // 4. Delete Department -> 409 because user is assigned
        mockMvc.perform(delete("/api/v1/departments/" + dept.getId())
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isConflict());
    }

    @Test
    void userDepartmentAssignmentAndMeReflectsDepartment() throws Exception {
        Institution inst = institutionRepository.save(new Institution("PXU"));
        Campus campus = campusRepository.save(new Campus(inst, "CS1"));
        Library lib = libraryRepository.save(new Library(campus, "Lib CS1", "Addr"));
        Department dept = departmentRepository.save(new Department(lib, "Khoa Ngôn ngữ"));

        AppUser user = new AppUser("sub-dept-test", "student.dept@phuxuan.edu.vn", "Sinh Vien Dept", UserRole.STUDENT);
        user = appUserRepository.save(user);

        // Verify initially department is null
        mockMvc.perform(get("/api/v1/auth/me").with(oidcLogin()
                        .idToken(t -> t.subject("sub-dept-test").claim("email", "student.dept@phuxuan.edu.vn"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentId").doesNotExist());

        // Non-admin cannot assign department
        mockMvc.perform(patch("/api/v1/admin/users/" + user.getId() + "/department")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignDepartmentRequest(dept.getId()))))
                .andExpect(status().isForbidden());

        // Admin assigns department to user
        mockMvc.perform(patch("/api/v1/admin/users/" + user.getId() + "/department")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignDepartmentRequest(dept.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentId").value(dept.getId()));

        // /api/v1/auth/me now reflects departmentId
        mockMvc.perform(get("/api/v1/auth/me").with(oidcLogin()
                        .idToken(t -> t.subject("sub-dept-test").claim("email", "student.dept@phuxuan.edu.vn"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentId").value(dept.getId()));

        // Unassign department
        mockMvc.perform(patch("/api/v1/admin/users/" + user.getId() + "/department")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignDepartmentRequest(null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentId").doesNotExist());

        // Now department can be deleted successfully
        mockMvc.perform(delete("/api/v1/departments/" + dept.getId())
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());

        // Followed by library, campus, institution deletion
        mockMvc.perform(delete("/api/v1/libraries/" + lib.getId())
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/campuses/" + campus.getId())
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/institutions/" + inst.getId())
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());

        assertThat(institutionRepository.count()).isZero();
    }
}
