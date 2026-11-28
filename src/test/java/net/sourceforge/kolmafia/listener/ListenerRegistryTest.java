package net.sourceforge.kolmafia.listener;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class ListenerRegistryTest {
  @Test
  void anotherThreadCanDeferWhileDeferredListenersAreFiring() throws Exception {
    var registry = new ListenerRegistry();
    var otherThreadFinished = new AtomicBoolean(false);

    Listener waitsOnOtherThread =
        () -> {
          var other =
              new Thread(
                  () -> {
                    registry.deferListeners(true);
                    registry.fireListener("b");
                    registry.deferListeners(false);
                  });
          other.start();
          try {
            other.join(2000);
          } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
          }
          otherThreadFinished.set(!other.isAlive());
        };
    registry.registerListener("a", waitsOnOtherThread);

    registry.deferListeners(true);
    registry.fireListener("a");
    registry.deferListeners(false);

    assertThat(otherThreadFinished.get(), is(true));
  }
}
