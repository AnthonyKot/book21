package book21.detection;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@RestController
public class SearchController {
    private final SearchCases cases;
    private final HeaderCases headers;
    public SearchController(SearchCases cases, HeaderCases headers) { this.cases = cases; this.headers = headers; }
    @GetMapping("/search/{mode}")
    public List<String> search(@PathVariable String mode, HttpServletRequest request) {
        return switch(mode) {
            case "direct" -> cases.direct(request);
            case "alias" -> cases.alias(request);
            case "wrapped" -> cases.wrapped(request);
            case "bound" -> cases.bound(request);
            case "repaired" -> cases.repaired(request);
            case "constant" -> cases.constant(request);
            case "header-unsafe" -> headers.unsafe(request);
            case "header-bound" -> headers.bound(request);
            case "header-wrapped" -> headers.wrapped(request);
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        };
    }
}
