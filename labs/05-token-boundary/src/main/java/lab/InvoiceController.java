package lab;

import java.security.Principal;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
class InvoiceController {
    record Invoice(String id, String tenantId, String customer, long amountCents) {}
    // Same Cedar/Birch data as essay 4, narrowed to a read-only fixture for token validation.
    private final Map<String, String> tenants = Map.of("alice", "cedar", "bob", "birch");
    private final Map<String, Invoice> invoices = Map.of(
            "C-1001", new Invoice("C-1001", "cedar", "Cedar Dental", 120000),
            "B-2001", new Invoice("B-2001", "birch", "Birch Legal", 990000));

    @GetMapping("/api/invoices/{id}")
    ResponseEntity<Invoice> read(@PathVariable String id, Principal user) {
        String tenant = tenants.get(user.getName());
        if (tenant == null) return ResponseEntity.status(403).build();
        Invoice invoice = invoices.get(id);
        return invoice != null && invoice.tenantId().equals(tenant)
                ? ResponseEntity.ok(invoice) : ResponseEntity.notFound().build();
    }
}
