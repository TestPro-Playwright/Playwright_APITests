package apiTests;

import api.AuthApi;
import base.BaseTest;
import com.microsoft.playwright.APIResponse;
import config.ConfigManager;
import io.qameta.allure.*;
import models.LoginRequest;
import org.junit.jupiter.api.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Epic("Course Enrollment System")
@Feature("Authentication")
class AuthTest extends BaseTest {

    private AuthApi authApi;

    @BeforeAll
    @Override
    public void setUp() {
        super.setUp();
        authApi = new AuthApi(unauthenticatedHttp);
    }

    @Test
    @Order(1)
    @DisplayName("Student login — valid credentials return a bearer token")
    void studentLogin_validCredentials_returnsToken() {
        String token = authApi.loginAsStudent(
                ConfigManager.get("student.username"),
                ConfigManager.get("student.password")
        );
        assertNotNull(token, "Token should not be null after successful login");
        assertFalse(token.isBlank(), "Token should not be blank after successful login");
    }

    @Test
    @Order(2)
    @DisplayName("Instructor login — valid credentials return a bearer token")
    void instructorLogin_validCredentials_returnsToken() {
        String token = authApi.loginAsInstructor(
                ConfigManager.get("instructor.username"),
                ConfigManager.get("instructor.password")
        );
        assertNotNull(token, "Instructor token should not be null");
        assertFalse(token.isBlank(), "Instructor token should not be blank");
    }

    @Test
    @Order(3)
    @DisplayName("Student login — invalid credentials return 401 Unauthorised")
    void studentLogin_invalidCredentials_returns401() {
        APIResponse response = unauthenticatedHttp.post(
                "/student/login",
                new LoginRequest("wrong_user", "wrongpassword")
        );
        assertEquals(401, response.status(),
                "Invalid credentials should return 401. Body: " + response.text());
    }

    @Test
    @Order(4)
    @DisplayName("Instructor login — invalid credentials return 401 Unauthorised")
    void instructorLogin_invalidCredentials_returns401() {
        APIResponse response = unauthenticatedHttp.post(
                "/instructor/login",
                new LoginRequest("wrong_user", "wrongpassword")
        );
        assertEquals(401, response.status(),
                "Invalid credentials should return 401. Body: " + response.text());
    }

    private void assertNotNull(String value, String message) {
        org.junit.jupiter.api.Assertions.assertNotNull(value, message);
    }

    private void assertFalse(boolean condition, String message) {
        org.junit.jupiter.api.Assertions.assertFalse(condition, message);
    }

    private void assertEquals(int expected, int actual, String message) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual, message);
    }
}
