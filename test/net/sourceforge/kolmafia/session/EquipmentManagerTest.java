package net.sourceforge.kolmafia.session;

import static internal.helpers.Player.equip;
import static internal.helpers.Player.setProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Coverage driven collection of tests for FightRequest. */
public class EquipmentManagerTest {

  @BeforeAll
  private static void beforeAll() {
    // Simulate logging out and back in again.
    GenericRequest.passwordHash = "";
    KoLCharacter.reset("");
    KoLCharacter.reset("equipment manager test");
    Preferences.saveSettingsToFile = false;
  }

  @AfterAll
  private static void afterAll() {
    Preferences.saveSettingsToFile = true;
  }

  private static final AdventureResult UNBREAKABLE_UMBRELLA =
      ItemPool.get(ItemPool.UNBREAKABLE_UMBRELLA);

  @ParameterizedTest
  @ValueSource(
      strings = {
        "broken",
        "forward-facing",
        "bucket style",
        "pitchfork style",
        "constantly twirling",
        "cocoon"
      })
  public void thatUnbreakableUmbrellaIsRecognized(String style) {
    var cleanups = new Cleanups(equip(EquipmentManager.OFFHAND, "unbreakable umbrella"));

    try (cleanups) {
      Preferences.setString("umbrellaState", style);
      assertEquals("unbreakable umbrella (" + style + ")", UNBREAKABLE_UMBRELLA.getName());
      assertEquals(
          UNBREAKABLE_UMBRELLA.getItemId(), ItemDatabase.getItemId(UNBREAKABLE_UMBRELLA.getName()));
    }
  }

    Preferences.setString("umbrellaState", "forward-facing");
    assertEquals("unbreakable umbrella (forward-facing)", unbrella.getName());
    assertEquals(unbrella.getItemId(), ItemDatabase.getItemId(unbrella.getName()));

    Preferences.setString("umbrellaState", "bucket style");
    assertEquals("unbreakable umbrella (bucket style)", unbrella.getName());
    assertEquals(unbrella.getItemId(), ItemDatabase.getItemId(unbrella.getName()));

    Preferences.setString("umbrellaState", "pitchfork style");
    assertEquals("unbreakable umbrella (pitchfork style)", unbrella.getName());
    assertEquals(unbrella.getItemId(), ItemDatabase.getItemId(unbrella.getName()));

    Preferences.setString("umbrellaState", "constantly twirling");
    assertEquals("unbreakable umbrella (constantly twirling)", unbrella.getName());
    assertEquals(unbrella.getItemId(), ItemDatabase.getItemId(unbrella.getName()));

    Preferences.setString("umbrellaState", "cocoon");
    assertEquals("unbreakable umbrella (cocoon)", unbrella.getName());
    assertEquals(unbrella.getItemId(), ItemDatabase.getItemId(unbrella.getName()));
  }
}
