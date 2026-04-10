package utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RequestHelper {

    private final APIRequestContext request;
    private final Gson gson = new Gson();

    public RequestHelper(APIRequestContext request) {
        this.request = request;
    }

    // ── HTTP Methods ──────────────────────────────────────────────────────────

    public APIResponse get(String endpoint) {
        return request.get(endpoint);
    }

    public APIResponse get(String endpoint, Map<String, String> params) {
        // Build query params using RequestOptions
        RequestOptions options = RequestOptions.create();
        params.forEach(options::setQueryParam);
        return request.get(endpoint, options);
    }

    public APIResponse post(String endpoint, Object body) {
        return request.post(endpoint,
                RequestOptions.create().setData(body));
    }

    public APIResponse put(String endpoint, Object body) {
        return request.put(endpoint,
                RequestOptions.create().setData(body));
    }

    public APIResponse patch(String endpoint, Object body) {
        return request.patch(endpoint,
                RequestOptions.create().setData(body));
    }

    public APIResponse delete(String endpoint) {
        return request.delete(endpoint);
    }

    // ── Parsing ───────────────────────────────────────────────────────────────

    public JsonObject parseToJson(APIResponse response) {
        return JsonParser.parseString(response.text()).getAsJsonObject();
    }

    public JsonArray parseToJsonArray(APIResponse response) {
        return JsonParser.parseString(response.text()).getAsJsonArray();
    }

    public <T> T parseResponse(APIResponse response, Class<T> clazz) {
        return gson.fromJson(response.text(), clazz);
    }

    // Java Streams support — filter/map over JSON arrays fluently
    public List<JsonObject> toList(JsonArray array) {
        return array.asList()
                .stream()
                .map(JsonElement::getAsJsonObject)
                .toList();
    }

    // ── Assertion ─────────────────────────────────────────────────────────────

    public void assertStatus(APIResponse response, int expectedStatus) {
        assertEquals(expectedStatus, response.status(),
                "Unexpected HTTP status. Response body: " + response.text());
    }
}
