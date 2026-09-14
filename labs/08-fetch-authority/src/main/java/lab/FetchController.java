package lab;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
@RestController
public class FetchController {
    private final FetchService service;
    private final Destinations destinations;
    public FetchController(FetchService service,Destinations destinations) {this.service=service;this.destinations=destinations;}
    @GetMapping(value="/api/fetch",produces="text/plain") String fetch(@RequestParam String url) { return service.fetch(url); }
    @GetMapping(value="/api/follow-one",produces="text/plain") String practice(@RequestParam String url) { return service.followOne(url); }
    @GetMapping("/lab/targets") Map<String,Object> targets() {
        return Map.of("partner",destinations.partnerUrl(),"internal",destinations.internalUrl(),
            "partnerHits",destinations.partnerHits.get(),"internalHits",destinations.internalHits.get(),
            "sawAuthorization",destinations.sawAuthorization.get());
    }
}
