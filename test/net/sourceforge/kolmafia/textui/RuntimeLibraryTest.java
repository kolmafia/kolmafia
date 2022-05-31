package net.sourceforge.kolmafia.textui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.request.CharSheetRequest;
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

  @Test
  void getPermedSkills() throws IOException {
    CharSheetRequest.parseStatus(
        Files.readString(Paths.get("request/test_charsheet_normal.html")).trim());

    String outputHardcore = execute("get_permed_skills()[$skill[Nimble Fingers]]");

    assertContinueState();
    assertThat(outputHardcore, containsString("Returned: true"));

    String outputSoftcore = execute("get_permed_skills()[$skill[Entangling Noodles]]");

    assertContinueState();
    assertThat(outputSoftcore, containsString("Returned: false"));

    String outputUnpermed = execute("get_permed_skills() contains $skill[Emotionally Chipped]");

    assertContinueState();
    assertThat(outputSoftcore, containsString("Returned: false"));
  }
}
