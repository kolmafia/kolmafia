package net.sourceforge.kolmafia.request;

import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class VolcanoIslandRequestTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("VolcanoIslandRequestTest");
    Preferences.reset("VolcanoIslandRequestTest");
  }

  @Test
  public void shouldUpdatePreferenceWhenGettingSlimeVials() {
    var cleanups =
        new Cleanups(withClass(AscensionClass.SAUCEROR), withProperty("_slimeVialsHarvested", 1));
    try (cleanups) {
      VolcanoIslandRequest.parseResponse(
          "volcanoisland.php?action=npc",
          "You ladle some slime out of one of the drums. Fortunately, you had an empty vial on hand for just such an opportunity.");
      assertThat("_slimeVialsHarvested", isSetTo(2));
    }
  }
}
