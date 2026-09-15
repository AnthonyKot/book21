package lab;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.security.Principal;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class DocumentController {
    private final PreviewService previews;
    private final DownloadService downloads;
    private final ExportService exportsService;
    private final DocumentStore store;

    @Value("${lab.fixtures}")
    boolean fixtures;

    public DocumentController(PreviewService previews, DownloadService downloads, ExportService exportsService,
            DocumentStore store) {
        this.previews = previews;
        this.downloads = downloads;
        this.exportsService = exportsService;
        this.store = store;
    }

    @GetMapping(value = "/api/documents/{id}/preview", produces = "text/plain")
    String preview(@PathVariable String id, Principal principal) {
        return previews.preview(id, Tenants.of(principal)).summary();
    }

    @GetMapping(value = "/api/documents/{id}/download", produces = "text/plain")
    String download(@PathVariable String id, Principal principal) {
        return downloads.download(id, Tenants.of(principal));
    }

    @PostMapping(value = "/api/documents/{id}/export", produces = "text/plain")
    String export(@PathVariable String id, Principal principal) {
        return exportsService.create(id, Tenants.of(principal));
    }

    @GetMapping(value = "/api/exports/{exportId}", produces = "text/plain")
    String fetchExport(@PathVariable String exportId, Principal principal) {
        return exportsService.fetch(exportId, Tenants.of(principal));
    }

    @GetMapping("/csrf")
    Map<String, String> csrf(CsrfToken token) {
        return Map.of("header", token.getHeaderName(), "token", token.getToken());
    }

    // Lab fixtures: disabled unless lab.fixtures=true. Not production administration.

    @GetMapping("/lab/state")
    Map<String, Integer> state() {
        requireFixtures();
        return Map.of("bodyLoads", store.bodyLoads.get(), "entries", previews.entries());
    }

    @PostMapping("/lab/reset")
    void reset() {
        requireFixtures();
        store.reset();
        previews.clear();
        exportsService.clear();
    }

    @PostMapping("/lab/archive/{id}")
    void archive(@PathVariable String id, Principal principal) {
        requireFixtures();
        store.archive(id, Tenants.of(principal));
    }

    private void requireFixtures() {
        if (!fixtures) {
            throw new ResponseStatusException(NOT_FOUND);
        }
    }
}
