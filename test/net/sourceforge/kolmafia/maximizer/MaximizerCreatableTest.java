package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Maximizer.*;
import static internal.helpers.Player.*;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RestrictedItemType;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class MaximizerCreatableTest {
  @BeforeAll
  public static void init() {
    KoLCharacter.reset("creator");
    Preferences.reset("creator");
  }

  @Test
  public void canPasteAsshat() {
    var cleanups = new Cleanups(withItem("bum cheek", 2), withItem("meat paste", 1));

    try (cleanups) {
      ConcoctionDatabase.refreshConcoctions();
      maximizeCreatable("sleaze dmg");
      recommendedSlotIs(Slot.HAT, "asshat");
    }
  }

  @Nested
  class BarrelShrine {
    @Test
    public void canCreateBarrelItems() {
      var cleanups = withProperty("barrelShrineUnlocked", true);

      try (cleanups) {
        ConcoctionDatabase.refreshConcoctions();
        maximizeCreatable("ml");
        recommendedSlotIs(Slot.OFFHAND, "barrel lid");
      }
    }

    @Test
    public void cannotCreateBarrelItemsTwice() {
      var cleanups =
          new Cleanups(
              withProperty("barrelShrineUnlocked", true),
              withProperty("prayedForProtection", true));

      try (cleanups) {
        ConcoctionDatabase.refreshConcoctions();
        maximizeCreatable("ml");
        recommendedSlotIsUnchanged(Slot.OFFHAND);
      }
    }

    @Test
    public void cannotCreateBarrelItemsAfterPrayer() {
      var cleanups =
          new Cleanups(
              withProperty("barrelShrineUnlocked", true), withProperty("_barrelPrayer", true));

      try (cleanups) {
        ConcoctionDatabase.refreshConcoctions();
        maximizeCreatable("ml");
        recommendedSlotIsUnchanged(Slot.OFFHAND);
      }
    }

    @Test
    public void cannotCreateBarrelItemsInStandard() {
      var cleanups =
          new Cleanups(
              withProperty("barrelShrineUnlocked", true),
              withRestricted(true),
              withNotAllowedInStandard(RestrictedItemType.ITEMS, "shrine to the Barrel god"));

      try (cleanups) {
        ConcoctionDatabase.refreshConcoctions();
        maximizeCreatable("ml");
        recommendedSlotIsUnchanged(Slot.OFFHAND);
      }
    }
  }

  @Nested
  class FantasyRealm {
    @ParameterizedTest
    @ValueSource(strings = {"frAlways", "_frToday"})
    public void canCreateFantasyRealmItems(String pref) {
      var cleanups = withProperty(pref, true);

      try (cleanups) {
        ConcoctionDatabase.refreshConcoctions();
        maximizeCreatable("moxie");
        recommendedSlotIs(Slot.HAT, "FantasyRealm Rogue's Mask");
      }
    }

    @Test
    public void cannotCreateFantasyRealmItemsIfAlreadyUsed() {
      var cleanups = new Cleanups(withProperty("frAlways", true), withProperty("_frHoursLeft", 5));

      try (cleanups) {
        ConcoctionDatabase.refreshConcoctions();
        maximizeCreatable("moxie");
        recommendedSlotIsUnchanged(Slot.HAT);
      }
    }

    @Test
    public void cannotCreateFantasyRealmItemsInStandard() {
      var cleanups =
          new Cleanups(
              withProperty("frAlways", true),
              withRestricted(true),
              withNotAllowedInStandard(RestrictedItemType.ITEMS, "FantasyRealm membership packet"));

      try (cleanups) {
        ConcoctionDatabase.refreshConcoctions();
        maximizeCreatable("moxie");
        recommendedSlotIsUnchanged(Slot.HAT);
      }
    }
  }

  @Nested
  class Floundry {
    @Test
    public void canCreateFloundryItems() {
      var cleanups =
          new Cleanups(
              withClanLoungeItem(ItemPool.CLAN_FLOUNDRY), withClanLoungeItem(ItemPool.CARPE));

      try (cleanups) {
        ConcoctionDatabase.refreshConcoctions();
        maximizeCreatable("meat");
        recommendedSlotIs(Slot.CONTAINER, "carpe");
      }
    }

    @Test
    public void cannotCreateFloundryItemsInStandard() {
      var cleanups =
          new Cleanups(
              withClanLoungeItem(ItemPool.CLAN_FLOUNDRY),
              withClanLoungeItem(ItemPool.CARPE),
              withRestricted(true),
              withNotAllowedInStandard(RestrictedItemType.ITEMS, "Clan Floundry"));

      try (cleanups) {
        ConcoctionDatabase.refreshConcoctions();
        maximizeCreatable("meat");
        recommendedSlotIsUnchanged(Slot.CONTAINER);
      }
    }
  }

  @Nested
  class NPCStore {
    @Test
    public void canBuyUtensil() {
      var cleanups =
          new Cleanups(
              withProperty("autoSatisfyWithNPCs", true),
              withProperty("autoBuyPriceLimit", 2_000),
              withMeat(2000));

      try (cleanups) {
        maximizeCreatable("spell dmg");
        recommendedSlotIs(Slot.WEAPON, "rubber spatula");
      }
    }

    @Test
    public void buyBestUtensil() {
      var cleanups =
          new Cleanups(
              withProperty("autoSatisfyWithNPCs", true),
              withProperty("autoBuyPriceLimit", 2_000),
              withMeat(2000),
              withStats(100, 100, 100));

      try (cleanups) {
        maximizeCreatable("spell dmg");
        recommendedSlotIs(Slot.WEAPON, "obsidian nutcracker");
      }
    }

    @Test
    public void canOnlyBuyOneSphygmayomanometer() {
      var cleanups =
          new Cleanups(
              withProperty("autoSatisfyWithNPCs", true),
              withWorkshedItem(ItemPool.MAYO_CLINIC),
              withStats(100, 100, 100));

      try (cleanups) {
        maximizeCreatable("muscle");
        recommends("sphygmayomanometer");
        recommendedSlotIsUnchanged(Slot.ACCESSORY2);
        recommendedSlotIsUnchanged(Slot.ACCESSORY3);
      }
    }

    @Test
    public void canOnlyBuyOneOversizedSparkler() {
      var cleanups =
          new Cleanups(
              withProperty("autoSatisfyWithNPCs", true),
              withSkill("Double-Fisted Skull Smashing"),
              withProperty("_fireworksShop", true));

      try (cleanups) {
        ConcoctionDatabase.refreshConcoctions();
        maximizeCreatable("item drop");
        recommendedSlotIs(Slot.WEAPON, "oversized sparkler");
        recommendedSlotIsUnchanged(Slot.OFFHAND);
      }
    }

    @Test
    public void cannotCreateFireworkHatIfAlreadyHave() {
      var cleanups =
          new Cleanups(
              withProperty("autoSatisfyWithNPCs", true),
              withProperty("_fireworksShop", true),
              withProperty("_fireworksShopHatBought", true));

      try (cleanups) {
        ConcoctionDatabase.refreshConcoctions();
        maximizeCreatable("-combat");
        recommendedSlotIsUnchanged(Slot.HAT);
      }
    }
  }
}
