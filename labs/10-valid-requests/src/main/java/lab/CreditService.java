package lab;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class CreditService {
  private final CreditOperation operation;

  @Value("${lab.conditional}")
  boolean conditional;

  public CreditService(CreditOperation operation) {
    this.operation = operation;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void apply(String id, String lane, String fault) {
    operation.apply(id, lane, fault, conditional);
  }
}
