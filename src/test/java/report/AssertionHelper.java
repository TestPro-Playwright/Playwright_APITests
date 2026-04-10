package report;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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

    public static void assertIdPresent(JsonObject body) {
        boolean hasId = body.has("id") || body.has("_id");
        assertTrue(hasId,
                "Response should contain an 'id' or '_id' field. Body: " + body);
    }

    public static void assertTokenPresent(JsonObject body) {
        boolean hasToken = body.has("token") || body.has("accessToken");
        assertTrue(hasToken,
                "Login response should contain a token. Body: " + body);
    }

    public static void assertArrayNotEmpty(JsonArray array, String context) {
        assertNotNull(array, context + " — response array should not be null");
        assertTrue(array.size() > 0,
                context + " — expected non-empty array but got empty");
    }

    public static void assertCourseAvailable(JsonObject body) {
        assertTrue(body.has("available"),
                "Availability response missing 'available' field. Body: " + body);
        assertTrue(body.get("available").getAsBoolean(),
                "Course should be available but was reported as unavailable");
    }
}
