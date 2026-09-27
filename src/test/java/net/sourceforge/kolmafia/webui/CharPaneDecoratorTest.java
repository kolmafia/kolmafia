package net.sourceforge.kolmafia.webui;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withSkill;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
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
    KoLConstants.activeEffects.clear();
  }

  @AfterAll
  public static void cleanup() {
    KoLAdventure.setLastAdventure("");
  }

  @ParameterizedTest
  @ValueSource(strings = {"basic", "compact"})
  public void decorateEffects(final String displayMode) {
    var cleanups =
        new Cleanups(
            withSkill(SkillPool.ODE_TO_BOOZE),
            withProperty("olfactedMonster", "novelty tropical skeleton"));

    try (cleanups) {
      String input = html("request/test_charpane_" + displayMode + ".html");
      assertNotNull(input);
      boolean ok = CharPaneRequest.processResults(input);
      assertTrue(ok);

      var actual = new StringBuffer(input);
      CharPaneDecorator.decorate(actual);

      assertThat(
          actual.toString(),
          containsString(html("request/test_charpane_" + displayMode + "_decorated_effects.html")));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"basic", "compact", "basic_20lbs", "compact_20lbs"})
  public void addDistillLink(final String displayMode) {
    final int drams = 50;
    var cleanups =
        new Cleanups(
            withProperty("familiarSweat", drams),
            withFamiliar(FamiliarPool.WOIM),
            withEquipped(Slot.FAMILIAR, "tiny stillsuit"));

    try (cleanups) {
      String input = html("request/test_charpane_woim_" + displayMode + ".html");
      CharPaneRequest.processResults(input);

      var buffer = new StringBuffer(input);
      CharPaneDecorator.decorate(buffer);
      var prefix = displayMode.startsWith("basic") ? "Oily Woim " : "lbs.<br><small>";
      assertThat(
          buffer.toString(),
          containsString(
              prefix
                  + "(<a target=mainpane href=\"inventory.php?action=distill&pwd="
                  + GenericRequest.passwordHash
                  + "\" title=\""
                  + drams
                  + " drams\">distill</a>)"));
    }
  }
}
