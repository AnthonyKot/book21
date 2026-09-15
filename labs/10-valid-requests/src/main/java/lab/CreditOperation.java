package lab;

import static org.springframework.http.HttpStatus.*;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CreditOperation {
  private final JdbcTemplate jdbc;
  private final RaceGate gate;

  public CreditOperation(JdbcTemplate jdbc, RaceGate gate) {
    this.jdbc = jdbc;
    this.gate = gate;
  }

  public void apply(String id, String lane, String fault, boolean conditional) {
    if (!fault.isEmpty()) {
      gate.requireEnabled();
      if (!fault.equals("claim") && !fault.equals("balance"))
        throw new ResponseStatusException(BAD_REQUEST);
    }
    var credits =
        jdbc.query(
            "SELECT amount_cents,applied FROM credit WHERE id=? AND tenant=?",
            (rs, n) -> new Credit(rs.getInt(1), rs.getBoolean(2)),
            id,
            "cedar");
    if (credits.isEmpty()) throw new ResponseStatusException(NOT_FOUND);
    var credit = credits.getFirst();
    if (credit.applied()) throw new ResponseStatusException(CONFLICT);
    gate.pause(lane); // Test seam: both requests can reach this point before either writes.
    String sql =
        "UPDATE credit SET applied=TRUE WHERE id=? AND tenant=?"
            + (conditional ? " AND applied=FALSE" : "");
    int changed = jdbc.update(sql, id, "cedar");
    if (changed != 1) throw new ResponseStatusException(CONFLICT);
    if (fault.equals("claim")) throw new IllegalStateException("Synthetic failure after claim");
    if (jdbc.update(
            "UPDATE account SET balance_cents=balance_cents+? WHERE tenant=?",
            credit.amount(),
            "cedar")
        != 1) throw new IllegalStateException("Account missing");
    if (fault.equals("balance")) throw new IllegalStateException("Synthetic failure after balance");
  }

  private record Credit(int amount, boolean applied) {}
}
