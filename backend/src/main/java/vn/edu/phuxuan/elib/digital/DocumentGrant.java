package vn.edu.phuxuan.elib.digital;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import vn.edu.phuxuan.elib.identity.AppUser;
import vn.edu.phuxuan.elib.organization.Campus;
import vn.edu.phuxuan.elib.organization.Department;
import vn.edu.phuxuan.elib.organization.Institution;

@Entity
@Table(name = "document_grant")
public class DocumentGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private DigitalDocument document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id")
    private Institution institution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id")
    private Campus campus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    public DocumentGrant() {
    }

    public DocumentGrant(DigitalDocument document, Institution institution, Campus campus,
                         Department department, AppUser user) {
        this.document = document;
        this.institution = institution;
        this.campus = campus;
        this.department = department;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public DigitalDocument getDocument() {
        return document;
    }

    public void setDocument(DigitalDocument document) {
        this.document = document;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Campus getCampus() {
        return campus;
    }

    public void setCampus(Campus campus) {
        this.campus = campus;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentGrant that = (DocumentGrant) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
