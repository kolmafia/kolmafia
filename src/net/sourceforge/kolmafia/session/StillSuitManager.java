package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.DebugDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;

public class StillSuitManager {
  private StillSuitManager() {}

  public static void clearSweat() {
    Preferences.setInteger("familiarSweat", 0);
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
  }

  private static final Pattern DRAMS = Pattern.compile("<b>(\\d+)</b> drams");
  private static final Pattern EFFECTS_BLOCK = Pattern.compile("<div.*?>(.*?)</div>");

  public static void parseChoice(final String text) {
    var dramMatcher = DRAMS.matcher(text);

    if (dramMatcher.find()) {
      var drams = Integer.parseInt(dramMatcher.group(1));
      Preferences.setInteger("familiarSweat", drams);
    }

    var modifiers = new Modifiers.ModifierList();
    DebugDatabase.parseStandardEnchantments(text, modifiers, new ArrayList<>(), EFFECTS_BLOCK);
    if (modifiers.size() > 0) {
      KoLmafia.updateDisplay("Your next distillate will give you: " + modifiers);
    }
  }
}
