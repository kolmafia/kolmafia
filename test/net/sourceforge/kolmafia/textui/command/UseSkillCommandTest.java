package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UseSkillCommandTest extends AbstractCommandTestBase {
  @BeforeEach
  public void initEach() {
    KoLCharacter.reset("testUser");
    KoLCharacter.reset(true);

    // Stop requests from actually running
    GenericRequest.sessionId = null;
  }

  public UseSkillCommandTest() {
    this.command = "cast";
  }

  @Test
  void expandsKnownShorthand() {
    KoLCharacter.addAvailableSkill(SkillPool.ODE_TO_BOOZE);
    KoLCharacter.addAvailableSkill("CHEAT CODE: Invisible Avatar");
    KoLCharacter.addAvailableSkill("CHEAT CODE: Triple Size");
    AdventureResult.addResultToList(
        KoLConstants.inventory, ItemPool.get(ItemPool.ANTIQUE_ACCORDION));
    KoLCharacter.setMP(100, 100, 100);

    String output = execute("ode");

    assertContinueState();
    assertThat(output, containsString("Casting The Ode to Booze"));
  }

  @Test
  void doesNotExpandUnknownShorthand() {
    KoLCharacter.addAvailableSkill(SkillPool.ODE_TO_BOOZE);
    KoLCharacter.addAvailableSkill("CHEAT CODE: Invisible Avatar");
    KoLCharacter.addAvailableSkill("CHEAT CODE: Triple Size");
    String output = execute("od");

    assertErrorState();
    assertThat(output, containsString("Possible matches:"));
  }
}
