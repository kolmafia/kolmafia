package net.sourceforge.kolmafia.request;

import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static org.junit.jupiter.api.Assertions.*;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.EquipmentRequirement;
import net.sourceforge.kolmafia.session.YouRobotManager;
import net.sourceforge.kolmafia.session.YouRobotManager.RobotUpgrade;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
    YouRobotManager.reset();
  }

  @AfterAll
  public static void afterAll() {
    KoLConstants.inventory.clear();
    RelayRequest.reset();
    EquipmentManager.resetEquipment();
    YouRobotManager.reset();
  }

  private String adventureURL(int adventureId, String confirmation) {
    StringBuilder buf = new StringBuilder();
    buf.append("adventure.php?snarfblat=");
    buf.append(adventureId);
    if (confirmation != null) {
      buf.append("&");
      buf.append(confirmation);
      buf.append("=on");
    }
    return buf.toString();
  }

  @Nested
  class MohawkWig {
    @Test
    public void thatMohawkWigWarningWorks() {
      RelayRequest request = new RelayRequest(false);

      // No warning needed if you are not in the Castle Top Floor
      request.constructURLString(adventureURL(AdventurePool.ABOO_PEAK, null), true);
      assertFalse(request.sendMohawkWigWarning());

      // No warning needed if this a resubmission of the "go ahead" URL
      request.constructURLString(
          adventureURL(AdventurePool.CASTLE_TOP, RelayRequest.CONFIRM_MOHAWK_WIG), true);
      assertFalse(request.sendMohawkWigWarning());
      assertTrue(RelayRequest.ignoreMohawkWigWarning);

      // No warning needed if we already said to ignore warning
      request.constructURLString(adventureURL(AdventurePool.CASTLE_TOP, null), true);
      assertFalse(request.sendMohawkWigWarning());
      RelayRequest.ignoreMohawkWigWarning = false;

      // No warning if we don't have a Mohawk wig in inventory
      assertFalse(request.sendMohawkWigWarning());

      // No warning if we have a Mohawk wig equipped
      int slot = EquipmentManager.HAT;
      AdventureResult wig = ItemPool.get(ItemPool.MOHAWK_WIG);
      EquipmentManager.setEquipment(slot, wig);
      assertFalse(request.sendMohawkWigWarning());
      EquipmentManager.setEquipment(slot, EquipmentRequest.UNEQUIP);

      // Put a Mohawk wig into inventory
      AdventureResult.addResultToList(KoLConstants.inventory, ItemPool.get(ItemPool.MOHAWK_WIG));

      // No warning if we have already completed the castle quest
      QuestDatabase.setQuestIfBetter(Quest.GARBAGE, "step10");
      assertFalse(request.sendMohawkWigWarning());
      QuestDatabase.setQuestProgress(Quest.GARBAGE, QuestDatabase.UNSTARTED);

      // At this point, we have the wig in inventory, have not completed the
      // castle quest, are adventuring on the top floor of the castle, and have
      // not previously told KoLmafia to stop nagging.

      // Set Base Moxie to 50 and Buffed Moxie to 60
      long base = KoLCharacter.calculatePointSubpoints(50);
      long buffed = KoLCharacter.calculatePointSubpoints(60);
      KoLCharacter.setStatPoints(0, 0, 0, 0, (int) buffed, (int) base);
      assertEquals(50, KoLCharacter.getBaseMoxie());

      // We expect a warning.
      KoLCharacter.setPath(Path.NONE);
      assertTrue(request.sendMohawkWigWarning());
      String expected =
          "You are about to adventure without your Mohawk Wig in the Castle."
              + " It requires base Moxie of 55, but yours is only 50."
              + " If you are sure you wish to adventure without it, click the icon to adventure.";
      assertEquals(expected, request.lastWarning);

      // Try it in You, Robot
      KoLCharacter.setPath(Path.YOU_ROBOT);
      assertTrue(request.sendMohawkWigWarning());
      expected =
          "You are about to adventure without your Mohawk Wig in the Castle."
              + " It requires base Moxie of 55, but yours is only 50."
              + " Perhaps it is time to visit Statbot 5000."
              + " If you are sure you wish to adventure without it, click the icon on the left to adventure."
              + " If you want to visit the Scrapheap, click the icon on the right.";
      assertEquals(expected, request.lastWarning);

      // Set your stats to make the item equippable
      base = KoLCharacter.calculatePointSubpoints(55);
      KoLCharacter.setStatPoints(0, 0, 0, 0, (int) buffed, (int) base);
      assertEquals(55, KoLCharacter.getBaseMoxie());

      // Try again with no path
      KoLCharacter.setPath(Path.NONE);
      assertTrue(request.sendMohawkWigWarning());
      expected =
          "You are about to adventure without your Mohawk Wig in the Castle."
              + " If you are sure you wish to adventure without it, click the icon on the left to adventure."
              + " If you want to put the hat on first, click the icon on the right.";
      assertEquals(expected, request.lastWarning);

      // Again in You, Robot, no Mannequin Head
      KoLCharacter.setPath(Path.YOU_ROBOT);
      assertTrue(request.sendMohawkWigWarning());
      expected =
          "You are about to adventure without your Mohawk Wig in the Castle."
              + " You need to attach a Mannequin Head in order to wear a hat."
              + " If you are sure you wish to adventure without it, click the icon on the left to adventure."
              + " If you want to visit the Scrapheap, click the icon on the right.";
      assertEquals(expected, request.lastWarning);

      // Again in You, Robot, with Mannequin Head
      YouRobotManager.testInstallUpgrade(RobotUpgrade.MANNEQUIN_HEAD);
      assertTrue(request.sendMohawkWigWarning());
      expected =
          "You are about to adventure without your Mohawk Wig in the Castle."
              + " If you are sure you wish to adventure without it, click the icon on the left to adventure."
              + " If you want to put the hat on first, click the icon on the right.";
      assertEquals(expected, request.lastWarning);
    }
  }

  @Nested
  class SurvivalKnife {
    @Test
    public void thatSurvivalKnifeWarningWorks() {
      RelayRequest request = new RelayRequest(false);

      // No warning needed if you are not in the Arid, Extra-Dry Desert
      request.constructURLString(adventureURL(AdventurePool.ABOO_PEAK, null), true);
      assertFalse(request.sendDesertWeaponWarning());

      // No warning needed if this a resubmission of the "go ahead" URL
      request.constructURLString(
          adventureURL(AdventurePool.ARID_DESERT, RelayRequest.CONFIRM_DESERT_WEAPON), true);
      assertFalse(request.sendDesertWeaponWarning());
      assertTrue(RelayRequest.ignoreDesertWeaponWarning);

      // No warning needed if we already said to ignore warning
      request.constructURLString(adventureURL(AdventurePool.ARID_DESERT, null), true);
      assertFalse(request.sendDesertWeaponWarning());
      RelayRequest.ignoreDesertWeaponWarning = false;

      // No warning if we don't have a survival knife in inventory
      assertFalse(request.sendDesertWeaponWarning());

      // No warning if we have a survival knife equipped
      int slot = EquipmentManager.WEAPON;
      AdventureResult knife = ItemPool.get(ItemPool.SURVIVAL_KNIFE);
      EquipmentManager.setEquipment(slot, knife);
      assertFalse(request.sendDesertWeaponWarning());
      EquipmentManager.setEquipment(slot, EquipmentRequest.UNEQUIP);

      // Put a survival knife into inventory
      AdventureResult.addResultToList(
          KoLConstants.inventory, ItemPool.get(ItemPool.SURVIVAL_KNIFE));

      // No warning if we have already completed enough desert exploration
      Preferences.setInteger("desertExploration", 99);
      assertFalse(request.sendDesertWeaponWarning());
      Preferences.setInteger("desertExploration", 0);

      // At this point, we have the survival knife, have not completed desert
      // exploration, are adventuring in the Arid, Extra-Dry Desert, and have not
      // previously told KoLmafia to stop nagging.

      // We expect a warning.
      KoLCharacter.setPath(Path.NONE);
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

    private void testMacheteZone(RelayRequest request, int zone, String property) {
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
      request.constructURLString(adventureURL(AdventurePool.ABOO_PEAK, null), true);
      assertFalse(request.sendMacheteWarning());

      // No warning needed if this a resubmission of the "go ahead" URL
      request.constructURLString(
          adventureURL(AdventurePool.NW_SHRINE, RelayRequest.CONFIRM_MACHETE), true);
      assertFalse(request.sendMacheteWarning());
      assertTrue(RelayRequest.ignoreMacheteWarning);

      // No warning needed if we already said to ignore warning
      request.constructURLString(adventureURL(AdventurePool.NW_SHRINE, null), true);
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
      testMacheteZone(request, AdventurePool.NW_SHRINE, "hiddenApartmentProgress");
      testMacheteZone(request, AdventurePool.SW_SHRINE, "hiddenHospitalProgress");
      testMacheteZone(request, AdventurePool.NE_SHRINE, "hiddenOfficeProgress");
      testMacheteZone(request, AdventurePool.SE_SHRINE, "hiddenBowlingAlleyProgress");
      testMacheteZone(request, AdventurePool.ZIGGURAT, "zigguratLianas");
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
}
