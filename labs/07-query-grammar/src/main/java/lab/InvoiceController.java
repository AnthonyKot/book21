package lab;
import java.security.Principal;
import java.util.*;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController
public class InvoiceController {
    private final InvoiceRepository invoices;
    public InvoiceController(InvoiceRepository invoices) { this.invoices = invoices; }
    private String tenant(Principal principal) {
        if (!"alice".equals(principal.getName())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return "cedar"; // Trusted one-account directory; no request-selected tenant.
    }
    @GetMapping("/api/search") List<InvoiceRepository.Invoice> search(Principal p, @RequestParam String title) {
        return invoices.search(tenant(p), title);
    }
    @GetMapping("/api/invoices/{id}") InvoiceRepository.Invoice read(Principal p, @PathVariable String id) {
        return invoices.scoped(tenant(p), id).stream().findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    @GetMapping("/api/unscoped/{id}") InvoiceRepository.Invoice counterexample(Principal p, @PathVariable String id) {
        tenant(p);
        return invoices.unscoped(id).stream().findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    @GetMapping("/api/sorted") List<InvoiceRepository.Invoice> sorted(Principal p, @RequestParam(defaultValue="id") String sort) {
        return invoices.sorted(tenant(p), sort);
    }
    @ExceptionHandler(DataAccessException.class) ResponseEntity<Map<String,String>> databaseFailure() {
        return ResponseEntity.status(500).body(Map.of("error", "database query failed"));
    }
}
