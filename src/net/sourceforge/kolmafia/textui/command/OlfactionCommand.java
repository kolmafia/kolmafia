package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.preferences.Preferences;

public class OlfactionCommand extends AbstractCommand {
  public OlfactionCommand() {
    this.usage =
        " ( none | monster <name> | [item] <list> | goals ) [abort] - tag next monster [that drops all items in list, or your goals].";
  }

  @Override
  public void run(final String cmd, String parameters) {
    String pref = cmd.equals("putty") ? "autoPutty" : "autoOlfact";
    parameters = parameters.toLowerCase();
    if (parameters.equals("none")) {
      Preferences.setString(pref, "");
    } else if (!parameters.equals("")) {
      boolean isAbort = false, isItem = false, isMonster = false;
      boolean isGoals = false;
      if (parameters.endsWith(" abort")) {
        isAbort = true;
        parameters = parameters.substring(0, parameters.length() - 6).trim();
      }
      if (parameters.startsWith("item ")) {
        parameters = parameters.substring(5).trim();
      } else if (parameters.startsWith("monster ")) {
        isMonster = true;
        parameters = parameters.substring(8).trim();
      } else if (parameters.equals("goals")) {
        isGoals = true;
      }
      StringBuffer result = new StringBuffer();
      if (isGoals) {
        result.append("goals");
      }
      if (!isGoals && !isMonster) {
        AdventureResult[] items = ItemFinder.getMatchingItemList(parameters);
        if (items != null && items.length > 0) {
          result.append("item ");
          for (int i = 0; i < items.length; ++i) {
            if (i != 0) {
              result.append(", ");
            }
            result.append(items[i].getName());
          }
          isItem = true;
        }
      }
      if (!isGoals && !isItem && parameters.length() >= 1) {
        result.append("monster ");
        result.append(parameters);
        isMonster = true;
      }
      if (!isGoals && !isItem && !isMonster) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Unable to interpret your conditions!");
        return;
      }

      if (isAbort) {
        result.append(" abort");
      }
      Preferences.setString(pref, result.toString());
    }
    String option = Preferences.getString(pref);
    if (option.equals("")) {
      KoLmafia.updateDisplay(pref + " is disabled.");
    } else {
      KoLmafia.updateDisplay(
          pref
              + ": "
              + option
                  .replaceFirst("^goals", "first monster that can drop your remaining goals")
                  .replaceFirst(" abort$", ", and then abort adventuring"));
    }
  }
}
