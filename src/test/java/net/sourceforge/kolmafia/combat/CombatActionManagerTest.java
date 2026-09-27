package net.sourceforge.kolmafia.combat;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class CombatActionManagerTest {
  @Test
  public void defaultBehaviourIsAttack() {
    assertThat(
        CombatActionManager.getShortCombatOptionName(Macrofier.macrofy()),
        containsString("attack"));
  }

  @Test
  public void invalidDiscoComboResetMacro() {
    try {
      Macrofier.setMacroOverride("combo invalid;", null);
      assertThat(
          CombatActionManager.getShortCombatOptionName(Macrofier.macrofy()), equalTo("skip"));
      var nextAction = CombatActionManager.getShortCombatOptionName(Macrofier.macrofy());
      assertThat(nextAction, containsString("attack"));
      assertThat(nextAction, not(containsString("combo")));
      assertThat(nextAction, not(containsString("skip")));
    } finally {
      Macrofier.resetMacroOverride();
    }
  }
}
