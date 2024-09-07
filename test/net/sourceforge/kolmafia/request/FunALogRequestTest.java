package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FunALogRequestTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("FunALogRequestTest");
    Preferences.reset("FunALogRequestTest");
    KoLCharacter.setCurrentRun(0);
    CharPaneRequest.reset();
  }

  @Test
  void discoverUnlocks() {
    var cleanups =
        new Cleanups(
            withProperty("pirateRealmUnlockedCrabsicle", false),
            withProperty("pirateRealmUnlockedRhum", false),
            withProperty("pirateRealmUnlockedShavingCream", false),
            withProperty("pirateRealmUnlockedScurvySkillbook", false));
    try (cleanups) {
      var responseText = html("request/test_shop_funalog.html");
      FunALogRequest.parseResponse("shop.php?whichshop=piraterealm", responseText);
      assertThat("pirateRealmUnlockedCrabsicle", isSetTo(true));
      assertThat("pirateRealmUnlockedRhum", isSetTo(true));
      assertThat("pirateRealmUnlockedShavingCream", isSetTo(true));
      assertThat("pirateRealmUnlockedScurvySkillbook", isSetTo(false));
    }
  }
}
