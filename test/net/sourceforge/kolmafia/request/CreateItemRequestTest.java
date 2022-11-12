package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;

import internal.helpers.Player;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class CreateItemRequestTest {
  @BeforeAll
  public static void init() {
    KoLCharacter.reset("CreateItemRequestTest");
    Preferences.reset("CreateItemRequestTest");
  }

  @Test
  public void recognisesCookbookBatFreeCraft() {
    var cleanups = Player.withProperty("_cookbookbatCrafting");

    try (cleanups) {
      CreateItemRequest.parseCrafting(
          "craft.php?action=craft&qty=1&mode=cook&target=423&ajax=1",
          html("request/test_create_cookbookbat.html"));

      assertThat("_cookbookbatCrafting", isSetTo(1));
    }
  }
}
