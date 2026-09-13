package lab;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
class InvoiceController {
    private final InvoiceAccess access;
    private final InvoiceRepository invoices;
    private final ExportJobRepository jobs;
    private final UserDirectory users;

    InvoiceController(InvoiceAccess access, InvoiceRepository invoices, ExportJobRepository jobs, UserDirectory users) {
        this.access = access;
        this.invoices = invoices;
        this.jobs = jobs;
        this.users = users;
    }

    @GetMapping("/invoices")
    List<Invoice> list(Principal user) {
        return invoices.findByTenantId(users.tenantOf(user.getName()));
    }

    @GetMapping("/invoices/{id}")
    ResponseEntity<Invoice> read(@PathVariable String id, Principal user) {
        return ResponseEntity.of(access.invoiceFor(user.getName(), id));
    }

    @PostMapping("/exports")
    ResponseEntity<Map<String, Object>> requestExport(@RequestParam String invoiceId, Principal user) {
        if (access.invoiceFor(user.getName(), invoiceId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ExportJob job = jobs.save(new ExportJob(null, user.getName(), invoiceId, "QUEUED", null));
        return ResponseEntity.accepted().body(Map.of("jobId", job.id()));
    }

    @GetMapping("/exports/{jobId}")
    ResponseEntity<ExportJob> download(@PathVariable long jobId, Principal user) {
        return ResponseEntity.of(access.jobFor(user.getName(), jobId));
    }
}
