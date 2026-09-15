package lab;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UploadController {
    private final UploadStore store;
    private final InvoiceParser invoices;
    private final StatementReader statements;
    private final Fixture fixture;

    @Value("${lab.hardened}")
    boolean hardened;

    public UploadController(UploadStore store, InvoiceParser invoices, StatementReader statements, Fixture fixture) {
        this.store = store;
        this.invoices = invoices;
        this.statements = statements;
        this.fixture = fixture;
    }

    @GetMapping("/csrf")
    Map<String, String> csrf(CsrfToken token) {
        return Map.of("header", token.getHeaderName(), "token", token.getToken());
    }

    @PostMapping(value = "/api/uploads", consumes = "application/xml", produces = "text/plain")
    @ResponseStatus(HttpStatus.CREATED)
    String upload(HttpServletRequest request) throws IOException {
        return store.save(request.getInputStream());
    }

    @PostMapping(value = "/api/uploads/{id}/preview", produces = "text/plain")
    String preview(@PathVariable String id) throws IOException {
        return invoices.title(store.read(id), hardened);
    }

    @PostMapping(value = "/api/uploads/{id}/statement", produces = "text/plain")
    String statement(@PathVariable String id) throws IOException {
        return statements.totals(store.read(id));
    }

    /** Lets support identify exactly which bytes were submitted, whether or not they are valid XML. */
    @GetMapping("/api/uploads/{id}/receipt")
    Map<String, Object> receipt(@PathVariable String id) throws IOException, NoSuchAlgorithmException {
        byte[] bytes = store.read(id);
        String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        return Map.of("sha256", sha256, "bytes", bytes.length);
    }

    @GetMapping("/lab/targets")
    Map<String, Object> targets() {
        return Map.of(
                "file", fixture.fileUri(),
                "http", fixture.httpUri(),
                "dtd", fixture.dtdUri(),
                "httpHits", fixture.httpHits.get(),
                "resolutions", fixture.resolutions.get());
    }
}
