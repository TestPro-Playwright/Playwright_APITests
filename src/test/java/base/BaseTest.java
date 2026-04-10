package base;

import com.microsoft.playwright.*;
import config.ConfigManager;
import org.junit.jupiter.api.*;
import utils.RequestHelper;

import java.util.HashMap;
import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseTest {

    protected Playwright    playwright;
    protected RequestHelper unauthenticatedHttp;

    @BeforeAll
    public void setUp() {
        playwright          = Playwright.create();
        unauthenticatedHttp = buildHttpClient(null);
    }

    /**
     * Builds an HTTP client.
     * Pass a token to get an authenticated client (Authorization: Bearer <token>).
     * Pass null to get an unauthenticated client for public endpoints.
     */
    protected RequestHelper buildHttpClient(String bearerToken) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept",       "application/json");

        if (bearerToken != null && !bearerToken.isBlank()) {
            headers.put("Authorization", "Bearer " + bearerToken);
        }

        APIRequestContext context = playwright.request().newContext(
                new APIRequest.NewContextOptions()
                        .setBaseURL(ConfigManager.get("base.url"))
                        .setExtraHTTPHeaders(headers)
                        .setTimeout(ConfigManager.getInt("timeout"))
        );
        return new RequestHelper(context);
    }

    @AfterAll
    public void tearDown() {
        if (playwright != null) playwright.close();
    }
}
