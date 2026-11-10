package net.sourceforge.kolmafia.utilities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PendingPromptTest {
  @AfterEach
  void afterEach() {
    var prompt = PendingPrompt.current();
    if (prompt != null) {
      prompt.complete(null);
    }
  }

  @Test
  void answerCompletesMatchingPrompt() {
    var prompt = PendingPrompt.open(PendingPrompt.Kind.TEXT, "name?", List.of());

    assertThat(PendingPrompt.current(), sameInstance(prompt));
    assertThat(PendingPrompt.answer(prompt.id, "Bob"), is(true));
    assertThat(prompt.join(), is("Bob"));
    assertThat(PendingPrompt.current(), nullValue());
  }

  @Test
  void answerIgnoresWrongId() {
    var prompt = PendingPrompt.open(PendingPrompt.Kind.CONFIRM, "sure?", List.of());

    assertThat(PendingPrompt.answer(prompt.id + 1, "true"), is(false));
    assertThat(prompt.isDone(), is(false));
  }

  @Test
  void firstAnswerWins() {
    var prompt = PendingPrompt.open(PendingPrompt.Kind.CONFIRM, "sure?", List.of());

    assertThat(prompt.complete("true"), is(true));
    assertThat(prompt.complete("false"), is(false));
    assertThat(prompt.join(), is("true"));
  }

  @Test
  void secondPromptWaitsForFirst() throws Exception {
    var first = PendingPrompt.open(PendingPrompt.Kind.TEXT, "one", List.of());
    var pool = Executors.newSingleThreadExecutor();
    try {
      var second = pool.submit(() -> PendingPrompt.open(PendingPrompt.Kind.TEXT, "two", List.of()));

      Thread.sleep(100);
      assertThat(second.isDone(), is(false));

      first.complete("a");
      assertThat(second.get(5, TimeUnit.SECONDS).message, is("two"));
    } finally {
      pool.shutdownNow();
    }
  }
}
