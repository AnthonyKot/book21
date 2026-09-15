package lab;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"lab.partition=${test.partition:false}"})
class ReviewReproductionTest extends ReviewHttp {
    @Test void ownCold()throws Exception{var r=preview("alice","C-1001");assertEquals(200,r.statusCode());assertEquals("CEDAR-PRIVATE-SUMMARY",r.body());assertEquals(1,store.bodyLoads.get());}
    @Test void foreignCold()throws Exception{noContent(preview("bob","C-1001"));assertEquals(0,previews.entries());}
    @Test void sameTenantWarm()throws Exception{preview("alice","C-1001");assertEquals("CEDAR-PRIVATE-SUMMARY",preview("alice","C-1001").body());assertEquals(1,store.bodyLoads.get());}
    @Test void cedarWarmsBirchReads()throws Exception{
        preview("alice","C-1001");var r=preview("bob","C-1001");
        assertEquals(200,r.statusCode());assertEquals("CEDAR-PRIVATE-SUMMARY",r.body());
        assertEquals(1,store.bodyLoads.get());
    }
    @Test void birchWarmsCedarReads()throws Exception{
        preview("bob","B-2001");var r=preview("alice","B-2001");
        assertEquals(200,r.statusCode());assertEquals("BIRCH-PRIVATE-SUMMARY",r.body());
    }
    @Test void deniedMissDoesNotPoisonOwner()throws Exception{noContent(preview("bob","C-1001"));assertEquals("CEDAR-PRIVATE-SUMMARY",preview("alice","C-1001").body());}
    @Test void queryTenantIsIgnored()throws Exception{noContent(get("bob","C-1001","preview","?tenant=cedar"));}
    @Test void unknownDocument()throws Exception{noContent(preview("alice","missing"));}
    @Test void anonymousCannotUseWarmCache()throws Exception{preview("alice","C-1001");assertEquals(401,preview("","C-1001").statusCode());assertEquals(1,store.bodyLoads.get());}
    @Test void anotherOwnDocument()throws Exception{preview("alice","C-1001");assertEquals("CEDAR-SECOND-SUMMARY",preview("alice","C-1002").body());assertEquals(2,previews.entries());}
}
