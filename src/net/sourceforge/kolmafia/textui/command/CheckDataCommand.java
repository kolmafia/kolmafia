package net.sourceforge.kolmafia.textui.command;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.CandyDatabase;
import net.sourceforge.kolmafia.persistence.DebugDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.ItemFinder.Match;
import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.ProfileRequest;
import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CheckDataCommand extends AbstractCommand {
  {
    this.usage = null;
  }

  @Override
  public void run(final String command, final String parameters) {
    if (command.equals("newdata")) {
      // EquipmentRequest registers new items with
      // ItemDatabase when it looks at the closet or at
      // inventory.
      RequestThread.postRequest(new EquipmentRequest(EquipmentRequest.REFRESH));

      // The api registers new status effects
      ApiRequest.updateStatus();

      // Write override files, if necessary
      KoLmafia.saveDataOverride();

      RequestLogger.printLine("Data tables updated.");
      return;
    }

    if (command.equals("checkcandy")) {
      String candy = parameters.trim();
      if (candy.equals("")) {
        Set<Integer> candies = CandyDatabase.candyForTier(0);
        for (Integer itemId : candies) {
          RequestLogger.printLine("***Unspaded candy: " + ItemDatabase.getDataName(itemId));
        }
      } else {
        Match filter = Match.CANDY;
        AdventureResult[] itemList = ItemFinder.getMatchingItemList(parameters, true, null, filter);
        for (AdventureResult item : itemList) {
          String type = CandyDatabase.getCandyType(item.getItemId());
          RequestLogger.printLine(item.getName() + ": " + type);
        }
      }
      return;
    }

    if (command.equals("checkconcoctions")) {
      DebugDatabase.checkConcoctions();
      RequestLogger.printLine("Concoctions checked.");
      return;
    }

    if (command.equals("checkconsumables")) {
      DebugDatabase.checkConsumables();
      RequestLogger.printLine("Consumables checked.");
      return;
    }

    if (command.equals("checkconsumption")) {
      DebugDatabase.checkConsumptionData();
      RequestLogger.printLine("Consumption data checked.");
      return;
    }

    if (command.equals("checkeffects")) {
      int effectId = StringUtilities.parseInt(parameters);
      DebugDatabase.checkEffects(effectId);
      RequestLogger.printLine("Internal status effect data checked.");
      return;
    }

    if (command.equals("checkfamiliars")) {
      boolean showVariable = parameters.equals("true");
      RequestLogger.printLine("Checking familiar powers from terrarium.");
      DebugDatabase.checkFamiliarsInTerrarium(showVariable);
      RequestLogger.printLine("Checking familiar images.");
      DebugDatabase.checkFamiliarImages();
      RequestLogger.printLine("Familiars checked.");
      return;
    }

    if (command.equals("checkitems")) {
      int itemId = StringUtilities.parseInt(parameters);
      DebugDatabase.checkItems(itemId);
      RequestLogger.printLine("Internal item data checked.");
      return;
    }

    if (command.equals("checkmanuel")) {
      DebugDatabase.checkManuel();
      RequestLogger.printLine("Monster Manuel checked.");
      return;
    }

    if (command.equals("checkmeat")) {
      DebugDatabase.checkMeat();
      RequestLogger.printLine("Monster Meat checked.");
      return;
    }

    if (command.equals("checkmodifiers")) {
      Modifiers.checkModifiers();
      RequestLogger.printLine("Modifiers checked.");
      return;
    }

    if (command.equals("checkoutfits")) {
      DebugDatabase.checkOutfits();
      RequestLogger.printLine("Internal outfit data checked.");
      return;
    }

    if (command.equals("checkplurals")) {
      DebugDatabase.checkPlurals(parameters);
      RequestLogger.printLine("Plurals checked.");
      return;
    }

    if (command.equals("checkpotions")) {
      DebugDatabase.checkPotions();
      RequestLogger.printLine("Potions checked.");
      return;
    }

    if (command.equals("checkpowers")) {
      DebugDatabase.checkPowers(parameters.trim());
      RequestLogger.printLine("Equipment power checked.");
      return;
    }

    if (command.equals("checkprofile")) {
      String playerId = ContactManager.getPlayerId(parameters);
      if (playerId.equals(parameters)) {
        String text = KoLmafia.whoisPlayer(playerId);
        Matcher idMatcher = Pattern.compile("\\(#(\\d+)\\)").matcher(text);

        if (idMatcher.find()) {
          ContactManager.registerPlayerId(parameters, idMatcher.group(1));
        } else {
          RequestLogger.printLine("no such player");
          return;
        }
      }
      ProfileRequest prof = new ProfileRequest(parameters);
      prof.run();
      RequestLogger.printLine("name [" + prof.getPlayerName() + "]");
      RequestLogger.printLine("id [" + prof.getPlayerId() + "]");
      RequestLogger.printLine("level [" + prof.getPlayerLevel() + "]");
      RequestLogger.printLine("class [" + prof.getClassType() + "]");
      RequestLogger.printLine("clan [" + prof.getClanName() + "]");
      RequestLogger.printLine("restrict [" + prof.getRestriction() + "]");
      return;
    }

    if (command.equals("checkpulverization")) {
      DebugDatabase.checkPulverizationData();
      RequestLogger.printLine("Pulverization data checked.");
      return;
    }

    if (command.equals("checkshields")) {
      DebugDatabase.checkShields();
      RequestLogger.printLine("Shield power checked.");
      return;
    }

    if (command.equals("checkskills")) {
      int itemId = StringUtilities.parseInt(parameters);
      DebugDatabase.checkSkills(itemId);
      RequestLogger.printLine("Internal skill data checked.");
      return;
    }

    if (command.equals("checkzapgroups")) {
      DebugDatabase.checkZapGroups();
      RequestLogger.printLine("Zap groups checked.");
    }
  }
}
