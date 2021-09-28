package net.sourceforge.kolmafia.textui.command;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.request.PeeVPeeRequest;
import net.sourceforge.kolmafia.request.ProfileRequest;
import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.session.PvpManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class PvpAttackCommand extends AbstractCommand {
  public PvpAttackCommand() {
    this.usage =
        " <target> [, <target>...] <stance=> - PvP for items or fame using the selected stance";
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

    String[] params = parameters.split("stance=");

    if (params.length < 2) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You must specify stance=STANCE");
      return;
    }

    String stanceString = params[1].trim();
    int stance = 0;

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

    String[] names = params[0].trim().split("\\s*,\\s*");
    ProfileRequest[] targets = new ProfileRequest[names.length];

    for (int i = 0; i < names.length; ++i) {
      String playerId = ContactManager.getPlayerId(names[i]);
      if (!playerId.equals(names[i])) {
        continue;
      }

      String text = KoLmafia.whoisPlayer(playerId);
      Matcher idMatcher = Pattern.compile("\\(#(\\d+)\\)").matcher(text);

      if (idMatcher.find()) {
        ContactManager.registerPlayerId(names[i], idMatcher.group(1));
      } else {
        names[i] = null;
      }
    }

    for (int i = 0; i < names.length; ++i) {
      if (names[i] == null) {
        continue;
      }

      KoLmafia.updateDisplay("Retrieving player data for " + names[i] + "...");
      targets[i] = new ProfileRequest(names[i]);
      targets[i].run();
    }

    String mission = KoLCharacter.canInteract() ? "lootwhatever" : "fame";
    PeeVPeeRequest request = new PeeVPeeRequest("", 0, mission);
    PvpManager.executePvpRequest(targets, request, stance);
  }
}
