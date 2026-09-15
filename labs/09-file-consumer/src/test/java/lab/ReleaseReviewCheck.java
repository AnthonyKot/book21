package lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Review check for the independent release task. Use it to compare with your own evidence
 * after saving your attempt; it is not a substitute for writing tests. It checks the stated
 * release contract through HTTP and fixture counters, so different sound designs can pass.
 * Default `mvn test` does not run it, and it is skipped unless -DreviewCheck=true is set, so an
 * IDE's "run all tests" does not show its results before your attempt.
 */
@EnabledIfSystemProperty(named = "reviewCheck", matches = "true")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"lab.fixture-port=0", "lab.hardened=true"})
class ReleaseReviewCheck extends UploadHttp {
    private static final String STATEMENT =
            "<statement>\n  <line cents='1200'>Toner &amp; paper</line>\n  <line cents='300'><![CDATA[Delivery <express>]]></line>\n</statement>";

    private void refusedEverywhere(byte[] xml) throws Exception {
        String id = upload(xml);
        assertEquals(422, statement(id).statusCode(), "statement");
        assertEquals(422, preview(id).statusCode(), "preview");
        assertEquals(0, fixture.resolutions.get(), "no external resolution requested");
        assertEquals(0, fixture.httpHits.get(), "no fixture contact");
    }

    private void refusedEverywhere(String xml) throws Exception {
        refusedEverywhere(xml.getBytes(StandardCharsets.UTF_8));
    }

    private String entityStatement(String uri) {
        return "<!DOCTYPE statement [<!ENTITY note SYSTEM '" + uri + "'>]>"
                + "<statement><line cents='100'>&note;</line></statement>";
    }

    @Test
    void ordinaryStatementStillTotals() throws Exception {
        var response = statement(upload(STATEMENT));
        assertEquals(200, response.statusCode());
        assertEquals("2 lines, 1500 cents: Toner & paper; Delivery <express>", response.body());
    }

    @Test
    void invalidStatementShapeRejected() throws Exception {
        assertEquals(422, statement(upload("<statement><line>No amount</line></statement>")).statusCode());
        assertEquals(422, statement(upload("<invoice><title>Not a statement</title></invoice>")).statusCode());
    }

    @Test
    void fileEntityRefused() throws Exception {
        refusedEverywhere(entityStatement(fixture.fileUri()));
    }

    @Test
    void httpEntityRefused() throws Exception {
        refusedEverywhere(entityStatement(fixture.httpUri()));
    }

    @Test
    void externalDtdRefused() throws Exception {
        refusedEverywhere("<!DOCTYPE statement SYSTEM '" + fixture.dtdUri() + "'>"
                + "<statement><line cents='100'>Paper</line></statement>");
    }

    @Test
    void parameterEntityRefused() throws Exception {
        refusedEverywhere("<!DOCTYPE statement [<!ENTITY % remote SYSTEM '" + fixture.dtdUri() + "'> %remote;]>"
                + "<statement><line cents='100'>Paper</line></statement>");
    }

    @Test
    void unusedInternalDtdRefused() throws Exception {
        refusedEverywhere("<!DOCTYPE statement [<!ENTITY note 'Cedar'>]>"
                + "<statement><line cents='100'>Paper</line></statement>");
    }

    @Test
    void utf16DoctypeRefused() throws Exception {
        String xml = "<?xml version='1.0' encoding='UTF-16'?>" + entityStatement(fixture.fileUri());
        refusedEverywhere(xml.getBytes(StandardCharsets.UTF_16));
    }

    @Test
    void receiptStillIdentifiesRejectedBytes() throws Exception {
        byte[] bytes = entityStatement(fixture.httpUri()).getBytes(StandardCharsets.UTF_8);
        var response = receipt(upload(bytes));
        assertEquals(200, response.statusCode());
        String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        assertTrue(response.body().contains(sha256), response.body());
        assertFalse(response.body().contains(Fixture.NOTE));
        assertEquals(0, fixture.resolutions.get());
        assertEquals(0, fixture.httpHits.get());
    }
}
