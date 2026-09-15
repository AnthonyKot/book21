package lab;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Export links: a tenant creates an export for one of its documents and later fetches the
 * full content through the export identifier, for example from a support ticket.
 */
@Service
public class ExportService {
    private final DocumentStore store;
    private final PreviewService previews;
    private final Map<String, Export> exports = new ConcurrentHashMap<>();

    public ExportService(DocumentStore store, PreviewService previews) {
        this.store = store;
        this.previews = previews;
    }

    private record Export(String tenant, String documentId) {}

    /** Authorizes the export against the document's current state, then records it. */
    public String create(String documentId, String tenant) {
        store.requireDownload(documentId, tenant);
        String exportId = UUID.randomUUID().toString();
        exports.put(exportId, new Export(tenant, documentId));
        return exportId;
    }

    /** Returns the exported content to the tenant that created the export. */
    public String fetch(String exportId, String tenant) {
        Export export = exports.get(exportId);
        if (export == null || !export.tenant().equals(tenant)) {
            throw new ResponseStatusException(NOT_FOUND);
        }
        return previews.preview(export.documentId(), tenant).content();
    }

    public void clear() {
        exports.clear();
    }
}
