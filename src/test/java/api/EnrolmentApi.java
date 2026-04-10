package api;

import com.microsoft.playwright.APIResponse;
import models.EnrolmentRequest;
import models.HistoryRequest;
import utils.RequestHelper;

/**
 * Encapsulates all /enrolments endpoint interactions.
 * Token is carried in the Authorization header via the HTTP context.
 * Username is passed in the request body as required by the API.
 */
public class EnrolmentApi {

    private final RequestHelper http;
    private final String        username;

    public EnrolmentApi(RequestHelper http, String username) {
        this.http     = http;
        this.username = username;
    }

    // ── POST /enrolments/enrol ────────────────────────────────────────────────
    public APIResponse enrol(String courseCode) {
        return http.post("/enrolments/enrol",
                new EnrolmentRequest(username, courseCode));
    }

    // ── POST /enrolments/history ──────────────────────────────────────────────
    public APIResponse getHistory() {
        return http.post("/enrolments/history",
                new HistoryRequest(username));
    }

    // ── POST /enrolments/active ───────────────────────────────────────────────
    public APIResponse getActiveEnrolments() {
        return http.post("/enrolments/active",
                new HistoryRequest(username));
    }

    // ── POST /enrolments/drop ─────────────────────────────────────────────────
    public APIResponse drop(String courseCode) {
        return http.post("/enrolments/drop",
                new EnrolmentRequest(username, courseCode));
    }
}
