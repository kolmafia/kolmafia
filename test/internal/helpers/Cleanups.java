package internal.helpers;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Cleanups implements Closeable {
  private final List<Runnable> cleanups = new ArrayList<>();

  public Cleanups() {}

  public Cleanups(Runnable r) {
    cleanups.add(r);
  }

  public Cleanups(Collection<Runnable> r) {
    this.addAll(r);
  }

  public void add(Runnable r) {
    cleanups.add(r);
  }

  public void add(Cleanups c) {
    this.addAll(c.cleanups);
  }

  public void addAll(Collection<Runnable> r) {
    cleanups.addAll(r);
  }

  public void run() {
    cleanups.forEach(Runnable::run);
  }

  @Override
  public void close() {
    run();
  }
}
