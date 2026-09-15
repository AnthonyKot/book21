package book21.detection;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReproductionTest extends HttpFixture {
    @ParameterizedTest @ValueSource(strings={"direct","alias","wrapped"})
    void injectionReturnsOtherTitles(String mode) throws Exception {
        assertEquals("[\"Budget\",\"Payroll\",\"O'Brien\"]", read(mode, ATTACK));
    }
    @ParameterizedTest @ValueSource(strings={"direct","alias","wrapped"})
    void ordinaryQueryStillWorks(String mode) throws Exception { assertEquals("[\"Budget\"]", read(mode,"Budget")); }
}
