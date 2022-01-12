package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.session.HeistManager;

public class HeistCommand extends AbstractCommand {
  public HeistCommand() {
    this.usage = " [ITEM] - display all heistable items, or heist a specific item";
  }

  @Override
  public void run(final String cmd, String parameter) {
    FamiliarData current = KoLCharacter.getFamiliar();
    if (current == null || current.getId() != FamiliarPool.CAT_BURGLAR) {
      KoLmafia.updateDisplay("You need to take your Cat Burglar with you");
      return;
    }

    parameter = parameter.trim();

    if (parameter.equals("")) {
      // show all items
      StringBuilder output = new StringBuilder();

      var heistManager = heistManager();
      var heistData = heistManager.getHeistable();

      output.append("You have ");
      output.append(heistData.heists);
      output.append(" heists.\n\n");

      for (var heistable : heistData.heistables.entrySet()) {
        var key = heistable.getKey();
        output.append("From ");
        output.append(key.pronoun);
        output.append(" ");
        output.append(key.name);
        output.append(": <ul>");

        for (var item : heistable.getValue()) {
          output.append("<li>");
          output.append(item.name);
          output.append("</li>");
        }

        output.append("</ul>");
      }

      RequestLogger.printLine(output.toString());

      return;
    }

    int id = ItemDatabase.getItemId(parameter);
    if (id == -1) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "What item is " + parameter + "?");
      return;
    }

    var heistManager = heistManager();
    var success = heistManager.heist(id);

    if (!success) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "Could not find " + ItemDatabase.getItemName(id) + " to heist");
      return;
    }

    KoLmafia.updateDisplay("Heisted " + ItemDatabase.getItemName(id));
    KoLCharacter.updateStatus();
  }

  protected HeistManager heistManager() {
    return new HeistManager();
  }
}
