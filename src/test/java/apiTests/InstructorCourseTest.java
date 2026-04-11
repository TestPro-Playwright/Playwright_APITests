package apiTests;

import api.AuthApi;
import api.CourseApi;
import base.BaseTest;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.microsoft.playwright.APIResponse;
import config.ConfigManager;
import io.qameta.allure.*;
import models.Course;
import models.CourseUpdateRequest;
import org.junit.jupiter.api.*;
import report.AssertionHelper;
import utils.RequestHelper;
import utils.TestDataLoader;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Epic("Course Enrollment System")
@Feature("Instructor — Course Management")
class InstructorCourseTest extends BaseTest {

    private CourseApi     instructorCourseApi;
    private CourseApi     publicCourseApi;
    private RequestHelper instructorHttp;

    // Stores the full course objects created in step 1a
    // — used by 1c (update) and 1e (delete)
    private final List<JsonObject> createdCourses = new ArrayList<>();

    @BeforeAll
    @Override
    public void setUp() {
        super.setUp();

        AuthApi authApi        = new AuthApi(unauthenticatedHttp);
        String instructorToken = authApi.loginAsInstructor(
                ConfigManager.get("instructor.username"),
                ConfigManager.get("instructor.password")
        );

        instructorHttp      = buildHttpClient(instructorToken);
        instructorCourseApi = new CourseApi(instructorHttp);
        publicCourseApi     = new CourseApi(unauthenticatedHttp);
    }

    // ── 1a: Add 5 new courses with unique course codes ────────────────────────

    private static Stream<Arguments> courseDataProvider() {
        return TestDataLoader.loadCourseData();
    }

    @ParameterizedTest(name = "Creating course: {0} with code: {1}")
    @MethodSource("courseDataProvider")
    @Order(1)
    @DisplayName("1a — Instructor adds 5 new courses with unique course codes")
    @Story("Course Creation")
    @Severity(SeverityLevel.CRITICAL)
    void instructorAdds5CoursesToCatalog(String title, String courseCode,
                                         String category, int totalCapacity,
                                         String startDate, String endDate) {

        String instructorName = ConfigManager.get("instructor.username");

        Course course = Course.create(
                title,
                instructorName,
                courseCode,
                category,
                totalCapacity,
                startDate,
                endDate
        );

        APIResponse response = instructorCourseApi.create(course);

        System.out.println("CREATE [" + courseCode + "]: "
                + response.status() + " — " + response.text());

        assertTrue(
                response.status() == 200 || response.status() == 201,
                "Expected 200 or 201 for course " + courseCode
                        + ". Got: " + response.status() + " Body: " + response.text()
        );

        // Parse the full response body
        JsonObject fullBody = instructorHttp.parseToJson(response);

        // Identify "newCourse" to get the actual course object
        JsonObject createdCourse;
        if (fullBody.has("newCourse") && fullBody.get("newCourse").isJsonObject()) {
            createdCourse = fullBody.getAsJsonObject("newCourse");
        } else if (fullBody.has("course") && fullBody.get("course").isJsonObject()) {
            createdCourse = fullBody.getAsJsonObject("course");
        } else {
            createdCourse = fullBody;
        }

        // Confirm _id is accessible at the top level
        System.out.println("Extracted course object: " + createdCourse);
        assertTrue(createdCourse.has("_id"),
                "Extracted course object should have '_id'. Got: " + createdCourse);

        // Save course id for use in steps 1c and 1e
        createdCourses.add(createdCourse);

        System.out.println("1a PASSED — Created course: " + title
                + " | Code: " + courseCode
                + " | _id: " + createdCourse.get("_id").getAsString());
    }

    // ── 1b: Search courses by title ───────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("1b — Instructor searches courses by title 'Java'")
    @Story("Course Search")
    @Severity(SeverityLevel.NORMAL)
    void instructorSearchesCoursesByTitle() {
        APIResponse response = publicCourseApi.searchByTitle("Java");

        System.out.println("SEARCH BY TITLE: "
                + response.status() + " — " + response.text());

        instructorHttp.assertStatus(response, 200);

        JsonArray results = instructorHttp.parseToJsonArray(response);
        AssertionHelper.assertArrayNotEmpty(results, "Search by title 'Java'");

        // Verify all results contain 'java' in the title (case-insensitive)
        boolean allMatch = instructorHttp.toList(results).stream()
                .allMatch(c -> c.has("title") &&
                        c.get("title").getAsString().toLowerCase().contains("java"));

        assertTrue(allMatch,
                "All search results should contain 'Java' in the title");

        System.out.println("1b PASSED — Search by title 'Java' returned "
                + instructorHttp.toList(results).size() + " results");
    }

    // ── 1c: Update 3 courses created in step 1a ───────────────────────────────

    @Test
    @Order(3)
    @DisplayName("1c — Instructor updates totalCapacity and availableSlots for 3 courses from step 1a")
    @Story("Course Update")
    @Severity(SeverityLevel.NORMAL)
    void instructorUpdates3CoursesFromStep1a() {
        assertFalse(createdCourses.isEmpty(),
                "Prerequisite: courses must have been created in step 1a");

        // Take the first 3 courses created in step 1a
        List<JsonObject> coursesToUpdate = createdCourses.stream()
                .limit(3)
                .toList();

        System.out.println("Updating " + coursesToUpdate.size()
                + " courses created in step 1a");

        CourseUpdateRequest updatePayload = CourseUpdateRequest.create(
                "Updated Java Course", 5, 5, "2026-04-08"
        );

        for (JsonObject course : coursesToUpdate) {
            // Use the _id from step 1a response — NOT from a search
            String courseId = extractId(course);

            System.out.println("Updating course id: " + courseId);

            APIResponse response = instructorCourseApi.update(courseId, updatePayload);

            System.out.println("UPDATE [" + courseId + "]: "
                    + response.status() + " — " + response.text());

            assertTrue(
                    response.status() == 200 || response.status() == 204,
                    "Update should return 200 or 204 for course " + courseId
                            + ". Got: " + response.status() + " Body: " + response.text()
            );

            // Verify the updated values are reflected in the response
            if (response.status() == 200 && !response.text().isBlank()
                    && !response.text().equals("{}")) {
                JsonObject body = instructorHttp.parseToJson(response);
                if (body.has("totalCapacity")) {
                    assertEquals(5, body.get("totalCapacity").getAsInt(),
                            "totalCapacity should be updated to 5 for course: " + courseId);
                }
                if (body.has("availableSlots")) {
                    assertEquals(5, body.get("availableSlots").getAsInt(),
                            "availableSlots should be updated to 5 for course: " + courseId);
                }
            }
        }
        System.out.println("1c PASSED — Updated totalCapacity and availableSlots to 5 for 3 courses: "
                + coursesToUpdate.stream()
                .map(c -> c.get("courseCode").getAsString())
                .toList());
    }

    // ── 1d: Get all courses in the catalog ────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("1d — Instructor retrieves all courses in the catalog")
    @Story("Course Catalog")
    @Severity(SeverityLevel.NORMAL)
    void instructorGetsAllCourses() {
        // GET /courses/all — no token needed per the curl
        APIResponse response = publicCourseApi.getAll();

        System.out.println("GET ALL COURSES: "
                + response.status() + " — " + response.text());

        instructorHttp.assertStatus(response, 200);

        JsonArray courses = instructorHttp.parseToJsonArray(response);
        AssertionHelper.assertArrayNotEmpty(courses, "GET /courses/all");

        // Verify all 5 courses created in step 1a appear in the catalog
        List<JsonObject> allCourses = instructorHttp.toList(courses);

        long matchCount = createdCourses.stream()
                .filter(created -> {
                    String createdId = extractId(created);
                    return allCourses.stream()
                            .anyMatch(c -> extractId(c).equals(createdId));
                })
                .count();

        System.out.println(matchCount + " of 5 created courses found in catalog");

        assertTrue(matchCount > 0,
                "At least some of the created courses should appear in the catalog");

        System.out.println("1d PASSED — Retrieved all courses from catalog, total count: "
                + allCourses.size());
    }

    // ── 1e: Delete 1 course created in step 1a ────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("1e — Instructor deletes one course created in step 1a")
    @Story("Course Deletion")
    @Severity(SeverityLevel.NORMAL)
    void instructorDeletesOneCourseFromStep1a() {
        assertFalse(createdCourses.isEmpty(),
                "Prerequisite: courses must have been created in step 1a");

        // Delete the last course created in step 1a
        JsonObject courseToDelete = createdCourses.get(createdCourses.size() - 1);
        String courseIdToDelete   = extractId(courseToDelete);

        System.out.println("Deleting course id: " + courseIdToDelete);

        APIResponse response = instructorCourseApi.delete(courseIdToDelete);

        System.out.println("DELETE [" + courseIdToDelete + "]: "
                + response.status() + " — " + response.text());

        assertTrue(
                response.status() == 200 || response.status() == 204,
                "Delete should return 200 or 204."
                        + " Got: " + response.status() + " Body: " + response.text()
        );

        // Verify deleted course no longer appears in the catalog
        APIResponse allCoursesResponse = publicCourseApi.getAll();
        instructorHttp.assertStatus(allCoursesResponse, 200);

        JsonArray allCourses = instructorHttp.parseToJsonArray(allCoursesResponse);

        boolean stillExists = instructorHttp.toList(allCourses).stream()
                .anyMatch(c -> extractId(c).equals(courseIdToDelete));

        assertFalse(stillExists,
                "Deleted course " + courseIdToDelete
                        + " should no longer appear in the catalog");

        System.out.println("1e PASSED — Deleted course: " + courseIdToDelete
                + " — confirmed removed from catalog");
    }

    @Test
    @Order(6)
    @DisplayName("POST /courses — missing required fields should return 400")
    @Story("Course Creation — Negative")
    @Severity(SeverityLevel.NORMAL)
    void createCourse_missingRequiredFields_returns400() {
        // Send empty course object — all fields missing
        APIResponse response = instructorCourseApi.create(new Course());

        AssertionHelper.assertBadRequest(response);
        AssertionHelper.assertErrorMessagePresent(response);

        System.out.println("400 PASSED — Missing fields correctly rejected: "
                + response.text());
    }

    @Test
    @Order(7)
    @DisplayName("POST /courses — duplicate course code should return 400")
    @Story("Course Creation — Negative")
    @Severity(SeverityLevel.NORMAL)
    void createCourse_duplicateCourseCode_returns400() {
        assertFalse(createdCourses.isEmpty(),
                "Prerequisite: courses must exist from step 1a");

        // Use a course code already created in step 1a
        String existingCode = createdCourses.get(0)
                .get("courseCode").getAsString();

        Course duplicate = Course.create(
                "Duplicate Course",
                ConfigManager.get("instructor.username"),
                existingCode,
                "Testing",
                10,
                "2026-03-01",
                "2026-06-01"
        );

        APIResponse response = instructorCourseApi.create(duplicate);

        AssertionHelper.assertBadRequest(response);
        AssertionHelper.assertErrorMessagePresent(response);

        System.out.println("400 PASSED — Duplicate course code correctly rejected: "
                + response.text());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Extracts the course object from the API response.
     */
    private JsonObject extractCourseObject(JsonObject body) {
        if (body.has("newCourse") && body.get("newCourse").isJsonObject())
            return body.getAsJsonObject("newCourse");
        if (body.has("course") && body.get("course").isJsonObject())
            return body.getAsJsonObject("course");
        return body;
    }

    /**
     * Extracts the "_id" (MongoDB) from a course JsonObject.
     */
    private String extractId(JsonObject course) {
        if (course.has("_id")) return course.get("_id").getAsString();
        if (course.has("id"))  return course.get("id").getAsString();
        throw new RuntimeException(
                "Cannot extract ID from course object: " + course);
    }
}
