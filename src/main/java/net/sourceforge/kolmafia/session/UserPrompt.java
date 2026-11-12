package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.swingui.UserPromptDialog;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.webui.RelayServer;

public final class UserPrompt<R> {
  public sealed interface Request<R> {
    String message();

    default String kind() {
      return this.getClass().getSimpleName().toLowerCase();
    }

    default List<?> choices() {
      return List.of();
    }

    default List<String> options() {
      return choices().stream().map(String::valueOf).toList();
    }

    default String initial() {
      return null;
    }

    R decode(String answer);
  }

  public record Alert(String message) implements Request<Void> {
    @Override
    public Void decode(final String answer) {
      return null;
    }
  }

  public record Confirm(String message) implements Request<Boolean> {
    @Override
    public Boolean decode(final String answer) {
      return "true".equals(answer);
    }
  }

  public record Text(String message, String initial) implements Request<String> {
    @Override
    public String decode(final String answer) {
      return answer;
    }
  }

  public record Select<T>(
      String message,
      List<T> choices,
      T initialChoice,
      BiFunction<Object, Boolean, String> renderer)
      implements Request<T> {
    @Override
    public T decode(final String answer) {
      var index = answer == null ? -1 : StringUtilities.parseInt(answer);
      return index >= 0 && index < choices.size() ? choices.get(index) : null;
    }
  }

  public record Multiple<T>(String message, List<T> choices, ListElementFilter filter)
      implements Request<List<T>> {
    public List<T> visibleChoices() {
      return filter == null ? choices : choices.stream().filter(filter::isVisible).toList();
    }

    @Override
    public List<String> options() {
      return visibleChoices().stream().map(String::valueOf).toList();
    }

    @Override
    public List<T> decode(final String answer) {
      var visible = visibleChoices();
      var selected = new ArrayList<T>();
      for (var index : StringUtilities.splitByComma(answer == null ? "" : answer)) {
        var i = StringUtilities.parseInt(index);
        if (!index.isEmpty() && i >= 0 && i < visible.size()) {
          selected.add(visible.get(i));
        }
      }
      return selected;
    }
  }

  private static final Object LOCK = new Object();
  private static volatile UserPrompt<?> current = null;
  private static long lastId = 0;

  public final long id;
  public final Request<R> request;
  private final CompletableFuture<R> answer = new CompletableFuture<>();

  private UserPrompt(final long id, final Request<R> request) {
    this.id = id;
    this.request = request;
  }

  public static <R> R ask(final Request<R> request) {
    return ask(request, 0, null);
  }

  public static <R> R ask(final Request<R> request, final long timeoutMillis, final R fallback) {
    if (StaticEntity.isHeadless()) {
      return UserPromptConsole.ask(request);
    }

    var prompt = open(request);
    if (timeoutMillis > 0) {
      prompt.expireAfter(timeoutMillis, fallback);
    }
    UserPromptDialog.show(prompt);
    return prompt.join();
  }

  public static <R> UserPrompt<R> open(final Request<R> request) {
    UserPrompt<R> prompt;
    synchronized (LOCK) {
      while (current != null) {
        try {
          LOCK.wait();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new CancellationException("Interrupted while waiting to prompt");
        }
      }
      prompt = new UserPrompt<>(++lastId, request);
      current = prompt;
    }
    RelayServer.promptChanged();
    return prompt;
  }

  public static UserPrompt<?> current() {
    return current;
  }

  public static boolean answer(final String command) {
    var parts = command.split(" ", 2);
    return answer(StringUtilities.parseLong(parts[0]), parts.length > 1 ? parts[1] : null);
  }

  public static boolean answer(final long id, final String value) {
    var prompt = current;
    return prompt != null && prompt.id == id && answer(prompt, value);
  }

  private static <R> boolean answer(final UserPrompt<R> prompt, final String value) {
    return prompt.complete(prompt.request.decode(value));
  }

  public boolean complete(final R value) {
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

  public void expireAfter(final long millis, final R fallback) {
    CompletableFuture.delayedExecutor(millis, TimeUnit.MILLISECONDS)
        .execute(() -> this.complete(fallback));
  }

  public boolean isDone() {
    return this.answer.isDone();
  }

  public void onAnswer(final Runnable action) {
    this.answer.thenRun(action);
  }

  public R join() {
    return this.answer.join();
  }
}
