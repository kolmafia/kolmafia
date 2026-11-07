package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.getPostRequestBody;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withAdventuresLeft;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withMeat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import internal.network.FakeHttpResponse;
import java.net.http.HttpRequest;
import java.util.List;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.session.FamiliarTrainingManager.Goal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FamiliarTrainingManagerTest {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("FamiliarTrainingManagerTest");
  }

  @AfterEach
  void afterEach() {
    KoLmafia.forceContinue();
  }

  // A Grey Goose (Weight Lossy) with 25 experience weighs 5 pounds
  private static final int GOOSE_EXPERIENCE = 25;

  // A contest is a POST to arena.php with action=go in its body
  private static boolean isMatch(final HttpRequest request) {
    return request.uri().getPath().equals("/arena.php")
        && request.method().equals("POST")
        && getPostRequestBody(request).contains("action=go");
  }

  /** The Cake-Shaped Arena: the opponent list on a visit, and [match] for every contest. */
  private static FakeHttpResponse<String> arena(final HttpRequest request, final String match) {
    if (isMatch(request)) {
      return new FakeHttpResponse<>(match);
    }
    if (request.uri().getPath().equals("/arena.php")) {
      return new FakeHttpResponse<>(
          html("request/test_adventure_internally_tracks_cake_arena_visit.html"));
    }
    return new FakeHttpResponse<>("");
  }

  private static String win() {
    // "Weight Lossy is the winner, and gains 18 experience!"
    return html("request/test_adventure_internally_tracks_cake_arena_train.html");
  }

  private static String loss() {
    return win()
        .replace("is the winner, and gains 18 experience!", "loses.")
        .replace("Weight Lossy gains a pound!", "");
  }

  private static List<HttpRequest> matches(final FakeHttpClientBuilder builder) {
    return builder.client.getRequests().stream()
        .filter(FamiliarTrainingManagerTest::isMatch)
        .toList();
  }

  @Nested
  class Training {
    @Test
    void stopsAtOnceWhenTheGoalIsMet() {
      var builder = new FakeHttpClientBuilder();
      builder.client.setResponseFunc(r -> arena(r, win()));
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withFamiliar(FamiliarPool.GREY_GOOSE, GOOSE_EXPERIENCE),
              withAdventuresLeft(10),
              withMeat(10000));

      try (cleanups) {
        assertThat(FamiliarTrainingManager.levelFamiliar(5, Goal.BASE, false), is(true));

        assertThat(KoLmafia.getLastMessage(), is("Training session completed."));
        // it read the arena's opponents, then had nothing to fight for
        assertThat(
            FamiliarTrainingManager.getResults().getContent(), containsString("Vine Vidi Vici"));
        assertThat(matches(builder), empty());
      }
    }

    @Test
    void stopsWithoutAdventures() {
      var builder = new FakeHttpClientBuilder();
      builder.client.setResponseFunc(r -> arena(r, win()));
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withFamiliar(FamiliarPool.GREY_GOOSE, GOOSE_EXPERIENCE),
              withAdventuresLeft(0),
              withMeat(10000));

      try (cleanups) {
        assertThat(FamiliarTrainingManager.levelFamiliar(6, Goal.BASE, false), is(false));

        assertThat(KoLmafia.getLastMessage(), is("Training stopped: out of adventures."));
        assertThat(matches(builder), empty());
      }
    }

    @Test
    void stopsWithoutMeat() {
      var builder = new FakeHttpClientBuilder();
      builder.client.setResponseFunc(r -> arena(r, win()));
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withFamiliar(FamiliarPool.GREY_GOOSE, GOOSE_EXPERIENCE),
              withAdventuresLeft(10),
              withMeat(50));

      try (cleanups) {
        assertThat(FamiliarTrainingManager.levelFamiliar(6, Goal.BASE, false), is(false));

        assertThat(KoLmafia.getLastMessage(), is("Training stopped: out of meat."));
        assertThat(matches(builder), empty());
      }
    }

    @Test
    void debugModeChoosesAnOpponentWithoutFighting() {
      var builder = new FakeHttpClientBuilder();
      builder.client.setResponseFunc(r -> arena(r, win()));
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withFamiliar(FamiliarPool.GREY_GOOSE, GOOSE_EXPERIENCE),
              withAdventuresLeft(10),
              withMeat(10000));

      try (cleanups) {
        assertThat(FamiliarTrainingManager.levelFamiliar(6, Goal.BASE, true), is(true));

        assertThat(matches(builder), empty());
      }
    }

    @Test
    void fightsUntilTheGoalIsMet() {
      var builder = new FakeHttpClientBuilder();
      builder.client.setResponseFunc(r -> arena(r, win()));
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withFamiliar(FamiliarPool.GREY_GOOSE, GOOSE_EXPERIENCE),
              withAdventuresLeft(10),
              withMeat(10000));

      try (cleanups) {
        assertThat(FamiliarTrainingManager.levelFamiliar(6, Goal.BASE, false), is(true));

        assertThat(KoLmafia.getLastMessage(), is("Training session completed."));
        // 5 to 6 pounds takes 36 - 25 = 11 experience: one win worth 18
        assertThat(matches(builder), hasSize(1));
      }
    }

    @Test
    void stopsWhenTooDrunkToEnter() {
      // KoL's real reply to an overdrunk character: no contest, no adventure spent
      var builder = new FakeHttpClientBuilder();
      builder.client.setResponseFunc(r -> arena(r, html("request/test_cake_arena_too_drunk.html")));
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withFamiliar(FamiliarPool.GREY_GOOSE, GOOSE_EXPERIENCE),
              withAdventuresLeft(10),
              withMeat(10000));

      try (cleanups) {
        assertThat(FamiliarTrainingManager.levelFamiliar(6, Goal.BASE, false), is(false));

        // CakeArenaRequest stops on Susie's refusal, so training asks only once. Today the
        // final message is the generic one, not "You're too drunk".
        assertThat(matches(builder), hasSize(1));
        assertThat(KoLmafia.getLastMessage(), is("Training session aborted."));
        // No contest took place, so nothing is counted as spent
        assertThat(KoLCharacter.getAdventuresLeft(), is(10));
        assertThat(KoLCharacter.getAvailableMeat(), is(10000L));
      }
    }

    @Test
    void givesUpAfterFiveLossesInARow() {
      var builder = new FakeHttpClientBuilder();
      builder.client.setResponseFunc(r -> arena(r, loss()));
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withFamiliar(FamiliarPool.GREY_GOOSE, GOOSE_EXPERIENCE),
              withAdventuresLeft(10),
              withMeat(10000));

      try (cleanups) {
        assertThat(FamiliarTrainingManager.levelFamiliar(6, Goal.BASE, false), is(false));

        assertThat(KoLmafia.getLastMessage(), is("Too many consecutive losses."));
        assertThat(matches(builder), hasSize(5));
      }
    }
  }

  @Nested
  class LevelFamiliar {
    @Test
    void needsAFamiliar() {
      var builder = new FakeHttpClientBuilder();

      try (var cleanups = new Cleanups(withHttpClientBuilder(builder))) {
        assertThat(FamiliarTrainingManager.levelFamiliar(20, Goal.BASE, false), is(false));

        assertThat(KoLmafia.getLastMessage(), is("No familiar selected to train."));
        assertThat(
            FamiliarTrainingManager.getResults().getContent(),
            containsString("No familiar selected to train."));
        assertThat(builder.client.getRequests(), empty());
      }
    }

    @Test
    void needsAFamiliarThatCanCompete() {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(withHttpClientBuilder(builder), withFamiliar(FamiliarPool.PET_ROCK));

      try (cleanups) {
        assertThat(FamiliarTrainingManager.levelFamiliar(20, Goal.BASE, false), is(false));

        assertThat(KoLmafia.getLastMessage(), is("Don't know how to train a Pet Rock yet."));
        assertThat(builder.client.getRequests(), empty());
      }
    }
  }

  @Nested
  class BuffFamiliar {
    @Test
    void needsAFamiliar() {
      assertThat(FamiliarTrainingManager.buffFamiliar(20), is(false));
      assertThat(KoLmafia.getLastMessage(), is("You don't have a familiar equipped."));
    }

    @Test
    void doesNothingWhenAlreadyHeavyEnough() {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(withHttpClientBuilder(builder), withFamiliar(FamiliarPool.MOSQUITO, 400));

      try (cleanups) {
        assertThat(FamiliarTrainingManager.buffFamiliar(1), is(true));
        assertThat(builder.client.getRequests(), empty());
      }
    }
  }
}
