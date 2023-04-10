package net.sourceforge.kolmafia.textui.command;

import java.util.List;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.request.MonkeyPawRequest;

public class MonkeyPawCommand extends AbstractCommand {
  public MonkeyPawCommand() {
    this.usage = " effect [effectname] | item [itemname] | wish [wish]";
  }

  private static final Pattern DISALLOWED_CHARACTER = Pattern.compile("[^a-z0-9A-Z ]");

  private static final Pattern RESPONSE = Pattern.compile("<span class='guts'>([^<]+)?</span>");

  @Override
  public void run(final String cmd, String parameters) {
    String wish = null;
    if (parameters.startsWith("wish ")) {
      wish = parameters.substring(5);
    } else if (parameters.startsWith("effect ")) {
      parameters = parameters.substring(7);
      List<String> effectNames = EffectDatabase.getMatchingNames(parameters);
      if (effectNames.size() != 1) {
        KoLmafia.updateDisplay(MafiaState.ERROR, parameters + " does not match exactly one effect");
        return;
      }
      String name = effectNames.get(0);
      wish = getValidEffectSubstring(name);
      if (wish == null) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "cannot find unique valid substring to wish for " + name);
        return;
      }
    } else if (parameters.startsWith("item ")) {
      parameters = parameters.substring(5);
      List<String> itemNames = ItemDatabase.getMatchingNames(parameters);
      if (itemNames.size() != 1) {
        KoLmafia.updateDisplay(MafiaState.ERROR, parameters + " does not match exactly one item");
        return;
      }
      String name = itemNames.get(0);
      wish = getValidItemSubstring(name);
      if (wish == null) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "cannot find unique valid substring to wish for " + name);
        return;
      }
    } else {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Usage: monkeypaw" + this.usage);
    }

    if (wish != null) {
      var req = new MonkeyPawRequest(wish);
      RequestThread.postRequest(req);
      if (req.responseText != null) {
        var matcher = RESPONSE.matcher(req.responseText);
        if (matcher.find()) {
          KoLmafia.updateDisplay(matcher.group(1));
        }
      }
    }
  }

  public static String getValidEffectSubstring(String name) {
    String[] split = DISALLOWED_CHARACTER.split(name);
    if (split.length == 1) return name;
    for (var entry : split) {
      var possibles = EffectDatabase.getMatchingNames(entry);
      if (possibles.size() == 1) {
        return entry;
      }
    }

    return null;
  }

  public static String getValidItemSubstring(String name) {
    String[] split = DISALLOWED_CHARACTER.split(name);
    if (split.length == 1) return name;
    for (var entry : split) {
      var possibles = ItemDatabase.getMatchingNames(entry);
      if (possibles.size() == 1) {
        return entry;
      }
    }

    return null;
  }
}
