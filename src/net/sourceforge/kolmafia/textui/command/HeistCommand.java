package net.sourceforge.kolmafia.textui.command;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.session.FamiliarManager;
import net.sourceforge.kolmafia.session.HeistManager;

public class HeistCommand extends AbstractCommand {
  public HeistCommand() {
    this.usage = " [[N] ITEM] - display all heistable items, or heist some number of items";
  }

  private static final Pattern ITEM_WITH_COUNT = Pattern.compile("(\\d+)\\s+(.*)");

  @Override
  public void run(final String cmd, String parameter) {
    if (!KoLCharacter.hasFamiliar(FamiliarPool.CAT_BURGLAR)) {
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
  }

  private void heistItem(String parameter) {
    int id = ItemDatabase.getItemId(parameter);
    int count = 1;
    if (id == -1) {
      // possibly number first
      Matcher itemMatcher = ITEM_WITH_COUNT.matcher(parameter);
      if (itemMatcher.matches()) {
        count = Integer.parseInt(itemMatcher.group(1));
        String possibleItemName = itemMatcher.group(2);
        id = ItemDatabase.getItemId(possibleItemName);
      }
      if (id == -1) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "What item is " + parameter + "?");
        return;
      }
    }

    var heistManager = heistManager();
    var success = heistManager.heist(count, id);

    if (!success) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "Could not find " + ItemDatabase.getItemName(id) + " to heist");
      return;
    }

    KoLmafia.updateDisplay(
        "Heisted " + (count > 1 ? count + " " : "") + ItemDatabase.getItemName(id));
    KoLCharacter.updateStatus();
  }

  protected HeistManager heistManager() {
    return new HeistManager();
  }
}
