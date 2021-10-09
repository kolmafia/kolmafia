package net.sourceforge.kolmafia.textui.command;

import java.util.List;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FamiliarRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class FamiliarCommand extends AbstractCommand {
  public FamiliarCommand() {
    this.usage =
        "[?] [list <filter>] | lock | unlock | <species> | none - list or change familiar types";
  }

  @Override
  public void run(final String cmd, String parameters) {
    if (KoLCharacter.inPokefam()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Familiars can't be used in Pokefam.");
      return;
    }

    if (KoLCharacter.inQuantum()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "In Quantum Terrarium, familiar chooses you.");
    }

    if (parameters.startsWith("list")) {
      ShowDataCommand.show("familiars " + parameters.substring(4).trim());
      return;
    }

    if (parameters.length() == 0) {
      ShowDataCommand.show("familiars");
      return;
    }

    if (parameters.equalsIgnoreCase("none") || parameters.equalsIgnoreCase("unequip")) {
      if (KoLCharacter.getFamiliar() == null
          || KoLCharacter.getFamiliar().equals(FamiliarData.NO_FAMILIAR)) {
        return;
      }

      RequestThread.postRequest(new FamiliarRequest(FamiliarData.NO_FAMILIAR));
      return;
    }

    if (parameters.equalsIgnoreCase("lock")) {
      if (EquipmentManager.familiarItemLocked()) {
        KoLmafia.updateDisplay("Familiar item already locked.");
        return;
      }
      if (!EquipmentManager.familiarItemLockable()) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Familiar item can't be locked.");
        return;
      }
      RequestThread.postRequest(new FamiliarRequest(true));
      return;
    }

    if (parameters.equalsIgnoreCase("unlock")) {
      if (!EquipmentManager.familiarItemLocked()) {
        KoLmafia.updateDisplay("Familiar item already unlocked.");
        return;
      }
      RequestThread.postRequest(new FamiliarRequest(true));
      return;
    }

    if (parameters.indexOf("(no change)") != -1) {
      return;
    }

    boolean unequip = false;
    if (parameters.startsWith("naked ")) {
      unequip = true;
      parameters = parameters.substring(6).trim();
    }

    List familiarList = KoLCharacter.getFamiliarList();

    String[] familiars = new String[familiarList.size()];
    for (int i = 0; i < familiarList.size(); ++i) {
      FamiliarData familiar = (FamiliarData) familiarList.get(i);
      familiars[i] = StringUtilities.getCanonicalName(familiar.getRace());
    }

    List matchList = StringUtilities.getMatchingNames(familiars, parameters);

    if (matchList.size() == 0) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "You don't have a " + parameters + " for a familiar.");
      return;
    }

    if (matchList.size() > 1) {
      RequestLogger.printList(matchList);
      RequestLogger.printLine();

      KoLmafia.updateDisplay(MafiaState.ERROR, "[" + parameters + "] has too many matches.");
      return;
    }

    String race = (String) matchList.get(0);
    FamiliarData change = null;
    for (int i = 0; i < familiars.length; ++i) {
      if (race.equals(familiars[i])) {
        change = (FamiliarData) familiarList.get(i);
        break;
      }
    }

    if (!change.canEquip()) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "You can't equip a " + race + " with your current restrictions.");
      return;
    }

    if (KoLmafiaCLI.isExecutingCheckOnlyCommand) {
      RequestLogger.printLine(change.toString());
      return;
    }

    FamiliarData current = KoLCharacter.getFamiliar();
    if (current != null && !current.equals(change)) {
      RequestThread.postRequest(new FamiliarRequest(change));

      // If we want the new familiar to be naked, unequip the familiar item
      if (KoLmafia.permitsContinue() && unequip) {
        RequestThread.postRequest(
            new EquipmentRequest(EquipmentRequest.UNEQUIP, EquipmentManager.FAMILIAR));
      }
    }
  }
}
