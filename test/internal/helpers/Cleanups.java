package internal.helpers;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class Cleanups implements Closeable {
  public record OrderedRunnable(Runnable runnable, int order) {
    public OrderedRunnable(Runnable runnable) {
      this(runnable, 1);
    }
  }

  private final List<OrderedRunnable> cleanups = new ArrayList<>();

  public Cleanups() {}

  public Cleanups(Runnable r) {
    this(new OrderedRunnable(r));
  }

  public Cleanups(OrderedRunnable o) {
    cleanups.add(o);
  }

  public Cleanups(Cleanups... cleanups) {
    for (var c : cleanups) {
      this.cleanups.addAll(c.cleanups);
    }
  }

  public void add(Runnable r) {
    cleanups.add(new OrderedRunnable(r));
  }

  public void add(Cleanups c) {
    this.addAll(c.cleanups);
  }

  public void addAll(Collection<OrderedRunnable> r) {
    cleanups.addAll(r);
  }

  public void run() {
    cleanups.sort(Comparator.comparingInt(OrderedRunnable::order));
    cleanups.forEach(c -> c.runnable.run());
  }

  @Override
  public void close() {
    run();
  }
}
