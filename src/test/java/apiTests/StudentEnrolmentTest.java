package apiTests;

import api.AuthApi;
import api.CourseApi;
import api.EnrolmentApi;
import base.BaseTest;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.microsoft.playwright.APIResponse;
import config.ConfigManager;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import report.AssertionHelper;
import utils.RequestHelper;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Epic("Course Enrollment System")
@Feature("Student — Enrolment Journey")
class StudentEnrolmentTest extends BaseTest {

    private CourseApi     publicCourseApi;
    private EnrolmentApi  enrolmentApi;
    private RequestHelper studentHttp;

    private String targetCourseCode;

    @BeforeAll
    @Override
    public void setUp() {
        super.setUp();

        AuthApi authApi     = new AuthApi(unauthenticatedHttp);
        String studentToken = authApi.loginAsStudent(
                ConfigManager.get("student.username"),
                ConfigManager.get("student.password")
        );

        studentHttp     = buildHttpClient(studentToken);
        enrolmentApi    = new EnrolmentApi(
                studentHttp,
                ConfigManager.get("student.username")
        );
        publicCourseApi = new CourseApi(unauthenticatedHttp);

        System.out.println("SETUP — Student logged in successfully: "
                + ConfigManager.get("student.username"));
    }

    // ── 2a: Search by instructor + check availability ─────────────────────────

    @Test
    @Order(1)
    @DisplayName("2a — Student searches courses by instructor and checks availability")
    @Story("Course Discovery")
    @Severity(SeverityLevel.NORMAL)
    void studentSearchesByInstructorAndChecksAvailability() {

        // Search by instructor
        APIResponse searchResponse = publicCourseApi
                .searchByInstructor(ConfigManager.get("instructor.username"));

        unauthenticatedHttp.assertStatus(searchResponse, 200);

        JsonArray results = unauthenticatedHttp.parseToJsonArray(searchResponse);
        AssertionHelper.assertArrayNotEmpty(results,
                "Search by instructor '" + ConfigManager.get("instructor.username") + "'");

        // Pick the first course from the results
        JsonObject firstCourse = results.get(0).getAsJsonObject();
        targetCourseCode = extractCourseCode(firstCourse);

        assertNotNull(targetCourseCode,
                "Should extract a courseCode from instructor search results");
        assertFalse(targetCourseCode.isBlank(),
                "Course code should not be blank");

        System.out.println("2a PASSED — Found " + results.size()
                + " courses for instructor: " + ConfigManager.get("instructor.username")
                + " | Selected course code: " + targetCourseCode);

        // Check availability for the selected course
        APIResponse availabilityResponse =
                publicCourseApi.checkAvailability(targetCourseCode);

        unauthenticatedHttp.assertStatus(availabilityResponse, 200);

        JsonObject availBody = unauthenticatedHttp.parseToJson(availabilityResponse);
        AssertionHelper.assertCourseAvailable(availBody);

        System.out.println("2a PASSED — Availability checked for course: "
                + targetCourseCode + " | Response: " + availBody);
    }

    // ── 2b: View enrolment history ────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("2b — Student views their enrolment history")
    @Story("Enrolment History")
    @Severity(SeverityLevel.NORMAL)
    void studentViewsEnrolmentHistory() {

        APIResponse response = enrolmentApi.getHistory();

        assertTrue(
                response.status() == 200 || response.status() == 204,
                "History endpoint should return 200 or 204."
                        + " Got: " + response.status() + " Body: " + response.text()
        );

        int historyCount = 0;
        if (response.status() == 200 && !response.text().isBlank()
                && !response.text().equals("{}")) {
            try {
                JsonArray history = unauthenticatedHttp.parseToJsonArray(response);
                historyCount = history.size();
            } catch (Exception e) {
                JsonObject history = unauthenticatedHttp.parseToJson(response);
                historyCount = history.entrySet().size();
            }
        }

        System.out.println("2b PASSED — Enrolment history retrieved for: "
                + ConfigManager.get("student.username")
                + " | Total enrolments: " + historyCount);
    }

    // ── 2c: Enrol in a course ─────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("2c — Student enrols in a course")
    @Story("Course Enrolment")
    @Severity(SeverityLevel.BLOCKER)
    void studentEnrolsInCourse() {
        assertNotNull(targetCourseCode,
                "Prerequisite: course code must be set from step 2a");

        APIResponse response = enrolmentApi.enrol(targetCourseCode);

        assertTrue(
                response.status() == 200 || response.status() == 201,
                "Enrolment should return 200 or 201."
                        + " Got: " + response.status() + " Body: " + response.text()
        );

        System.out.println("2c PASSED — Student: "
                + ConfigManager.get("student.username")
                + " successfully enrolled in course: " + targetCourseCode);
    }

    // ── 2d: View active enrolments ────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("2d — Student views their active enrolments")
    @Story("Active Enrolments")
    @Severity(SeverityLevel.NORMAL)
    void studentViewsActiveEnrolments() {

        APIResponse response = enrolmentApi.getActiveEnrolments();

        unauthenticatedHttp.assertStatus(response, 200);

        int activeCount = 0;
        boolean foundEnrolledCourse = false;

        try {
            JsonArray active = unauthenticatedHttp.parseToJsonArray(response);
            activeCount = active.size();

            foundEnrolledCourse = unauthenticatedHttp.toList(active)
                    .stream()
                    .anyMatch(e -> {
                        String code = extractCourseCode(e);
                        return code != null && code.equals(targetCourseCode);
                    });

        } catch (Exception e) {
            JsonObject body = unauthenticatedHttp.parseToJson(response);
            activeCount = body.entrySet().size();
        }

        assertTrue(foundEnrolledCourse,
                "Course " + targetCourseCode
                        + " should appear in active enrolments after enrolling");

        System.out.println("2d PASSED — Active enrolments retrieved for: "
                + ConfigManager.get("student.username")
                + " | Active count: " + activeCount
                + " | Course " + targetCourseCode + " found: " + foundEnrolledCourse);
    }

    // ── 2e: Drop out of the course ───────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("2e — Student drops out of the enrolled course")
    @Story("Course Drop")
    @Severity(SeverityLevel.NORMAL)
    void studentDropsCourse() {
        assertNotNull(targetCourseCode,
                "Prerequisite: course code must be set from step 2a");

        APIResponse dropResponse = enrolmentApi.drop(targetCourseCode);

        assertTrue(
                dropResponse.status() == 200 || dropResponse.status() == 204,
                "Drop should return 200 or 204."
                        + " Got: " + dropResponse.status() + " Body: " + dropResponse.text()
        );

        // Verify course no longer appears in active enrolments
        APIResponse activeResponse = enrolmentApi.getActiveEnrolments();
        unauthenticatedHttp.assertStatus(activeResponse, 200);

        boolean stillEnrolled = false;
        try {
            JsonArray active = unauthenticatedHttp.parseToJsonArray(activeResponse);
            stillEnrolled = unauthenticatedHttp.toList(active)
                    .stream()
                    .anyMatch(e -> {
                        String code = extractCourseCode(e);
                        return code != null && code.equals(targetCourseCode);
                    });
        } catch (Exception e) {
            System.out.println("Drop verification note: " + activeResponse.text());
        }

        assertFalse(stillEnrolled,
                "Course " + targetCourseCode
                        + " should no longer appear in active enrolments after dropping");

        System.out.println("2e PASSED — Student: "
                + ConfigManager.get("student.username")
                + " successfully dropped course: " + targetCourseCode
                + " | Still enrolled: " + stillEnrolled);
    }

    private String extractCourseCode(JsonObject course) {
        if (course.has("courseCode"))  return course.get("courseCode").getAsString();
        if (course.has("course_code")) return course.get("course_code").getAsString();
        if (course.has("code"))        return course.get("code").getAsString();
        return null;
    }
}
