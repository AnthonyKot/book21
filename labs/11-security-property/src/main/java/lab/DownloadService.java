package lab;
import org.springframework.stereotype.Service;
@Service
public class DownloadService {
    private final DocumentStore store;private final PreviewService previews;
    public DownloadService(DocumentStore store,PreviewService previews){this.store=store;this.previews=previews;}
    public String download(String id,String tenant){
        // Independent exercise: preview permission does not imply current download permission.
        return previews.preview(id,tenant).content();
    }
}
