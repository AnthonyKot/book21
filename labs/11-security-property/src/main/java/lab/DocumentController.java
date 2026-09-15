package lab;
import java.security.Principal;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.NOT_FOUND;
@RestController
public class DocumentController {
    private final PreviewService previews;private final DownloadService downloads;private final DocumentStore store;
    @Value("${lab.fixtures}") boolean fixtures;
    public DocumentController(PreviewService previews,DownloadService downloads,DocumentStore store){this.previews=previews;this.downloads=downloads;this.store=store;}
    @GetMapping(value="/api/documents/{id}/preview",produces="text/plain") String preview(@PathVariable String id,Principal p){return previews.preview(id,Tenants.of(p)).summary();}
    @GetMapping(value="/api/documents/{id}/download",produces="text/plain") String download(@PathVariable String id,Principal p){return downloads.download(id,Tenants.of(p));}
    @GetMapping("/csrf") Map<String,String> csrf(CsrfToken t){return Map.of("header",t.getHeaderName(),"token",t.getToken());}
    @GetMapping("/lab/state") Map<String,Integer> state(){requireFixtures();return Map.of("bodyLoads",store.bodyLoads.get(),"entries",previews.entries());}
    @PostMapping("/lab/reset") void reset(){requireFixtures();store.reset();previews.clear();}
    @PostMapping("/lab/archive/{id}") void archive(@PathVariable String id,Principal p){requireFixtures();store.archive(id,Tenants.of(p));}
    private void requireFixtures(){if(!fixtures)throw new ResponseStatusException(NOT_FOUND);}
}
