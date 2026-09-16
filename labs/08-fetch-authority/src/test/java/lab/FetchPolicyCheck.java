package lab;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Post-attempt behaviour check for the /api/follow-one practice task. Leave this
 * file closed until you have saved your own attempt and tests: it encodes the
 * observable outcomes the task requires, not a design. Run it with
 *   mvn -DreviewCheck=true -Dtest=FetchPolicyCheck test
 * It is excluded from the default `mvn test`. Any design that produces these
 * outcomes passes; it does not prescribe how you structure the policy or map
 * errors. It cannot judge your justification, your own tests or the limits you
 * did not verify — the review guide covers those.
 */
@EnabledIfSystemProperty(named = "reviewCheck", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"lab.partner-port=0", "lab.mirror-port=0", "lab.internal-port=0", "lab.follow-redirects=false"})
class FetchPolicyCheck extends FetchHttp {

    private int followOne(String url) throws Exception {
        return call("/api/follow-one", url, true).statusCode();
    }

    @Test void approvedPartnerDocumentReturns() throws Exception {
        var r = call("/api/follow-one", destinations.partnerUrl() + "/document", true);
        assertEquals(200, r.statusCode());
        assertEquals("PARTNER-INVOICE-C-1001", r.body());
    }

    @Test void approvedMirrorDocumentReturns() throws Exception {
        var r = call("/api/follow-one", destinations.mirrorUrl() + "/document", true);
        assertEquals(200, r.statusCode(), "the second approved origin must be reachable");
        assertEquals("PARTNER-INVOICE-C-1001", r.body());
    }

    @Test void partnerRedirectToMirrorIsFollowed() throws Exception {
        var r = call("/api/follow-one", destinations.partnerUrl() + "/to-mirror", true);
        assertEquals(200, r.statusCode(), "one redirect to an approved origin should succeed");
        assertEquals("PARTNER-INVOICE-C-1001", r.body());
        assertTrue(destinations.mirrorHits.get() >= 1, "the mirror should have served the document");
    }

    @Test void partnerRelativeRedirectToItsDocumentIsFollowed() throws Exception {
        var r = call("/api/follow-one", destinations.partnerUrl() + "/to-document", true);
        assertEquals(200, r.statusCode(), "a relative redirect to an approved document should succeed");
        assertEquals("PARTNER-INVOICE-C-1001", r.body());
        assertEquals(2, destinations.partnerHits.get(), "partner hit twice: the redirect then the document");
    }

    @Test void redirectToInternalNeverContactsInternal() throws Exception {
        int status = followOne(destinations.partnerUrl() + "/to-internal");
        assertNotEquals(200, status, "a redirect to the forbidden origin must not succeed");
        assertEquals(0, destinations.internalHits.get(), "the forbidden origin must not be contacted");
    }

    @Test void directInternalDenied() throws Exception {
        int status = followOne(destinations.internalUrl());
        assertNotEquals(200, status);
        assertEquals(0, destinations.internalHits.get());
    }

    @Test void redirectLoopIsBounded() throws Exception {
        int status = followOne(destinations.partnerUrl() + "/loop");
        assertNotEquals(200, status, "an endless redirect must not be followed to success");
        assertTrue(destinations.partnerHits.get() <= 2, "at most one redirect should be followed");
    }

    @Test void credentialNotForwardedOnApprovedPath() throws Exception {
        call("/api/follow-one", destinations.partnerUrl() + "/to-mirror", true);
        assertFalse(destinations.sawAuthorization.get(), "outbound requests must not copy the caller credential");
    }
}
