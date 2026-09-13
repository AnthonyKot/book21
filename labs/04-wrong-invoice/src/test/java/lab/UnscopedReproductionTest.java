package lab;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

/** Reproduction: these tests PASS because the vulnerability is present. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"lab.lookup=unscoped", "lab.worker.enabled=false"})
@DirtiesContext
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UnscopedReproductionTest extends LabHttp {

    @Test @Order(1)
    void legitimateReadWorks() throws Exception {
        assertThat(call("alice", "GET", "/invoices/C-1001").statusCode()).isEqualTo(200);
    }

    @Test @Order(2)
    void listIsAlreadyScoped() throws Exception {
        assertThat(call("alice", "GET", "/invoices").body()).doesNotContain("B-2001");
    }

    @Test @Order(3)
    void aliceReadsBirchInvoiceById() throws Exception {
        var response = call("alice", "GET", "/invoices/B-2001");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Birch Legal");
    }

    @Test @Order(4)
    void aliceExportsBirchInvoice() throws Exception {
        long job = jobId(call("alice", "POST", "/exports?invoiceId=B-2001"));
        worker.runPending();
        assertThat(statusOf(job)).isEqualTo("DONE");
        assertThat(call("alice", "GET", "/exports/" + job).body()).contains("Birch Legal");
    }

    @Test @Order(5)
    void bobDownloadsAlicesExportJob() throws Exception {
        long job = jobId(call("alice", "POST", "/exports?invoiceId=C-1001"));
        worker.runPending();
        var response = call("bob", "GET", "/exports/" + job);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Cedar Dental");
    }

    @Test @Order(6)
    void workerTrustsAQueuedRowThatNamesAForeignInvoice() {
        jdbc.sql("INSERT INTO export_job (requester, invoice_id, status) VALUES ('alice', 'B-2001', 'QUEUED')").update();
        long job = jdbc.sql("SELECT MAX(id) FROM export_job").query(Long.class).single();
        worker.runPending();
        System.out.println("  worker: queued row alice/B-2001 -> " + statusOf(job));
        assertThat(statusOf(job)).isEqualTo("DONE");
    }
}
