package lab;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Runs queued exports. It has no HTTP request and no session: only the job row says who asked. */
@Component
class ExportWorker {
    private final ExportJobRepository jobs;
    private final InvoiceAccess access;

    ExportWorker(ExportJobRepository jobs, InvoiceAccess access) {
        this.jobs = jobs;
        this.access = access;
    }

    void runPending() {
        for (ExportJob job : jobs.findByStatus("QUEUED")) {
            ExportJob result = access.invoiceFor(job.requester(), job.invoiceId())
                    .map(inv -> job.withResult("DONE", inv.id() + "," + inv.customer() + "," + inv.amountCents()))
                    .orElseGet(() -> job.withResult("DENIED", null));
            jobs.save(result);
        }
    }
}

@Component
@ConditionalOnProperty(name = "lab.worker.enabled", havingValue = "true")
class ExportPoller {
    private final ExportWorker worker;

    ExportPoller(ExportWorker worker) {
        this.worker = worker;
    }

    @Scheduled(fixedDelay = 500)
    void poll() {
        worker.runPending();
    }
}
