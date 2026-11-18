package net.sourceforge.kolmafia.textui.command;

import java.util.Map;
import java.util.TreeMap;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FamiliarRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class EnthroneCommand extends AbstractCommand {
  private static final AdventureResult HATSEAT = ItemPool.get(ItemPool.HATSEAT, 1);

  public EnthroneCommand() {
    this.usage = "[?] <species> - place a familiar in the Crown of Thrones.";
  }

  @Override
  public void run(final String cmd, String parameters) {
    if (parameters.isEmpty()) {
      ShowDataCommand.show("familiars");
      return;
    }

    if (parameters.equalsIgnoreCase("none") || parameters.equalsIgnoreCase("unequip")) {
      if (KoLCharacter.getEnthroned().equals(FamiliarData.NO_FAMILIAR)) {
        return;
      }

      RequestThread.postRequest(FamiliarRequest.enthroneRequest(FamiliarData.NO_FAMILIAR));
      return;
    }

    if (parameters.contains("(no change)")) {
      return;
    }

    Map<String, FamiliarData> familiars = new TreeMap<>();
    for (var familiar : KoLCharacter.usableFamiliars()) {
      familiars.put(StringUtilities.getCanonicalName(familiar.getRace()), familiar);
    }

    var matchList = StringUtilities.getMatchingNames(familiars.keySet(), parameters);

    if (matchList.size() > 1) {
      RequestLogger.printList(matchList);
      RequestLogger.printLine();

      KoLmafia.updateDisplay(MafiaState.ERROR, "[" + parameters + "] has too many matches.");
    } else if (matchList.size() == 1) {
      var change = familiars.get(matchList.getFirst());

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

      if (!KoLCharacter.hasEquipped(HATSEAT, Slot.HAT)) {
        RequestThread.postRequest(new EquipmentRequest(HATSEAT));
      }

      if (KoLmafia.permitsContinue() && !KoLCharacter.getEnthroned().equals(change)) {
        RequestThread.postRequest(FamiliarRequest.enthroneRequest(change));
      }
    } else {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "You don't have a " + parameters + " for a familiar.");
    }
  }
}
