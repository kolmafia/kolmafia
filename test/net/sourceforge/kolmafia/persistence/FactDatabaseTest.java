package net.sourceforge.kolmafia.persistence;

import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.blankString;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.persistence.FactDatabase.FactType;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FactDatabaseTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("FactDatabaseTest");
    Preferences.reset("FactDatabaseTest");
  }

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
    "SEAL_CLUBBER, WILDFIRE, ratbat, EFFECT, Egg-cellent Vocabulary (10)",
    "SEAL_CLUBBER, NONE, Octorok, EFFECT, Egg-stortionary Tactics (10)",
    "PASTAMANCER, NONE, Jacob's adder, EFFECT, Egg-stortionary Tactics (10)",
    "PASTAMANCER, NONE, Black Crayon Penguin, MEAT, 149"
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
}
