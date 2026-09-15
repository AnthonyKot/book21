package lab;

import static org.springframework.http.HttpStatus.*;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class RaceGate {
  @Value("${lab.gates}")
  boolean enabled;

  static class Lane {
    final CountDownLatch arrived = new CountDownLatch(1), release = new CountDownLatch(1);
    final AtomicBoolean taken = new AtomicBoolean();
  }

  private volatile Map<String, Lane> lanes = Map.of("A", new Lane(), "B", new Lane());

  void requireEnabled() {
    if (!enabled) throw new ResponseStatusException(NOT_FOUND);
  }

  private Lane lane(String name) {
    requireEnabled();
    Lane l = lanes.get(name);
    if (l == null) throw new ResponseStatusException(BAD_REQUEST);
    return l;
  }

  public void pause(String name) {
    if (name.isEmpty()) return;
    Lane l = lane(name);
    if (!l.taken.compareAndSet(false, true)) throw new ResponseStatusException(CONFLICT);
    l.arrived.countDown();
    try {
      if (!l.release.await(30, TimeUnit.SECONDS))
        throw new IllegalStateException("Lab gate timed out");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  public void release(String name) {
    lane(name).release.countDown();
  }

  public Map<String, Boolean> status() {
    requireEnabled();
    return Map.of(
        "A", lanes.get("A").arrived.getCount() == 0, "B", lanes.get("B").arrived.getCount() == 0);
  }

  public void awaitBoth() throws InterruptedException {
    for (Lane l : lanes.values())
      if (!l.arrived.await(10, TimeUnit.SECONDS))
        throw new IllegalStateException("Both requests did not arrive");
  }

  public void reset() {
    requireEnabled();
    for (Lane l : lanes.values()) l.release.countDown();
    lanes = Map.of("A", new Lane(), "B", new Lane());
  }
}
