package apiTests;

import api.AuthApi;
import api.CourseApi;
import api.EnrolmentApi;
import base.BaseTest;
import com.google.gson.JsonObject;
import com.microsoft.playwright.APIResponse;
import config.ConfigManager;
import io.qameta.allure.*;
import models.Course;
import models.CourseUpdateRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import report.AssertionHelper;
import utils.RequestHelper;

import java.time.Instant;
import java.util.function.Supplier;
import java.util.stream.Stream;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Epic("Course Enrollment System")
@Feature("Security — Authentication and Authorisation")
public class SecurityTests extends BaseTest {

    private CourseApi    unauthCourseApi;
    private CourseApi    invalidCourseApi;
    private EnrolmentApi unauthEnrolmentApi;
    private EnrolmentApi invalidEnrolmentApi;

    private String existingCourseCode;
    private String existingCourseId;

    @BeforeAll
    @Override
    public void setUp() {
        super.setUp();

        // ── Unauthenticated client — no token ─────────────────────────────────
        unauthCourseApi    = new CourseApi(unauthenticatedHttp);
        unauthEnrolmentApi = new EnrolmentApi(
                unauthenticatedHttp,
                ConfigManager.get("student.username")
        );

        // ── Invalid token client ──────────────────────────────────────────────
        RequestHelper invalidHttp = buildHttpClient("invalid.token.here");
        invalidCourseApi    = new CourseApi(invalidHttp);
        invalidEnrolmentApi = new EnrolmentApi(
                invalidHttp,
                ConfigManager.get("student.username")
        );

        // ── Create a test course to use as target for security tests ──────────
        AuthApi authApi        = new AuthApi(unauthenticatedHttp);
        String instructorToken = authApi.loginAsInstructor(
                ConfigManager.get("instructor.username"),
                ConfigManager.get("instructor.password")
        );
        RequestHelper instructorHttp      = buildHttpClient(instructorToken);
        CourseApi     instructorCourseApi = new CourseApi(instructorHttp);

        String courseCode = "SEC_" + Instant.now().toEpochMilli();

        Course course = Course.create(
                "Security Test Course",
                ConfigManager.get("instructor.username"),
                courseCode,
                "Testing",
                5,
                "2026-03-01",
                "2026-06-01"
        );

        // Store response to extract both _id and courseCode
        APIResponse response = instructorCourseApi.create(course);
        unauthenticatedHttp.assertStatus(response, 201);

        JsonObject body      = unauthenticatedHttp.parseToJson(response);
        JsonObject newCourse = body.getAsJsonObject("newCourse");

        existingCourseId   = newCourse.get("_id").getAsString();
        existingCourseCode = newCourse.get("courseCode").getAsString();

        System.out.println("Security test setup complete."
                + " courseCode: " + existingCourseCode
                + " | _id: " + existingCourseId);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 401 — NO TOKEN PROVIDED
    // ══════════════════════════════════════════════════════════════════════════

    private Stream<Arguments> noTokenRequests() {
        return Stream.of(
                Arguments.of(
                        "POST /courses — no token",
                        (Supplier<APIResponse>) () ->
                                unauthCourseApi.create(Course.create(
                                        "Unauth Course",
                                        ConfigManager.get("instructor.username"),
                                        "UNAUTH_" + Instant.now().toEpochMilli(),
                                        "Testing", 10, "2026-03-01", "2026-06-01"))
                ),
                Arguments.of(
                        "PUT /courses/{id} — no token",
                        (Supplier<APIResponse>) () ->
                                unauthCourseApi.update(existingCourseId,
                                        CourseUpdateRequest.create(
                                                "Updated", 5, 5, "2026-04-08"))
                ),
                Arguments.of(
                        "DELETE /courses/{id} — no token",
                        (Supplier<APIResponse>) () ->
                                unauthCourseApi.delete(existingCourseId)
                ),
                Arguments.of(
                        "POST /enrolments/enrol — no token",
                        (Supplier<APIResponse>) () ->
                                unauthEnrolmentApi.enrol(existingCourseCode)
                ),
                Arguments.of(
                        "POST /enrolments/drop — no token",
                        (Supplier<APIResponse>) () ->
                                unauthEnrolmentApi.drop(existingCourseCode)
                ),
                Arguments.of(
                        "POST /enrolments/history — no token",
                        (Supplier<APIResponse>) () ->
                                unauthEnrolmentApi.getHistory()
                ),
                Arguments.of(
                        "POST /enrolments/active — no token",
                        (Supplier<APIResponse>) () ->
                                unauthEnrolmentApi.getActiveEnrolments()
                )
        );
    }

    @ParameterizedTest(name = "{0} should return 401")
    @MethodSource("noTokenRequests")
    @DisplayName("Requests without token should return 401 Unauthorised")
    @Story("401 — No Token Provided")
    @Severity(SeverityLevel.CRITICAL)
    void request_noToken_returns401(
            String description,
            Supplier<APIResponse> request) {

        APIResponse response = request.get();

        AssertionHelper.assertUnauthorised(response);

        System.out.println("401 PASSED — " + description);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 403 — INVALID TOKEN
    // ══════════════════════════════════════════════════════════════════════════

    private Stream<Arguments> invalidTokenRequests() {
        return Stream.of(
                Arguments.of(
                        "POST /courses — invalid token",
                        (Supplier<APIResponse>) () ->
                                invalidCourseApi.create(Course.create(
                                        "Invalid Token Course",
                                        ConfigManager.get("instructor.username"),
                                        "INVTOK_" + Instant.now().toEpochMilli(),
                                        "Testing", 10, "2026-03-01", "2026-06-01"))
                ),
                Arguments.of(
                        "PUT /courses/{id} — invalid token",
                        (Supplier<APIResponse>) () ->
                                invalidCourseApi.update(existingCourseId,
                                        CourseUpdateRequest.create(
                                                "Updated", 5, 5, "2026-04-08"))
                ),
                Arguments.of(
                        "DELETE /courses/{id} — invalid token",
                        (Supplier<APIResponse>) () ->
                                invalidCourseApi.delete(existingCourseId)
                ),
                Arguments.of(
                        "POST /enrolments/enrol — invalid token",
                        (Supplier<APIResponse>) () ->
                                invalidEnrolmentApi.enrol(existingCourseCode)
                ),
                Arguments.of(
                        "POST /enrolments/drop — invalid token",
                        (Supplier<APIResponse>) () ->
                                invalidEnrolmentApi.drop(existingCourseCode)
                ),
                Arguments.of(
                        "POST /enrolments/history — invalid token",
                        (Supplier<APIResponse>) () ->
                                invalidEnrolmentApi.getHistory()
                ),
                Arguments.of(
                        "POST /enrolments/active — invalid token",
                        (Supplier<APIResponse>) () ->
                                invalidEnrolmentApi.getActiveEnrolments()
                )
        );
    }

    @ParameterizedTest(name = "{0} should return 403")
    @MethodSource("invalidTokenRequests")
    @DisplayName("Requests with invalid token should return 403 Forbidden")
    @Story("403 — Invalid Token")
    @Severity(SeverityLevel.CRITICAL)
    void request_invalidToken_returns403(
            String description,
            Supplier<APIResponse> request) {

        APIResponse response = request.get();

        AssertionHelper.assertForbidden(response);

        System.out.println("403 PASSED — " + description);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 404 — RESOURCE NOT FOUND
    // ══════════════════════════════════════════════════════════════════════════

    private Stream<Arguments> notFoundRequests() {
        AuthApi authApi        = new AuthApi(unauthenticatedHttp);
        String instructorToken = authApi.loginAsInstructor(
                ConfigManager.get("instructor.username"),
                ConfigManager.get("instructor.password")
        );
        RequestHelper instructorHttp      = buildHttpClient(instructorToken);
        CourseApi     instructorCourseApi = new CourseApi(instructorHttp);

        String studentToken  = authApi.loginAsStudent(
                ConfigManager.get("student.username"),
                ConfigManager.get("student.password")
        );
        RequestHelper studentHttp     = buildHttpClient(studentToken);
        EnrolmentApi  studentEnrolApi = new EnrolmentApi(
                studentHttp,
                ConfigManager.get("student.username")
        );

        String invalidCourseId   = ConfigManager.get("invalid.course.id");
        String invalidCourseCode = ConfigManager.get("invalid.course.code");

        return Stream.of(
                Arguments.of(
                        "PUT /courses/{id} — non-existent ID",
                        (Supplier<APIResponse>) () ->
                                instructorCourseApi.update(invalidCourseId,
                                        CourseUpdateRequest.create(
                                                "Updated", 5, 5, "2026-04-08"))
                ),
                Arguments.of(
                        "DELETE /courses/{id} — non-existent ID",
                        (Supplier<APIResponse>) () ->
                                instructorCourseApi.delete(invalidCourseId)
                ),
                Arguments.of(
                        "GET /courses/availability — non-existent code",
                        (Supplier<APIResponse>) () ->
                                unauthCourseApi.checkAvailability(invalidCourseCode)
                ),
                Arguments.of(
                        "POST /enrolments/enrol — non-existent course",
                        (Supplier<APIResponse>) () ->
                                studentEnrolApi.enrol(invalidCourseCode)
                ),
                Arguments.of(
                        "POST /enrolments/drop — no active enrolment",
                        (Supplier<APIResponse>) () ->
                                studentEnrolApi.drop(invalidCourseCode)
                )
        );
    }

    @ParameterizedTest(name = "{0} should return 404")
    @MethodSource("notFoundRequests")
    @DisplayName("Requests for non-existent resources should return 404 Not Found")
    @Story("404 — Resource Not Found")
    @Severity(SeverityLevel.NORMAL)
    void request_nonExistentResource_returns404(
            String description,
            Supplier<APIResponse> request) {

        APIResponse response = request.get();

        AssertionHelper.assertNotFound(response);

        System.out.println("404 PASSED — " + description);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 400 — VALIDATION ERRORS
    // ══════════════════════════════════════════════════════════════════════════

    private Stream<Arguments> invalidCourseRequests() {
        AuthApi authApi        = new AuthApi(unauthenticatedHttp);
        String instructorToken = authApi.loginAsInstructor(
                ConfigManager.get("instructor.username"),
                ConfigManager.get("instructor.password")
        );
        RequestHelper instructorHttp      = buildHttpClient(instructorToken);
        CourseApi     instructorCourseApi = new CourseApi(instructorHttp);

        String duplicateCode = existingCourseCode != null
                ? existingCourseCode
                : ConfigManager.get("invalid.course.code");

        return Stream.of(
                Arguments.of(
                        "missing required fields",
                        (Supplier<APIResponse>) () ->
                                instructorCourseApi.create(new Course())
                ),
                Arguments.of(
                        "duplicate course code",
                        (Supplier<APIResponse>) () ->
                                instructorCourseApi.create(Course.create(
                                        "Duplicate",
                                        ConfigManager.get("instructor.username"),
                                        duplicateCode,
                                        "Testing", 10,
                                        "2026-03-01", "2026-06-01"
                                ))
                )
        );
    }

    @ParameterizedTest(name = "POST /courses — {0} should return 400")
    @MethodSource("invalidCourseRequests")
    @DisplayName("POST /courses — invalid data should return 400 Bad Request")
    @Story("400 — Validation Errors")
    @Severity(SeverityLevel.NORMAL)
    void createCourse_invalidData_returns400(
            String description,
            Supplier<APIResponse> request) {

        APIResponse response = request.get();

        AssertionHelper.assertBadRequest(response);
        AssertionHelper.assertErrorMessagePresent(response);

        System.out.println("400 PASSED — " + description);
    }
}
