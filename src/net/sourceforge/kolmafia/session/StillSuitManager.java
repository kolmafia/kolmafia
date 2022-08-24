package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
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
        KoLCharacter.familiars.stream()
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

    var modifiers = new Modifiers.ModifierList();
    DebugDatabase.parseStandardEnchantments(text, modifiers, new ArrayList<>(), EFFECTS_BLOCK);
    if (modifiers.size() > 0) {
      KoLmafia.updateDisplay("Your next distillate will give you: " + modifiers);
    }
  }

  private static void setSweat(final int drams) {
    Preferences.setInteger("familiarSweat", drams);
    ConsumablesDatabase.setDistillateData();
  }
}
