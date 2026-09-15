package lab;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Post-attempt comparison for the release 2 review. It checks the stated property over HTTP,
 * so different sound designs can pass. It is not a substitute for your own tests. It is not part
 * of the default `mvn test` run, and it is skipped unless -DreviewCheck=true is set, so an IDE's
 * "run all tests" does not show its results before your attempt.
 */
@EnabledIfSystemProperty(named = "reviewCheck", matches = "true")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"lab.partition=true"})
class ReleaseReviewCheck extends ReviewHttp {
    @Test
    void activeDownloadStillWorks() throws Exception {
        assertEquals("CEDAR-FULL-INVOICE-CONTENT", download("alice", "C-1001").body());
    }

    @Test
    void archivedDownloadDeniedWarmAndCold() throws Exception {
        preview("alice", "C-1001");
        store.archive("C-1001", "cedar");
        noContent(download("alice", "C-1001"));
        previews.clear();
        noContent(download("alice", "C-1001"));
    }

    @Test
    void exportRoundTripStillWorks() throws Exception {
        String exportId = export("alice", "C-1001");
        assertEquals("CEDAR-FULL-INVOICE-CONTENT", fetchExport("alice", exportId).body());
    }

    @Test
    void archiveAfterExportDeniesFetchWarm() throws Exception {
        String exportId = export("alice", "C-1001");
        assertEquals(200, fetchExport("alice", exportId).statusCode());
        store.archive("C-1001", "cedar");
        noContent(fetchExport("alice", exportId));
    }

    @Test
    void archiveAfterExportDeniesFetchCold() throws Exception {
        String exportId = export("alice", "C-1001");
        store.archive("C-1001", "cedar");
        previews.clear();
        noContent(fetchExport("alice", exportId));
    }

    @Test
    void foreignTenantCannotFetchExport() throws Exception {
        String exportId = export("alice", "C-1001");
        preview("alice", "C-1001");
        noContent(fetchExport("bob", exportId));
    }

    @Test
    void ownArchivedSummaryStillVisible() throws Exception {
        store.archive("C-1001", "cedar");
        assertEquals("CEDAR-PRIVATE-SUMMARY", preview("alice", "C-1001").body());
    }

    @Test
    void ownerRepeatPreviewStillUsesCache() throws Exception {
        preview("alice", "C-1001");
        preview("alice", "C-1001");
        assertEquals(1, store.bodyLoads.get());
    }

    @Test
    void ownerArchivedPreviewStillUsesCache() throws Exception {
        preview("alice", "C-1001");
        store.archive("C-1001", "cedar");
        assertEquals("CEDAR-PRIVATE-SUMMARY", preview("alice", "C-1001").body());
        assertEquals(1, store.bodyLoads.get());
    }

    @Test
    void unaffectedDocumentStillExports() throws Exception {
        store.archive("C-1001", "cedar");
        String exportId = export("alice", "C-1002");
        assertEquals("CEDAR-SECOND-CONTENT", fetchExport("alice", exportId).body());
    }
}
