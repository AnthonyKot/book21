package lab;
import java.io.IOException;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
@RestController
public class UploadController {
    private final UploadStore store;private final InvoiceParser parser;private final Fixture fixture;private final SummaryWorker worker;
    @Value("${lab.hardened}") boolean hardened;
    public UploadController(UploadStore store,InvoiceParser parser,Fixture fixture,SummaryWorker worker){this.store=store;this.parser=parser;this.fixture=fixture;this.worker=worker;}
    @GetMapping("/csrf") Map<String,String> csrf(CsrfToken token){return Map.of("header",token.getHeaderName(),"token",token.getToken());}
    @PostMapping(value="/api/uploads",consumes="application/xml",produces="text/plain")
    @ResponseStatus(HttpStatus.CREATED)
    String upload(HttpServletRequest request)throws IOException{return store.save(request.getInputStream());}
    @PostMapping(value="/api/uploads/{id}/preview",produces="text/plain")
    String preview(@PathVariable String id)throws IOException{return parser.title(store.read(id),hardened);}
    @PostMapping(value="/api/uploads/{id}/summary",produces="text/plain")
    String summary(@PathVariable String id)throws IOException{return worker.summarize(id);}
    @GetMapping("/lab/targets") Map<String,Object> targets(){return Map.of("file",fixture.fileUri(),"http",fixture.httpUri(),"dtd",fixture.dtdUri(),"httpHits",fixture.httpHits.get(),"resolutions",fixture.resolutions.get());}
}
