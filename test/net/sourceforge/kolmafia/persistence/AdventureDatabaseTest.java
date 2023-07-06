package net.sourceforge.kolmafia.persistence;

import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.AdventureRequest;
import net.sourceforge.kolmafia.request.AdventureRequest.ShadowRift;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class AdventureDatabaseTest {

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("AdventureDatabase");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("AdventureDatabase");
  }

  @Nested
  class GetAdventure {
    @Test
    public void getAdventureHandlesErrorCases() {
      assertThat(AdventureDatabase.getAdventure(null), is(nullValue()));
      assertThat(AdventureDatabase.getAdventure(""), is(nullValue()));
    }

    @Test
    public void getAdventureHandlesRequiredExactMatch() {
      var adventure = AdventureDatabase.getAdventure("\"Summoning Chamber\"");
      assertThat(adventure.getAdventureName(), is("Summoning Chamber"));
    }

    @Test
    public void getAdventureHandlesSinglePartialMatch() {
      var adventure = AdventureDatabase.getAdventure("Summoning Chambe");
      assertThat(adventure.getAdventureName(), is("Summoning Chamber"));
    }

    @Test
    public void getAdventureReturnsNullOnNoMatch() {
      var adventure = AdventureDatabase.getAdventure("  what??? ");
      assertThat(adventure, is(nullValue()));
    }

    @Test
    public void getAdventureReturnsNullOnManyMatch() {
      var adventure = AdventureDatabase.getAdventure("S");
      assertThat(adventure, is(nullValue()));
    }

    @ParameterizedTest
    @CsvSource({
      "Hippy Camp (Hippy Disguise), Hippy Camp in Disguise",
      "Frat House (Frat Disguise), Frat House in Disguise",
      "The Junkyard, Post-War Junkyard",
    })
    public void getAdventureRecognizesLegacySynonyms(String name, String synonym) {
      var adventure1 = AdventureDatabase.getAdventure(name);
      var adventure2 = AdventureDatabase.getAdventure(synonym);
      assertEquals(adventure1, adventure2);
    }
  }

  @Nested
  class ValidateAdventure {
    @Test
    public void validatesCorrectArea() {
      assertTrue(AdventureDatabase.validateAdventureArea("Burnbarrel Blvd."));
    }

    @Test
    public void validatesInvalidArea() {
      assertFalse(AdventureDatabase.validateAdventureArea("Honest John's Shop"));
    }
  }

  @Nested
  class ShadowRiftAdventure {
    private static final String SHADOW_RIFT_URL =
        "adventure.php?snarfblat=" + AdventurePool.SHADOW_RIFT;

    @Test
    public void canFindGenericShadowRiftAdventure() {
      var cleanups = new Cleanups(withProperty("shadowRiftIngress", ""));
      try (cleanups) {
        var adventure = AdventureDatabase.getAdventureByURL(SHADOW_RIFT_URL);
        assertFalse(adventure == null);
        assertThat(adventure.getAdventureName(), is("Shadow Rift"));
      }
    }

    @ParameterizedTest
    @CsvSource({
      "desertbeach, Shadow Rift (Desert Beach)",
      "forestvillage, Shadow Rift (Forest Village)",
      "mclargehuge, Shadow Rift (Mt. McLargeHuge)",
      "beanstalk, Shadow Rift (Somewhere Over the Beanstalk)",
      "manor3, Shadow Rift (Spookyraven Manor Third Floor)",
      "8bit, Shadow Rift (The 8-Bit Realm)",
      "pyramid, Shadow Rift (The Ancient Buried Pyramid)",
      "giantcastle, Shadow Rift (The Castle in the Clouds in the Sky)",
      "woods, Shadow Rift (The Distant Woods)",
      "hiddencity, Shadow Rift (The Hidden City)",
      "cemetery, Shadow Rift (The Misspelled Cemetary)",
      "plains, Shadow Rift (The Nearby Plains)",
      "town_right, Shadow Rift (The Right Side of the Tracks)",
    })
    public void canFindShadowRiftAdventureFromProperty(String property, String adventureName) {
      var cleanups = new Cleanups(withProperty("shadowRiftIngress", property));
      try (cleanups) {
        var adventure = AdventureDatabase.getAdventureByURL(SHADOW_RIFT_URL);
        assertFalse(adventure == null);
        assertThat(adventure.getAdventureName(), is(adventureName));
      }
    }

    private static final AdventureResult SHADOW_AFFINITY =
        EffectPool.get(EffectPool.SHADOW_AFFINITY);

    private void runRiftAdventure(ShadowRift rift, boolean first) {
      boolean free = KoLConstants.activeEffects.contains(SHADOW_AFFINITY);

      String adventureName = rift.getAdventureName();
      var adventure = AdventureDatabase.getAdventure(adventureName);
      assertFalse(adventure == null);
      assertThat(adventure.getAdventureName(), is(adventureName));
      var request = (AdventureRequest) adventure.getRequest();
      assertFalse(request == null);

      // We have a request ready to go. Rather than actually running it,
      // we'll call updateFields(), which chooses the URL in run(), just
      // before the request is submitted
      request.updateFields();

      // The first time we visit a rift, we expect the URL that will
      // be submitted is the place.php with whichplace/action for the
      // specified rift. Subsequently, we'll go straight to adventure.php
      String expectedURL = first ? rift.getCurrentURL() : SHADOW_RIFT_URL;
      String actualURL = request.getURLString();
      if (first && free) {
        assertTrue(expectedURL.contains("_free"));
      }
      assertEquals(expectedURL, actualURL);

      // As a side effect of (preparing to) run the request, we save
      // the container where the Rift was seen
      assertEquals(Preferences.getString("shadowRiftIngress"), rift.getPlace());
    }

    @Test
    public void canMinimizeRedirections() {
      var cleanups = new Cleanups(withProperty("shadowRiftIngress", ""));
      try (cleanups) {
        // Enter a rift for the first time
        runRiftAdventure(ShadowRift.CITY, true);
        // Enter the same rift again
        runRiftAdventure(ShadowRift.CITY, false);
        // Enter a different rift for the first time
        runRiftAdventure(ShadowRift.PLAINS, true);
        // Enter a first rift again for the first time
        runRiftAdventure(ShadowRift.CITY, true);
      }
    }

    @Test
    public void willUseFreeURLWithShadowAffinity() {
      var cleanups =
          new Cleanups(withProperty("shadowRiftIngress", ""), withEffect("Shadow Affinity", 11));
      try (cleanups) {
        // Enter a rift for the first time
        runRiftAdventure(ShadowRift.CITY, true);
        // Enter the same rift again
        runRiftAdventure(ShadowRift.CITY, false);
        // Enter a different rift for the first time
        runRiftAdventure(ShadowRift.PLAINS, true);
        // Enter a first rift again for the first time
        runRiftAdventure(ShadowRift.CITY, true);
      }
    }
  }
}
