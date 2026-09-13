package lab;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

abstract class TokenHttp {
    @Value("${local.server.port}") int port;
    @Autowired TokenFixtures tokens;
    private final HttpClient client = HttpClient.newHttpClient();

    HttpResponse<String> read(String kind, String invoice) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/invoices/" + invoice));
        if (kind != null) request.header("Authorization", "Bearer " + tokens.issue(kind));
        var response = client.send(request.GET().build(), HttpResponse.BodyHandlers.ofString());
        System.out.printf("fixture=%s invoice=%s status=%d body=%s%n", kind, invoice, response.statusCode(), response.body());
        return response;
    }
}
