package net.sourceforge.kolmafia.combat;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class MacrofierTest {
  @Test
  public void stackingMacrosKeepsOriginalOverride() {
    // example: an adv1 call
    Macrofier.setMacroOverride("abort;", null);
    // example: a pre-adventure script
    Macrofier.setMacroOverride(null, null);
    // reset from the pre-adventure script
    Macrofier.resetMacroOverride();
    assertThat(Macrofier.macrofy(), equalTo("abort;"));
  }
}
