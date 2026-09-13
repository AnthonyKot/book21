package lab;

import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Every read of an invoice or export job, from the controller or the worker, goes through here. */
interface InvoiceAccess {
    Optional<Invoice> invoiceFor(String username, String invoiceId);
    Optional<ExportJob> jobFor(String username, long jobId);
}

/** Vulnerable: the user is authenticated, but the lookup ignores who they are. */
@Component
@ConditionalOnProperty(name = "lab.lookup", havingValue = "unscoped")
class UnscopedAccess implements InvoiceAccess {
    private final InvoiceRepository invoices;
    private final ExportJobRepository jobs;

    UnscopedAccess(InvoiceRepository invoices, ExportJobRepository jobs) {
        this.invoices = invoices;
        this.jobs = jobs;
    }

    public Optional<Invoice> invoiceFor(String username, String invoiceId) {
        return invoices.findById(invoiceId);
    }

    public Optional<ExportJob> jobFor(String username, long jobId) {
        return jobs.findById(jobId);
    }
}

/** Repaired: the tenant and the job owner are part of the query, so a foreign row is never loaded. */
@Component
@ConditionalOnProperty(name = "lab.lookup", havingValue = "scoped")
class ScopedAccess implements InvoiceAccess {
    private final InvoiceRepository invoices;
    private final ExportJobRepository jobs;
    private final UserDirectory users;

    ScopedAccess(InvoiceRepository invoices, ExportJobRepository jobs, UserDirectory users) {
        this.invoices = invoices;
        this.jobs = jobs;
        this.users = users;
    }

    public Optional<Invoice> invoiceFor(String username, String invoiceId) {
        return invoices.findByIdAndTenantId(invoiceId, users.tenantOf(username));
    }

    public Optional<ExportJob> jobFor(String username, long jobId) {
        return jobs.findByIdAndRequester(jobId, username)
                .filter(job -> invoiceFor(username, job.invoiceId()).isPresent());
    }
}
