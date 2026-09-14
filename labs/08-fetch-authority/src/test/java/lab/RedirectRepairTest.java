package lab;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
 properties={"lab.partner-port=0","lab.internal-port=0","lab.follow-redirects=${test.follow:false}"})
class RedirectRepairTest extends FetchHttp {
    @Test void documentWorks() throws Exception {
        var r=fetch(destinations.partnerUrl()+"/document");assertEquals(200,r.statusCode());assertEquals("PARTNER-INVOICE-C-1001",r.body());
        assertEquals(1,destinations.partnerHits.get());assertEquals(0,destinations.internalHits.get());assertFalse(destinations.sawAuthorization.get());
    }
    @Test void directInternalDenied() throws Exception {deniedWithoutContact(destinations.internalUrl());}
    @Test void redirectInternal() throws Exception {
        var r=fetch(destinations.partnerUrl()+"/to-internal");assertEquals(502,r.statusCode());
        assertEquals(1,destinations.partnerHits.get());assertEquals(0,destinations.internalHits.get());
        assertFalse(r.body().contains("INTERNAL-SYNTHETIC-REPORT"));
        assertFalse(destinations.sawAuthorization.get());
    }
    @Test void sameOriginRedirect() throws Exception {
        var r=fetch(destinations.partnerUrl()+"/to-document");assertEquals(502,r.statusCode());
        assertEquals(1,destinations.partnerHits.get());assertEquals(0,destinations.internalHits.get());
    }
    @Test void wrongScheme() throws Exception {deniedWithoutContact(destinations.partnerUrl().replace("http:","https:")+"/document");}
    @Test void userInfo() throws Exception {deniedWithoutContact(destinations.partnerUrl().replace("http://","http://alice@")+"/document");}
    @Test void fragment() throws Exception {deniedWithoutContact(destinations.partnerUrl()+"/document#ignored");}
    @Test void hostSuffix() throws Exception {deniedWithoutContact(destinations.partnerUrl().replace("127.0.0.1","127.0.0.1.example.test")+"/document");}
    @Test void malformed() throws Exception {deniedWithoutContact("http://[");}
    @Test void upstreamError() throws Exception {assertEquals(502,fetch(destinations.partnerUrl()+"/unavailable").statusCode());assertEquals(0,destinations.internalHits.get());}
    @Test void anonymousDenied() throws Exception {
        assertEquals(401,call("/api/fetch",destinations.partnerUrl()+"/document",false).statusCode());assertEquals(0,destinations.partnerHits.get());
    }
}
