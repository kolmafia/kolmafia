package net.sourceforge.kolmafia.session;

import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withHttpClientBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
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
