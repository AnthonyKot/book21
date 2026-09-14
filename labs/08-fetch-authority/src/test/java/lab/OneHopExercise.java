package lab;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
 properties={"lab.partner-port=0","lab.internal-port=0","lab.follow-redirects=false"})
class OneHopExercise extends FetchHttp {
    @Test void directDocument() throws Exception {assertEquals(200,call("/api/follow-one",destinations.partnerUrl()+"/document",true).statusCode());}
    @Test void relativeRedirectWorks() throws Exception {
        var r=call("/api/follow-one",destinations.partnerUrl()+"/to-document",true);
        assertEquals(200,r.statusCode());assertEquals("PARTNER-INVOICE-C-1001",r.body());assertEquals(2,destinations.partnerHits.get());
    }
    @Test void internalRedirectBlockedBeforeContact() throws Exception {
        assertEquals(502,call("/api/follow-one",destinations.partnerUrl()+"/to-internal",true).statusCode());assertEquals(0,destinations.internalHits.get());
    }
    @Test void loopStopsAfterOneHop() throws Exception {
        assertEquals(502,call("/api/follow-one",destinations.partnerUrl()+"/loop",true).statusCode());assertTrue(destinations.partnerHits.get()<=2);
    }
    @Test void chainStopsAfterOneHop() throws Exception {
        assertEquals(502,call("/api/follow-one",destinations.partnerUrl()+"/chain",true).statusCode());assertTrue(destinations.partnerHits.get()<=2);
    }
}
