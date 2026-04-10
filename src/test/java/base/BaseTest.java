package base;

import com.microsoft.playwright.*;
import config.ConfigManager;
import org.junit.jupiter.api.*;
import utils.RequestHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseTest {

    protected Playwright        playwright;
    protected RequestHelper     unauthenticatedHttp;

    // Registry — all HTTP clients created by subclasses are registered here
    // BaseTest.tearDown() disposes all of them automatically
    private final List<RequestHelper> httpClients = new ArrayList<>();

    @BeforeAll
    public void setUp() {
        playwright          = Playwright.create();
        unauthenticatedHttp = buildHttpClient(null);
    }

    /**
     * Builds an authenticated or unauthenticated HTTP client.
     * Every client created here is automatically registered for disposal.
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

        RequestHelper client = new RequestHelper(context);

        // Register client so tearDown can dispose it automatically
        httpClients.add(client);

        return client;
    }

    @AfterAll
    public void tearDown() {
        // Dispose all registered HTTP clients
        httpClients.forEach(client -> {
            try {
                client.dispose();
            } catch (Exception e) {
                System.err.println("Warning: failed to dispose HTTP client: "
                        + e.getMessage());
            }
        });
        httpClients.clear();

        // Close Playwright engine last
        try {
            if (playwright != null) {
                playwright.close();
            }
        } catch (Exception e) {
            System.err.println("Warning: failed to close Playwright: "
                    + e.getMessage());
        }
    }
}
