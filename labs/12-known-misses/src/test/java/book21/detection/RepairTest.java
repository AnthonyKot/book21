package book21.detection;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RepairTest extends HttpFixture {
    @ParameterizedTest @ValueSource(strings={"bound","repaired"})
    void attackIsLiteralData(String mode) throws Exception { assertEquals("[]", read(mode,ATTACK)); }
    @ParameterizedTest @ValueSource(strings={"bound","repaired"})
    void ordinaryTitleWorks(String mode) throws Exception { assertEquals("[\"Budget\"]",read(mode,"Budget")); }
    @ParameterizedTest @ValueSource(strings={"bound","repaired"})
    void apostropheIsPreserved(String mode) throws Exception { assertEquals("[\"O'Brien\"]",read(mode,"O'Brien")); }
}
