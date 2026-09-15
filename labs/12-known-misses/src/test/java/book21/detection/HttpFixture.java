package book21.detection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.test.web.server.LocalServerPort;
import static org.junit.jupiter.api.Assertions.*;
abstract class HttpFixture {
    @LocalServerPort int port;
    static final String ATTACK = "' OR 1=1 -- ";
    String read(String mode, String title) throws Exception {
        var uri = URI.create("http://127.0.0.1:" + port + "/search/" + mode + "?title=" + URLEncoder.encode(title, StandardCharsets.UTF_8));
        var req = HttpRequest.newBuilder(uri).GET().build();
        var res = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), res.body());
        return res.body();
    }
}
