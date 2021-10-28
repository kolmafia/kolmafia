package net.sourceforge.kolmafia.textui.command;

import java.util.List;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.moods.MoodManager;
import net.sourceforge.kolmafia.persistence.EffectDatabase;

public class ExtendEffectCommand extends AbstractCommand {
  public ExtendEffectCommand() {
    this.usage = "[?] <effect> [, <effect>]... - extend duration of effects.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    if (parameters.indexOf(",") != -1) {
      String[] effects = parameters.split("\\s*,\\s*");
      for (int i = 0; i < effects.length; ++i) {
        KoLmafiaCLI.DEFAULT_SHELL.executeCommand(cmd, effects[i]);
      }

      return;
    }

    int effectId = EffectDatabase.getEffectId(parameters);
    if (effectId != -1) {
      String effect = EffectDatabase.getEffectName(effectId);
      String action = MoodManager.getDefaultAction("lose_effect", effect);
      if (action.equals("")) {
        action = EffectDatabase.getActionNote(effectId);
        if (action != null) {
          KoLmafia.updateDisplay(MafiaState.ERROR, "No direct source for: " + effect);
          RequestLogger.printLine("It may be obtainable via " + action + ".");
        } else {
          KoLmafia.updateDisplay(MafiaState.ERROR, "No booster known: " + effect);
        }
        return;
      }

      if (KoLmafiaCLI.isExecutingCheckOnlyCommand) {
        KoLmafia.updateDisplay(effect + " &lt;= " + action);
      } else {
        KoLmafiaCLI.DEFAULT_SHELL.executeLine(action);
      }
      return;
    }

    List<String> names = EffectDatabase.getMatchingNames(parameters);
    if (names.isEmpty()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Unknown effect: " + parameters);
      return;
    }

    KoLmafia.updateDisplay(MafiaState.ERROR, "Ambiguous effect name: " + parameters);
    RequestLogger.printList(names);
  }
}
