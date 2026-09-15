package lab;

import static org.junit.jupiter.api.Assertions.*;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"lab.gates=true", "lab.conditional=${test.conditional:true}"})
class CreditRepairTest extends CreditHttp {
  @Test
  void ordinaryCredit() throws Exception {
    assertEquals(200, apply("C-1001", ""));
    state(1000, 1);
  }

  @Test
  void sequentialDuplicate() throws Exception {
    assertEquals(200, apply("C-1001", ""));
    assertEquals(409, apply("C-1001", ""));
    state(1000, 1);
  }

  @Test
  void aCommitsFirst() throws Exception {
    race("A", true, false);
  }

  @Test
  void bCommitsFirst() throws Exception {
    race("B", true, false);
  }

  @Test
  void bothReleased() throws Exception {
    var a = send("C-1001", "A");
    var b = send("C-1001", "B");
    try {
      gate.awaitBoth();
    } finally {
      gate.release("A");
      gate.release("B");
    }
    var statuses =
        List.of(a.get(10, TimeUnit.SECONDS).statusCode(), b.get(10, TimeUnit.SECONDS).statusCode())
            .stream()
            .sorted()
            .toList();
    System.out.println("RELEASE BOTH: " + statuses);
    assertEquals(List.of(200, 409), statuses);
    state(1000, 1);
  }

  @Test
  void distinctCredits() throws Exception {
    race("A", true, true);
  }

  @Test
  void otherTenant() throws Exception {
    assertEquals(404, apply("B-2001", ""));
    state(0, 0);
    assertEquals(
        0,
        jdbc.queryForObject(
            "SELECT balance_cents FROM account WHERE tenant='birch'", Integer.class));
  }

  @Test
  void unknownCredit() throws Exception {
    assertEquals(404, apply("missing", ""));
    state(0, 0);
  }

  @Test
  void rollbackAfterClaim() throws Exception {
    assertEquals(500, apply("C-1001", "claim"));
    state(0, 0);
    assertEquals(200, apply("C-1001", ""));
    state(1000, 1);
  }

  @Test
  void rollbackAfterBalance() throws Exception {
    assertEquals(500, apply("C-1001", "balance"));
    state(0, 0);
    assertEquals(200, apply("C-1001", ""));
    state(1000, 1);
  }

  @Test
  void csrfRequired() throws Exception {
    assertEquals(
        401,
        client
            .send(
                request("C-1001", "apply", "", "", false, true),
                HttpResponse.BodyHandlers.ofString())
            .statusCode());
    state(0, 0);
  }

  @Test
  void authenticationRequired() throws Exception {
    assertEquals(
        401,
        client
            .send(
                request("C-1001", "apply", "", "", true, false),
                HttpResponse.BodyHandlers.ofString())
            .statusCode());
    state(0, 0);
  }
}
