package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withMP;
import static internal.helpers.Player.withSkill;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import org.junit.jupiter.api.Test;

public class UseSkillCommandTest extends AbstractCommandTestBase {
  public UseSkillCommandTest() {
    this.command = "cast";
  }

  @Test
  void expandsKnownShorthand() {
    try (var _ =
        new Cleanups(
            withSkill(SkillPool.ODE_TO_BOOZE),
            withSkill("CHEAT CODE: Invisible Avatar"),
            withSkill("CHEAT CODE: Triple Size"),
            withItem(ItemPool.ANTIQUE_ACCORDION),
            withMP(100, 100, 100))) {
      String output = execute("ode");

      assertContinueState();
      assertThat(output, containsString("Casting The Ode to Booze"));
    }
  }

  @Test
  void doesNotExpandUnknownShorthand() {
    try (var _ =
        new Cleanups(
            withSkill(SkillPool.ODE_TO_BOOZE),
            withSkill("CHEAT CODE: Invisible Avatar"),
            withSkill("CHEAT CODE: Triple Size"))) {
      String output = execute("od");

      assertErrorState();
      assertThat(output, containsString("Possible matches:"));
    }
  }

  @Test
  void canCastDiscoNap() {
    try (var _ = new Cleanups(withSkill(SkillPool.DISCO_NAP), withMP(100, 100, 100))) {
      String output = execute("disco nap");

      assertContinueState();
      assertThat(output, containsString("Casting Disco Nap"));
    }
  }

  @Test
  void canCastSkillsForEffects() {
    try (var _ = new Cleanups(withSkill(SkillPool.EMPATHY_OF_THE_NEWT), withMP(100, 100, 100))) {
      String output = execute("empathy ^ empathy");

      assertContinueState();
      assertThat(output, containsString("Casting Empathy of the Newt"));
    }
  }
}
