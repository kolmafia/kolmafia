package net.sourceforge.kolmafia.session;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
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
    var prompt = UserPrompt.open(new UserPrompt.Text("name?", null));

    assertThat(UserPrompt.current(), sameInstance(prompt));
    assertThat(UserPrompt.answer(prompt.id, "Bob"), is(true));
    assertThat(prompt.join(), is("Bob"));
    assertThat(UserPrompt.current(), nullValue());
  }

  @Test
  void answerIgnoresWrongId() {
    var prompt = UserPrompt.open(new UserPrompt.Confirm("sure?"));

    assertThat(UserPrompt.answer(prompt.id + 1, "true"), is(false));
    assertThat(prompt.isDone(), is(false));
  }

  @Test
  void firstAnswerWins() {
    var prompt = UserPrompt.open(new UserPrompt.Confirm("sure?"));

    assertThat(prompt.complete(true), is(true));
    assertThat(prompt.complete(false), is(false));
    assertThat(prompt.join(), is(true));
  }

  @Test
  void expiredPromptCompletesWithFallback() {
    var prompt = UserPrompt.open(new UserPrompt.Confirm("sure?"));

    prompt.expireAfter(10, false);

    assertThat(prompt.join(), is(false));
    assertThat(UserPrompt.current(), nullValue());
  }

  @Test
  void answerBeforeExpiryWins() {
    var prompt = UserPrompt.open(new UserPrompt.Confirm("sure?"));

    prompt.expireAfter(60_000, false);
    UserPrompt.answer(prompt.id, "true");

    assertThat(prompt.join(), is(true));
  }

  @Test
  void secondPromptWaitsForFirst() throws Exception {
    var first = UserPrompt.open(new UserPrompt.Text("one", null));
    var pool = Executors.newSingleThreadExecutor();
    try {
      var second = pool.submit(() -> UserPrompt.open(new UserPrompt.Text("two", null)));

      Thread.sleep(100);
      assertThat(second.isDone(), is(false));

      first.complete("a");
      assertThat(second.get(5, TimeUnit.SECONDS).request.message(), is("two"));
    } finally {
      pool.shutdownNow();
    }
  }

  @Nested
  class Decoding {
    @Test
    void selectDecodesIndex() {
      var request = new UserPrompt.Select<>("pick", List.of("a", "b", "c"), null, null);

      assertThat(request.decode("1"), is("b"));
      assertThat(request.decode("7"), nullValue());
      assertThat(request.decode(null), nullValue());
    }

    @Test
    void multipleDecodesIndices() {
      var request = new UserPrompt.Multiple<>("pick", List.of("a", "b", "c"), null);

      assertThat(request.decode("0,2"), contains("a", "c"));
      assertThat(request.decode(""), empty());
      assertThat(request.decode(null), empty());
    }

    @Test
    void kindIsRecordName() {
      assertThat(new UserPrompt.Confirm("sure?").kind(), is("confirm"));
      assertThat(new UserPrompt.Multiple<>("pick", List.of(), null).kind(), is("multiple"));
    }
  }
}
