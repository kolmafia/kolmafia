package internal.helpers;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.sourceforge.kolmafia.KoLConstants.filterType;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.maximizer.Boost;
import net.sourceforge.kolmafia.maximizer.EquipScope;
import net.sourceforge.kolmafia.maximizer.PriceLevel;
import net.sourceforge.kolmafia.modifiers.Modifier;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.swingui.MaximizerFrame;

public class Maximizer {

  public static boolean maximize(String maximizerString) {
    return net.sourceforge.kolmafia.maximizer.Maximizer.maximize(
        maximizerString, 0, PriceLevel.DONT_CHECK, true);
  }

  public static void maximizeCreatable(String maximizerString) {
    MaximizerFrame.expressionSelect.setSelectedItem(maximizerString);
    net.sourceforge.kolmafia.maximizer.Maximizer.maximize(
        EquipScope.SPECULATE_CREATABLE,
        0,
        PriceLevel.DONT_CHECK,
        false,
        EnumSet.allOf(filterType.class));
  }

  public static void maximizeAny(String maximizerString) {
    MaximizerFrame.expressionSelect.setSelectedItem(maximizerString);
    net.sourceforge.kolmafia.maximizer.Maximizer.maximize(
        EquipScope.SPECULATE_ANY, 0, PriceLevel.DONT_CHECK, false, EnumSet.allOf(filterType.class));
  }

  public static double modFor(Modifier modifier) {
    return ModifierDatabase.getNumericModifier(ModifierType.GENERATED, "_spec", modifier);
  }

  public static List<Boost> getBoosts() {
    return new ArrayList<>(net.sourceforge.kolmafia.maximizer.Maximizer.boosts);
  }
}
