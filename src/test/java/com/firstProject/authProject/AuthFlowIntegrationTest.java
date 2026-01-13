package com.firstProject.authProject;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",

                // JWT config for tests (must be >= 32 chars for HS256)
                "security.jwt.secret=THIS_IS_A_TEST_SECRET_AT_LEAST_32_CHARS_LONG",
                "security.jwt.accessTokenMinutes=15",
                "security.jwt.refreshTokenDays=7"
        }
)
class AuthFlowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("auth_sso")
            .withUsername("postgres")
            .withPassword("1234");

    @LocalServerPort
    int port;

    private final TestRestTemplate rest = new TestRestTemplate();

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void register_login_me_refresh_logout_flow() {
        String email = "it.user+" + System.currentTimeMillis() + "@example.com";
        String password = "StrongPass123!";

        // 1) Register
        ResponseEntity<Map> reg = rest.postForEntity(
                url("/api/v1/auth/register"),
                Map.of("email", email, "password", password),
                Map.class
        );

        assertThat(reg.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(reg.getBody()).isNotNull();
        assertThat(reg.getBody()).containsKeys("userId", "email", "message");

        // 2) Login
        ResponseEntity<Map> login = rest.postForEntity(
                url("/api/v1/auth/login"),
                Map.of("email", email, "password", password),
                Map.class
        );

        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login.getBody()).isNotNull();

        String accessToken = (String) login.getBody().get("accessToken");
        String refreshToken = (String) login.getBody().get("refreshToken");

        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        // 3) /users/me (protected)
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        ResponseEntity<Map> me = rest.exchange(
                url("/api/v1/users/me"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );

        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody()).isNotNull();
        assertThat(me.getBody()).containsEntry("email", email);

        // 4) Refresh (rotation expected => new refresh token)
        ResponseEntity<Map> refreshed = rest.postForEntity(
                url("/api/v1/auth/refresh"),
                Map.of("refreshToken", refreshToken),
                Map.class
        );

        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshed.getBody()).isNotNull();

        String newAccess = (String) refreshed.getBody().get("accessToken");
        String newRefresh = (String) refreshed.getBody().get("refreshToken");

        assertThat(newAccess).isNotBlank();
        assertThat(newRefresh).isNotBlank();
        assertThat(newRefresh).isNotEqualTo(refreshToken);

        //System.out.println("old refresh=" + refreshToken);
        //System.out.println("new refresh=" + newRefresh);
        // 5) Logout (revokes new refresh)
        ResponseEntity<Map> logout = rest.postForEntity(
                url("/api/v1/auth/logout"),
                Map.of("refreshToken", newRefresh),
                Map.class
        );

        //System.out.println("logout status=" + logout.getStatusCode() + " body=" + logout.getBody());
        //System.out.println("refreshed status=" + refreshed.getStatusCode() + " body=" + refreshed.getBody());

        assertThat(logout.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 6) Refresh again should fail (revoked)
        ResponseEntity<Map> refreshAfterLogout = rest.postForEntity(
                url("/api/v1/auth/refresh"),
                Map.of("refreshToken", newRefresh),
                Map.class
        );

        assertThat(refreshAfterLogout.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}