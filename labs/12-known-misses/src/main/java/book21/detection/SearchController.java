package book21.detection;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class SearchController {
    private final SearchCases cases;
    private final ReportCases reports;

    public SearchController(SearchCases cases, ReportCases reports) {
        this.cases = cases;
        this.reports = reports;
    }

    @GetMapping("/search/{mode}")
    public List<String> search(@PathVariable String mode, HttpServletRequest request) {
        return switch (mode) {
            case "direct" -> cases.direct(request);
            case "alias" -> cases.alias(request);
            case "wrapped" -> cases.wrapped(request);
            case "bound" -> cases.bound(request);
            case "repaired" -> cases.repaired(request);
            case "constant" -> cases.constant(request);
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        };
    }

    @GetMapping("/reports/{report}")
    public List<String> report(@PathVariable String report, HttpServletRequest request) {
        return switch (report) {
            case "by-owner" -> reports.byOwner(request);
            case "rows" -> reports.rows(request);
            case "sorted" -> reports.sorted(request);
            case "page" -> reports.page(request);
            case "owner-list" -> reports.ownerList(request);
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        };
    }

    @ExceptionHandler(NumberFormatException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String badNumber() {
        return "invalid number";
    }
}
