package net.sourceforge.kolmafia.textui.command;

import java.util.Iterator;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.combat.CombatActionManager;
import net.sourceforge.kolmafia.preferences.Preferences;

public class ChangeCombatScriptCommand extends AbstractCommand {
  public ChangeCombatScriptCommand() {
    this.usage = " [<script>] - show [or select] Custom Combat Script in use.";
  }

  @Override
  public void run(final String command, String parameters) {
    update(parameters);
  }

  public static void update(String parameters) {
    if (parameters.length() > 0) {
      parameters = parameters.toLowerCase();

      while (parameters.endsWith(".ccs")) {
        parameters = parameters.substring(0, parameters.length() - 4);
      }

      boolean foundScript = false;
      Iterator<String> iterator = CombatActionManager.getAvailableLookups().iterator();

      while (iterator.hasNext() && !foundScript) {
        String script = iterator.next();

        if (script.equalsIgnoreCase(parameters)) {
          foundScript = true;
          CombatActionManager.loadStrategyLookup(script);
          KoLmafia.updateDisplay("CCS set to " + CombatActionManager.getStrategyLookupName());
        }
      }

      iterator = CombatActionManager.getAvailableLookups().iterator();

      while (iterator.hasNext() && !foundScript) {
        String script = iterator.next();

        if (script.toLowerCase().indexOf(parameters) != -1) {
          foundScript = true;
          CombatActionManager.loadStrategyLookup(script);
          KoLmafia.updateDisplay("CCS set to " + CombatActionManager.getStrategyLookupName());
        }
      }

      if (!foundScript) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "No matching CCS found!");
        return;
      }
    } else {
      KoLmafia.updateDisplay("CCS is " + CombatActionManager.getStrategyLookupName());
    }

    String battleAction = Preferences.getString("battleAction");

    if (!battleAction.startsWith("custom")) {
      KoLmafia.updateDisplay("(but battle action is currently set to " + battleAction + ")");
    }
  }
}
