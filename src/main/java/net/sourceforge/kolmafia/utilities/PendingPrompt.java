package net.sourceforge.kolmafia.utilities;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.sourceforge.kolmafia.webui.RelayServer;

public class PendingPrompt {
  public enum Kind {
    CONFIRM,
    TEXT,
    SELECT
  }

  private static final Object LOCK = new Object();
  private static volatile PendingPrompt current = null;
  private static long lastId = 0;

  public final long id;
  public final Kind kind;
  public final String message;
  public final List<String> options;
  private final CompletableFuture<String> answer = new CompletableFuture<>();

  private PendingPrompt(
      final long id, final Kind kind, final String message, final List<String> options) {
    this.id = id;
    this.kind = kind;
    this.message = message;
    this.options = options;
  }

  public static PendingPrompt open(
      final Kind kind, final String message, final List<String> options) {
    PendingPrompt prompt;
    synchronized (LOCK) {
      while (current != null) {
        try {
          LOCK.wait();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }
      prompt = new PendingPrompt(++lastId, kind, message, options);
      current = prompt;
    }
    RelayServer.promptChanged();
    return prompt;
  }

  public static PendingPrompt current() {
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
