package lab;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties="lab.bind-title=${test.bind-title:false}")
class QueryReproductionTest extends QueryHttp {
    @Test void ownTitle() throws Exception { ids(search("Office"),"C-1001"); }
    @Test void noMatch() throws Exception { ids(search("missing")); }
    @Test void foreignTitle() throws Exception { ids(search("Private")); }
    @Test void apostrophe() throws Exception { assertEquals(500,search("O'Brien").statusCode()); }
    @Test void percentIsLiteral() throws Exception { ids(search("100% sample"),"C-1003"); ids(search("%")); }
    @Test void predicateInjection() throws Exception {
        var r=search("' OR 1=1 -- ");
        assertEquals(200,r.statusCode());
        assertTrue(r.body().contains("B-2001"));
        assertEquals(4,PatternCount.ids(r.body()));
    }
    @Test void targetedInjection() throws Exception {
        ids(search("' OR id = 'B-2001' -- "),"B-2001");
    }
    @Test void anonymousDenied() throws Exception { assertEquals(401,get("/api/search?title=Office",false).statusCode()); }
    @Test void scopedOwn() throws Exception { ids(get("/api/invoices/C-1001",true),"C-1001"); }
    @Test void scopedForeignDenied() throws Exception { assertEquals(404,get("/api/invoices/B-2001",true).statusCode()); }
    @Test void boundButUnscopedStillLeaks() throws Exception { ids(get("/api/unscoped/B-2001",true),"B-2001"); }
}
