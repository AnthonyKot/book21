package lab;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Post-attempt check for the /api/sorted practice contract. Leave this file closed
 * until you have saved your own attempt and regression: it encodes the contract's
 * observable outcomes, not a design. Excluded from the default `mvn test`; run with
 *   mvn -DreviewCheck=true -Dtest=SortContractCheck test
 * Distinct fixture titles and amounts verify primary ordering only; a tie regression
 * is your own work.
 */
@EnabledIfSystemProperty(named = "reviewCheck", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "lab.bind-title=true")
class SortContractCheck extends QueryHttp {
    @Test void absentSortUsesDefault() throws Exception { ids(get("/api/sorted", true), "C-1001", "C-1002", "C-1003"); }
    @Test void emptySortUsesDefault() throws Exception { ids(get("/api/sorted?sort=", true), "C-1001", "C-1002", "C-1003"); }
    @Test void titleOrderHolds() throws Exception { ids(get("/api/sorted?sort=title", true), "C-1003", "C-1002", "C-1001"); }
    @Test void amountOrderHolds() throws Exception { ids(get("/api/sorted?sort=amount", true), "C-1002", "C-1003", "C-1001"); }
    @Test void unknownKeyIsClientError() throws Exception { assertEquals(400, get("/api/sorted?sort=missing_column", true).statusCode()); }
    @Test void expressionIsClientError() throws Exception { assertEquals(400, get("/api/sorted?sort=" + enc("amount DESC"), true).statusCode()); }
}
