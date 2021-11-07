package net.sourceforge.kolmafia.textui.command;

import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FamiliarRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class EnthroneCommand extends AbstractCommand {
  private static final AdventureResult HATSEAT = ItemPool.get(ItemPool.HATSEAT, 1);

  public EnthroneCommand() {
    this.usage = "[?] <species> - place a familiar in the Crown of Thrones.";
  }

  @Override
  public void run(final String cmd, String parameters) {
    if (parameters.length() == 0) {
      ShowDataCommand.show("familiars");
      return;
    } else if (parameters.equalsIgnoreCase("none") || parameters.equalsIgnoreCase("unequip")) {
      if (KoLCharacter.getEnthroned().equals(FamiliarData.NO_FAMILIAR)) {
        return;
      }

      RequestThread.postRequest(FamiliarRequest.enthroneRequest(FamiliarData.NO_FAMILIAR));
      return;
    } else if (parameters.indexOf("(no change)") != -1) {
      return;
    }

    List<FamiliarData> familiarList = KoLCharacter.getFamiliarList();

    String[] familiars = new String[familiarList.size()];
    for (int i = 0; i < familiarList.size(); ++i) {
      FamiliarData familiar = familiarList.get(i);
      familiars[i] = StringUtilities.getCanonicalName(familiar.getRace());
    }

    List<String> matchList = StringUtilities.getMatchingNames(familiars, parameters);

    if (matchList.size() > 1) {
      RequestLogger.printList(matchList);
      RequestLogger.printLine();

      KoLmafia.updateDisplay(MafiaState.ERROR, "[" + parameters + "] has too many matches.");
    } else if (matchList.size() == 1) {
      String race = matchList.get(0);
      FamiliarData change = null;
      for (int i = 0; i < familiars.length; ++i) {
        if (race.equals(familiars[i])) {
          change = familiarList.get(i);
          break;
        }
      }

      if (change == null) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "You can't enthrone an unknown familiar!");
        return;
      }

      if (KoLmafiaCLI.isExecutingCheckOnlyCommand) {
        RequestLogger.printLine(change.toString());
        return;
      }

      if (!change.canCarry()) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "You can't enthrone a " + change.getRace() + "!");
        return;
      }

      if (KoLCharacter.getFamiliar().equals(change)) {
        RequestThread.postRequest(new FamiliarRequest(FamiliarData.NO_FAMILIAR));
      } else if (KoLCharacter.getBjorned().equals(change)) {
        RequestThread.postRequest(FamiliarRequest.bjornifyRequest(FamiliarData.NO_FAMILIAR));
      }

      RequestThread.postRequest(new EquipmentRequest(HATSEAT, EquipmentManager.HAT));

      if (KoLmafia.permitsContinue() && !KoLCharacter.getEnthroned().equals(change)) {
        RequestThread.postRequest(FamiliarRequest.enthroneRequest(change));
      }
    } else {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "You don't have a " + parameters + " for a familiar.");
    }
  }
}
