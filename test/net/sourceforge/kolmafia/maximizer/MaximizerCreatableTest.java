package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Maximizer.*;
import static internal.helpers.Player.*;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
      recommendedSlotIs(EquipmentManager.HAT, "asshat");
    }
  }

  @Nested
  class NPCStore {
    @Test
    public void canBuyUtensil() {
      var cleanups = new Cleanups(withProperty("autoSatisfyWithNPCs", true),
          withProperty("autoBuyPriceLimit", 2_000), withMeat(2000));

      try (cleanups) {
        maximizeCreatable("spell dmg");
        recommendedSlotIs(EquipmentManager.WEAPON, "rubber spatula");
      }
    }

    @Test
    public void buyBestUtensil() {
      var cleanups = new Cleanups(withProperty("autoSatisfyWithNPCs", true),
          withProperty("autoBuyPriceLimit", 2_000), withMeat(2000), withStats(100, 100, 100));

      try (cleanups) {
        maximizeCreatable("spell dmg");
        recommendedSlotIs(EquipmentManager.WEAPON, "obsidian nutcracker");
      }
    }

    @Test
    public void canOnlyBuyOneSphygmayomanometer() {
      var cleanups = new Cleanups(withProperty("autoSatisfyWithNPCs", true), withWorkshedItem(ItemPool.MAYO_CLINIC), withStats(100, 100, 100));

      try (cleanups) {
        maximizeCreatable("muscle");
        recommends("sphygmayomanometer");
        recommendedSlotIsUnchanged(EquipmentManager.ACCESSORY2);
        recommendedSlotIsUnchanged(EquipmentManager.ACCESSORY3);
      }
    }

    @Test
    public void canOnlyBuyOneOversizedSparkler() {
      var cleanups =
          new Cleanups(withProperty("autoSatisfyWithNPCs", true),
              withSkill("Double-Fisted Skull Smashing"), withProperty("_fireworksShop", true));

      try (cleanups) {
        ConcoctionDatabase.refreshConcoctions();
        maximizeCreatable("item drop");
        recommendedSlotIs(EquipmentManager.WEAPON, "oversized sparkler");
        recommendedSlotIsUnchanged(EquipmentManager.OFFHAND);
      }
    }

    @Test
    public void cannotCreateFireworkHatIfAlreadyHave() {
      var cleanups =
          new Cleanups(withProperty("autoSatisfyWithNPCs", true),
              withProperty("_fireworksShop", true),
              withProperty("_fireworksShopHatBought", true));

      try (cleanups) {
        ConcoctionDatabase.refreshConcoctions();
        maximizeCreatable("-combat");
        recommendedSlotIsUnchanged(EquipmentManager.HAT);
      }
    }
  }
}
