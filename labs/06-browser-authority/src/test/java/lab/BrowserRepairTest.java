package lab;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties="lab.csrf=${test.csrf:true}")
class BrowserRepairTest extends BrowserHttp {
    @Test void missingToken() throws Exception {
        assertEquals(403, foreignPost("/api/approve", "invoice=C-1001").statusCode());
        approved(false);
    }
    @Test void invalidToken() throws Exception {
        assertEquals(403, foreignPost("/api/approve", "invoice=C-1001&_csrf=wrong").statusCode());
        approved(false);
    }
    @Test void legitimateApproval() throws Exception {
        assertEquals(200, post("/api/approve", "invoice=C-1001&_csrf=" + token()).statusCode());
        approved(true);
    }
    @Test void otherTenantStillDenied() throws Exception {
        assertEquals(404, post("/api/approve", "invoice=B-2001&_csrf=" + token()).statusCode());
        approved(false);
    }
    @Test void getCannotApprove() throws Exception {
        assertEquals(405, get("/api/approve?invoice=C-1001").statusCode()); approved(false);
    }
    @Test void noCorsReadPermission() throws Exception {
        assertTrue(foreignPost("/api/approve", "invoice=C-1001&_csrf=" + token())
            .headers().firstValue("Access-Control-Allow-Origin").isEmpty());
    }
}
