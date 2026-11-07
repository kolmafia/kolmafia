package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.getPostRequestBody;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withAdventuresLeft;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withMP;
import static internal.helpers.Player.withMeat;
import static internal.helpers.Player.withSkill;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import internal.network.FakeHttpResponse;
import java.net.http.HttpRequest;
import java.util.List;
import javax.swing.SwingUtilities;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
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

  private static Cleanups with5PoundGreyGoose() {
    return withFamiliar(FamiliarPool.GREY_GOOSE, 25);
  }

  private static boolean isMatch(final HttpRequest request) {
    return request.uri().getPath().equals("/arena.php")
        && request.method().equals("POST")
        && getPostRequestBody(request).contains("action=go");
  }

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
    return html("request/test_cake_arena_loss.html");
  }

  /** The results buffer adds text on the Swing thread: let it catch up before reading it. */
  private static String results() throws Exception {
    SwingUtilities.invokeAndWait(() -> {});
    return FamiliarTrainingManager.getResults().getContent();
  }

  private static List<HttpRequest> matches(final FakeHttpClientBuilder builder) {
    return builder.client.getRequests().stream()
        .filter(FamiliarTrainingManagerTest::isMatch)
        .toList();
  }

  @Nested
  class Training {
    @Test
    void stopsAtOnceWhenTheGoalIsMet() throws Exception {
      var builder = new FakeHttpClientBuilder();
      builder.client.setResponseFunc(r -> arena(r, win()));
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              with5PoundGreyGoose(),
              withAdventuresLeft(10),
              withMeat(10000));

      try (cleanups) {
        assertThat(FamiliarTrainingManager.levelFamiliar(5, Goal.BASE, false), is(true));

        assertThat(KoLmafia.getLastMessage(), is("Training session completed."));
        assertThat(results(), containsString("Vine Vidi Vici"));
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
              with5PoundGreyGoose(),
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
              with5PoundGreyGoose(),
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
              with5PoundGreyGoose(),
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
              with5PoundGreyGoose(),
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
      var builder = new FakeHttpClientBuilder();
      builder.client.setResponseFunc(r -> arena(r, html("request/test_cake_arena_too_drunk.html")));
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              with5PoundGreyGoose(),
              withAdventuresLeft(10),
              withMeat(10000));

      try (cleanups) {
        assertThat(FamiliarTrainingManager.levelFamiliar(6, Goal.BASE, false), is(false));

        assertThat(matches(builder), hasSize(1));
        assertThat(KoLmafia.getLastMessage(), is("Training session aborted."));
        // too drunk to enter: KoL charges no adventure or meat
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
              with5PoundGreyGoose(),
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
    void needsAFamiliar() throws Exception {
      var builder = new FakeHttpClientBuilder();

      try (var cleanups = new Cleanups(withHttpClientBuilder(builder))) {
        assertThat(FamiliarTrainingManager.levelFamiliar(20, Goal.BASE, false), is(false));

        assertThat(KoLmafia.getLastMessage(), is("No familiar selected to train."));
        assertThat(results(), containsString("No familiar selected to train."));
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
    void needsAFamiliar() throws Exception {
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

  private static List<HttpRequest> requestsWith(
      final FakeHttpClientBuilder builder, final String field, final int value) {
    String wanted = field + "=" + value;
    return builder.client.getRequests().stream()
        .filter(
            r ->
                String.valueOf(r.uri().getQuery()).contains(wanted)
                    || (r.method().equals("POST") && getPostRequestBody(r).contains(wanted)))
        .toList();
  }

  @Nested
  class LearnParameters {
    @Test
    void needsTwelveAdventuresATrial() {
      var builder = new FakeHttpClientBuilder();
      builder.client.setResponseFunc(r -> arena(r, win()));
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              with5PoundGreyGoose(),
              withAdventuresLeft(11),
              withMeat(10000));

      try (cleanups) {
        assertThat(FamiliarTrainingManager.learnFamiliarParameters(1), nullValue());

        assertThat(
            KoLmafia.getLastMessage(), is("You need to have at least 12 adventures available."));
        assertThat(matches(builder), empty());
      }
    }

    @Test
    void needsMeatForEveryContest() {
      var builder = new FakeHttpClientBuilder();
      builder.client.setResponseFunc(r -> arena(r, win()));
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              with5PoundGreyGoose(),
              withAdventuresLeft(20),
              withMeat(1199));

      try (cleanups) {
        assertThat(FamiliarTrainingManager.learnFamiliarParameters(1), nullValue());

        assertThat(
            KoLmafia.getLastMessage(), is("You need to have at least 1,200 meat available."));
        assertThat(matches(builder), empty());
      }
    }

    @Test
    void entersEachEventAtEachRank() {
      var builder = new FakeHttpClientBuilder();
      builder.client.setResponseFunc(r -> arena(r, win()));
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              with5PoundGreyGoose(),
              withAdventuresLeft(20),
              withMeat(10000));

      try (cleanups) {
        int[] skills = FamiliarTrainingManager.learnFamiliarParameters(1);

        assertThat(skills, notNullValue());
        assertThat(skills.length, is(4));
        // four events, three ranks each
        assertThat(matches(builder), hasSize(12));
      }
    }
  }

  @Nested
  class BuffsAndGear {
    @Test
    void castsEmpathyToReachTheGoal() {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              with5PoundGreyGoose(),
              withSkill(SkillPool.EMPATHY_OF_THE_NEWT),
              // Empathy needs a turtle-taming tool
              withItem(ItemPool.TURTLE_TOTEM),
              withMP(100, 100, 100));

      try (cleanups) {
        FamiliarTrainingManager.buffFamiliar(6);

        assertThat(requestsWith(builder, "whichskill", SkillPool.EMPATHY_OF_THE_NEWT), hasSize(1));
      }
    }

    @Test
    void wearsWeightGearToReachTheGoal() {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              with5PoundGreyGoose(),
              withItem(ItemPool.LEAD_NECKLACE));

      try (cleanups) {
        FamiliarTrainingManager.buffFamiliar(8);

        assertThat(requestsWith(builder, "whichitem", ItemPool.LEAD_NECKLACE), hasSize(1));
      }
    }
  }
}
