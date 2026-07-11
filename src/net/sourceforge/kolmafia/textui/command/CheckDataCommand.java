package net.sourceforge.kolmafia.textui.command;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.CandyDatabase;
import net.sourceforge.kolmafia.persistence.CandyDatabase.CandyType;
import net.sourceforge.kolmafia.persistence.DebugDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.ItemFinder.Match;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest.EquipmentRequestType;
import net.sourceforge.kolmafia.request.ProfileRequest;
import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CheckDataCommand extends AbstractCommand {
  {
    this.usage = null;
  }

  @Override
  public void run(final String command, final String parameters) {
    switch (command) {
      case "newdata" -> {
        // EquipmentRequest registers new items with
        // ItemDatabase when it looks at the closet or at
        // inventory.
        RequestThread.postRequest(new EquipmentRequest(EquipmentRequestType.REFRESH));

        // The api registers new status effects
        ApiRequest.updateStatus();

        // Write override files, if necessary
        KoLmafia.saveDataOverride();

        RequestLogger.printLine("Data tables updated.");
        return;
      }
      case "checkcandy" -> {
        String candy = parameters.trim();
        if (candy.isEmpty()) {
          Set<Integer> candies = CandyDatabase.candyForTier(0);
          for (Integer itemId : candies) {
            RequestLogger.printLine("***Unspaded candy: " + ItemDatabase.getDataName(itemId));
          }
        } else {
          Match filter = Match.CANDY;
          AdventureResult[] itemList =
              ItemFinder.getMatchingItemList(parameters, true, null, filter);
          for (AdventureResult item : itemList) {
            CandyType type = CandyDatabase.getCandyType(item.getItemId());
            RequestLogger.printLine(item.getName() + ": " + type);
          }
        }
        return;
      }
      case "checkconcoctions" -> {
        DebugDatabase.checkConcoctions();
        RequestLogger.printLine("Concoctions checked.");
        return;
      }
      case "checkconsumables" -> {
        DebugDatabase.checkConsumables();
        RequestLogger.printLine("Consumables checked.");
        return;
      }
      case "checkconsumption" -> {
        DebugDatabase.checkConsumptionData();
        RequestLogger.printLine("Consumption data checked.");
        return;
      }
      case "checkeffects" -> {
        int effectId = StringUtilities.parseInt(parameters);
        DebugDatabase.checkEffects(effectId);
        RequestLogger.printLine("Internal status effect data checked.");
        return;
      }
      case "checkfamiliars" -> {
        boolean showVariable = parameters.equals("true");
        RequestLogger.printLine("Checking familiar powers from terrarium.");
        DebugDatabase.checkFamiliarsInTerrarium(showVariable);
        RequestLogger.printLine("Checking familiar images.");
        DebugDatabase.checkFamiliars();
        RequestLogger.printLine("Familiars checked.");
        return;
      }
      case "checkitems" -> {
        int itemId = StringUtilities.parseInt(parameters);
        DebugDatabase.checkItems(itemId);
        RequestLogger.printLine("Internal item data checked.");
        return;
      }
      case "checkmanuel" -> {
        DebugDatabase.checkManuel();
        RequestLogger.printLine("Monster Manuel checked.");
        return;
      }
      case "checkmeat" -> {
        DebugDatabase.checkMeat();
        RequestLogger.printLine("Monster Meat checked.");
        return;
      }
      case "checkmodifiers" -> {
        ModifierDatabase.checkModifiers();
        RequestLogger.printLine("Modifiers checked.");
        return;
      }
      case "checkoutfits" -> {
        DebugDatabase.checkOutfits();
        RequestLogger.printLine("Internal outfit data checked.");
        return;
      }
      case "checkplurals" -> {
        DebugDatabase.checkPlurals(parameters);
        RequestLogger.printLine("Plurals checked.");
        return;
      }
      case "checkmuseumplurals" -> {
        DebugDatabase.checkMuseumPlurals();
        RequestLogger.printLine("Plurals checked.");
        return;
      }
      case "checkmuseumitems" -> {
        DebugDatabase.checkMuseumItems();
        RequestLogger.printLine("Items checked.");
        return;
      }
      case "checkpotions" -> {
        DebugDatabase.checkPotions();
        RequestLogger.printLine("Potions checked.");
        return;
      }
      case "checkpowers" -> {
        DebugDatabase.checkPowers(parameters.trim());
        RequestLogger.printLine("Equipment power checked.");
        return;
      }
      case "checkprofile" -> {
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
      case "checkpulverization" -> {
        DebugDatabase.checkPulverizationData();
        RequestLogger.printLine("Pulverization data checked.");
        return;
      }
      case "checkrepo" -> {
        DebugDatabase.checkLocalSVNRepository(KoLConstants.SVN_LOCATION);
        RequestLogger.printLine("Local SVN repos scanned for possible duplicates.");
        return;
      }
      case "checkshields" -> {
        DebugDatabase.checkShields();
        RequestLogger.printLine("Shield power checked.");
        return;
      }
      case "checkskills" -> {
        int itemId = StringUtilities.parseInt(parameters);
        DebugDatabase.checkSkills(itemId);
        RequestLogger.printLine("Internal skill data checked.");
        return;
      }
      case "checksvngit" -> {
        DebugDatabase.checkLocalSVNRepositoryForGitHub(KoLConstants.SVN_LOCATION);
        RequestLogger.printLine("Local SVN repos scanned for possible GitHub access via SVN.");
        return;
      }
      case "checkwikimonsterelementalattacks" -> {
        DebugDatabase.checkWikiMonsterElementalAttacks();
        RequestLogger.printLine("Wiki monster elemental attacks checked.");
        return;
      }
      case "checkwikimonsters" -> {
        DebugDatabase.checkWikiMonsters();
        RequestLogger.printLine("Wiki monsters checked.");
        return;
      }
      case "checkambiguous" -> {
        DebugDatabase.checkForAmbiguous();
        RequestLogger.printLine("Ambigiuous names looked for.");
        return;
      }
      case "checkzapgroups" -> {
        DebugDatabase.checkZapGroups();
        RequestLogger.printLine("Zap groups checked.");
      }
    }
  }
}
