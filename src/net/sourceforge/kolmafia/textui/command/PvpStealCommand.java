package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.session.PvpManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class PvpStealCommand extends AbstractCommand {
  public PvpStealCommand() {
    this.usage =
        " [attacks] ( flowers | loot | fame) <stance> - commit random acts of PvP using the specified stance.";
  }

  @Override
  public void run(final String cmd, String parameters) {
    if (!PvpManager.checkStances()) {
      KoLmafia.updateDisplay("Cannot determine valid stances");
      return;
    }

    parameters = parameters.trim();

    if (parameters.equals("")) {
      for (Integer option : PvpManager.optionToStance.keySet()) {
        RequestLogger.printLine(option + ": " + PvpManager.optionToStance.get(option));
      }
      return;
    }

    int attacks = 0;
    String mission = null;
    int stance = 0;

    int spaceIndex = parameters.indexOf(" ");

    if (spaceIndex == -1) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Must specify both mission and stance");
      return;
    }

    String param = parameters.substring(0, spaceIndex);
    parameters = parameters.substring(spaceIndex).trim();

    if (StringUtilities.isNumeric(param)) {
      attacks = StringUtilities.parseInt(param);

      spaceIndex = parameters.indexOf(" ");
      if (spaceIndex == -1) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Must specify both mission and stance");
        return;
      }

      param = parameters.substring(0, spaceIndex);
      parameters = parameters.substring(spaceIndex).trim();
    }

    String missionType = param;

    if (missionType.equals("flowers") || missionType.equals("fame")) {
      mission = missionType;
    } else if (missionType.startsWith("loot")) {
      if (!KoLCharacter.canInteract()) {
        KoLmafia.updateDisplay(MafiaState.ABORT, "You cannot attack for loot now.");
        return;
      }
      mission = "lootwhatever";
    } else {
      KoLmafia.updateDisplay(MafiaState.ERROR, "What do you want to steal?");
      return;
    }

    String stanceString = parameters;

    if (StringUtilities.isNumeric(stanceString)) {
      stance = StringUtilities.parseInt(stanceString);
      stanceString = PvpManager.findStance(stance);
      if (stanceString == null) {
        KoLmafia.updateDisplay(MafiaState.ERROR, stance + " is not a valid stance");
        return;
      }
    } else {
      // Find stance using fuzzy matching
      stance = PvpManager.findStance(stanceString);
      if (stance < 0) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR,
            "\"" + stanceString + "\" does not uniquely match a currently known stance");
        return;
      }
      stanceString = PvpManager.findStance(stance);
    }

    KoLmafia.updateDisplay(
        "Use "
            + (attacks == 0 ? "all remaining" : String.valueOf(attacks))
            + " PVP attacks to steal "
            + missionType
            + " via "
            + stanceString);

    PvpManager.executePvpRequest(attacks, mission, stance);
  }
}
