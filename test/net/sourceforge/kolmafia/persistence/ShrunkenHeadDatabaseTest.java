package net.sourceforge.kolmafia.persistence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

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
}
