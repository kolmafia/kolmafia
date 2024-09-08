package net.sourceforge.kolmafia.webui;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class IslandDecoratorTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("IslandDecoratorTest");
  }

  @Test
  public void decoratesGremlinMessage() {
    var cleanups = withItem(ItemPool.MOLYBDENUM_MAGNET);

    try (cleanups) {
      StringBuffer text = new StringBuffer(html("request/test_fight_gremlin_good.html"));
      MonsterData gremlin = MonsterDatabase.findMonsterById(551);

      IslandDecorator.decorateGremlinFight(gremlin, text);

      assertThat(
          text.toString(),
          containsString(
              "<font color=#DD00FF>It pummels you with a head of cabbage.  It doesn't hurt, but then a stork swoops down and grabs the cabbage, biting your neck in the process.</font>"));
    }
  }
}
