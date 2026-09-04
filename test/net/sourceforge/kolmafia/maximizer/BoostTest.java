package net.sourceforge.kolmafia.maximizer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.nullValue;

import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.Modeable;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import org.junit.jupiter.api.Test;

class BoostTest {
  @Test
  void disabledBoostsAreGrayAndEscapeAmpersands() {
    var boost = new Boost("", "buy & use", ItemPool.get(ItemPool.POCKET_WISH), 1);

    assertThat(boost.toString(), containsString("<font color=gray>"));
    assertThat(boost.toString(), containsString("buy &amp; use"));
    assertThat(boost.execute(false), is(false));
  }

  @Test
  void ordersEquipmentPriorityAndScoreInThatOrder() {
    var equipment =
        new Boost(
            "equip hat \u00B6" + ItemPool.HELMET_TURTLE,
            "helmet",
            Slot.HAT,
            ItemPool.get(ItemPool.HELMET_TURTLE),
            1);
    var priority =
        new Boost(
            "gain", "priority", EffectPool.get(EffectPool.LEASH_OF_LINGUINI), false, null, 1, true);
    var ordinary =
        new Boost(
            "gain",
            "ordinary",
            EffectPool.get(EffectPool.LEASH_OF_LINGUINI),
            false,
            null,
            2,
            false);

    assertThat(equipment.compareTo(priority), lessThan(0));
    assertThat(priority.compareTo(ordinary), lessThan(0));
    assertThat(
        ordinary.compareTo(new Boost("gain", "lower", (AdventureResult) null, 1)), lessThan(0));
    assertThat(ordinary.compareTo(null), lessThan(0));
  }

  @Test
  void appliesEquipmentFamiliarEffectsAndHorseryToLoadouts() {
    var loadout = new MaximizerLoadout();
    var item = ItemPool.get(ItemPool.HELMET_TURTLE);
    var equipment =
        new Boost(
            "equip",
            "equip",
            Slot.HAT,
            item,
            1,
            FamiliarData.NO_FAMILIAR,
            FamiliarData.NO_FAMILIAR,
            Map.<Modeable, String>of());
    equipment.addTo(loadout);
    assertThat(loadout.equipment.get(Slot.HAT), is(item));

    var familiar = new FamiliarData(1);
    new Boost("familiar", "familiar", familiar, 1).addTo(loadout);
    assertThat(loadout.getFamiliar(), is(familiar));

    var effect = EffectPool.get(EffectPool.LEASH_OF_LINGUINI);
    var gain = new Boost("gain", "gain", effect, false, null, 1, false);
    gain.addTo(loadout);
    assertThat(loadout.hasEffect(effect), is(true));
    assertThat(gain.getItem(), is(effect));
    assertThat(gain.getItem(false), nullValue());

    new Boost("shrug", "shrug", effect, true, null, 1, false).addTo(loadout);
    assertThat(loadout.hasEffect(effect), is(false));

    new Boost("horsery dark", "horse", "dark horse", 1).addTo(loadout);
    assertThat(loadout.getHorsery(), is("dark horse"));
  }

  @Test
  void executesCommandsUnlessRestrictedToEquipment() {
    var boost = new Boost("echo boost test", "echo", (AdventureResult) null, 1);

    assertThat(boost.execute(true), is(false));
    assertThat(boost.execute(false), is(true));
  }
}
