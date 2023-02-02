package net.sourceforge.kolmafia.request;

import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withEquippableItem;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withInebriety;
import static internal.helpers.Player.withIntrinsicEffect;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withQuestProgress;
import static internal.helpers.Player.withStats;
import static internal.helpers.Player.withUnequipped;
import static org.junit.jupiter.api.Assertions.*;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.EquipmentRequirement;
import net.sourceforge.kolmafia.session.YouRobotManager;
import net.sourceforge.kolmafia.session.YouRobotManager.RobotUpgrade;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class RelayRequestWarningsTest {

  // The Relay Browser will show "warnings" if you might be doing something unintentionally.
  //
  // There are two types:
  //
  // A "General" warning gives you no options; it tells you about something you are about to do and
  // you can either click the (single) icon to proceed, or you can go do something (possibly in the
  // GUI) and THEN click the icon.
  //
  // An "Optional" warning gives you such an icon and also at one or more additional icons to go
  // somewhere else or perform some action in-line before continuing.
  //
  // The internal mechanism for the "go ahead anyway" icon is to add a "confirm" field to the
  // original URL. If that field is detected in the resubmitted URL, you have already confirmed.
  //
  // Additionally, some locations will be repeatedly adventured in and, once you have confirmed, you
  // don't want to be nagged again. There are global static booleans in RelayRequest for such
  // warnings. Once you have confirmed once, it holds for rest of the session.
  //
  // "Counter Warnings" are a special case, since they apply to both
  // Relay Browser adventuring and Automation. Tests for those are in session/TurnCounterTest.java

  @BeforeAll
  public static void beforeAll() {
    // Simulate logging out and back in again.
    GenericRequest.passwordHash = "";
    KoLCharacter.reset("");
    KoLCharacter.reset("relay request warnings user");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("relay request warnings user");
    KoLConstants.inventory.clear();
    RelayRequest.reset();
    EquipmentManager.resetEquipment();
    // Would not be necessary if tests used Cleanups...
    KoLCharacter.setPath(Path.NONE);
  }

  @AfterAll
  public static void afterAll() {
    KoLConstants.inventory.clear();
    RelayRequest.reset();
    EquipmentManager.resetEquipment();
    // Would not be necessary if tests used Cleanups...
    KoLCharacter.setPath(Path.NONE);
  }

  private String adventureURL(KoLAdventure adventure, String confirmation) {
    var request = adventure.getRequest();
    StringBuilder buf = new StringBuilder();
    buf.append(request.getURLString());
    if (confirmation != null) {
      buf.append("&");
      buf.append(confirmation);
      buf.append("=on");
    }
    return buf.toString();
  }

  @Nested
  class KungFuFighting {
    private static final KoLAdventure WARREN =
        AdventureDatabase.getAdventureByName("The Dire Warren");
    private static final String confirm = RelayRequest.CONFIRM_KUNGFU;

    @Test
    public void shouldNotWarnWithoutEffect() {
      var cleanups = withEquipped(EquipmentManager.WEAPON, ItemPool.SEAL_CLUB);
      try (cleanups) {
        RelayRequest request = new RelayRequest(false);
        request.constructURLString(WARREN.getRequest().getURLString());
        assertFalse(request.sendKungFuWarning());
      }
    }

    @Test
    public void shouldWarnWithIntrinsicAndFullHands() {
      var cleanups =
          new Cleanups(
              withEquipped(EquipmentManager.WEAPON, ItemPool.SEAL_CLUB),
              withIntrinsicEffect("Kung Fu Fighting"));
      try (cleanups) {
        RelayRequest request = new RelayRequest(false);
        request.constructURLString(WARREN.getRequest().getURLString());
        assertTrue(request.sendKungFuWarning());
      }
    }

    @Test
    public void shouldNotWarnWithIntrinsicAndEmptyHands() {
      var cleanups =
          new Cleanups(
              withUnequipped(EquipmentManager.WEAPON),
              withUnequipped(EquipmentManager.OFFHAND),
              withIntrinsicEffect("Kung Fu Fighting"));
      try (cleanups) {
        RelayRequest request = new RelayRequest(false);
        request.constructURLString(WARREN.getRequest().getURLString());
        assertFalse(request.sendKungFuWarning());
      }
    }

    @Test
    public void shouldNotWarnWithNonIntrinsicEffect() {
      var cleanups =
          new Cleanups(
              withEquipped(EquipmentManager.WEAPON, ItemPool.SEAL_CLUB),
              withEffect(EffectPool.KUNG_FU_FIGHTING));
      try (cleanups) {
        RelayRequest request = new RelayRequest(false);
        request.constructURLString(WARREN.getRequest().getURLString());
        assertFalse(request.sendKungFuWarning());
      }
    }

    @Test
    public void shouldNotWarnWithIntrinsicIfAlreadyConfirmed() {
      var cleanups =
          new Cleanups(
              withEquipped(EquipmentManager.WEAPON, ItemPool.SEAL_CLUB),
              withIntrinsicEffect("Kung Fu Fighting"));
      try (cleanups) {
        RelayRequest request = new RelayRequest(false);
        request.constructURLString(WARREN.getRequest().getURLString() + "&" + confirm + "=on");
        assertFalse(request.sendKungFuWarning());
      }
    }
  }

  @Nested
  class WineGlass {
    private static final KoLAdventure WARREN =
        AdventureDatabase.getAdventureByName("The Dire Warren");
    private static final KoLAdventure CELLAR =
        AdventureDatabase.getAdventureByName("The Typical Tavern Cellar");
    private static final String confirm = RelayRequest.CONFIRM_WINEGLASS;

    @Test
    public void thatNoWarningNeededIfNotOverDrunk() {
      var cleanups =
          new Cleanups(
              withClass(AscensionClass.ACCORDION_THIEF),
              withPath(Path.NONE),
              withInebriety(5),
              withEquippableItem(ItemPool.DRUNKULA_WINEGLASS));
      try (cleanups) {
        RelayRequest request = new RelayRequest(false);
        request.constructURLString(WARREN.getRequest().getURLString());
        assertFalse(request.sendWineglassWarning(WARREN));
      }
    }

    @Test
    public void thatNoWarningNeededIfNoWineglass() {
      var cleanups = new Cleanups(withInebriety(30), withStats(100, 100, 100));
      try (cleanups) {
        RelayRequest request = new RelayRequest(false);
        request.constructURLString(WARREN.getRequest().getURLString());
        assertFalse(request.sendWineglassWarning(WARREN));
      }
    }

    @Test
    public void thatWarningNeededIfEquippableInOffhand() {
      var cleanups =
          new Cleanups(withInebriety(30), withEquippableItem(ItemPool.DRUNKULA_WINEGLASS));
      try (cleanups) {
        RelayRequest request = new RelayRequest(false);
        request.constructURLString(WARREN.getRequest().getURLString());
        assertTrue(request.sendWineglassWarning(WARREN));
        String expected =
            "KoLmafia has detected that you are about to adventure while overdrunk. "
                + "If you are sure you wish to adventure in a Drunken Stupor, click the icon on the left to adventure. "
                + "If this was an accident, click the icon in the center to equip Drunkula's wineglass. "
                + "If you want to adventure in a Drunken Stupor and not be nagged, click the icon on the right to closet Drunkula's wineglass.";
        assertEquals(expected, request.lastWarning);
      }
    }

    @Test
    public void thatNoWarningNeededIfNotSnarfblatAdventure() {
      var cleanups =
          new Cleanups(withInebriety(30), withEquippableItem(ItemPool.DRUNKULA_WINEGLASS));
      try (cleanups) {
        RelayRequest request = new RelayRequest(false);
        request.constructURLString(CELLAR.getRequest().getURLString());
        assertFalse(request.sendWineglassWarning(CELLAR));
      }
    }

    @ParameterizedTest
    @ValueSource(ints = {EquipmentManager.OFFHAND, EquipmentManager.FAMILIAR})
    public void thatNoWarningNeededIfWineglassEquipped(final int slot) {
      var cleanups =
          new Cleanups(
              withInebriety(30),
              withFamiliar(FamiliarPool.LEFT_HAND),
              withEquipped(slot, ItemPool.DRUNKULA_WINEGLASS));
      try (cleanups) {
        RelayRequest request = new RelayRequest(false);
        request.constructURLString(WARREN.getRequest().getURLString());
        assertFalse(request.sendWineglassWarning(WARREN));
      }
    }

    @Test
    public void thatNoWarningNeededIfUnequippableInOffhand() {
      var cleanups = new Cleanups(withInebriety(30), withItem(ItemPool.DRUNKULA_WINEGLASS));
      try (cleanups) {
        RelayRequest request = new RelayRequest(false);
        request.constructURLString(WARREN.getRequest().getURLString());
        assertFalse(request.sendWineglassWarning(WARREN));
      }
    }

    @Test
    public void thatNoWarningNeededIfConfirmedForOffhand() {
      var cleanups =
          new Cleanups(withInebriety(30), withEquippableItem(ItemPool.DRUNKULA_WINEGLASS));
      try (cleanups) {
        RelayRequest request = new RelayRequest(false);
        request.constructURLString(WARREN.getRequest().getURLString() + "&" + confirm + "=on");
        assertFalse(request.sendWineglassWarning(WARREN));
      }
    }

    @Test
    public void thatNoWarningNeededIfConfirmedForFamiliar() {
      var cleanups =
          new Cleanups(
              withInebriety(30),
              withFamiliar(FamiliarPool.LEFT_HAND),
              withItem(ItemPool.DRUNKULA_WINEGLASS));
      try (cleanups) {
        RelayRequest request = new RelayRequest(false);
        request.constructURLString(WARREN.getRequest().getURLString() + "&" + confirm + "=on");
        assertFalse(request.sendWineglassWarning(WARREN));
      }
    }
  }

  @Nested
  class MohawkWig {
    private static final KoLAdventure A_BOO_PEAK =
        AdventureDatabase.getAdventureByName("A-Boo Peak");
    private static final KoLAdventure CASTLE_TOP_FLOOR =
        AdventureDatabase.getAdventureByName("The Castle in the Clouds in the Sky (Top Floor)");
    private static final String confirm = RelayRequest.CONFIRM_MOHAWK_WIG;
    private static final AdventureResult MOHAWK_WIG = ItemPool.get(ItemPool.MOHAWK_WIG);

    @BeforeEach
    public void beforeEach() {
      RelayRequest.ignoreMohawkWigWarning = false;
    }

    @Test
    public void noWarningIfNotTopFloor() {
      var cleanups = new Cleanups();
      try (cleanups) {
        RelayRequest request = new RelayRequest(false);
        request.constructURLString(adventureURL(A_BOO_PEAK, null), false);
        // No warning needed if you are not in the Castle Top Floor
        assertFalse(request.sendMohawkWigWarning());
      }
    }

    @Test
    public void noWarningWithNoMohawkWigInInventory() {
      var cleanups = new Cleanups();
      try (cleanups) {
        RelayRequest request = new RelayRequest(false);
        request.constructURLString(adventureURL(CASTLE_TOP_FLOOR, null), false);
        // With no Mahawk Wig in Inventory, no warning
        assertFalse(request.sendMohawkWigWarning());
      }
    }

    @Test
    public void noWarningIfConfirmed() {
      var cleanups = new Cleanups(withEquippableItem(ItemPool.MOHAWK_WIG));
      try (cleanups) {
        RelayRequest request = new RelayRequest(false);
        request.constructURLString(adventureURL(CASTLE_TOP_FLOOR, confirm), false);
        // No warning needed if this a resubmission with confirmation
        assertFalse(request.sendMohawkWigWarning());
        assertTrue(RelayRequest.ignoreMohawkWigWarning);
      }
    }

    @Test
    public void noWarningIfPreviouslyConfirmed() {
      var cleanups = new Cleanups(withEquippableItem(ItemPool.MOHAWK_WIG));
      try (cleanups) {
        RelayRequest request = new RelayRequest(false);
        request.constructURLString(adventureURL(CASTLE_TOP_FLOOR, null), false);
        // No warning needed if this a resubmission with confirmation
        RelayRequest.ignoreMohawkWigWarning = true;
        assertFalse(request.sendMohawkWigWarning());
        assertTrue(RelayRequest.ignoreMohawkWigWarning);
      }
    }

    @Test
    public void noWarningIfMohawkWigEquipped() {
      var cleanups = new Cleanups(withEquipped(EquipmentManager.HAT, MOHAWK_WIG));
      try (cleanups) {
        RelayRequest request = new RelayRequest(false);
        request.constructURLString(adventureURL(CASTLE_TOP_FLOOR, null), false);
        // No warning needed if Mohawk wig already equipped
        assertFalse(request.sendMohawkWigWarning());
      }
    }

    @Test
    public void noWarningIfCastleQuestCompleted() {
      var cleanups =
          new Cleanups(withEquippableItem(MOHAWK_WIG), withQuestProgress(Quest.GARBAGE, "step10"));
      try (cleanups) {
        RelayRequest request = new RelayRequest(false);
        request.constructURLString(adventureURL(CASTLE_TOP_FLOOR, null), false);
        // No warning needed if Mohawk wig already equipped
        assertFalse(request.sendMohawkWigWarning());
      }
    }

    @Test
    public void warningIfUnequippableMohawkWig() {
      var cleanups = new Cleanups(withItem(MOHAWK_WIG), withStats(50, 50, 50));
      try (cleanups) {
        RelayRequest request = new RelayRequest(false);
        request.constructURLString(adventureURL(CASTLE_TOP_FLOOR, null), false);
        // No warning needed if Mohawk wig already equipped
        assertTrue(request.sendMohawkWigWarning());
        String expected =
            "You are about to adventure without your Mohawk Wig in the Castle."
                + " It requires base Moxie of 55, but yours is only 50."
                + " If you are sure you wish to adventure without it, click the icon to adventure.";
        assertEquals(expected, request.lastWarning);
      }
    }

    @Test
    public void warningIfEquippableMohawkWig() {
      var cleanups = new Cleanups(withEquippableItem(MOHAWK_WIG));
      try (cleanups) {
        RelayRequest request = new RelayRequest(false);
        request.constructURLString(adventureURL(CASTLE_TOP_FLOOR, null), false);
        assertTrue(request.sendMohawkWigWarning());
        String expected =
            "You are about to adventure without your Mohawk Wig in the Castle."
                + " If you are sure you wish to adventure without it, click the icon on the left to adventure."
                + " If you want to put the hat on first, click the icon on the right.";
        assertEquals(expected, request.lastWarning);
      }
    }

    @Nested
    class YouRobot {

      @AfterEach
      public void AfterEach() {
        YouRobotManager.reset();
      }

      @Test
      public void warningIfUnequippableMohawkWig() {
        var cleanups =
            new Cleanups(withPath(Path.YOU_ROBOT), withItem(MOHAWK_WIG), withStats(50, 50, 50));
        try (cleanups) {
          RelayRequest request = new RelayRequest(false);
          request.constructURLString(adventureURL(CASTLE_TOP_FLOOR, null), false);
          // No warning needed if Mohawk wig already equipped
          assertTrue(request.sendMohawkWigWarning());
          String expected =
              "You are about to adventure without your Mohawk Wig in the Castle."
                  + " It requires base Moxie of 55, but yours is only 50."
                  + " Perhaps it is time to visit Statbot 5000."
                  + " If you are sure you wish to adventure without it, click the icon on the left to adventure."
                  + " If you want to visit the Scrapheap, click the icon on the right.";
          assertEquals(expected, request.lastWarning);
        }
      }

      @Test
      public void warningIfEquippableMohawkWigWithoutMannequinHead() {
        var cleanups = new Cleanups(withPath(Path.YOU_ROBOT), withEquippableItem(MOHAWK_WIG));
        try (cleanups) {
          RelayRequest request = new RelayRequest(false);
          request.constructURLString(adventureURL(CASTLE_TOP_FLOOR, null), false);
          // No warning needed if Mohawk wig already equipped
          assertTrue(request.sendMohawkWigWarning());
          String expected =
              "You are about to adventure without your Mohawk Wig in the Castle."
                  + " You need to attach a Mannequin Head in order to wear a hat."
                  + " If you are sure you wish to adventure without it, click the icon on the left to adventure."
                  + " If you want to visit the Scrapheap, click the icon on the right.";
          assertEquals(expected, request.lastWarning);
        }
      }

      @Test
      public void warningIfEquippableMohawkWigWithMannequinHead() {
        var cleanups = new Cleanups(withPath(Path.YOU_ROBOT), withEquippableItem(MOHAWK_WIG));
        try (cleanups) {
          YouRobotManager.testInstallUpgrade(RobotUpgrade.MANNEQUIN_HEAD);
          RelayRequest request = new RelayRequest(false);
          request.constructURLString(adventureURL(CASTLE_TOP_FLOOR, null), false);
          // No warning needed if Mohawk wig already equipped
          assertTrue(request.sendMohawkWigWarning());
          String expected =
              "You are about to adventure without your Mohawk Wig in the Castle."
                  + " If you are sure you wish to adventure without it, click the icon on the left to adventure."
                  + " If you want to put the hat on first, click the icon on the right.";
          assertEquals(expected, request.lastWarning);
        }
      }
    }
  }

  @Nested
  class SurvivalKnife {
    private static final KoLAdventure A_BOO_PEAK =
        AdventureDatabase.getAdventureByName("A-Boo Peak");
    private static final KoLAdventure ARID_DESERT =
        AdventureDatabase.getAdventureByName("The Arid, Extra-Dry Desert");
    private static final String confirm = RelayRequest.CONFIRM_DESERT_WEAPON;
    private static final AdventureResult SURVIVAL_KNIFE = ItemPool.get(ItemPool.SURVIVAL_KNIFE);

    @Test
    public void thatSurvivalKnifeWarningWorks() {
      RelayRequest request = new RelayRequest(false);

      // No warning needed if you are not in the Arid, Extra-Dry Desert
      request.constructURLString(adventureURL(A_BOO_PEAK, null), true);
      assertFalse(request.sendDesertWeaponWarning());

      // No warning needed if this a resubmission of the "go ahead" URL
      request.constructURLString(adventureURL(ARID_DESERT, confirm), true);
      assertFalse(request.sendDesertWeaponWarning());
      assertTrue(RelayRequest.ignoreDesertWeaponWarning);

      // No warning needed if we already said to ignore warning
      request.constructURLString(adventureURL(ARID_DESERT, null), true);
      assertFalse(request.sendDesertWeaponWarning());
      RelayRequest.ignoreDesertWeaponWarning = false;

      // No warning if we don't have a survival knife in inventory
      assertFalse(request.sendDesertWeaponWarning());

      // No warning if we have a survival knife equipped
      int slot = EquipmentManager.WEAPON;
      AdventureResult knife = ItemPool.get(ItemPool.SURVIVAL_KNIFE);
      EquipmentManager.setEquipment(slot, SURVIVAL_KNIFE);
      assertFalse(request.sendDesertWeaponWarning());
      EquipmentManager.setEquipment(slot, EquipmentRequest.UNEQUIP);

      // Put a survival knife into inventory
      AdventureResult.addResultToList(KoLConstants.inventory, SURVIVAL_KNIFE);

      // No warning if we have already completed enough desert exploration
      Preferences.setInteger("desertExploration", 99);
      assertFalse(request.sendDesertWeaponWarning());
      Preferences.setInteger("desertExploration", 0);

      // At this point, we have the survival knife, have not completed desert
      // exploration, are adventuring in the Arid, Extra-Dry Desert, and have not
      // previously told KoLmafia to stop nagging.

      // We expect a warning.
      assertTrue(request.sendDesertWeaponWarning());
      String expected =
          "You are about to adventure without your survival knife in the desert. "
              + "If you are sure you wish to adventure without it, click the icon on the left to adventure. "
              + "If you want to equip the survival knife first, click the icon on the right. ";
      assertEquals(expected, request.lastWarning);
    }
  }

  @Nested
  class Machete {
    private static final KoLAdventure A_BOO_PEAK =
        AdventureDatabase.getAdventureByName("A-Boo Peak");
    private static final KoLAdventure NW_SHRINE =
        AdventureDatabase.getAdventureByName("An Overgrown Shrine (Northwest)");
    private static final KoLAdventure SW_SHRINE =
        AdventureDatabase.getAdventureByName("An Overgrown Shrine (Southwest)");
    private static final KoLAdventure NE_SHRINE =
        AdventureDatabase.getAdventureByName("An Overgrown Shrine (Northeast)");
    private static final KoLAdventure SE_SHRINE =
        AdventureDatabase.getAdventureByName("An Overgrown Shrine (Southeast)");
    private static final KoLAdventure ZIGGURAT =
        AdventureDatabase.getAdventureByName("A Massive Ziggurat");
    private static final String confirm = RelayRequest.CONFIRM_MACHETE;

    private void testMacheteItem(RelayRequest request, int itemId) {
      // No warning if we have the machete equipped
      int slot = EquipmentManager.WEAPON;
      AdventureResult machete = ItemPool.get(itemId);
      String name = machete.getName();
      EquipmentManager.setEquipment(slot, machete);
      assertFalse(request.sendMacheteWarning());
      EquipmentManager.setEquipment(slot, EquipmentRequest.UNEQUIP);

      // Put the machete in inventory
      AdventureResult.addResultToList(KoLConstants.inventory, machete);

      // Machetes have an Muscle requirement.
      EquipmentRequirement req =
          new EquipmentRequirement(EquipmentDatabase.getEquipRequirement(itemId));
      int required = req.getAmount();

      // Set Base Muscle to 5 below required and Buffed Muscle to 10 above required
      int muscle = required - 5;
      long base = KoLCharacter.calculatePointSubpoints(muscle);
      long buffed = KoLCharacter.calculatePointSubpoints(required + 10);
      KoLCharacter.setStatPoints((int) buffed, (int) base, 0, 0, 0, 0);
      assertEquals(muscle, KoLCharacter.getBaseMuscle());

      // Try it with no special path
      KoLCharacter.setPath(Path.NONE);
      assertTrue(request.sendMacheteWarning());
      String expected =
          "You are about to adventure without your "
              + name
              + " to fight dense lianas."
              + " It requires base Muscle of "
              + required
              + ", but yours is only "
              + muscle
              + "."
              + " If you are sure you wish to adventure without it, click the icon to adventure.";
      assertEquals(expected, request.lastWarning);

      // Try it in You, Robot
      KoLCharacter.setPath(Path.YOU_ROBOT);
      assertTrue(request.sendMacheteWarning());
      expected =
          "You are about to adventure without your "
              + name
              + " to fight dense lianas."
              + " It requires base Muscle of "
              + required
              + ", but yours is only "
              + muscle
              + "."
              + " Perhaps it is time to visit Statbot 5000."
              + " If you are sure you wish to adventure without it, click the icon on the left to adventure."
              + " If you want to visit the Scrapheap, click the icon on the right.";
      assertEquals(expected, request.lastWarning);

      // Set your stats to make the item equippable
      base = KoLCharacter.calculatePointSubpoints(required);
      KoLCharacter.setStatPoints((int) buffed, (int) base, 0, 0, 0, 0);
      assertEquals(required, KoLCharacter.getBaseMuscle());

      // Try again with no path
      KoLCharacter.setPath(Path.NONE);
      assertTrue(request.sendMacheteWarning());
      expected =
          "You are about to adventure without your "
              + name
              + " to fight dense lianas."
              + " If you are sure you wish to adventure without it, click the icon on the left to adventure."
              + " If you want to equip the "
              + name
              + " first, click the icon on the right.";
      assertEquals(expected, request.lastWarning);

      // Again in You, Robot, no Vice Grips
      KoLCharacter.setPath(Path.YOU_ROBOT);
      assertTrue(request.sendMacheteWarning());
      expected =
          "You are about to adventure without your "
              + name
              + " to fight dense lianas."
              + " You need to attach Vice Grips in order to wield a weapon."
              + " If you are sure you wish to adventure without it, click the icon on the left to adventure."
              + " If you want to visit the Scrapheap, click the icon on the right.";
      assertEquals(expected, request.lastWarning);

      // Again in You, Robot, with Vice Grips
      YouRobotManager.testInstallUpgrade(RobotUpgrade.VICE_GRIPS);
      assertTrue(request.sendMacheteWarning());
      expected =
          "You are about to adventure without your "
              + name
              + " to fight dense lianas."
              + " If you are sure you wish to adventure without it, click the icon on the left to adventure."
              + " If you want to equip the "
              + name
              + " first, click the icon on the right.";
      assertEquals(expected, request.lastWarning);

      YouRobotManager.reset();
      KoLCharacter.setPath(Path.NONE);

      // Remove the machete from inventory
      KoLConstants.inventory.clear();
    }

    private void testMacheteZone(RelayRequest request, KoLAdventure zone, String property) {
      request.constructURLString(adventureURL(zone, null), true);

      // If the property is zero, we have not yet seen the choice
      // adventure at the end of clearing out the dense lianas.
      // If greater than 0, no lianas remain.
      Preferences.setInteger(property, 1);
      assertFalse(request.sendMacheteWarning());
      Preferences.setInteger(property, 0);

      // Test each of the four machetes
      testMacheteItem(request, ItemPool.MUCULENT_MACHETE);
      testMacheteItem(request, ItemPool.PAPIER_MACHETE);
      testMacheteItem(request, ItemPool.MACHETITO);
      testMacheteItem(request, ItemPool.ANTIQUE_MACHETE);
    }

    @Test
    public void thatMacheteWarningWorks() {
      RelayRequest request = new RelayRequest(false);

      // No warning needed if you are not in a zone with lianas
      request.constructURLString(adventureURL(A_BOO_PEAK, null), true);
      assertFalse(request.sendMacheteWarning());

      // No warning needed if this a resubmission of the "go ahead" URL
      request.constructURLString(adventureURL(NW_SHRINE, RelayRequest.CONFIRM_MACHETE), true);
      assertFalse(request.sendMacheteWarning());
      assertTrue(RelayRequest.ignoreMacheteWarning);

      // No warning needed if we already said to ignore warning
      request.constructURLString(adventureURL(NW_SHRINE, null), true);
      assertFalse(request.sendMacheteWarning());
      RelayRequest.ignoreMacheteWarning = false;

      // No warning needed if the Hidden City quest is finished
      QuestDatabase.setQuestProgress(Quest.WORSHIP, QuestDatabase.FINISHED);
      assertFalse(request.sendMacheteWarning());
      QuestDatabase.setQuestProgress(Quest.WORSHIP, QuestDatabase.UNSTARTED);

      // No warning needed if you are on a path that does not allow you to equip a machete
      KoLCharacter.setPath(Path.SURPRISING_FIST);
      assertFalse(request.sendMacheteWarning());
      KoLCharacter.setPath(Path.AVATAR_OF_BORIS);
      assertFalse(request.sendMacheteWarning());
      KoLCharacter.setPath(Path.GLOVER);
      assertFalse(request.sendMacheteWarning());
      KoLCharacter.setPath(Path.NONE);

      // Warning if we don't have a machete in inventory
      assertTrue(request.sendMacheteWarning());
      String expected =
          "You are about to adventure without a machete to fight dense lianas. If you are sure you want to do this, click on the image to proceed.";
      assertEquals(expected, request.lastWarning);

      // Test each of the liana zones
      testMacheteZone(request, NW_SHRINE, "hiddenApartmentProgress");
      testMacheteZone(request, SW_SHRINE, "hiddenHospitalProgress");
      testMacheteZone(request, NE_SHRINE, "hiddenOfficeProgress");
      testMacheteZone(request, SE_SHRINE, "hiddenBowlingAlleyProgress");
      testMacheteZone(request, ZIGGURAT, "zigguratLianas");
    }
  }

  @Nested
  class Prism {
    private String prismURL(String confirmation) {
      StringBuilder buf = new StringBuilder();
      buf.append("place.php?whichplace=nstower&action=ns_11_prism");
      if (confirmation != null) {
        buf.append("&");
        buf.append(confirmation);
        buf.append("=on");
      }
      return buf.toString();
    }

    @Nested
    class Exploathing {
      @Test
      public void shouldNotWarnInExploathingWithNoIsotopes() {
        var cleanups = withPath(Path.KINGDOM_OF_EXPLOATHING);
        try (cleanups) {
          String URL = prismURL(null);
          RelayRequest request = new RelayRequest(false);
          request.constructURLString(URL);
          assertFalse(request.sendBreakPrismWarning(URL));
        }
      }

      @Test
      public void shouldWarnInExploathingWithIsotopes() {
        var cleanups =
            new Cleanups(
                withPath(Path.KINGDOM_OF_EXPLOATHING), withItem(ItemPool.RARE_MEAT_ISOTOPE));
        try (cleanups) {
          String URL = prismURL(null);
          RelayRequest request = new RelayRequest(false);
          request.constructURLString(URL);
          assertTrue(request.sendBreakPrismWarning(URL));
          String expected =
              "You are about to free King Ralph and end your Kingdom of Exploathing run."
                  + " Before you do so, you might want to redeem your rare Meat isotopes at Cosmic Ray's Bazaar,"
                  + " since you will not be able to do so after you free the king."
                  + " If you are ready to break the prism, click on the icon on the left."
                  + " If you wish to visit Cosmic Ray's Bazaar, click on icon on the right.";
          assertEquals(expected, request.lastWarning);
        }
      }

      @Test
      public void shouldNotWarnInExploathingIfAlreadyConfirmed() {
        var cleanups = withPath(Path.KINGDOM_OF_EXPLOATHING);
        try (cleanups) {
          String URL = prismURL(RelayRequest.CONFIRM_RALPH);
          RelayRequest request = new RelayRequest(false);
          request.constructURLString(URL);
          assertFalse(request.sendBreakPrismWarning(URL));
        }
      }
    }

    @Nested
    class RoboCore {
      public static Cleanups withYouRobotEnergy(final int energy) {
        var current = KoLCharacter.getYouRobotEnergy();
        KoLCharacter.setYouRobotEnergy(energy);
        return new Cleanups(() -> KoLCharacter.setYouRobotEnergy(current));
      }

      @Test
      public void thatNoWarningNeededIfNotAboutToBreakPrism() {
        var cleanups = new Cleanups(withPath(Path.YOU_ROBOT));
        try (cleanups) {
          RelayRequest request = new RelayRequest(false);
          String URL = "place.php?whichplace=scrapheap";
          request.constructURLString(URL, true);
          assertFalse(request.sendBreakPrismWarning(URL));
        }
      }

      @Test
      public void thatNoWarningNeededIfAlreadyConfirmed() {
        // Set path to You, Robot
        var cleanups = new Cleanups(withPath(Path.YOU_ROBOT), withYouRobotEnergy(100));
        try (cleanups) {
          // No warning needed if already confirmed
          RelayRequest request = new RelayRequest(false);
          String URL = prismURL(RelayRequest.CONFIRM_RALPH);
          request.constructURLString(URL, true);
          assertFalse(request.sendBreakPrismWarning(URL));
        }
      }

      @Test
      public void thatNoWarningIfInsufficientEnergy() {
        // Set path to You, Robot
        var cleanups =
            new Cleanups(
                withPath(Path.YOU_ROBOT),
                withYouRobotEnergy(10),
                withProperty("statbotUses", 10),
                withProperty("_chronolithNextCost", 30));
        try (cleanups) {
          // energy < Statbot < Chronolith
          RelayRequest request = new RelayRequest(false);
          String URL = prismURL(null);
          request.constructURLString(URL, true);
          assertFalse(request.sendBreakPrismWarning(URL));
        }
      }

      @Test
      public void thatWarningIfEnergyForStatbot() {
        // Set path to You, Robot
        var cleanups =
            new Cleanups(
                withPath(Path.YOU_ROBOT),
                withProperty("statbotUses", 10),
                withYouRobotEnergy(25),
                withProperty("_chronolithNextCost", 30));
        try (cleanups) {
          // Statbot < energy < Chronolith
          RelayRequest request = new RelayRequest(false);
          String URL = prismURL(null);
          request.constructURLString(URL, true);
          assertTrue(request.sendBreakPrismWarning(URL));
          String expected =
              "You are about to free King Ralph and stop being a Robot."
                  + " Before you do so, you might want to spend your remaining 25 energy in the Scrapheap,"
                  + " since you will not be able to do so after you free the king."
                  + " You can gain 5 points of the stat of your choice at Statbot 5000 for 20 energy."
                  + " If you are ready to break the prism, click on the icon on the left."
                  + " If you wish to visit the Scrapheap, click on icon on the right.";
          assertEquals(expected, request.lastWarning);
        }
      }

      @Test
      public void thatWarningIfEnergyForChronolith() {
        // Set path to You, Robot
        var cleanups =
            new Cleanups(
                withPath(Path.YOU_ROBOT),
                withProperty("_chronolithNextCost", 20),
                withYouRobotEnergy(25),
                withProperty("statbotUses", 20));
        try (cleanups) {
          // Chronolith < energy < Statbot
          RelayRequest request = new RelayRequest(false);
          String URL = prismURL(null);
          request.constructURLString(URL, true);
          assertTrue(request.sendBreakPrismWarning(URL));
          String expected =
              "You are about to free King Ralph and stop being a Robot."
                  + " Before you do so, you might want to spend your remaining 25 energy in the Scrapheap,"
                  + " since you will not be able to do so after you free the king."
                  + " You can gain 10 Adventures at the Chronolith for 20 energy."
                  + " If you are ready to break the prism, click on the icon on the left."
                  + " If you wish to visit the Scrapheap, click on icon on the right.";
          assertEquals(expected, request.lastWarning);
        }
      }

      @Test
      public void thatWarningIfEnergyForEither() {
        // Set path to You, Robot
        var cleanups =
            new Cleanups(
                withPath(Path.YOU_ROBOT),
                withProperty("statbotUses", 20),
                withProperty("_chronolithNextCost", 20),
                withYouRobotEnergy(50));
        try (cleanups) {
          // Chronolith < Statbot < energy
          RelayRequest request = new RelayRequest(false);
          String URL = prismURL(null);
          request.constructURLString(URL, true);
          assertTrue(request.sendBreakPrismWarning(URL));
          String expected =
              "You are about to free King Ralph and stop being a Robot."
                  + " Before you do so, you might want to spend your remaining 50 energy in the Scrapheap,"
                  + " since you will not be able to do so after you free the king."
                  + " You can gain 10 Adventures at the Chronolith for 20 energy."
                  + " You can gain 5 points of the stat of your choice at Statbot 5000 for 30 energy."
                  + " If you are ready to break the prism, click on the icon on the left."
                  + " If you wish to visit the Scrapheap, click on icon on the right.";
          assertEquals(expected, request.lastWarning);
        }
      }
    }

    @Nested
    class DinoCore {
      @Test
      public void shouldNotWarnInDinocoreWithNoDollars() {
        var cleanups = withPath(Path.DINOSAURS);
        try (cleanups) {
          String URL = prismURL(null);
          RelayRequest request = new RelayRequest(false);
          request.constructURLString(URL);
          assertFalse(request.sendBreakPrismWarning(URL));
        }
      }

      @Test
      public void shouldWarnInDinocoreWithDollars() {
        var cleanups = new Cleanups(withPath(Path.DINOSAURS), withItem(ItemPool.DINODOLLAR));
        try (cleanups) {
          String URL = prismURL(null);
          RelayRequest request = new RelayRequest(false);
          request.constructURLString(URL);
          assertTrue(request.sendBreakPrismWarning(URL));
          String expected =
              "You are about to free King Ralph and end your Fall of the Dinosaurs run."
                  + " Before you do so, you might want to spend your Dinodollars at the Dino Staur,"
                  + " since you will not be able to do so after you free the king."
                  + " If you are ready to break the prism, click on the icon on the left."
                  + " If you wish to visit the Dino Staur, click on icon on the right.";
          assertEquals(expected, request.lastWarning);
        }
      }

      @Test
      public void shouldNotWarnInDinocoreIfAlreadyConfirmed() {
        var cleanups = withPath(Path.DINOSAURS);
        try (cleanups) {
          String URL = prismURL(RelayRequest.CONFIRM_RALPH);
          RelayRequest request = new RelayRequest(false);
          request.constructURLString(URL);
          assertFalse(request.sendBreakPrismWarning(URL));
        }
      }
    }
  }

  @Nested
  class UnhydratedDesert {
    private static final KoLAdventure A_BOO_PEAK =
        AdventureDatabase.getAdventureByName("A-Boo Peak");
    private static final KoLAdventure DESERT =
        AdventureDatabase.getAdventureByName("The Arid, Extra-Dry Desert");
    private static final String confirm = RelayRequest.CONFIRM_DESERT_UNHYDRATED;

    @BeforeEach
    public void beforeEach() {
      RelayRequest.ignoreDesertWarning = false;
    }

    @Test
    public void noWarningIfNotDesert() {
      var cleanups = new Cleanups();
      try (cleanups) {
        RelayRequest request = new RelayRequest(false);
        request.constructURLString(adventureURL(A_BOO_PEAK, null), false);
        // No warning needed if you are not in The Arid, Ultra-Dry Desert
        assertFalse(request.sendUnhydratedDesertWarning());
      }
    }

    @Test
    public void noWarningIfConfirmed() {
      var cleanups = new Cleanups();
      try (cleanups) {
        RelayRequest request = new RelayRequest(false);
        request.constructURLString(adventureURL(DESERT, confirm), false);
        // No warning needed if this a resubmission with confirmation
        assertFalse(request.sendUnhydratedDesertWarning());
        assertTrue(RelayRequest.ignoreDesertWarning);
      }
    }

    @Test
    public void noWarningIfPreviouslyConfirmed() {
      var cleanups = new Cleanups();
      try (cleanups) {
        RelayRequest request = new RelayRequest(false);
        request.constructURLString(adventureURL(DESERT, null), false);
        // No warning needed if this was previously confirmed
        RelayRequest.ignoreDesertWarning = true;
        assertFalse(request.sendUnhydratedDesertWarning());
        assertTrue(RelayRequest.ignoreDesertWarning);
      }
    }

    @Test
    public void noWarningIfUltrahydrated() {
      var cleanups = withEffect(EffectPool.ULTRAHYDRATED, 10);
      try (cleanups) {
        RelayRequest request = new RelayRequest(false);
        request.constructURLString(DESERT.getRequest().getURLString());
        assertFalse(request.sendUnhydratedDesertWarning());
      }
    }

    @Test
    public void noWarningIfOasisNotOpenYet() {
      var cleanups =
          new Cleanups(
              withProperty("oasisAvailable", false), withProperty("desertExploration", 10));
      try (cleanups) {
        RelayRequest request = new RelayRequest(false);
        request.constructURLString(DESERT.getRequest().getURLString());
        assertFalse(request.sendUnhydratedDesertWarning());
      }
    }

    @Test
    public void noWarningIfDesertFullyExplored() {
      var cleanups =
          new Cleanups(
              withProperty("oasisAvailable", true), withProperty("desertExploration", 100));
      try (cleanups) {
        RelayRequest request = new RelayRequest(false);
        request.constructURLString(DESERT.getRequest().getURLString());
        assertFalse(request.sendUnhydratedDesertWarning());
      }
    }

    @Test
    public void warningIfOasisOpenAndDesertNotFullyExplored() {
      var cleanups =
          new Cleanups(withProperty("oasisAvailable", true), withProperty("desertExploration", 20));
      try (cleanups) {
        RelayRequest request = new RelayRequest(false);
        request.constructURLString(DESERT.getRequest().getURLString());
        assertTrue(request.sendUnhydratedDesertWarning());
      }
    }
  }
}
