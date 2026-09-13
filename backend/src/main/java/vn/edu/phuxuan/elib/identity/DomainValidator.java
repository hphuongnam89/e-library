package vn.edu.phuxuan.elib.identity;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DomainValidator {

    private final List<String> allowedDomains;

    public DomainValidator(@Value("${elib.auth.allowed-domains:}") String allowedDomainsStr) {
        if (StringUtils.hasText(allowedDomainsStr)) {
            this.allowedDomains = Arrays.stream(allowedDomainsStr.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(s -> s.toLowerCase(Locale.ROOT))
                    .toList();
        } else {
            this.allowedDomains = List.of();
        }
    }

    public void validate(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_email"), "Invalid email address."
            );
        }

        if (allowedDomains.isEmpty()) {
            return; // No restrictions if not configured
        }

        String domain = email.substring(email.indexOf('@') + 1).toLowerCase(Locale.ROOT);
        boolean allowed = allowedDomains.stream().anyMatch(d -> d.equalsIgnoreCase(domain));
        if (!allowed) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("domain_not_allowed"),
                    "Email domain @" + domain + " is not authorized for login."
            );
        }
    }
}
