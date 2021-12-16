package net.sourceforge.kolmafia.webui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class CharPaneDecoratorTest {
  @BeforeEach
  public void beforeEach() {
    GenericRequest.passwordHash = "abc123";
    KoLCharacter.reset("Test Character");
    Preferences.reset("Test Character");
  }

  @ParameterizedTest
  @ValueSource(strings = {"basic", "compact"})
  public void decorateEffects(final String displayMode) throws IOException {
    KoLCharacter.addAvailableSkill(SkillPool.ODE_TO_BOOZE);
    Preferences.setString("olfactedMonster", "novelty tropical skeleton");

    String input = Files.readString(Paths.get("request/test_charpane_" + displayMode + ".html"));
    CharPaneRequest.processResults(input);

    var expected =
        Files.readString(
            Paths.get("request/test_charpane_" + displayMode + "_decorated_effects.html"));
    var actual = CharPaneDecorator.decorateEffects(new StringBuffer(input)).toString();

    assertEquals(expected, actual);
  }
}
