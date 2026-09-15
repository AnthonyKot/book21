package lab;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Tests supplied with the release 2 candidate. See review/release-2/EXPLANATION.md. */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"lab.partition=true"})
class ExportCandidateTest extends ReviewHttp {
    @Test
    void activeDownload() throws Exception {
        var response = download("alice", "C-1001");
        assertEquals(200, response.statusCode());
        assertEquals("CEDAR-FULL-INVOICE-CONTENT", response.body());
    }

    @Test
    void archivedDownloadDenied() throws Exception {
        preview("alice", "C-1001");
        store.archive("C-1001", "cedar");
        noContent(download("alice", "C-1001"));
    }

    @Test
    void archivedSummaryStillVisible() throws Exception {
        store.archive("C-1001", "cedar");
        assertEquals("CEDAR-PRIVATE-SUMMARY", preview("alice", "C-1001").body());
    }

    @Test
    void exportRoundTrip() throws Exception {
        String exportId = export("alice", "C-1001");
        var response = fetchExport("alice", exportId);
        assertEquals(200, response.statusCode());
        assertEquals("CEDAR-FULL-INVOICE-CONTENT", response.body());
    }

    @Test
    void exportOfArchivedDocumentRefused() throws Exception {
        store.archive("C-1001", "cedar");
        assertEquals(404, post("alice", "/api/documents/C-1001/export").statusCode());
    }

    @Test
    void foreignTenantCannotFetchExport() throws Exception {
        String exportId = export("alice", "C-1001");
        noContent(fetchExport("bob", exportId));
    }

    @Test
    void unknownExport() throws Exception {
        noContent(fetchExport("alice", "00000000-0000-0000-0000-000000000000"));
    }
}
