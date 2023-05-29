package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withChateau;
import static internal.helpers.Player.withLimitMode;
import static internal.helpers.Player.withNotAllowedInStandard;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withRestricted;
import static internal.helpers.Player.withSign;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RestrictedItemType;
import net.sourceforge.kolmafia.ZodiacSign;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.LimitMode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class ChateauRequestTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("ChateauRequestTest");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("ChateauRequestTest");
    ChateauRequest.reset();
  }

  @Nested
  class FreeRest {
    @BeforeEach
    public void beforeEach() {
      ChateauRequest.gainItem(ItemPool.get(ItemPool.CHATEAU_FAN, 1));
    }

    public Cleanups propertyCleanups() {
      return new Cleanups(withProperty("chateauAvailable", true), withProperty("timesRested", 0));
    }

    @ParameterizedTest
    @CsvSource({
      "chateau_restlabelfree, request/test_request_chateau_restlabelfree_next_free.html",
      "chateau_restbox, request/test_request_chateau_restbox_next_free.html"
    })
    public void tracksAndDoesNotSetRestsToMaxIfNextFree(String action, String filename) {
      var cleanups = propertyCleanups();
      try (cleanups) {
        ChateauRequest request = new ChateauRequest(action);
        request.responseText = html(filename);
        request.setHasResult(true);
        request.processResponse();
        assertThat("timesRested", isSetTo(1));
      }
    }

    @ParameterizedTest
    @CsvSource({
      "chateau_restlabelfree, request/test_request_chateau_restlabelfree_next_nonfree.html",
      "chateau_restbox, request/test_request_chateau_restbox_next_nonfree.html",
      "chateau_restlabel, request/test_request_chateau_restlabel.html"
    })
    public void setsRestsToMaxIfNextNonFree(String action, String filename) {
      var cleanups = propertyCleanups();
      try (cleanups) {
        ChateauRequest request = new ChateauRequest(action);
        request.responseText = html(filename);
        request.setHasResult(true);
        request.processResponse();
        assertThat("timesRested", isSetTo(KoLCharacter.freeRestsAvailable()));
      }
    }
  }

  @Nested
  class Modifiers {
    @ParameterizedTest
    @CsvSource({
      ItemPool.CHATEAU_SKYLIGHT + ", Adventures",
      ItemPool.CHATEAU_CHANDELIER + ", PvP Fights",
    })
    void appliesModifiersFromChateau(final int itemId, final String modifierName) {
      var cleanups = withChateau(itemId);

      try (cleanups) {
        assertThat(
            KoLCharacter.currentNumericModifier(DoubleModifier.byCaselessName(modifierName)),
            is(3.0));
      }
    }
  }

  @Nested
  class Available {
    @Test
    void notAvailableIfNoChateau() {
      assertThat(ChateauRequest.chateauAvailable(), is(false));
    }

    @Test
    void availableIfInNoPath() {
      var cleanups = withProperty("chateauAvailable", true);

      try (cleanups) {
        assertThat(ChateauRequest.chateauAvailable(), is(true));
      }
    }

    @Test
    void notAvailableIfRestricted() {
      var cleanups =
          new Cleanups(
              withProperty("chateauAvailable", true),
              withPath(Path.STANDARD),
              withRestricted(true),
              withNotAllowedInStandard(RestrictedItemType.ITEMS, "Chateau Mantegna room key"));

      try (cleanups) {
        assertThat(ChateauRequest.chateauAvailable(), is(false));
      }
    }

    @Test
    void availableIfLoLChateauEvenIfRestricted() {
      var cleanups =
          new Cleanups(
              withProperty("replicaChateauAvailable", true),
              withPath(Path.LEGACY_OF_LOATHING),
              withRestricted(true),
              withNotAllowedInStandard(RestrictedItemType.ITEMS, "Chateau Mantegna room key"));

      try (cleanups) {
        assertThat(ChateauRequest.chateauAvailable(), is(true));
      }
    }

    @Test
    void notAvailableIfInExploathing() {
      var cleanups =
          new Cleanups(
              withProperty("chateauAvailable", true), withPath(Path.KINGDOM_OF_EXPLOATHING));

      try (cleanups) {
        assertThat(ChateauRequest.chateauAvailable(), is(false));
      }
    }

    @Test
    void notAvailableIfInBadMoon() {
      var cleanups =
          new Cleanups(
              withProperty("chateauAvailable", true),
              withPath(Path.BAD_MOON),
              withSign(ZodiacSign.BAD_MOON));

      try (cleanups) {
        assertThat(ChateauRequest.chateauAvailable(), is(false));
      }
    }

    @Test
    void notAvailableIfNoMountains() {
      var cleanups =
          new Cleanups(withProperty("chateauAvailable", true), withLimitMode(LimitMode.SPELUNKY));

      try (cleanups) {
        assertThat(ChateauRequest.chateauAvailable(), is(false));
      }
    }
  }
}
