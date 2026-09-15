package lab;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
public class CreditController {
  private final CreditService service;
  private final CreditWorker worker;
  private final JdbcTemplate jdbc;
  private final RaceGate gate;

  public CreditController(
      CreditService service, CreditWorker worker, JdbcTemplate jdbc, RaceGate gate) {
    this.service = service;
    this.worker = worker;
    this.jdbc = jdbc;
    this.gate = gate;
  }

  @GetMapping("/csrf")
  Map<String, String> csrf(CsrfToken t) {
    return Map.of("header", t.getHeaderName(), "token", t.getToken());
  }

  @PostMapping(value = "/api/credits/{id}/apply", produces = "text/plain")
  String apply(
      @PathVariable String id,
      @RequestHeader(defaultValue = "", name = "X-Lab-Lane") String lane,
      @RequestHeader(defaultValue = "", name = "X-Lab-Fault") String fault) {
    service.apply(id, lane, fault);
    return "applied";
  }

  @PostMapping(value = "/api/credits/{id}/worker", produces = "text/plain")
  String worker(
      @PathVariable String id,
      @RequestHeader(defaultValue = "", name = "X-Lab-Lane") String lane,
      @RequestHeader(defaultValue = "", name = "X-Lab-Fault") String fault) {
    worker.apply(id, lane, fault);
    return "applied";
  }

  @GetMapping("/api/state")
  Map<String, Object> state() {
    return Map.of(
        "balanceCents",
        jdbc.queryForObject(
            "SELECT balance_cents FROM account WHERE tenant='cedar'", Integer.class),
        "appliedCredits",
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM credit WHERE tenant='cedar' AND applied=TRUE", Integer.class));
  }

  @GetMapping("/lab/gates")
  Map<String, Boolean> gates() {
    return gate.status();
  }

  @PostMapping("/lab/release/{lane}")
  void release(@PathVariable String lane) {
    gate.release(lane);
  }

  @PostMapping("/lab/reset")
  void reset() {
    gate.requireEnabled();
    gate.reset();
    jdbc.update("UPDATE credit SET applied=FALSE");
    jdbc.update("UPDATE account SET balance_cents=0");
  }
}
