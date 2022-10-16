package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.session.FamiliarManager;
import net.sourceforge.kolmafia.session.HeistManager;

public class HeistCommand extends AbstractCommand {
  public HeistCommand() {
    this.usage =
        " [ [<count>] <item> ] - display all heistable items, or heist some number of items";
  }

  @Override
  public void run(final String cmd, String parameter) {
    if (!KoLCharacter.canUseFamiliar(FamiliarPool.CAT_BURGLAR)) {
      KoLmafia.updateDisplay("You don't have a Cat Burglar");
      return;
    }

    FamiliarData current = KoLCharacter.getFamiliar();
    FamiliarManager.changeFamiliar(FamiliarPool.CAT_BURGLAR, false);

    parameter = parameter.trim();

    if (parameter.equals("")) {
      showAllItems();
    } else {
      heistItem(parameter);
    }
    FamiliarManager.changeFamiliar(current);
  }

  private void showAllItems() {
    StringBuilder output = new StringBuilder();

    var heistManager = heistManager();
    var heistData = heistManager.getHeistTargets();

    output.append("You have ").append(heistData.heists).append(" heists.\n\n");

    for (var heistable : heistData.heistables.entrySet()) {
      var key = heistable.getKey();
      output.append("From ").append(key.pronoun).append(" ").append(key.name).append(": <ul>");

      for (var item : heistable.getValue()) {
        output.append("<li>").append(item.name).append("</li>");
      }

      output.append("</ul>");
    }

    RequestLogger.printLine(output.toString());
  }

  private void heistItem(String parameter) {
    AdventureResult item = ItemFinder.getFirstMatchingItem(parameter);

    if (item == null) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "What item is " + parameter + "?");
      return;
    }

    int id = item.getItemId();
    int count = item.getCount();
    var heistManager = heistManager();
    var success = heistManager.heist(count, id);

    if (!success) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "Could not find " + ItemDatabase.getItemName(id) + " to heist");
      return;
    }

    KoLmafia.updateDisplay("Heisted " + (count > 1 ? count + " " : "") + item.getPluralName());
    KoLCharacter.updateStatus();
  }

  protected HeistManager heistManager() {
    return new HeistManager();
  }
}
