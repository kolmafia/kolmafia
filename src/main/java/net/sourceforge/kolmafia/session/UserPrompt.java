package net.sourceforge.kolmafia.session;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.sourceforge.kolmafia.webui.RelayServer;

public class UserPrompt {
  public enum Kind {
    ALERT,
    CONFIRM,
    TEXT,
    SELECT,
    MULTIPLE
  }

  private static final Object LOCK = new Object();
  private static volatile UserPrompt current = null;
  private static long lastId = 0;

  public final long id;
  public final Kind kind;
  public final String message;
  public final List<String> options;
  public final String initial;
  private final CompletableFuture<String> answer = new CompletableFuture<>();

  private UserPrompt(
      final long id,
      final Kind kind,
      final String message,
      final List<String> options,
      final String initial) {
    this.id = id;
    this.kind = kind;
    this.message = message;
    this.options = options;
    this.initial = initial;
  }

  public static UserPrompt open(final Kind kind, final String message, final List<String> options) {
    return open(kind, message, options, null);
  }

  public static UserPrompt open(
      final Kind kind, final String message, final List<String> options, final String initial) {
    UserPrompt prompt;
    synchronized (LOCK) {
      while (current != null) {
        try {
          LOCK.wait();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }
      prompt = new UserPrompt(++lastId, kind, message, options, initial);
      current = prompt;
    }
    RelayServer.promptChanged();
    return prompt;
  }

  public static UserPrompt current() {
    return current;
  }

  public static boolean answer(final long id, final String value) {
    var prompt = current;
    return prompt != null && prompt.id == id && prompt.complete(value);
  }

  public boolean complete(final String value) {
    if (!this.answer.complete(value)) {
      return false;
    }
    synchronized (LOCK) {
      if (current == this) {
        current = null;
      }
      LOCK.notifyAll();
    }
    RelayServer.promptChanged();
    return true;
  }

  public void expireAfter(final long millis, final String value) {
    CompletableFuture.delayedExecutor(millis, TimeUnit.MILLISECONDS)
        .execute(() -> this.complete(value));
  }

  public boolean isDone() {
    return this.answer.isDone();
  }

  public void onAnswer(final Runnable action) {
    this.answer.thenRun(action);
  }

  public String join() {
    return this.answer.join();
  }
}
