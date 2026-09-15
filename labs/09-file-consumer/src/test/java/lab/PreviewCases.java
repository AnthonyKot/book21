package lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

/**
 * The guided preview cases. Each subclass states which parser policy it expects; the running
 * application's policy comes from its own lab.hardened property, so the two can be mismatched
 * deliberately for a negative control.
 */
abstract class PreviewCases extends UploadHttp {
    abstract boolean expectHardened();

    static String externalEntity(String uri) {
        return "<!DOCTYPE invoice [<!ENTITY note SYSTEM '" + uri + "'>]><invoice><title>&note;</title></invoice>";
    }

    private void referenceCase(String xml, boolean contactsHttp) throws Exception {
        referenceCase(xml.getBytes(StandardCharsets.UTF_8), contactsHttp);
    }

    private void referenceCase(byte[] xml, boolean contactsHttp) throws Exception {
        String id = upload(xml);
        assertEquals(0, fixture.resolutions.get(), "storing must not parse");
        assertEquals(0, fixture.httpHits.get(), "storing must not parse");

        var response = preview(id);
        if (expectHardened()) {
            assertEquals(422, response.statusCode());
            assertEquals(0, fixture.resolutions.get());
            assertEquals(0, fixture.httpHits.get());
        } else {
            assertEquals(200, response.statusCode());
            assertEquals(Fixture.NOTE, response.body());
            assertEquals(1, fixture.resolutions.get());
            assertEquals(contactsHttp ? 1 : 0, fixture.httpHits.get());
        }
    }

    private void rejected(String xml) throws Exception {
        assertEquals(422, preview(upload(xml)).statusCode());
        assertEquals(0, fixture.httpHits.get());
    }

    @Test
    void ordinaryInvoice() throws Exception {
        assertEquals("Cedar & Sons", preview(upload("<invoice><title>Cedar &amp; Sons</title></invoice>")).body());
    }

    @Test
    void cdataText() throws Exception {
        assertEquals("A < B", preview(upload("<invoice><title><![CDATA[A < B]]></title></invoice>")).body());
    }

    @Test
    void fileEntity() throws Exception {
        referenceCase(externalEntity(fixture.fileUri()), false);
    }

    @Test
    void httpEntity() throws Exception {
        referenceCase(externalEntity(fixture.httpUri()), true);
    }

    @Test
    void externalDtd() throws Exception {
        referenceCase("<!DOCTYPE invoice SYSTEM '" + fixture.dtdUri() + "'><invoice><title>&note;</title></invoice>", true);
    }

    @Test
    void utf16Entity() throws Exception {
        String xml = "<?xml version='1.0' encoding='UTF-16'?>" + externalEntity(fixture.fileUri());
        referenceCase(xml.getBytes(StandardCharsets.UTF_16), false);
    }

    @Test
    void internalDtd() throws Exception {
        var response = preview(upload("<!DOCTYPE invoice [<!ENTITY note 'Cedar'>]><invoice><title>&note;</title></invoice>"));
        assertEquals(expectHardened() ? 422 : 200, response.statusCode());
        assertEquals(0, fixture.resolutions.get());
    }

    @Test
    void malformed() throws Exception {
        rejected("<invoice><title>broken</invoice>");
    }

    @Test
    void duplicateTitle() throws Exception {
        rejected("<invoice><title>A</title><title>B</title></invoice>");
    }

    @Test
    void longTitle() throws Exception {
        rejected("<invoice><title>" + "x".repeat(121) + "</title></invoice>");
    }

    @Test
    void xincludeStaysInactive() throws Exception {
        rejected("<invoice xmlns:xi='http://www.w3.org/2001/XInclude'><title><xi:include href='"
                + fixture.httpUri() + "' parse='text'/></title></invoice>");
        assertEquals(0, fixture.resolutions.get());
    }

    @Test
    void wrongMediaType() throws Exception {
        assertEquals(415, post("/api/uploads", "hello".getBytes(), "text/plain", true, true).statusCode());
    }

    @Test
    void oversizedUpload() throws Exception {
        assertEquals(413, post("/api/uploads", new byte[8193], "application/xml", true, true).statusCode());
    }

    @Test
    void exactUploadLimit() throws Exception {
        String xml = "<invoice><title>Cedar</title></invoice>";
        xml += " ".repeat(8192 - xml.length());
        assertEquals("Cedar", preview(upload(xml)).body());
    }

    @Test
    void unknownId() throws Exception {
        assertEquals(404, preview("00000000-0000-0000-0000-000000000000").statusCode());
    }

    @Test
    void csrfRequired() throws Exception {
        // Observed with this Basic-auth fixture on Spring Security 7.1.1; not a universal CSRF status.
        assertEquals(401, post("/api/uploads", new byte[0], "application/xml", false, true).statusCode());
    }

    @Test
    void authenticationRequired() throws Exception {
        assertEquals(401, post("/api/uploads", new byte[0], "application/xml", true, false).statusCode());
    }

    @Test
    void parserDepthLimit() throws Exception {
        String allowed = "<n>".repeat(32) + "</n>".repeat(32);
        String denied = "<n>".repeat(33) + "</n>".repeat(33);
        assertNotNull(parser.builder(expectHardened()).parse(new ByteArrayInputStream(allowed.getBytes())));
        SAXException e = assertThrows(SAXException.class,
                () -> parser.builder(expectHardened()).parse(new ByteArrayInputStream(denied.getBytes())));
        assertTrue(e.getMessage().contains("32"), e.getMessage());
    }
}
