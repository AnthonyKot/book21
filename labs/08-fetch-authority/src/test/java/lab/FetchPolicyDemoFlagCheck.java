package lab;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Post-attempt check, second part. The guided demo's lab.follow-redirects flag exists to
 * show the vulnerable mode of /api/fetch. It must not change what /api/follow-one does:
 * the practice endpoint decides every destination itself. Leave this file closed until
 * your attempt is saved. Run both parts with
 *   mvn -DreviewCheck=true -Dtest='FetchPolicy*' test
 */
@EnabledIfSystemProperty(named = "reviewCheck", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"lab.partner-port=0", "lab.mirror-port=0", "lab.internal-port=0", "lab.follow-redirects=true"})
class FetchPolicyDemoFlagCheck extends FetchHttp {

    @Test void demoFlagDoesNotReopenInternalViaPractice() throws Exception {
        int status = call("/api/follow-one", destinations.partnerUrl() + "/to-internal", true).statusCode();
        assertNotEquals(200, status, "the demo flag must not make the practice endpoint follow to the forbidden origin");
        assertEquals(0, destinations.internalHits.get(), "the forbidden origin must not be contacted");
    }

    @Test void approvedRedirectStillWorksUnderDemoFlag() throws Exception {
        var r = call("/api/follow-one", destinations.partnerUrl() + "/to-mirror", true);
        assertEquals(200, r.statusCode());
        assertEquals("PARTNER-INVOICE-C-1001", r.body());
    }
}
