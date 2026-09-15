package lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * Real HTTP helpers for tests. Fixture state and the cache are reset before each test.
 * Extend this class for your own tests.
 */
abstract class ReviewHttp {
    @Value("${local.server.port}")
    int port;

    @Autowired
    DocumentStore store;

    @Autowired
    PreviewService previews;

    @Autowired
    ExportService exports;

    HttpClient client;

    @BeforeEach
    void reset() {
        store.reset();
        previews.clear();
        exports.clear();
        client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    }

    @AfterEach
    void stop() {
        client.close();
    }

    HttpResponse<String> get(String user, String path) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
        if (!user.isEmpty()) {
            String credentials = Base64.getEncoder().encodeToString((user + ":local-only").getBytes(StandardCharsets.UTF_8));
            builder.header("Authorization", "Basic " + credentials);
        }
        var response = client.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString());
        System.out.println(user + " GET " + path + " -> " + response.statusCode() + " body=" + response.body()
                + " bodyLoads=" + store.bodyLoads.get());
        return response;
    }

    /** Authenticated POST with the session's CSRF token, as the application requires. */
    HttpResponse<String> post(String user, String path) throws Exception {
        var csrf = get(user, "/csrf");
        var matcher = Pattern.compile("\"token\":\"([^\"]+)\"").matcher(csrf.body());
        String token = matcher.find() ? matcher.group(1) : "";
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("X-CSRF-TOKEN", token);
        if (!user.isEmpty()) {
            String credentials = Base64.getEncoder().encodeToString((user + ":local-only").getBytes(StandardCharsets.UTF_8));
            builder.header("Authorization", "Basic " + credentials);
        }
        var response = client.send(builder.POST(HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
        System.out.println(user + " POST " + path + " -> " + response.statusCode() + " body=" + response.body());
        return response;
    }

    HttpResponse<String> get(String user, String id, String route, String suffix) throws Exception {
        return get(user, "/api/documents/" + id + "/" + route + suffix);
    }

    HttpResponse<String> preview(String user, String id) throws Exception {
        return get(user, id, "preview", "");
    }

    HttpResponse<String> download(String user, String id) throws Exception {
        return get(user, id, "download", "");
    }

    String export(String user, String id) throws Exception {
        var response = post(user, "/api/documents/" + id + "/export");
        assertEquals(200, response.statusCode());
        return response.body();
    }

    HttpResponse<String> fetchExport(String user, String exportId) throws Exception {
        return get(user, "/api/exports/" + exportId);
    }

    /** 404 with no private content of any tenant in the body. */
    void noContent(HttpResponse<String> response) {
        assertEquals(404, response.statusCode());
        assertFalse(response.body().contains("FULL-INVOICE"));
        assertFalse(response.body().contains("PRIVATE-SUMMARY"));
        assertFalse(response.body().contains("-CONTENT"));
    }
}
