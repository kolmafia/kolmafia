package net.sourceforge.kolmafia.session;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class UserPromptTest {
  @AfterEach
  void afterEach() {
    var prompt = UserPrompt.current();
    if (prompt != null) {
      prompt.complete(null);
    }
  }

  @Test
  void answerCompletesMatchingPrompt() {
    var prompt = UserPrompt.open(UserPrompt.Kind.TEXT, "name?", List.of());

    assertThat(UserPrompt.current(), sameInstance(prompt));
    assertThat(UserPrompt.answer(prompt.id, "Bob"), is(true));
    assertThat(prompt.join(), is("Bob"));
    assertThat(UserPrompt.current(), nullValue());
  }

  @Test
  void answerIgnoresWrongId() {
    var prompt = UserPrompt.open(UserPrompt.Kind.CONFIRM, "sure?", List.of());

    assertThat(UserPrompt.answer(prompt.id + 1, "true"), is(false));
    assertThat(prompt.isDone(), is(false));
  }

  @Test
  void firstAnswerWins() {
    var prompt = UserPrompt.open(UserPrompt.Kind.CONFIRM, "sure?", List.of());

    assertThat(prompt.complete("true"), is(true));
    assertThat(prompt.complete("false"), is(false));
    assertThat(prompt.join(), is("true"));
  }

  @Test
  void expiredPromptCompletesWithDefault() {
    var prompt = UserPrompt.open(UserPrompt.Kind.CONFIRM, "sure?", List.of());

    prompt.expireAfter(10, "false");

    assertThat(prompt.join(), is("false"));
    assertThat(UserPrompt.current(), nullValue());
  }

  @Test
  void answerBeforeExpiryWins() {
    var prompt = UserPrompt.open(UserPrompt.Kind.CONFIRM, "sure?", List.of());

    prompt.expireAfter(60_000, "false");
    UserPrompt.answer(prompt.id, "true");

    assertThat(prompt.join(), is("true"));
  }

  @Test
  void secondPromptWaitsForFirst() throws Exception {
    var first = UserPrompt.open(UserPrompt.Kind.TEXT, "one", List.of());
    var pool = Executors.newSingleThreadExecutor();
    try {
      var second = pool.submit(() -> UserPrompt.open(UserPrompt.Kind.TEXT, "two", List.of()));

      Thread.sleep(100);
      assertThat(second.isDone(), is(false));

      first.complete("a");
      assertThat(second.get(5, TimeUnit.SECONDS).message, is("two"));
    } finally {
      pool.shutdownNow();
    }
  }
}
