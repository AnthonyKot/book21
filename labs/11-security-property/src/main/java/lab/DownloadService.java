package lab;

import org.springframework.stereotype.Service;

/** Full-content download. */
@Service
public class DownloadService {
    private final DocumentStore store;
    private final PreviewService previews;

    public DownloadService(DocumentStore store, PreviewService previews) {
        this.store = store;
        this.previews = previews;
    }

    public String download(String id, String tenant) {
        store.requireDownload(id, tenant);
        return previews.preview(id, tenant).content();
    }
}
