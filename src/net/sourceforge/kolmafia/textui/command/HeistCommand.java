package net.sourceforge.kolmafia.textui.command;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.request.GenericRequest;

public class HeistCommand extends AbstractCommand {
  public HeistCommand() {
    this.usage = " [ITEM] - display all heistable items, or heist a specific item";
  }

  private static final Pattern HEIST_COUNT = Pattern.compile("(\\d+) more heists available");
  private static final Pattern MONSTER = Pattern.compile("From (?<pronoun>[^ ]*) (?<monster>.*?):<br />(?<items>(<input [^/]+ />)+)");
  private static final Pattern ITEM = Pattern.compile("<input type=\"submit\" name=\"st:(?<monsterId>\\d+):(?<itemId>\\d+)\" value=\"(?<itemName>[^\"]+)\" class=\"button\" />");

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
      int heists = 0;
      StringBuilder output = new StringBuilder();

      var heistResponse = heistRequest();

      Matcher countMatcher = HeistCommand.HEIST_COUNT.matcher(heistResponse);
      if (countMatcher.find()) {
        heists = Integer.parseInt(countMatcher.group(1));
      }

      output.append("You have ");
      output.append(heists);
      output.append(" heists.\n\n");

      Matcher monsterMatcher = HeistCommand.MONSTER.matcher(heistResponse);
      while (monsterMatcher.find()) {
        output.append("From ");
        output.append(monsterMatcher.group("pronoun"));
        output.append(" ");
        output.append(monsterMatcher.group("monster"));
        output.append(": <ul>");

        Matcher itemMatcher = ITEM.matcher(monsterMatcher.group("items"));
        while (itemMatcher.find()) {
          output.append("<li>");
          output.append(itemMatcher.group("itemName"));
          output.append("</li>");
        }

        output.append("</ul>");
      }

      RequestLogger.printLine(output.toString());

      return;
    }

    int id = ItemDatabase.getItemId(parameter);
    if (id == -1) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "What item is " + parameter + "?");
      return;
    }

    String itemId = String.valueOf(id);

    var heistResponse = heistRequest();
    Matcher itemMatcher = ITEM.matcher(heistResponse);
    boolean found = false;
    while (itemMatcher.find()) {
      if (!itemId.equals(itemMatcher.group("itemId"))) continue;

      found = true;
      break;
    }

    if (!found) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "Could not find " + ItemDatabase.getItemName(id) + " to heist");
      return;
    }

    String monsterId = itemMatcher.group("monsterId");
    String itemName = itemMatcher.group("itemName");

    GenericRequest request = new GenericRequest("choice.php");
    request.addFormField("whichchoice", "1320");
    request.addFormField("option", "1");
    request.addFormField("st:" + itemId + ":" + monsterId, itemName);
    request.addFormField("pwd", GenericRequest.passwordHash);
    RequestThread.postRequest(request);

    KoLmafia.updateDisplay("Heisted " + itemName);
    KoLCharacter.updateStatus();
  }

  protected String heistRequest() {
    var heistRequest = new GenericRequest("main.php?heist=1");
    RequestThread.postRequest(heistRequest);
    return heistRequest.responseText;
  }
}
