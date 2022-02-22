package net.sourceforge.kolmafia.textui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.textui.command.AbstractCommandTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RuntimeLibraryTest extends AbstractCommandTestBase {

  @BeforeEach
  public void initEach() {
    KoLCharacter.reset("testUser");
    KoLCharacter.reset(true);

    // Stop requests from actually running
    GenericRequest.sessionId = null;
  }

  public RuntimeLibraryTest() {
    this.command = "ash";
  }

  @Test
  void normalMonsterExpectedDamage() {
    String output = execute("expected_damage($monster[blooper])");

    assertContinueState();
    assertThat(output, containsString("Returned: 35"));
  }

  @Test
  void ninjaSnowmanAssassinExpectedDamage() {
    String output = execute("expected_damage($monster[ninja snowman assassin])");

    assertContinueState();
    assertThat(output, containsString("Returned: 297"));
  }
}
