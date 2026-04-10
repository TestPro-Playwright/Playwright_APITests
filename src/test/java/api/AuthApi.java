package api;

import com.google.gson.JsonObject;
import com.microsoft.playwright.APIResponse;
import models.LoginRequest;
import utils.RequestHelper;

public class AuthApi {

    private final RequestHelper http;

    public AuthApi(RequestHelper http) {
        this.http = http;
    }

    public String loginAsStudent(String username, String password) {
        return login("/student/login", username, password);
    }

    public String loginAsInstructor(String username, String password) {
        return login("/instructor/login", username, password);
    }

    private String login(String endpoint, String username, String password) {
        APIResponse response = http.post(endpoint,
                new LoginRequest(username, password));

        System.out.println("LOGIN RESPONSE [" + endpoint + "]: " + response.text());
        http.assertStatus(response, 200);

        JsonObject body = http.parseToJson(response);

        if (body.has("token"))       return body.get("token").getAsString();
        if (body.has("accessToken")) return body.get("accessToken").getAsString();
        if (body.has("jwt"))         return body.get("jwt").getAsString();
        if (body.has("data") && body.getAsJsonObject("data").has("token"))
            return body.getAsJsonObject("data").get("token").getAsString();

        throw new RuntimeException(
                "Login response missing token field. Body: " + body);
    }
}
