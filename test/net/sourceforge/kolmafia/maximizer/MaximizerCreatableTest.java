package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Maximizer.*;
import static internal.helpers.Player.*;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MaximizerCreatableTest {
  @BeforeEach
  public void init() {
    KoLCharacter.reset("creator");
    Preferences.reset("creator");
    Preferences.setInteger("autoBuyPriceLimit", 1_000_000);
    Preferences.setBoolean("autoSatisfyWithNPCs", true);
    KoLCharacter.setAvailableMeat(1_000_000);
  }

  @Test
  public void canBuyUtensil() {
    maximizeCreatable("spell dmg");
    recommends("rubber spatula");
  }

  @Test
  public void canPasteAsshat() {
    addItem("bum cheek", 2);
    addItem("meat paste", 1);
    maximizeCreatable("sleaze dmg");
    recommendedSlotIs(EquipmentManager.HAT, "asshat");
  }

  @Test
  public void canOnlyBuyOneSphygmayomanometer() {
    CampgroundRequest.setCurrentWorkshedItem(ItemPool.MAYO_CLINIC);
    maximizeCreatable("muscle");
    recommends("sphygmayomanometer");
    // TODO: two accessory slots are empty
  }

}
