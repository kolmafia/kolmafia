package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Maximizer.*;
import static internal.helpers.Player.*;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MaximizerCreatableTest {
  @BeforeEach
  public void init() {
    KoLCharacter.reset("creator");
    KoLCharacter.reset(true);
    Preferences.reset("creator");
    Preferences.setInteger("autoBuyPriceLimit", 1_000_000);
    Preferences.setBoolean("autoSatisfyWithNPCs", true);
    KoLCharacter.setAvailableMeat(1_000_000);
  }

  @Test
  public void canBuyUtensil() {
    maximizeCreatable("spell dmg");
    recommendedSlotIs(EquipmentManager.WEAPON, "rubber spatula");
  }

  @Test
  public void buyBestUtensil() {
    setStats(100, 100, 100);
    maximizeCreatable("spell dmg");
    recommendedSlotIs(EquipmentManager.WEAPON, "obsidian nutcracker");
  }

  @Test
  public void canPasteAsshat() {
    addItem("bum cheek", 2);
    addItem("meat paste", 1);
    ConcoctionDatabase.refreshConcoctions();
    maximizeCreatable("sleaze dmg");
    recommendedSlotIs(EquipmentManager.HAT, "asshat");
  }

  @Test
  public void canOnlyBuyOneSphygmayomanometer() {
    CampgroundRequest.setCurrentWorkshedItem(ItemPool.MAYO_CLINIC);
    setStats(100, 100, 100);
    maximizeCreatable("muscle");
    recommends("sphygmayomanometer");
    recommendedSlotIsUnchanged(EquipmentManager.ACCESSORY2);
    recommendedSlotIsUnchanged(EquipmentManager.ACCESSORY3);
  }

  @Test
  public void canOnlyBuyOneOversizedSparkler() {
    addSkill("Double-Fisted Skull Smashing");
    Preferences.setBoolean("_fireworksShop", true);
    ConcoctionDatabase.refreshConcoctions();
    maximizeCreatable("item drop");
    recommendedSlotIs(EquipmentManager.WEAPON, "oversized sparkler");
    recommendedSlotIsUnchanged(EquipmentManager.OFFHAND);
  }

  @Test
  public void cannotCreateFireworkHatIfAlreadyHave() {
    Preferences.setBoolean("_fireworksShop", true);
    Preferences.setBoolean("_fireworksShopHatBought", true);
    ConcoctionDatabase.refreshConcoctions();
    maximizeCreatable("-combat");
    recommendedSlotIsUnchanged(EquipmentManager.HAT);
  }
}
