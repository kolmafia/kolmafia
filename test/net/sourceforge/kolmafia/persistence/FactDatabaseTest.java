package net.sourceforge.kolmafia.persistence;

import static internal.helpers.Player.withContinuationState;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.blankString;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.FactDatabase.FactType;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

class FactDatabaseTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("FactDatabaseTest");
    Preferences.reset("FactDatabaseTest");
  }

  @Nested
  class Database {
    @Test
    void databaseParsesWithNoErrors() {
      var stream = new ByteArrayOutputStream();
      try (var out = new PrintStream(stream, true)) {
        RequestLogger.openCustom(out);
        FactDatabase.reset();
        RequestLogger.closeCustom();
      }
      String logged = stream.toString().trim();
      assertThat(logged, blankString());
    }

    @ParameterizedTest
    @CsvSource({
      // Invalid phylum
      "Bird\tItem\ttoast, Invalid phylum: Bird",
      "Orc\tTruth\tGausie is cute, Invalid fact type: Truth",
      "Fish\tItem, Fact for Fish must specify at least one item",
      "Fish\tEffect, Fact for Fish must specify at least one effect",
      "'Dude\tItem\t[pref(somepref,somevalue)]', Fact for Dude must specify at least one item",
      "Mer-kin\tItem\tnonexistent item of gausie, Fact for Mer-kin specifies an invalid item",
      "Constellation\tEffect\tnonexistent effect of gausie, Fact for Constellation specifies an invalid effect",
      "Pirate\tHP, Fact for Pirate must specify a value for hp",
      "Pirate\tMP, Fact for Pirate must specify a value for mp",
      "Pirate\tModifier, Fact for Pirate must specify a value for modifier",
      "Weird\tStats, Fact for Weird stats must specify a value and type",
      "Undead\tStats\tbork\tmoxie, Fact for Undead stats has a bad value",
      "Undead\tStats\t3\tmelancholy, Fact for Undead stats has a bad type",
    })
    void databaseParserCatchesErrors(final String input, final String error) {
      var stream = new ByteArrayOutputStream();
      try (var mock = Mockito.mockStatic(FileUtilities.class, Mockito.CALLS_REAL_METHODS);
          var out = new PrintStream(stream, true);
          var cleanups = withContinuationState()) {
        mock.when(
                () ->
                    FileUtilities.getVersionedReader(
                        "bookoffacts.txt", KoLConstants.BOOKOFFACTS_VERSION))
            .thenAnswer(invocation -> new BufferedReader(new StringReader(input)));
        RequestLogger.openCustom(out);
        FactDatabase.reset();
        RequestLogger.closeCustom();

        var logged = stream.toString().trim();
        assertThat(StaticEntity.getContinuationState(), is(KoLConstants.MafiaState.ERROR));
        assertThat(logged, containsString("Error loading fact database."));
        assertThat(logged, containsString(error));
      }
    }

    @Test
    void heapItemsAreValid() {
      var invalidItems =
          FactDatabase.HEAP_ITEMS.stream()
              .map(name -> ItemPool.get(name, 1))
              .filter(a -> a.getItemId() == -1)
              .toList();
      assertThat(invalidItems, empty());
    }
  }

  @ParameterizedTest
  @CsvSource({
    "SEAL_CLUBBER, WILDFIRE, ratbat, EFFECT, Egg-cellent Vocabulary (10)",
    "SEAL_CLUBBER, NONE, Octorok, EFFECT, Egg-stortionary Tactics (10)",
    "PASTAMANCER, NONE, Jacob's adder, EFFECT, Egg-stortionary Tactics (10)",
    "PASTAMANCER, NONE, Black Crayon Penguin, MEAT, 149 Meat",
    "DISCO_BANDIT, SMALL, Bob Racecar, EFFECT, Feeling Excited (15)",
    "SEAL_CLUBBER, NONE, BASIC Elemental, STATS, +1 all substats",
    "SEAL_CLUBBER, NONE, poutine ooze, STATS, +3 all substats",
    "SEAL_CLUBBER, NONE, Assembly Elemental, STATS, +3 muscle substats",
    "SEAL_CLUBBER, NONE, amorphous blob, MODIFIER, Experience (familiar): +1",
    "SEAL_CLUBBER, NONE, Alphabet Giant, MODIFIER, Item Drop: +25",
    "SEAL_CLUBBER, NONE, Family Jewels, ITEM, line (3)",
    "SEAL_CLUBBER, NONE, angry mushroom guy, HP, 50% HP restore",
    "SEAL_CLUBBER, NONE, ancestral Spookyraven portrait, MP, 20% MP restore",
    "TURTLE_TAMER, STANDARD, Mob Penguin hitman, STATS, +3 moxie substats",
    "TURTLE_TAMER, STANDARD, Candied Yam Golem, STATS, +3 mysticality substats",
    "TURTLE_TAMER, NONE, amateur elf, STATS, +10 moxie substats",
    "ACCORDION_THIEF, UNDER_THE_SEA, grouper groupie, EFFECT, Fishy Fortification (10)",
  })
  void picksCorrectFact(
      final AscensionClass ascensionClass,
      final Path path,
      final String monsterName,
      final FactType factType,
      final String factString) {
    var monster = MonsterDatabase.findMonster(monsterName);
    var fact = FactDatabase.getFact(ascensionClass, path, monster, false);
    assertThat(fact.getType(), is(factType));
    assertThat(fact.toString(), is(factString));
  }

  @ParameterizedTest
  @CsvSource({
    "0, true",
    "1, true",
    "2, true",
    "3, false",
  })
  void pocketWishesAreStateful(final int dropped, final boolean drops) {
    try (var cleanups = withProperty("_bookOfFactsWishes", dropped)) {
      var monster = MonsterDatabase.findMonster("taco fish");
      var fact = FactDatabase.getFact(AscensionClass.ACCORDION_THIEF, Path.BIG, monster, true);
      if (drops) {
        assertThat(fact.getType(), is(FactType.ITEM));
        assertThat(fact.toString(), is("pocket wish"));
      } else {
        assertThat(fact.getType(), is(FactType.NONE));
      }
    }
  }

  @ParameterizedTest
  @CsvSource({
    "0, true",
    "1, true",
    "10, true",
    "11, false",
  })
  void tattersAreStateful(final int dropped, final boolean drops) {
    try (var cleanups = withProperty("_bookOfFactsTatters", dropped)) {
      var monster = MonsterDatabase.findMonster("Elite Beer Bongadier");
      var fact = FactDatabase.getFact(AscensionClass.SAUCEROR, Path.LOWKEY, monster, true);
      if (drops) {
        assertThat(fact.getType(), is(FactType.ITEM));
        assertThat(fact.toString(), is("tattered scrap of paper"));
      } else {
        assertThat(fact.getType(), is(FactType.NONE));
      }
    }
  }

  @ParameterizedTest
  @CsvSource({
    "0, true",
    "1, false",
    "2, false",
    "3, false",
  })
  void gummiEffectsAreStateful(final int dropped, final boolean drops) {
    try (var cleanups = withProperty("bookOfFactsGummi", dropped)) {
      var monster = MonsterDatabase.findMonster("raven");
      var fact =
          FactDatabase.getFact(
              AscensionClass.DISCO_BANDIT, Path.CRAZY_RANDOM_SUMMER, monster, true);
      if (drops) {
        assertThat(fact.getType(), is(FactType.EFFECT));
        assertThat(fact.toString(), is("Gummibrain (10)"));
      } else {
        assertThat(fact.getType(), is(FactType.NONE));
      }
    }
  }

  @ParameterizedTest
  @CsvSource({
    "0, true",
    "1, false",
  })
  void pinataEffectsAreStateful(final int dropped, final boolean drops) {
    try (var cleanups = withProperty("bookOfFactsPinata", dropped)) {
      var monster = MonsterDatabase.findMonster("oil slick");
      var fact =
          FactDatabase.getFact(AscensionClass.TURTLE_TAMER, Path.NUCLEAR_AUTUMN, monster, true);
      if (drops) {
        assertThat(fact.getType(), is(FactType.EFFECT));
        assertThat(fact.toString(), is("Sweet and Yellow (10)"));
      } else {
        assertThat(fact.getType(), is(FactType.NONE));
      }
    }
  }

  @Nested
  class StatefulFlags {
    @Test
    void basicFactsDontHaveStatefulFlags() {
      var fact = new FactDatabase.Fact(FactType.NONE, "");
      assertThat(fact.isGummi(), is(false));
      assertThat(fact.isPinata(), is(false));
      assertThat(fact.isWish(), is(false));
      assertThat(fact.isTatter(), is(false));
    }

    @ParameterizedTest
    @CsvSource({
      EffectPool.GUMMIBRAIN + ", true",
      EffectPool.GUMMIHEART + ", true",
      EffectPool.GUMMISKIN + ", true",
      EffectPool.SLEEPY + ", false",
    })
    void factsGivingGummiEffectsAreGummiFacts(final int effectId, final boolean gummi) {
      var fact = new FactDatabase.AdventureResultFact(FactType.EFFECT, EffectPool.get(effectId));
      assertThat(fact.isGummi(), is(gummi));
    }

    @ParameterizedTest
    @CsvSource({
      EffectPool.SWEET_AND_GREEN + ", true",
      EffectPool.SWEET_AND_RED + ", true",
      EffectPool.SWEET_AND_YELLOW + ", true",
      EffectPool.SLEEPY + ", false",
    })
    void factsGivingPinataEffectsArePinataFacts(final int effectId, final boolean pinata) {
      var fact = new FactDatabase.AdventureResultFact(FactType.EFFECT, EffectPool.get(effectId));
      assertThat(fact.isPinata(), is(pinata));
    }
  }
}
