package api;

import com.google.gson.JsonObject;
import com.microsoft.playwright.APIResponse;
import models.Course;
import models.CourseUpdateRequest;
import utils.RequestHelper;

import java.util.List;

/**
 * Encapsulates all /courses endpoint interactions.
 * Token is carried in the Authorization header via the HTTP context —
 * never passed manually here.
 */
public class CourseApi {

    private final RequestHelper http;

    public CourseApi(RequestHelper http) {
        this.http = http;
    }

    // ── POST /courses — instructor creates a course ───────────────────────────
    public APIResponse create(Course course) {
        return http.post("/courses", course);
    }

    // ── GET /courses/title/{title} — search by title ──────────────────────────
    public APIResponse searchByTitle(String title) {
        return http.get("/courses/title/" + title);
    }

    // ── GET /courses/instructor/{instructor} — search by instructor ───────────
    public APIResponse searchByInstructor(String instructor) {
        return http.get("/courses/instructor/" + instructor);
    }

    // ── GET /courses/availability/{courseCode} — check availability ───────────
    public APIResponse checkAvailability(String courseCode) {
        return http.get("/courses/availability/" + courseCode);
    }

    // ── GET /courses/all — get all courses (no token needed) ──────────────────
    public APIResponse getAll() {
        return http.get("/courses/all");
    }

    // ── PUT /courses/{id} — update course ─────────────────────────────────────
    public APIResponse update(String courseId, CourseUpdateRequest updateRequest) {
        return http.put("/courses/" + courseId, updateRequest);
    }

    // ── DELETE /courses/{id} — delete course ──────────────────────────────────
    public APIResponse delete(String courseId) {
        return http.delete("/courses/" + courseId);
    }

    // ── Parse all courses as list ────────────────────────────────
    public List<JsonObject> getAllAsList() {
        APIResponse response = getAll();
        http.assertStatus(response, 200);
        return http.toList(http.parseToJsonArray(response));
    }

    // ── Create course and return its ID ──────────────────────────
    public String createAndGetId(Course course) {
        APIResponse response = create(course);
        System.out.println("CREATE COURSE RESPONSE: " + response.text());
        http.assertStatus(response, 201);
        return extractId(http.parseToJson(response));
    }

    // ── Search by title and return list ──────────────────────────
    public List<JsonObject> searchByTitleAsList(String title) {
        APIResponse response = searchByTitle(title);
        http.assertStatus(response, 200);
        System.out.println("SEARCH BY TITLE RESPONSE: " + response.text());
        return http.toList(http.parseToJsonArray(response));
    }

    private String extractId(JsonObject body) {
        // Handle { "newCourse": { "_id": "..." } } — create response shape
        if (body.has("newCourse") && body.get("newCourse").isJsonObject()) {
            JsonObject newCourse = body.getAsJsonObject("newCourse");
            if (newCourse.has("_id")) return newCourse.get("_id").getAsString();
            if (newCourse.has("id"))  return newCourse.get("id").getAsString();
        }
        // Handle { "course": { "_id": "..." } }
        if (body.has("course") && body.get("course").isJsonObject()) {
            JsonObject course = body.getAsJsonObject("course");
            if (course.has("_id")) return course.get("_id").getAsString();
            if (course.has("id"))  return course.get("id").getAsString();
        }
        // Handle flat { "_id": "..." }
        if (body.has("_id")) return body.get("_id").getAsString();
        if (body.has("id"))  return body.get("id").getAsString();

        throw new RuntimeException(
                "Could not extract course ID from response: " + body);
    }
}
