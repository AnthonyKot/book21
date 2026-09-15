package lab;

import org.springframework.stereotype.Service;

@Service
public class CreditWorker {
  private final CreditOperation operation;

  public CreditWorker(CreditOperation operation) {
    this.operation = operation;
  }

  public void apply(String id, String lane, String fault) {
    operation.apply(id, lane, fault, true);
  }
}
