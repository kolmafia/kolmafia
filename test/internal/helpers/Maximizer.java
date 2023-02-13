package internal.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.filterType;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.maximizer.Boost;
import net.sourceforge.kolmafia.maximizer.EquipScope;
import net.sourceforge.kolmafia.modifiers.Modifier;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.swingui.MaximizerFrame;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class Maximizer {

  public static boolean maximize(String maximizerString) {
    return net.sourceforge.kolmafia.maximizer.Maximizer.maximize(maximizerString, 0, 0, true);
  }

  public static void maximizeCreatable(String maximizerString) {
    MaximizerFrame.expressionSelect.setSelectedItem(maximizerString);
    net.sourceforge.kolmafia.maximizer.Maximizer.maximize(
        EquipScope.SPECULATE_CREATABLE, 0, 0, false, EnumSet.allOf(filterType.class));
  }

  public static double modFor(Modifier modifier) {
    return ModifierDatabase.getNumericModifier(ModifierType.GENERATED, "_spec", modifier);
  }

  public static List<Boost> getBoosts() {
    return net.sourceforge.kolmafia.maximizer.Maximizer.boosts;
  }

  public static Optional<AdventureResult> getSlot(Slot slot) {
    var boost =
        getBoosts().stream().filter(Boost::isEquipment).filter(b -> b.getSlot() == slot).findAny();
    return boost.map(Boost::getItem);
  }

  public static void recommendedSlotIs(Slot slot, String item) {
    Optional<AdventureResult> equipment = getSlot(slot);
    assertTrue(equipment.isPresent(), "Expected " + item + " to be recommended, but it was not");
    assertEquals(AdventureResult.tallyItem(StringUtilities.getEntityEncode(item)), equipment.get());
  }

  public static void recommendedSlotIsUnchanged(Slot slot) {
    Optional<AdventureResult> equipment = getSlot(slot);
    assertTrue(
        equipment.isEmpty(),
        () ->
            "Expected empty slot "
                + slot
                + ", but it was "
                + equipment.map(AdventureResult::toString).orElse(""));
  }

  public static void recommends(String item) {
    Optional<Boost> found =
        getBoosts().stream()
            .filter(Boost::isEquipment)
            .filter(b -> item.equals(b.getItem().getName()))
            .findAny();
    assertTrue(found.isPresent(), "Expected " + item + " to be recommended, but it was not");
  }

  public static void recommends(int itemId) {
    Optional<Boost> found =
        getBoosts().stream()
            .filter(Boost::isEquipment)
            .filter(b -> (itemId == b.getItem().getItemId()))
            .findAny();
    assertTrue(found.isPresent(), "Expected " + itemId + " to be recommended, but it was not");
  }

  public static boolean someBoostIs(Predicate<Boost> predicate) {
    return getBoosts().stream().anyMatch(predicate);
  }

  public static boolean commandStartsWith(Boost boost, String prefix) {
    return boost.getCmd().startsWith(prefix);
  }
}
