package net.sourceforge.kolmafia.webui;

import static internal.helpers.Networking.html;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class CharPaneDecoratorTest {
  @BeforeEach
  public void beforeEach() {
    GenericRequest.passwordHash = "abc123";
    KoLCharacter.reset("Test Character");
    Preferences.reset("Test Character");
    CharPaneRequest.reset();
  }

  @AfterAll
  public static void cleanup() {
    KoLAdventure.setLastAdventure("");
  }

  @ParameterizedTest
  @ValueSource(strings = {"basic", "compact"})
  public void decorateEffects(final String displayMode) {
    KoLCharacter.addAvailableSkill(SkillPool.ODE_TO_BOOZE);
    Preferences.setString("olfactedMonster", "novelty tropical skeleton");

    String input = html("request/test_charpane_" + displayMode + ".html");
    CharPaneRequest.processResults(input);

    var expected = html("request/test_charpane_" + displayMode + "_decorated_effects.html");
    var actual =
        CharPaneDecorator.decorateIntrinsics(
                CharPaneDecorator.decorateEffects(new StringBuffer(input)))
            .toString();

    assertEquals(expected, actual);
  }
}
