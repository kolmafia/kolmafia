package net.sourceforge.kolmafia.persistence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;

import net.sourceforge.kolmafia.AscensionPath.Path;
import org.junit.jupiter.api.Test;

public class ShrunkenHeadDatabaseTest {
  @Test
  void generatesZombieModifiers() {
    var swampOwl = MonsterDatabase.findMonster("swamp owl");
    var path = Path.ACTUALLY_ED_THE_UNDYING;
    var effects = ShrunkenHeadDatabase.shrunkenHeadZombie(swampOwl.getId(), path.id);

    assertThat(
        effects, contains("Item Drop Bonus", "Hot Attack", "Sleaze Attack", "Spooky Attack"));
  }

  @Test
  void generatesZombieModifiersWithWeights() {
    var swampOwl = MonsterDatabase.findMonster("swamp owl");
    var path = Path.ACTUALLY_ED_THE_UNDYING;
    var effects = ShrunkenHeadDatabase.shrunkenHeadZombieWithWeights(swampOwl.getId(), path.id);

    assertThat(effects, hasEntry("Item Drop Bonus", 22));
    assertThat(effects, hasEntry("Hot Attack", 27));
    assertThat(effects, hasEntry("Sleaze Attack", 19));
    assertThat(effects, hasEntry("Spooky Attack", 32));
  }
}
