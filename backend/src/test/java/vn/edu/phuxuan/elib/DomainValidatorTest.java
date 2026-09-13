package vn.edu.phuxuan.elib;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import vn.edu.phuxuan.elib.identity.DomainValidator;

class DomainValidatorTest {

    @Test
    void allowsAnyDomainWhenConfigIsEmpty() {
        DomainValidator validator = new DomainValidator("");
        assertThatCode(() -> validator.validate("user@example.com")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate("student@phuxuan.edu.vn")).doesNotThrowAnyException();
    }

    @Test
    void enforcesConfiguredDomainAllowlist() {
        DomainValidator validator = new DomainValidator("phuxuan.edu.vn, student.phuxuan.edu.vn");

        assertThatCode(() -> validator.validate("lecturer@phuxuan.edu.vn")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate("k18.cntt@student.phuxuan.edu.vn")).doesNotThrowAnyException();

        assertThatThrownBy(() -> validator.validate("user@gmail.com"))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("Email domain @gmail.com is not authorized");

        assertThatThrownBy(() -> validator.validate("attacker@phuxuan.edu.vn.attacker.com"))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void rejectsInvalidEmailFormat() {
        DomainValidator validator = new DomainValidator("phuxuan.edu.vn");

        assertThatThrownBy(() -> validator.validate("not-an-email"))
                .isInstanceOf(OAuth2AuthenticationException.class);
        assertThatThrownBy(() -> validator.validate(""))
                .isInstanceOf(OAuth2AuthenticationException.class);
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }
}
