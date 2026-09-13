package vn.edu.phuxuan.elib;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import vn.edu.phuxuan.elib.identity.*;

@ExtendWith(MockitoExtension.class)
class CustomOidcUserServiceTest {

    @Mock
    private AppUserRepository userRepository;

    private DomainValidator domainValidator;
    private CustomOidcUserService service;

    @BeforeEach
    void setup() {
        domainValidator = new DomainValidator("phuxuan.edu.vn");
        service = new CustomOidcUserService(userRepository, domainValidator);
    }

    private OidcUserRequest createRequest(Map<String, Object> claims) {
        ClientRegistration registration = ClientRegistration.withRegistrationId("google")
                .clientId("test-client")
                .clientSecret("test-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .userNameAttributeName("sub")
                .clientName("Google")
                .build();

        OidcIdToken idToken = new OidcIdToken(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                claims
        );

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "access-token",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );

        return new OidcUserRequest(registration, accessToken, idToken);
    }

    @Test
    void rejectsUnverifiedEmail() {
        OidcUserRequest request = createRequest(Map.of(
                "sub", "google-1",
                "email", "student@phuxuan.edu.vn",
                "email_verified", false,
                "name", "Student One"
        ));

        assertThatThrownBy(() -> service.loadUser(request))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("Email is not verified by Google");
    }

    @Test
    void rejectsDisallowedDomain() {
        OidcUserRequest request = createRequest(Map.of(
                "sub", "google-2",
                "email", "attacker@gmail.com",
                "email_verified", true,
                "name", "Attacker"
        ));

        assertThatThrownBy(() -> service.loadUser(request))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("Email domain @gmail.com is not authorized");
    }

    @Test
    void rejectsInactiveUser() {
        OidcUserRequest request = createRequest(Map.of(
                "sub", "google-3",
                "email", "inactive@phuxuan.edu.vn",
                "email_verified", true,
                "name", "Inactive User"
        ));

        AppUser existingUser = new AppUser("google-3", "inactive@phuxuan.edu.vn", "Inactive User", UserRole.STUDENT);
        existingUser.setStatus(UserStatus.INACTIVE);
        when(userRepository.findByGoogleSubject("google-3")).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> service.loadUser(request))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("User account is inactive");
    }

    @Test
    void provisionsNewUserWhenNotFound() {
        OidcUserRequest request = createRequest(Map.of(
                "sub", "google-new",
                "email", "freshman@phuxuan.edu.vn",
                "email_verified", true,
                "name", "Freshman Student"
        ));

        when(userRepository.findByGoogleSubject("google-new")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("freshman@phuxuan.edu.vn")).thenReturn(Optional.empty());

        AppUser savedUser = new AppUser("google-new", "freshman@phuxuan.edu.vn", "Freshman Student", UserRole.STUDENT);
        savedUser.setId(100L);
        when(userRepository.save(any(AppUser.class))).thenReturn(savedUser);

        OidcUser result = service.loadUser(request);

        assertThat(result).isInstanceOf(CustomOidcUser.class);
        CustomOidcUser customUser = (CustomOidcUser) result;
        assertThat(customUser.getUserId()).isEqualTo(100L);
        assertThat(customUser.getEmail()).isEqualTo("freshman@phuxuan.edu.vn");
        assertThat(customUser.getRole()).isEqualTo(UserRole.STUDENT);
        assertThat(customUser.getAuthorities()).extracting("authority").containsExactly("ROLE_STUDENT");
    }

    @Test
    void updatesExistingUserFullName() {
        OidcUserRequest request = createRequest(Map.of(
                "sub", "google-existing",
                "email", "librarian@phuxuan.edu.vn",
                "email_verified", true,
                "name", "New Librarian Name"
        ));

        AppUser existingUser = new AppUser("google-existing", "librarian@phuxuan.edu.vn", "Old Name", UserRole.LIBRARIAN);
        existingUser.setId(5L);
        when(userRepository.findByGoogleSubject("google-existing")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        OidcUser result = service.loadUser(request);

        assertThat(existingUser.getFullName()).isEqualTo("New Librarian Name");
        assertThat(((CustomOidcUser) result).getRole()).isEqualTo(UserRole.LIBRARIAN);
        assertThat(result.getAuthorities()).extracting("authority").containsExactly("ROLE_LIBRARIAN");
    }
}
