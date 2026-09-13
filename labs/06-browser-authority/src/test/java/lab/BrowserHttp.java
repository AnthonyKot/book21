package lab;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Value;
import static org.junit.jupiter.api.Assertions.*;
abstract class BrowserHttp {
    @Value("${local.server.port}") int port;
    HttpClient client;
    @BeforeEach void login() throws Exception {
        client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        String page = get("/login").body();
        var matcher = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(page);
        String token = matcher.find() ? matcher.group(1) : "";
        assertEquals(302, post("/login", "username=alice&password=local-only&_csrf=" + enc(token)).statusCode());
    }
    static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
    HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    HttpResponse<String> post(String path, String body) throws Exception {
        return post(path, body, "http://127.0.0.1:" + port);
    }
    HttpResponse<String> foreignPost(String path, String body) throws Exception {
        return post(path, body, "http://127.0.0.1:8084");
    }
    HttpResponse<String> post(String path, String body, String origin) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Origin", origin)
            .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    String token() throws Exception {
        var m = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]*)\"").matcher(get("/").body());
        assertTrue(m.find()); return enc(m.group(1));
    }
    void approved(boolean expected) throws Exception {
        assertTrue(get("/api/state").body().contains("\"approved\":" + expected));
    }
}
