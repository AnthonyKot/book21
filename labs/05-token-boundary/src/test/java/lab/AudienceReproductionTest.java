package lab;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "lab.audience-check=false")
class AudienceReproductionTest extends TokenChecks {
    @Test void billingTokenIsWronglyAccepted() throws Exception {
        assertThat(read("billing", "C-1001").statusCode()).isEqualTo(200);
    }
    @Test void missingAudienceIsWronglyAccepted() throws Exception {
        assertThat(read("no-audience", "C-1001").statusCode()).isEqualTo(200);
    }
}
