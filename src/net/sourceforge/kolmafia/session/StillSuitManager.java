package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.modifiers.ModifierList;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConsumablesDatabase;
import net.sourceforge.kolmafia.persistence.DebugDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;

public class StillSuitManager {
  private StillSuitManager() {}

  public static void clearSweat() {
    setSweat(0);
  }

  public static void handleSweat(final String responseText) {
    if (!responseText.contains("stillsuit.gif")) {
      return;
    }

    var familiar =
        KoLCharacter.ownedFamiliars().stream()
            .filter(f -> f.getItem() != null && f.getItem().getItemId() == ItemPool.STILLSUIT)
            .findFirst()
            .orElse(null);

    int drams = 0;
    if (familiar == KoLCharacter.getFamiliar()) {
      drams = 3;
    } else if (familiar != null) {
      drams = 1;
    }

    Preferences.increment("familiarSweat", drams);
    ConsumablesDatabase.setDistillateData();
  }

  private static final Pattern DRAMS =
      Pattern.compile("<b>(\\d+)</b> drams|Looks like there are (\\d+) drams");
  private static final Pattern EFFECTS_BLOCK = Pattern.compile("<div.*?>(.*?)</div>");

  public static void parseChoice(final String text) {
    var dramMatcher = DRAMS.matcher(text);

    if (dramMatcher.find()) {
      Stream.of(1, 2)
          .map(dramMatcher::group)
          .filter(Objects::nonNull)
          .map(Integer::parseInt)
          .findFirst()
          .ifPresent(StillSuitManager::setSweat);
    }

    var modifiers = new ModifierList();
    DebugDatabase.parseStandardEnchantments(text, modifiers, new ArrayList<>(), EFFECTS_BLOCK);
    Preferences.setString("nextDistillateMods", (modifiers.size() > 0) ? modifiers.toString() : "");
  }

  public static void handleDrink(final String responseText) {
    if (!responseText.contains("You put your lips to the nozzle")) return;

    clearSweat();
    DebugDatabase.readEffectDescriptionText(EffectPool.BUZZED_ON_DISTILLATE);
    Preferences.setString("nextDistillateMods", "");
  }

  private static void setSweat(final int drams) {
    Preferences.setInteger("familiarSweat", drams);
    ConsumablesDatabase.setDistillateData();
  }
}
