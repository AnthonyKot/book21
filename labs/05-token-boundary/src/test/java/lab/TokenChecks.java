package lab;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

/** Controls that must keep working in both modes, not only after the audience repair. */
abstract class TokenChecks extends TokenHttp {
    @Test void legitimateRead() throws Exception {
        var response = read("valid", "C-1001");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Cedar Dental").doesNotContain("Birch Legal");
    }
    @Test void tenantBoundarySurvivesAuthentication() throws Exception {
        assertThat(read("valid", "B-2001").statusCode()).isEqualTo(404);
    }
    @Test void noBearerIsRejected() throws Exception { assertThat(read(null, "C-1001").statusCode()).isEqualTo(401); }
    @Test void expiredIsRejected() throws Exception { assertThat(read("expired", "C-1001").statusCode()).isEqualTo(401); }
    @Test void futureIsRejected() throws Exception { assertThat(read("future", "C-1001").statusCode()).isEqualTo(401); }
    @Test void wrongIssuerIsRejected() throws Exception { assertThat(read("wrong-issuer", "C-1001").statusCode()).isEqualTo(401); }
    @Test void wrongKeyIsRejected() throws Exception { assertThat(read("wrong-key", "C-1001").statusCode()).isEqualTo(401); }
    @Test void expiryIsRequired() throws Exception { assertThat(read("no-expiry", "C-1001").statusCode()).isEqualTo(401); }
    @Test void subjectIsRequired() throws Exception { assertThat(read("no-subject", "C-1001").statusCode()).isEqualTo(401); }
    @Test void blankSubjectIsRejected() throws Exception { assertThat(read("blank-subject", "C-1001").statusCode()).isEqualTo(401); }
    @Test void unknownSubjectHasNoLocalAccount() throws Exception { assertThat(read("unknown-user", "C-1001").statusCode()).isEqualTo(403); }
}
