package lab;

import com.sun.net.httpserver.HttpServer;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Synthetic targets for the lab: a temporary text file and a loopback HTTP server.
 * The counters let tests observe external resolution and network contact directly.
 */
@Component
public class Fixture {
    static final String NOTE = "SERVER-ONLY-SYNTHETIC-NOTE";

    final AtomicInteger httpHits = new AtomicInteger();
    final AtomicInteger resolutions = new AtomicInteger();

    private final Path note;
    private final HttpServer server;

    public Fixture(@Value("${lab.fixture-port}") int port) throws IOException {
        note = Files.createTempFile("book21-note-", ".txt");
        Files.writeString(note, NOTE);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", exchange -> {
            httpHits.incrementAndGet();
            String body = exchange.getRequestURI().getPath().endsWith(".dtd")
                    ? "<!ENTITY note '" + NOTE + "'>"
                    : NOTE;
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (var out = exchange.getResponseBody()) {
                out.write(bytes);
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    public String fileUri() {
        return note.toUri().toString();
    }

    public String httpUri() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/note";
    }

    public String dtdUri() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/invoice.dtd";
    }

    /**
     * Lab safety guard, installed by every XML consumer in this project. It counts each request
     * to resolve an external reference and refuses anything other than the synthetic targets
     * above. It never decides whether a consumer should resolve references at all: returning
     * normally lets the XML processor open the permitted target itself.
     */
    void confine(String systemId) throws IOException {
        resolutions.incrementAndGet();
        if (!fileUri().equals(systemId) && !httpUri().equals(systemId) && !dtdUri().equals(systemId)) {
            throw new IOException("Lab safety: only synthetic fixture targets may be opened");
        }
    }

    @PreDestroy
    void stop() throws IOException {
        server.stop(0);
        Files.deleteIfExists(note);
    }
}
