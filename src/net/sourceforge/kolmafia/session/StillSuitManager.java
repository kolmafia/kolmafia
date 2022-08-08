package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.DebugDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;

public class StillSuitManager {
  private StillSuitManager() {}

  public static void clearSweat() {
    Preferences.setInteger("familiarSweat", 0);
  }

  public static void handleSweat(final String responseText) {
    handleSweat(responseText, false);
  }

  public static void handleSweat(final String responseText, final boolean checkEffect) {
    if (!responseText.contains("tiny_stillsuit.gif")) {
      return;
    }

    var familiar =
        KoLCharacter.familiars.stream()
            .filter(f -> f.getItem() != null && f.getItem().getItemId() == ItemPool.STILLSUIT)
            .findFirst()
            .orElse(null);

    int drams = 0;
    if (familiar == KoLCharacter.getFamiliar()) {
      drams = 2;
    } else if (familiar != null) {
      drams = 1;
    }

    Preferences.increment("familiarSweat", drams);

    if (checkEffect) visit();
  }

  private static void visit() {
    RequestThread.postRequest(new GenericRequest("inventory.php?action=distill&pwd"));
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

    // Not sure what to do with modifiers just yet
  }
}
