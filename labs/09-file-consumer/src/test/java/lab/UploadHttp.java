package lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
 * Real HTTP helpers for tests: an authenticated client with a session cookie and CSRF token.
 * Fixture counters are reset before each test. Extend this class for your own tests.
 */
abstract class UploadHttp {
    private static final Pattern TOKEN = Pattern.compile("\"token\":\"([^\"]+)\"");

    @Value("${local.server.port}")
    int port;

    @Autowired
    Fixture fixture;

    @Autowired
    InvoiceParser parser;

    HttpClient client;
    String token;

    @BeforeEach
    void start() throws Exception {
        client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        var response = client.send(request("/csrf", true).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        var matcher = TOKEN.matcher(response.body());
        assertTrue(matcher.find());
        token = matcher.group(1);
        fixture.httpHits.set(0);
        fixture.resolutions.set(0);
    }

    @AfterEach
    void stop() {
        client.close();
    }

    HttpRequest.Builder request(String path, boolean authenticated) {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
        if (authenticated) {
            String credentials = Base64.getEncoder().encodeToString("alice:local-only".getBytes(StandardCharsets.UTF_8));
            builder.header("Authorization", "Basic " + credentials);
        }
        return builder;
    }

    HttpResponse<String> post(String path, byte[] body, String type, boolean csrf, boolean authenticated)
            throws Exception {
        var builder = request(path, authenticated).header("Content-Type", type);
        if (csrf) {
            builder.header("X-CSRF-TOKEN", token);
        }
        var response = client.send(
                builder.POST(HttpRequest.BodyPublishers.ofByteArray(body)).build(),
                HttpResponse.BodyHandlers.ofString());
        log(path, response);
        return response;
    }

    HttpResponse<String> get(String path) throws Exception {
        var response = client.send(request(path, true).GET().build(), HttpResponse.BodyHandlers.ofString());
        log(path, response);
        return response;
    }

    String upload(byte[] bytes) throws Exception {
        var response = post("/api/uploads", bytes, "application/xml", true, true);
        assertEquals(201, response.statusCode());
        return response.body();
    }

    String upload(String xml) throws Exception {
        return upload(xml.getBytes(StandardCharsets.UTF_8));
    }

    HttpResponse<String> preview(String id) throws Exception {
        return post("/api/uploads/" + id + "/preview", new byte[0], "application/xml", true, true);
    }

    HttpResponse<String> statement(String id) throws Exception {
        return post("/api/uploads/" + id + "/statement", new byte[0], "application/xml", true, true);
    }

    HttpResponse<String> receipt(String id) throws Exception {
        return get("/api/uploads/" + id + "/receipt");
    }

    private void log(String path, HttpResponse<String> response) {
        System.out.println(path + " status=" + response.statusCode() + " httpHits=" + fixture.httpHits.get()
                + " resolutions=" + fixture.resolutions.get() + " body=" + response.body());
    }
}
