package report;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.microsoft.playwright.APIResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Domain-specific assertions.
 * Failure messages are written so a stakeholder understands what broke.
 */
public class AssertionHelper {

    public static void assertFieldEquals(JsonObject body, String field, String expected) {
        assertTrue(body.has(field),
                "Response is missing expected field '" + field + "'. Body: " + body);
        assertEquals(expected, body.get(field).getAsString(),
                "Field '" + field + "' did not match expected value");
    }

    public static void assertFieldNotBlank(JsonObject body, String field) {
        assertTrue(body.has(field),
                "Response is missing field '" + field + "'. Body: " + body);
        assertFalse(body.get(field).getAsString().isBlank(),
                "Field '" + field + "' should not be blank. Body: " + body);
    }

    public static void assertArrayNotEmpty(JsonArray array, String context) {
        assertNotNull(array, context + " — response array should not be null");
        assertTrue(array.size() > 0,
                context + " — expected non-empty array but got empty");
    }

    public static void assertCourseAvailable(JsonObject body) {
        if (body.has("available")) {
            assertTrue(body.get("available").getAsBoolean(),
                    "Course should be available but was reported unavailable. Body: "
                            + body);
            return;
        }

        if (body.has("availableSlots")) {
            assertTrue(body.get("availableSlots").getAsInt() > 0,
                    "Course should have available slots but availableSlots was 0. Body: "
                            + body);
            return;
        }

        if (body.has("totalCapacity")) {
            assertTrue(body.get("totalCapacity").getAsInt() > 0,
                    "Course should have capacity but totalCapacity was 0. Body: "
                            + body);
            return;
        }

        fail("Availability response missing expected fields "
                + "('available', 'availableSlots' or 'totalCapacity'). Body: " + body);
    }

    public static void assertBadRequest(APIResponse response) {
        assertEquals(400, response.status(),
                "Expected 400 Bad Request. Got: " + response.status()
                        + " Body: " + response.text());
    }

    public static void assertUnauthorised(APIResponse response) {
        assertEquals(401, response.status(),
                "Expected 401 Unauthorised — no token provided. Got: "
                        + response.status() + " Body: " + response.text());
    }

    public static void assertForbidden(APIResponse response) {
        assertEquals(403, response.status(),
                "Expected 403 Forbidden — invalid or wrong token. Got: "
                        + response.status() + " Body: " + response.text());
    }

    public static void assertNotFound(APIResponse response) {
        assertEquals(404, response.status(),
                "Expected 404 Not Found. Got: " + response.status()
                        + " Body: " + response.text());
    }

    public static void assertErrorMessagePresent(APIResponse response) {
        try {
            JsonObject body = com.google.gson.JsonParser
                    .parseString(response.text()).getAsJsonObject();
            assertTrue(
                    body.has("error") || body.has("message"),
                    "Error response should contain 'error' or 'message' field. Body: "
                            + response.text());
        } catch (Exception e) {
            fail("Response body is not valid JSON: " + response.text());
        }
    }
}
