package lab;
import com.sun.net.httpserver.*;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Four loopback listeners used by the lab. All bind 127.0.0.1 and differ only by
 * port, so "internal" names a destination the policy forbids, not a network the
 * test client cannot reach.
 *   partner   an approved integration origin
 *   mirror    a second approved origin (used by the practice task)
 *   internal  a forbidden origin that counts the requests it receives
 * Each fixture records whether an inbound request carried an Authorization header
 * so a test can show the outbound request never copies the caller's credential.
 */
@Component
public class Destinations {
    private final HttpServer partner, mirror, internal;
    final AtomicInteger partnerHits = new AtomicInteger();
    final AtomicInteger mirrorHits = new AtomicInteger();
    final AtomicInteger internalHits = new AtomicInteger();
    final AtomicBoolean sawAuthorization = new AtomicBoolean();

    public Destinations(@Value("${lab.partner-port}") int partnerPort,
                        @Value("${lab.mirror-port}") int mirrorPort,
                        @Value("${lab.internal-port}") int internalPort) throws IOException {
        partner = HttpServer.create(new InetSocketAddress("127.0.0.1", partnerPort), 0);
        mirror = HttpServer.create(new InetSocketAddress("127.0.0.1", mirrorPort), 0);
        internal = HttpServer.create(new InetSocketAddress("127.0.0.1", internalPort), 0);

        internal.createContext("/", exchange -> {
            internalHits.incrementAndGet();
            noteAuthorization(exchange);
            reply(exchange, 200, "INTERNAL-SYNTHETIC-REPORT");
        });

        mirror.createContext("/", exchange -> {
            mirrorHits.incrementAndGet();
            noteAuthorization(exchange);
            switch (exchange.getRequestURI().getPath()) {
                case "/document" -> reply(exchange, 200, "PARTNER-INVOICE-C-1001");
                default -> reply(exchange, 503, "fixture unavailable");
            }
        });

        partner.createContext("/", exchange -> {
            partnerHits.incrementAndGet();
            noteAuthorization(exchange);
            switch (exchange.getRequestURI().getPath()) {
                case "/document" -> reply(exchange, 200, "PARTNER-INVOICE-C-1001");
                case "/to-internal" -> redirect(exchange, internalUrl());
                case "/to-mirror" -> redirect(exchange, mirrorUrl() + "/document");
                case "/to-document" -> redirect(exchange, "/document");
                case "/loop" -> redirect(exchange, "/loop");
                case "/chain" -> redirect(exchange, "/to-document");
                default -> reply(exchange, 503, "fixture unavailable");
            }
        });

        internal.start();
        mirror.start();
        partner.start();
    }

    public String partnerUrl() { return "http://127.0.0.1:" + partner.getAddress().getPort(); }
    public String mirrorUrl() { return "http://127.0.0.1:" + mirror.getAddress().getPort(); }
    public String internalUrl() { return "http://127.0.0.1:" + internal.getAddress().getPort() + "/report"; }

    private void noteAuthorization(HttpExchange exchange) {
        if (exchange.getRequestHeaders().containsKey("Authorization")) {
            sawAuthorization.set(true);
        }
    }

    private static void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

    private static void reply(HttpExchange exchange, int status, String text) throws IOException {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, body.length);
        try (var out = exchange.getResponseBody()) {
            out.write(body);
        } finally {
            exchange.close();
        }
    }

    @PreDestroy
    void stop() {
        partner.stop(0);
        mirror.stop(0);
        internal.stop(0);
    }
}
