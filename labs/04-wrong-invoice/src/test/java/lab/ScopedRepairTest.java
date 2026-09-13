package lab;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

/** Repair: the same requests, denied, while the legitimate paths still work. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"lab.lookup=scoped", "lab.worker.enabled=false"})
@DirtiesContext
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ScopedRepairTest extends LabHttp {

    @Test @Order(1)
    void legitimateReadsStillWork() throws Exception {
        assertThat(call("alice", "GET", "/invoices/C-1001").statusCode()).isEqualTo(200);
        assertThat(call("bob", "GET", "/invoices/B-2001").statusCode()).isEqualTo(200);
    }

    @Test @Order(2)
    void foreignInvoiceLooksLikeAMissingOne() throws Exception {
        var foreign = call("alice", "GET", "/invoices/B-2001");
        var missing = call("alice", "GET", "/invoices/X-9999");
        assertThat(foreign.statusCode()).isEqualTo(404);
        assertThat(missing.statusCode()).isEqualTo(404);
        assertThat(foreign.body()).isEqualTo(missing.body());
    }

    @Test @Order(3)
    void foreignExportIsRefusedBeforeAJobExists() throws Exception {
        long before = jdbc.sql("SELECT COUNT(*) FROM export_job").query(Long.class).single();
        assertThat(call("alice", "POST", "/exports?invoiceId=B-2001").statusCode()).isEqualTo(404);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM export_job").query(Long.class).single()).isEqualTo(before);
    }

    @Test @Order(4)
    void legitimateExportStillWorks() throws Exception {
        long job = jobId(call("alice", "POST", "/exports?invoiceId=C-1001"));
        worker.runPending();
        assertThat(statusOf(job)).isEqualTo("DONE");
        assertThat(call("alice", "GET", "/exports/" + job).body()).contains("Cedar Dental");
    }

    @Test @Order(5)
    void anotherUserCannotDownloadTheJob() throws Exception {
        long job = jobId(call("alice", "POST", "/exports?invoiceId=C-1002"));
        worker.runPending();
        assertThat(call("bob", "GET", "/exports/" + job).statusCode()).isEqualTo(404);
    }

    @Test @Order(6)
    void workerRechecksAQueuedRowThatNamesAForeignInvoice() {
        jdbc.sql("INSERT INTO export_job (requester, invoice_id, status) VALUES ('alice', 'B-2001', 'QUEUED')").update();
        long job = jdbc.sql("SELECT MAX(id) FROM export_job").query(Long.class).single();
        worker.runPending();
        System.out.println("  worker: queued row alice/B-2001 -> " + statusOf(job));
        assertThat(statusOf(job)).isEqualTo("DENIED");
    }
}
