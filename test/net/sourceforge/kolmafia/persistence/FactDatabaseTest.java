package net.sourceforge.kolmafia.persistence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.persistence.FactDatabase.FactType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FactDatabaseTest {
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
    var fact = FactDatabase.getFact(ascensionClass, path, monster);
    assertThat(fact.getType(), is(factType));
    assertThat(fact.toString(), is(factString));
  }
}
