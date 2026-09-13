package vn.edu.phuxuan.elib.identity;

import java.util.List;
import java.util.Locale;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CustomOidcUserService extends OidcUserService {

    private final AppUserRepository userRepository;
    private final DomainValidator domainValidator;

    public CustomOidcUserService(AppUserRepository userRepository, DomainValidator domainValidator) {
        this.userRepository = userRepository;
        this.domainValidator = domainValidator;
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser;
        try {
            oidcUser = super.loadUser(userRequest);
        } catch (OAuth2AuthenticationException ex) {
            if (userRequest.getIdToken() != null && userRequest.getIdToken().getEmail() != null) {
                oidcUser = new DefaultOidcUser(
                        List.of(new SimpleGrantedAuthority("ROLE_USER")),
                        userRequest.getIdToken()
                );
            } else {
                throw ex;
            }
        }

        String googleSubject = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        Boolean emailVerified = oidcUser.getEmailVerified();

        String fullName = oidcUser.getFullName();
        if (!StringUtils.hasText(fullName)) {
            fullName = oidcUser.getClaimAsString("name");
            if (!StringUtils.hasText(fullName)) {
                fullName = email;
            }
        }

        // 1. Verify email is verified by Google
        if (!Boolean.TRUE.equals(emailVerified)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("unverified_email"), "Email is not verified by Google."
            );
        }

        // 2. Validate institutional domain allowlist
        domainValidator.validate(email);

        // 3. Find or provision user in PostgreSQL
        final String finalFullName = fullName;
        AppUser user = userRepository.findByGoogleSubject(googleSubject)
                .or(() -> userRepository.findByEmailIgnoreCase(email))
                .orElseGet(() -> {
                    AppUser newUser = new AppUser(
                            googleSubject,
                            email.toLowerCase(Locale.ROOT),
                            finalFullName,
                            UserRole.STUDENT
                    );
                    return userRepository.save(newUser);
                });

        // 4. Verify account status
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("account_inactive"), "User account is inactive."
            );
        }

        // 5. Update user info if changed
        boolean changed = false;
        if (!googleSubject.equals(user.getGoogleSubject())) {
            user.setGoogleSubject(googleSubject);
            changed = true;
        }
        if (StringUtils.hasText(finalFullName) && !finalFullName.equals(user.getFullName())) {
            user.setFullName(finalFullName);
            changed = true;
        }
        if (changed) {
            user = userRepository.save(user);
        }

        // 5. Map app role to Spring Security GrantedAuthority
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        return new CustomOidcUser(
                authorities,
                oidcUser.getIdToken(),
                oidcUser.getUserInfo(),
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getStudentCode(),
                user.getRole(),
                user.getStatus()
        );
    }
}
