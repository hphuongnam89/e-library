package vn.edu.phuxuan.elib;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class FoundationIntegrationTest {
    @Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");
    @Container static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379).withCommand("redis-server", "--requirepass", "integration-test-only");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry props) {
        props.add("spring.datasource.url", postgres::getJdbcUrl);
        props.add("spring.datasource.username", postgres::getUsername);
        props.add("spring.datasource.password", postgres::getPassword);
        props.add("spring.data.redis.host", redis::getHost);
        props.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        props.add("spring.data.redis.password", () -> "integration-test-only");
    }

    @Autowired TestRestTemplate http;
    @Autowired ObjectMapper mapper;
    @Autowired Flyway flyway;
    @Autowired DataSource dataSource;

    @Test
    void healthUsesRealDatabaseAndRedisWithoutExposingDetails() throws Exception {
        for (String path : new String[]{"", "/liveness", "/readiness"}) {
            ResponseEntity<String> response = http.getForEntity("/api/system/health" + path, String.class);
            assertThat(response.getStatusCode().value()).isEqualTo(200);
            JsonNode body = mapper.readTree(response.getBody());
            assertThat(body.get("status").asText()).isEqualTo("UP");
            assertThat(body.has("components")).isFalse();
            assertThat(body.has("details")).isFalse();
            if (!path.isEmpty()) assertThat(body.size()).isEqualTo(1);
        }
    }

    @Test
    void prometheusMetricsEndpointIsAccessible() {
        ResponseEntity<String> response = http.getForEntity("/api/system/prometheus", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("jvm_memory_used_bytes");
    }

    @Test
    void privateEndpointsDenyAccessAndReplaceUntrustedRequestId() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Request-ID", "untrusted-client-value");
        ResponseEntity<String> response = http.exchange("/api/admin/users", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getHeaders().getContentType().toString()).contains("application/problem+json");
        JsonNode body = mapper.readTree(response.getBody());
        assertThat(body.get("requestId").asText()).isEqualTo(response.getHeaders().getFirst("X-Request-ID"))
                .matches("[a-f0-9-]{36}");
        assertThat(response.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getHeaders().getOrEmpty("Set-Cookie"))
                .noneMatch(cookie -> cookie.contains("ELIB_SESSION"));
    }

    @Test
    void csrfRejectsUnsafeRequests() {
        assertThat(http.postForEntity("/api/admin/users", "{}", String.class).getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void browserCrossOriginIsNotAllowed() {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin("https://untrusted.example");
        ResponseEntity<String> response = http.exchange("/api/system/health", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isNull();
    }

    @Test
    void migrationsValidateAndDoNotReapply() {
        flyway.validate();
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

    @Test
    void phaseZeroSchemaExecutesAndEnforcesCopyAndLoanConstraints() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.createStatement().execute("CREATE SCHEMA design_verification");
            connection.createStatement().execute("SET search_path TO design_verification");
            try {
                ScriptUtils.executeSqlScript(connection, new FileSystemResource(Path.of(System.getProperty("elib.design-schema"))));
                sql(connection, "INSERT INTO institution(name) VALUES ('Test institution')");
                sql(connection, "INSERT INTO campus(institution_id,name) VALUES (1,'Test campus')");
                sql(connection, "INSERT INTO library(campus_id,name) VALUES (1,'Test library')");
                sql(connection, "INSERT INTO department(library_id,name) VALUES (1,'Test department')");
                sql(connection, "INSERT INTO app_user(google_subject,email,full_name,department_id,role) VALUES ('test-sub','test@example.invalid','Test',1,'STUDENT')");
                sql(connection, "INSERT INTO borrowing_policy(library_id,loan_days,daily_fine,effective_from) VALUES (1,7,1000,now())");
                sql(connection, "INSERT INTO book_title(title) VALUES ('One title, two copies')");
                sql(connection, "INSERT INTO book_copy(book_title_id,library_id,barcode) VALUES (1,1,'TEST-1'),(1,1,'TEST-2')");
                assertThatThrownBy(() -> sql(connection, "INSERT INTO book_copy(book_title_id,library_id,barcode) VALUES (1,1,'TEST-1')")).isInstanceOf(SQLException.class);
                String loan = "INSERT INTO borrow(user_id,book_copy_id,policy_id,due_at,daily_fine) VALUES (1,1,1,now()+interval '7 days',1000)";
                sql(connection, loan);
                assertThatThrownBy(() -> sql(connection, loan)).isInstanceOf(SQLException.class);
                assertThatThrownBy(() -> sql(connection, "UPDATE borrow SET status='RETURNED' WHERE book_copy_id=1")).isInstanceOf(SQLException.class);
                sql(connection, "UPDATE borrow SET status='RETURNED',returned_at=now() WHERE book_copy_id=1");
                sql(connection, loan);
                assertThatThrownBy(() -> sql(connection, "UPDATE borrow SET status=NULL")).isInstanceOf(SQLException.class);
                sql(connection, "INSERT INTO category(name) VALUES ('Root')");
                assertThatThrownBy(() -> sql(connection, "INSERT INTO category(name) VALUES ('Root')")).isInstanceOf(SQLException.class);
                sql(connection, "INSERT INTO digital_document(library_id,title,storage_key,content_type,size_bytes) VALUES (1,'Test PDF','private/test','application/pdf',100)");
                assertThatThrownBy(() -> sql(connection, "INSERT INTO document_grant(document_id) VALUES(1)")).isInstanceOf(SQLException.class);
                sql(connection, "INSERT INTO document_grant(document_id,user_id) VALUES(1,1)");
                assertThatThrownBy(() -> sql(connection, "INSERT INTO document_grant(document_id,user_id) VALUES(1,1)")).isInstanceOf(SQLException.class);
            } finally {
                connection.createStatement().execute("SET search_path TO elib");
                connection.createStatement().execute("DROP SCHEMA design_verification CASCADE");
            }
        }
    }

    private void sql(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement()) { statement.execute(sql); }
    }
}
