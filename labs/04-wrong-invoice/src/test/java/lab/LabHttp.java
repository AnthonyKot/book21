package lab;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Real HTTP requests against the running app, authenticated as a lab user. */
abstract class LabHttp {
    @Value("${local.server.port}") int port;
    @Autowired ExportWorker worker;
    @Autowired JdbcClient jdbc;

    final HttpClient client = HttpClient.newHttpClient();

    HttpResponse<String> call(String user, String method, String path) throws Exception {
        String auth = Base64.getEncoder().encodeToString((user + ":" + user + "-pass").getBytes());
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Basic " + auth)
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.printf("  %-5s %-4s %-26s -> %d %s%n", user, method, path, response.statusCode(), response.body());
        return response;
    }

    long jobId(HttpResponse<String> accepted) {
        var m = java.util.regex.Pattern.compile("\"jobId\":(\\d+)").matcher(accepted.body());
        if (!m.find()) throw new AssertionError("no jobId in " + accepted.body());
        return Long.parseLong(m.group(1));
    }

    String statusOf(long jobId) {
        return jdbc.sql("SELECT status FROM export_job WHERE id = ?").param(jobId).query(String.class).single();
    }
}
