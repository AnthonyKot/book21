package lab;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;
// Explicitly selected exercise; intentionally not part of the passing default *Test suite.
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties="lab.bind-title=true")
class SortRepairExercise extends QueryHttp {
    // Distinct fixtures verify primary ordering; an independent tie regression is reader work.
    @Test void defaultOrder() throws Exception { ids(get("/api/sorted",true),"C-1001","C-1002","C-1003"); }
    @Test void emptyUsesDefault() throws Exception { ids(get("/api/sorted?sort=",true),"C-1001","C-1002","C-1003"); }
    @Test void titleOrder() throws Exception { ids(get("/api/sorted?sort=title",true),"C-1003","C-1002","C-1001"); }
    @Test void amountOrder() throws Exception { ids(get("/api/sorted?sort=amount",true),"C-1002","C-1003","C-1001"); }
    @Test void unknownSortRejected() throws Exception { assertEquals(400,get("/api/sorted?sort=missing_column",true).statusCode()); }
    @Test void expressionRejected() throws Exception { assertEquals(400,get("/api/sorted?sort="+enc("amount DESC"),true).statusCode()); }
}
