package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import internal.helpers.Player;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.StaticEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ModiferTraceCommandTest extends AbstractCommandTestBase {

  public ModiferTraceCommandTest() {
    this.command = "modtrace";
  }

  @BeforeEach
  public void setUp() {
    StaticEntity.setContinuationState(MafiaState.CONTINUE);
  }

  @Test
  public void noMatchIsError() {
    String output = execute("fhqwhgads");

    assertErrorState();
    assertThat(output.trim(), equalTo("No matching modifiers - use 'modref' to list."));
  }

  @Test
  public void tooManyMatchIsError() {
    String output = execute("");

    assertErrorState();
    assertThat(output.trim(), equalTo("Too many matching modifiers - use 'modref' to list."));
  }

  @Test
  public void matchDisplaysExtantModifiers() {
    var cleanups = Player.addEffect("Fat Leon's Phat Loot Lyric");

    try (cleanups) {
      String output = execute("item drop");

      assertContinueState();
      assertThat(output, containsString("Fat Leon's Phat Loot Lyric"));
      assertThat(output, containsString("+20.00"));
      assertThat(output, not(containsString("Cosmic Ball in the Air")));
    }
  }
}
