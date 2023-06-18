package net.sourceforge.kolmafia.webui;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.webui.UseLinkDecorator.UseLink;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UseLinkDecoratorTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("UseLinkDecoratorTest");
    Preferences.reset("UseLinkDecoratorTest");
  }

  @Nested
  class ProfessorWhatTeeShirt {
    @Test
    void gettingGarmentFromAdventureGivesNavigationLink() {
      var cleanups = new Cleanups(withItem(ItemPool.PROFESSOR_WHAT_GARMENT));
      try (cleanups) {
        String text = html("request/test_melvign_get_garment.html");
        var location = "adventure.php?snarfblat=387";
        UseLink link =
            UseLinkDecorator.generateUseLink(ItemPool.PROFESSOR_WHAT_GARMENT, 1, location, text);
        assertNotNull(link);
        assertEquals("visit Melvign", link.getUseType());
        assertEquals("place.php?whichplace=mountains&action=mts_melvin", link.getUseLocation());
      }
    }

    @Test
    void gettingShirtFromMelvignGivesEquipLink() {
      var cleanups = new Cleanups(withItem(ItemPool.PROFESSOR_WHAT_TSHIRT));
      try (cleanups) {
        String text = html("request/test_melvign_return_shirt.html");
        var location = "place.php?whichplace=mountains&action=mts_melvin";
        UseLink link =
            UseLinkDecorator.generateUseLink(ItemPool.PROFESSOR_WHAT_TSHIRT, 1, location, text);
        assertNotNull(link);
        // Equipment is wrapped in a "speculation", which shows what
        // will change in your modifiers when you equip an item
        assertTrue(link.getUseType().contains("</table>equip</span>"));
        assertEquals(
            "inv_equip.php?which=2&action=equip&whichitem=7297&pwd=", link.getUseLocation());
      }
    }
  }
}
