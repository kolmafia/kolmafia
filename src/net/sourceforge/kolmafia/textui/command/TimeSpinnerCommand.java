package net.sourceforge.kolmafia.textui.command;

import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.AdventureQueueDatabase;
import net.sourceforge.kolmafia.persistence.ConsumablesDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.ItemFinder.Match;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.EatItemRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.InventoryManager;

public class TimeSpinnerCommand extends AbstractCommand {
  public TimeSpinnerCommand() {
    this.usage =
        " list (food|monsters [<filter>]) | eat <foodname> | prank <target> msg=<message>] - Use the Time-Spinner";
  }

  @Override
  public void run(final String cmd, String parameters) {
    if (!InventoryManager.hasItem(ItemPool.TIME_SPINNER)) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You don't have a Time-Spinner.");
      return;
    }

    if (KoLCharacter.getAdventuresLeft() == 0) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR,
          "You need to have adventures available in order to use your Time-Spinner.");
      return;
    }

    if (parameters.startsWith("eat ")) {
      if (Preferences.getInteger("_timeSpinnerMinutesUsed") > 7) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "You don't have enough time to eat a past meal.");
        return;
      }
      parameters = parameters.substring(4);
      AdventureResult food = ItemFinder.getFirstMatchingItem(parameters, false, Match.FOOD);
      if (food == null) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "That isn't a valid food.");
        return;
      }

      String[] spinnerFoods = Preferences.getString("_timeSpinnerFoodAvailable").split(",");
      String foodIdString = String.valueOf(food.getItemId());
      String foodName = food.getName();
      boolean found = false;
      for (String temp : spinnerFoods) {
        if (temp.equals(foodIdString)) {
          found = true;
          break;
        }
      }
      if (!found) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "You haven't eaten this yet today.");
        return;
      }

      if (ConsumablesDatabase.getFullness(foodName)
          > (KoLCharacter.getFullnessLimit() - KoLCharacter.getFullness())) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "You are too full to eat that.");
        return;
      }

      if (!EatItemRequest.allowFoodConsumption(foodName, 1)) {
        return;
      }

      // If we have a MayoMinder set, and we are autostocking it, do so
      // Don't get Mayostat if it's a 1 fullness food, or it'd be wasted
      // Don't get Mayodiol if it'd cause you to overdrink
      String minderSetting = Preferences.getString("mayoMinderSetting");
      AdventureResult workshedItem = CampgroundRequest.getCurrentWorkshedItem();
      if (!minderSetting.equals("")
          && Preferences.getBoolean("autoFillMayoMinder")
          && !(minderSetting.equals("Mayostat") && ConsumablesDatabase.getFullness(foodName) == 1)
          && !(minderSetting.equals("Mayodiol")
              && KoLCharacter.getInebrietyLimit() == KoLCharacter.getInebriety())
          && workshedItem != null
          && workshedItem.getItemId() == ItemPool.MAYO_CLINIC) {
        if (Preferences.getString("mayoInMouth").equals("")
            && ConsumablesDatabase.getFullness(foodName) != 0) {
          InventoryManager.retrieveItem(minderSetting, 1);
        }
      }

      // inv_use.php?whichitem=9104&ajax=1&pwd
      GenericRequest request = new GenericRequest("inv_use.php");
      request.addFormField("whichitem", String.valueOf(ItemPool.TIME_SPINNER));
      request.addFormField("ajax", "1");
      request.addFormField("pwd", GenericRequest.passwordHash);
      RequestThread.postRequest(request);

      // Redirect to:
      // choice.php?forceoption=0
      // request = new GenericRequest( "choice.php" );
      // request.addFormField( "forceoption", "0" );
      // RequestThread.postRequest( request );

      // choice.php?pwd&whichchoice=1195&option=2
      request = new GenericRequest("choice.php");
      request.addFormField("whichchoice", "1195");
      request.addFormField("option", "2");
      request.addFormField("pwd", GenericRequest.passwordHash);
      RequestThread.postRequest(request);

      // choice.php?pwd&whichchoice=1197&option=1&foodid=2527
      request = new GenericRequest("choice.php");
      request.addFormField("whichchoice", "1197");
      request.addFormField("option", "1");
      request.addFormField("foodid", foodIdString);
      request.addFormField("pwd", GenericRequest.passwordHash);
      RequestThread.postRequest(request);

      // Redirect to:
      // inv_eat.php?pwd&whichitem=2527&ts=1
      // request = new GenericRequest( "inv_eat.php" );
      // request.addFormField( "whichitem", foodIdString );
      // request.addFormField( "ajax", "1" );
      // request.addFormField( "ts", "1" );
      // request.addFormField( "pwd", GenericRequest.passwordHash );
      // RequestThread.postRequest( request );
      return;
    }

    if (parameters.startsWith("prank ")) {
      if (Preferences.getInteger("_timeSpinnerMinutesUsed") == 10) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "You don't have enough time to prank anyone.");
        return;
      }

      String target = parameters.substring(6);
      String message = null;
      int index = target.indexOf("msg=");
      if (index != -1) {
        message = target.substring(index + 4).trim();
        target = target.substring(0, index).trim();
      }

      GenericRequest request = new GenericRequest("inv_use.php");
      request.addFormField("whichitem", String.valueOf(ItemPool.TIME_SPINNER));
      request.addFormField("ajax", "1");
      request.addFormField("pwd", GenericRequest.passwordHash);
      RequestThread.postRequest(request);

      request = new GenericRequest("choice.php");
      request.addFormField("whichchoice", "1195");
      request.addFormField("option", "5");
      request.addFormField("pwd", GenericRequest.passwordHash);
      RequestThread.postRequest(request);

      request = new GenericRequest("choice.php");
      request.addFormField("whichchoice", "1198");
      request.addFormField("option", "1");
      request.addFormField("pwd", GenericRequest.passwordHash);
      request.addFormField("pl", target);
      if (message != null) {
        request.addFormField("th", message);
      }
      RequestThread.postRequest(request);

      String responseText = request.responseText;
      if (responseText.contains("paradoxical time copy")) {
        return;
      }

      RequestLogger.printLine("Somebody was already waiting to prank " + target);
      request = new GenericRequest("choice.php");
      request.addFormField("whichchoice", "1198");
      request.addFormField("option", "2");
      request.addFormField("pwd", GenericRequest.passwordHash);
      RequestThread.postRequest(request);

      return;
    }

    if (parameters.trim().equals("list food")) {
      if (Preferences.getString("_timeSpinnerFoodAvailable").equals("")) {
        RequestLogger.printLine("No food available.");
        return;
      }
      String[] spinnerFoods = Preferences.getString("_timeSpinnerFoodAvailable").split(",");
      RequestLogger.printLine("Available food:");
      for (String food : spinnerFoods) {
        AdventureResult item = ItemPool.get(Integer.valueOf(food));
        RequestLogger.printLine(item.getName());
      }
      return;
    }

    if (parameters.startsWith("list monsters")) {
      String filter = parameters.substring(13).trim().toLowerCase();
      boolean filterExists = !filter.equals("");

      List<String> monsters = new ArrayList<String>();
      for (KoLAdventure adv : AdventureDatabase.getAsLockableListModel()) {
        if (!adv.getRequest().getURLString().startsWith("adventure.php")) {
          continue;
        }
        for (String monster : AdventureQueueDatabase.getZoneQueue(adv)) {
          if (!monsters.contains(monster)
              && (!filterExists || monster.toLowerCase().contains(filter))) {
            monsters.add(monster);
          }
        }
      }
      monsters.sort(String.CASE_INSENSITIVE_ORDER);
      if (monsters.isEmpty()) {
        RequestLogger.printLine("No monsters are available.");
        return;
      }

      RequestLogger.printLine("Available monsters:");
      for (String monster : monsters) {
        RequestLogger.printLine(monster);
      }
      return;
    }
  }
}
