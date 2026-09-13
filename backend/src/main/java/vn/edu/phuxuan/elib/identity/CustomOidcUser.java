package vn.edu.phuxuan.elib.identity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

public class CustomOidcUser extends DefaultOidcUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String email;
    private final String fullName;
    private final String studentCode;
    private final UserRole role;
    private final UserStatus status;

    public CustomOidcUser(Collection<? extends GrantedAuthority> authorities,
                          OidcIdToken idToken,
                          OidcUserInfo userInfo,
                          Long userId,
                          String email,
                          String fullName,
                          String studentCode,
                          UserRole role,
                          UserStatus status) {
        super(authorities, idToken, userInfo != null ? userInfo : new OidcUserInfo(idToken.getClaims()), "sub");
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.studentCode = studentCode;
        this.role = role;
        this.status = status;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public UserRole getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }
}
