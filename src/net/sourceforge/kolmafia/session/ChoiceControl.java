package net.sourceforge.kolmafia.session;

import static net.sourceforge.kolmafia.utilities.Statics.DateTimeManager;
import static net.sourceforge.kolmafia.utilities.StringUtilities.extractIidFromURL;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.EdServantData;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLCharacter.Gender;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.VYKEACompanionData;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.modifiers.ModifierList;
import net.sourceforge.kolmafia.modifiers.ModifierList.ModifierValue;
import net.sourceforge.kolmafia.moods.HPRestoreItemList;
import net.sourceforge.kolmafia.moods.MPRestoreItemList;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.DebugDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Phylum;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.AdventureRequest;
import net.sourceforge.kolmafia.request.AlliedRadioRequest;
import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.ArcadeRequest;
import net.sourceforge.kolmafia.request.BeachCombRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest.Mushroom;
import net.sourceforge.kolmafia.request.CargoCultistShortsRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.DeckOfEveryCardRequest;
import net.sourceforge.kolmafia.request.DecorateTentRequest;
import net.sourceforge.kolmafia.request.EatItemRequest;
import net.sourceforge.kolmafia.request.EdBaseRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FloristRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.GenieRequest;
import net.sourceforge.kolmafia.request.LatteRequest;
import net.sourceforge.kolmafia.request.LedCandleRequest;
import net.sourceforge.kolmafia.request.LocketRequest;
import net.sourceforge.kolmafia.request.MonkeyPawRequest;
import net.sourceforge.kolmafia.request.MummeryRequest;
import net.sourceforge.kolmafia.request.PantogramRequest;
import net.sourceforge.kolmafia.request.PyramidRequest;
import net.sourceforge.kolmafia.request.QuestLogRequest;
import net.sourceforge.kolmafia.request.ResearchBenchRequest;
import net.sourceforge.kolmafia.request.SaberRequest;
import net.sourceforge.kolmafia.request.SpaaaceRequest;
import net.sourceforge.kolmafia.request.SpelunkyRequest;
import net.sourceforge.kolmafia.request.SweetSynthesisRequest;
import net.sourceforge.kolmafia.request.TavernRequest;
import net.sourceforge.kolmafia.request.UmbrellaRequest;
import net.sourceforge.kolmafia.request.WildfireCampRequest;
import net.sourceforge.kolmafia.request.concoction.BurningLeavesRequest;
import net.sourceforge.kolmafia.session.ChoiceAdventures.Spoilers;
import net.sourceforge.kolmafia.session.TrackManager.Tracker;
import net.sourceforge.kolmafia.textui.command.EdPieceCommand;
import net.sourceforge.kolmafia.textui.command.JurassicParkaCommand;
import net.sourceforge.kolmafia.textui.command.SnowsuitCommand;
import net.sourceforge.kolmafia.utilities.ChoiceUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class ChoiceControl {

  private static int abooPeakLevel = 0;

  private static final AdventureResult CANDY_CANE_SWORD =
      ItemPool.get(ItemPool.CANDY_CANE_SWORD, 1);

  public static final Pattern URL_QTY_PATTERN = Pattern.compile("qty=(\\d+)");

  public static int extractQtyFromURL(final String urlString) {
    Matcher matcher = URL_QTY_PATTERN.matcher(urlString);
    return matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : -1;
  }

  private static final Pattern TELEGRAM_PATTERN = Pattern.compile("value=\"RE: (.*?)\"");

  public static final void preChoice(final GenericRequest request) {
    switch (ChoiceManager.lastChoice) {
        // Wheel In the Sky Keep on Turning: Muscle Position
      case 9 -> Preferences.setString(
          "currentWheelPosition",
          ChoiceManager.lastDecision == 1
              ? "mysticality"
              : ChoiceManager.lastDecision == 2 ? "moxie" : "muscle");

        // Wheel In the Sky Keep on Turning: Mysticality Position
      case 10 -> Preferences.setString(
          "currentWheelPosition",
          ChoiceManager.lastDecision == 1
              ? "map quest"
              : ChoiceManager.lastDecision == 2 ? "muscle" : "mysticality");

        // Wheel In the Sky Keep on Turning: Map Quest Position
      case 11 -> Preferences.setString(
          "currentWheelPosition",
          ChoiceManager.lastDecision == 1
              ? "moxie"
              : ChoiceManager.lastDecision == 2 ? "mysticality" : "map quest");

        // Wheel In the Sky Keep on Turning: Moxie Position
      case 12 -> Preferences.setString(
          "currentWheelPosition",
          ChoiceManager.lastDecision == 1
              ? "muscle"
              : ChoiceManager.lastDecision == 2 ? "map quest" : "moxie");

        // Maidens: disambiguate the Knights
      case 89 -> AdventureRequest.setNameOverride(
          "Knight", ChoiceManager.lastDecision == 1 ? "Knight (Wolf)" : "Knight (Snake)");

        // Strung-Up Quartet
      case 106 -> {
        Preferences.setInteger("lastQuartetAscension", KoLCharacter.getAscensions());
        Preferences.setInteger("lastQuartetRequest", ChoiceManager.lastDecision);

        if (KoLCharacter.recalculateAdjustments()) {
          KoLCharacter.updateStatus();
        }
      }

      case 123 -> { // At Least It's Not Full Of Trash
        if (ChoiceManager.lastDecision == 2) {
          // Raise your hands up towards the heavens
          // This takes take a turn and advances to the tiles
          ResultProcessor.processAdventuresUsed(1);
        }
      }

        // Start the Island War Quest
      case 142, 146 -> {
        if (ChoiceManager.lastDecision == 3) {
          QuestDatabase.setQuestProgress(Quest.ISLAND_WAR, "step1");
          Preferences.setString("warProgress", "started");
          if (KoLCharacter.inPokefam()) {
            // The following is a guess. Since all
            // sidequests are open, it is at least
            // 458, and surely both sides are equal
            Preferences.setInteger("hippiesDefeated", 500);
            Preferences.setInteger("fratboysDefeated", 500);
          }
        }
      }

        // The Gong Has Been Bung
      case 276 -> {
        ResultProcessor.processItem(ItemPool.GONG, -1);
        Preferences.setInteger("moleTunnelLevel", 0);
        Preferences.setInteger("birdformCold", 0);
        Preferences.setInteger("birdformHot", 0);
        Preferences.setInteger("birdformRoc", 0);
        Preferences.setInteger("birdformSleaze", 0);
        Preferences.setInteger("birdformSpooky", 0);
        Preferences.setInteger("birdformStench", 0);
      }

        // The Horror...
      case 611 -> // To find which step we're on, look at the responseText from the _previous_
      // request.  This should still be in lastResponseText.
      // Handle changing the progress level in postChoice1 where we know the result.
      abooPeakLevel =
          findBooPeakLevel(
              ChoiceUtilities.findChoiceDecisionText(1, ChoiceManager.lastResponseText));

        // Behind the world there is a door...
      case 612 -> {
        TurnCounter.stopCounting("Silent Invasion window begin");
        TurnCounter.stopCounting("Silent Invasion window end");
        TurnCounter.startCounting(35, "Silent Invasion window begin loc=*", "lparen.gif");
        TurnCounter.startCounting(40, "Silent Invasion window end loc=*", "rparen.gif");
      }

      case 794 -> ResultProcessor.removeItem(ItemPool.FUNKY_JUNK_KEY);

      case 804 -> { // Trick-or-Treating!
        {
          // Whether you win or lose the following encounter, or if there is just a non-combat,
          // once the selection is made, the house is "used", so we can reflect that in the pref
          // here.
          var housePattern = Pattern.compile("whichhouse=(\\d+)");
          var houseMatcher = housePattern.matcher(request.getURLString());
          if (houseMatcher.find()) {
            var house = Integer.parseInt(houseMatcher.group(1));
            var state = Preferences.getString("_trickOrTreatBlock").toCharArray();
            state[house] = Character.toLowerCase(state[house]);
            Preferences.setString("_trickOrTreatBlock", String.valueOf(state));
            String message = "[" + KoLAdventure.getAdventureCount() + "] Trick-or-Treating";
            RequestLogger.printLine(message);
            RequestLogger.updateSessionLog(message);
            KoLAdventure.clearLocation();
          }
        }
      }

      case 931 -> // Life Ain't Nothin But Witches and Mummies
      QuestDatabase.setQuestIfBetter(Quest.CITADEL, "step6");

      case 932 -> // No Whammies
      QuestDatabase.setQuestIfBetter(Quest.CITADEL, "step8");

      case 1005, // 'Allo
          1006, // One Small Step For Adventurer
          1007, // Twisty Little Passages, All Hedge
          1008, // Pooling Your Resources
          1009, // Good Ol' 44% Duck
          1010, // Another Day, Another Fork
          1011, // Of Mouseholes and Manholes
          1012, // The Last Temptation
          1013 -> // Mazel Tov!
      // Taking any of these takes a turn. We'll eventually
      // be informed of that in a charpane/api refresh, but
      // that's too late for logging.
      ResultProcessor.processAdventuresUsed(1);

      case 1023 -> { // Like a Bat into Hell
        if (ChoiceManager.lastDecision == 2) {
          int edDefeats = Preferences.getInteger("_edDefeats");
          int kaCost = edDefeats > 2 ? (int) (Math.pow(2, Math.min(edDefeats - 3, 5))) : 0;
          AdventureResult cost = ItemPool.get(ItemPool.KA_COIN, -kaCost);
          ResultProcessor.processResult(cost);
          KoLCharacter.setLimitMode(LimitMode.NONE);
        }
      }

      case 1024 -> { // Like a Bat out of Hell
        switch (ChoiceManager.lastDecision) {
          case 2 -> {
            Preferences.setInteger("_edDefeats", 0);
            Preferences.setBoolean("edUsedLash", false);
            MonsterStatusTracker.reset();
            KoLCharacter.setLimitMode(LimitMode.NONE);
          }
          case 1 -> {
            int edDefeats = Preferences.getInteger("_edDefeats");
            int kaCost = edDefeats > 2 ? (int) (Math.pow(2, Math.min(edDefeats - 3, 5))) : 0;
            AdventureResult cost = ItemPool.get(ItemPool.KA_COIN, -kaCost);
            ResultProcessor.processResult(cost);
            KoLCharacter.setLimitMode(LimitMode.NONE);
          }
        }
      }

      case 1028 -> // A Shop
      SpelunkyRequest.logShop(ChoiceManager.lastResponseText, ChoiceManager.lastDecision);

      case 1085 -> { // Deck of Every Card
        if (ChoiceManager.lastDecision == 1) {
          Preferences.increment("_deckCardsDrawn", 1, 15, false);
        }
      }

      case 1086 -> { // Pick a Card
        if (ChoiceManager.lastDecision == 1) {
          // The extra 1 will be covered in choice 1085
          Preferences.increment("_deckCardsDrawn", 4, 15, false);
        }
      }

      case 1171 -> { // LT&T Office
        if (ChoiceManager.lastDecision < 4) {
          QuestDatabase.setQuestProgress(Quest.TELEGRAM, QuestDatabase.STARTED);
          Preferences.setInteger("lttQuestDifficulty", ChoiceManager.lastDecision);
          Preferences.setInteger("lttQuestStageCount", 0);
          Matcher matcher = TELEGRAM_PATTERN.matcher(ChoiceManager.lastResponseText);
          for (int i = 0; i < ChoiceManager.lastDecision; i++) {
            if (!matcher.find()) {
              break;
            }
          }
          Preferences.setString("lttQuestName", matcher.group(1));
        } else if (ChoiceManager.lastDecision == 5) {
          QuestDatabase.setQuestProgress(Quest.TELEGRAM, QuestDatabase.UNSTARTED);
          Preferences.setInteger("lttQuestDifficulty", 0);
          Preferences.setInteger("lttQuestStageCount", 0);
          Preferences.setString("lttQuestName", "");
        }
      }

      case 1197 -> {
        // Travel back to a Delicious Meal
        if (ChoiceManager.lastDecision == 1 && !request.getURLString().contains("foodid=0")) {
          EatItemRequest.timeSpinnerUsed = true;
        }
      }

      case 1261 -> {
        // Which Door?
        if (ChoiceManager.lastResponseText.contains("Boris")) {
          Preferences.setString("_villainLairKey", "boris");
        } else if (ChoiceManager.lastResponseText.contains("Jarlsberg")) {
          Preferences.setString("_villainLairKey", "jarlsberg");
        } else if (ChoiceManager.lastResponseText.contains("Sneaky Pete")) {
          Preferences.setString("_villainLairKey", "pete");
        }
      }

      case 1345 -> // Blech House
      Preferences.setInteger("smutOrcNoncombatProgress", 0);

      case 1500 -> {
        // Like a Loded Stone
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setInteger("lastShadowForgeUnlockAdventure", KoLCharacter.getCurrentRun());
          ConcoctionDatabase.refreshConcoctions();
        }
      }
    }
  }

  public static void postChoice0(int choice, final String urlString, final GenericRequest request) {
    String text = request.responseText;
    switch (choice) {
      case 125 -> { // No Visible Means of Support
        if (ChoiceManager.lastChoice == 0) {
          // If we are visiting for the first time,
          // finish the tiles
          DvorakManager.lastTile(text);
        }
      }

      case 360 -> // Wumpus Cave
      WumpusManager.preWumpus(ChoiceManager.lastDecision);

      case 1019 -> { // Bee Rewarded
        if (ChoiceManager.lastDecision == 1) {
          // This does not contain an "Encounter", so was
          // not logged.
          RequestLogger.registerLocation("The Black Forest");
        }
      }

      case 1308 -> {
        {
          // place.php?whichplace=monorail&action=monorail_downtown
          //
          // On a Downtown Train
          //
          // Everything you do at this location uses this choice
          // adventure #. As you select options, you stay in the
          // same choice, but the available options change.
          //
          // We must deduce what is happening by looking at the
          // response text

          if (text.contains("muffin is not yet ready")) {
            Preferences.setBoolean("_muffinOrderedToday", true);

            // We submitted a choice. If we are buying a muffin,
            // remove the muffin tin here, since the response is
            // indistinguishable from simply visiting the Breakfast
            // Counter with a muffin on order.
            //
            // Therefore, look at the previous response. If it is
            // selling muffins and we jut bought a muffin, remove
            // the muffin tin.

            if (ChoiceManager.lastResponseText != null
                && ChoiceManager.lastResponseText.contains("Order a blueberry muffin")) {
              ResultProcessor.processResult(ItemPool.get(ItemPool.EARTHENWARE_MUFFIN_TIN, -1));
            }
          }
        }
      }

      case 1313, // Bastille Battalion
          1314, // Bastille Battalion (Master of None)
          1315, // Castle vs. Castle
          1316, // GAME OVER
          1317, // A Hello to Arms (Battalion)
          1318, // Defensive Posturing
          1319 -> // Cheese Seeking Behavior
      BastilleBattalionManager.preChoice(urlString, request);

      case 1356, // Smooth Sailing
          1357, // High Tide, Low Morale
          1358, // The Starboard is Bare
          1359, // Grog for the Grogless
          1360, // Like Shops in the Night
          1361, // Avast, a Mast!
          1362, // Stormy Weather
          1363, // Who Pirates the Pirates?
          1364, // An Opportunity for Dastardly Do
          1365 -> { // A Sea Monster!
        // This could be in postChoice1 but doing it here allows us to use a single line of code.

        // Shops in the Night only takes its turn when you decide to leave (decision 6),
        // otherwise we just need to make sure that we made a decision at all
        var takesTurn =
            (choice == 1360)
                ? (ChoiceManager.lastDecision == 6)
                : (ChoiceManager.lastDecision != 0);
        if (takesTurn) {
          var turns = Preferences.increment("_pirateRealmSailingTurns", 1);
          if (turns >= Preferences.getInteger("_pirateRealmShipSpeed")) {
            QuestManager.setPirateRealmIslandQuestProgress(1);
          }
        }
      }

      case 1451 -> // Fire Captain Hagnk
      WildfireCampRequest.parseCaptain(text);
      case 1452 -> {
        // Sprinkler Joe
        if (text.contains("Thanks again for your help!")) {
          Preferences.setBoolean("wildfireSprinkled", true);
        }
      }
      case 1453 -> {
        // Fracker Dan
        if (text.contains("Thanks for the help!")) {
          Preferences.setBoolean("wildfireFracked", true);
        }
      }
      case 1454 -> {
        // Cropduster Dusty
        if (text.contains("Thanks for helping out.")) {
          Preferences.setBoolean("wildfireDusted", true);
        }
      }
      case 1523 -> // Research Bench
      ResearchBenchRequest.postChoice0(urlString, text);
    }
  }

  private static final Pattern BROKEN_CHAMPAGNE_PATTERN =
      Pattern.compile("Looks like it has (\\d+) ounce");
  private static final Pattern DAYCARE_RECRUIT_PATTERN =
      Pattern.compile("attract (.*?) new children");
  private static final Pattern DAYCARE_EQUIPMENT_PATTERN =
      Pattern.compile("manage to find (.*?) used");
  private static final Pattern DAYCARE_ITEM_PATTERN =
      Pattern.compile("<td valign=center>You lose an item: </td>.*?<b>(.*?)</b> \\((.*?)\\)</td>");
  private static final Pattern DECEASED_TREE_PATTERN =
      Pattern.compile("Looks like it has (.*?) needle");
  private static final Pattern DESCID_PATTERN = Pattern.compile("descitem\\((.*?)\\)");
  // You toss the space wine off the edge of the floating battlefield and 21 frat boys jump off
  // after it.
  private static final Pattern FRAT_RATIONING_PATTERN =
      Pattern.compile(
          "You toss the (.*?) off the edge of the floating battlefield and (\\d+) frat boys? jumps? off after it.");
  private static final Pattern GARBAGE_SHIRT_PATTERN =
      Pattern.compile("Looks like you can read roughly (\\d+) scrap");
  private static final Pattern GERALD_PATTERN =
      Pattern.compile("Gerald wants (\\d+)<table>.*?descitem\\((\\d+)\\)");
  private static final Pattern GERALDINE_PATTERN =
      Pattern.compile("Geraldine wants (\\d+)<table>.*?descitem\\((\\d+)\\)");
  private static final Pattern HELLEVATOR_PATTERN =
      Pattern.compile(
          "the (lobby|first|second|third|fourth|fifth|sixth|seventh|eighth|ninth|tenth|eleventh) (button|floor)");
  // You toss the mana curds into the crowd.  10 hippies dive onto it, greedily consume it, and pass
  // out.
  private static final Pattern HIPPY_RATIONING_PATTERN =
      Pattern.compile(
          "You toss the (.*?) into the crowd.  (\\d+) hippies dive onto it, greedily consume it, and pass out.");
  // You select a Gold Tier client, Norma "Smelly" Jackson, a hobo from The Oasis.
  private static final Pattern GUZZLR_LOCATION_PATTERN =
      Pattern.compile(
          "You select a (Bronze|Gold|Platinum) Tier client, (.*?), +an? (.*?) from (.*?)\\.<p>");
  private static final Pattern ITEMID_PATTERN = Pattern.compile("itemid(\\d+)=(\\d+)");
  private static final Pattern ORACLE_QUEST_PATTERN =
      Pattern.compile("don't remember leaving any spoons in (.*?)&quot;");
  private static final Pattern QTY_PATTERN = Pattern.compile("qty(\\d+)=(\\d+)");
  private static final Pattern SAFE_PATTERN = Pattern.compile("find ([\\d,]+) Meat in the safe");
  private static final Pattern SKELETON_PATTERN =
      Pattern.compile("You defeated <b>(\\d+)</b> skeletons");
  private static final Pattern TATTOO_PATTERN =
      Pattern.compile("otherimages/sigils/hobotat(\\d+).gif");
  private static final Pattern TOSSID_PATTERN = Pattern.compile("tossid=(\\d+)");
  private static final Pattern TRASH_PATTERN =
      Pattern.compile("must have been (\\d+) pieces of trash");
  private static final Pattern UNPERM_PATTERN =
      Pattern.compile("Turning (.+)(?: \\(HP\\)) into (\\d+) karma.");
  private static final Pattern URL_SKILLID_PATTERN = Pattern.compile("skillid=(\\d+)");
  private static final Pattern URL_VOTE_PATTERN = Pattern.compile("local\\[\\]=(\\d)");
  private static final Pattern YEARBOOK_TARGET_PATTERN =
      Pattern.compile("<b>Results:</b>.*?<b>(.*?)</b>");

  private static final String[] FLOORS =
      new String[] {
        "lobby",
        "first",
        "second",
        "third",
        "fourth",
        "fifth",
        "sixth",
        "seventh",
        "eighth",
        "ninth",
        "tenth",
        "eleventh",
      };

  private static final AdventureResult BINDER_CLIP = ItemPool.get(ItemPool.BINDER_CLIP, 1);
  private static final AdventureResult MCCLUSKY_FILE = ItemPool.get(ItemPool.MCCLUSKY_FILE, 1);

  public static void postChoice1(final String urlString, final GenericRequest request) {
    String text = request.responseText;

    switch (ChoiceManager.lastChoice) {
      case 4 -> {
        // Finger-Lickin'... Death.
        if (ChoiceManager.lastDecision == 4) {
          Preferences.setBoolean("_candyCaneSwordSouthOfTheBorder", true);
        }
      }

      case 109 -> {
        // Dumpster Diving
        if (ChoiceManager.lastDecision == 4) {
          Preferences.setBoolean("_candyCaneSwordBackAlley", true);
        }
      }

      case 127 -> {
        // No sir, away! A papaya war is on!
        if (ChoiceManager.lastDecision == 4) {
          Preferences.setBoolean("_candyCaneSwordPalindome", true);
        }
      }

      case 139 -> {
        // Bait and Switch
        if (ChoiceManager.lastDecision == 4) {
          Preferences.setBoolean("candyCaneSwordWarHippyBait", true);
        }
      }

      case 140 -> {
        // The Thin Tie-Dyed Line
        if (ChoiceManager.lastDecision == 4) {
          Preferences.setBoolean("candyCaneSwordWarHippyLine", true);
        }
      }

      case 143 -> {
        // Catching Some Zetas
        if (ChoiceManager.lastDecision == 4) {
          Preferences.setBoolean("candyCaneSwordWarFratZetas", true);
        }
      }

      case 144 -> {
        // One Less Room Than In That Movie
        if (ChoiceManager.lastDecision == 4) {
          Preferences.setBoolean("candyCaneSwordWarFratRoom", true);
        }
      }

      case 147 -> {
        // Cornered!
        int ducks1 =
            switch (ChoiceManager.lastDecision) {
              case 1 -> AdventurePool.THE_GRANARY;
              case 2 -> AdventurePool.THE_BOG;
              case 3 -> AdventurePool.THE_POND;
              default -> 0;
            };
        Preferences.setString("duckAreasSelected", String.valueOf(ducks1));
      }

      case 148 -> {
        // Cornered Again!
        int ducks2 =
            switch (ChoiceManager.lastDecision) {
              case 1 -> AdventurePool.THE_BACK_40;
              case 2 -> AdventurePool.THE_FAMILY_PLOT;
              default -> 0;
            };
        Preferences.setString(
            "duckAreasSelected", Preferences.getString("duckAreasSelected") + "," + ducks2);
      }

      case 149 -> {
        // How Many Corners Does this Stupid Barn Have?
        int ducks3 =
            switch (ChoiceManager.lastDecision) {
              case 1 -> AdventurePool.THE_SHADY_THICKET;
              case 2 -> AdventurePool.THE_OTHER_BACK_40;
              default -> 0;
            };
        Preferences.setString(
            "duckAreasSelected", Preferences.getString("duckAreasSelected") + "," + ducks3);
      }

      case 151 -> {
        // Adventurer, $1.99
        if (ChoiceManager.lastDecision == 3) {
          Preferences.setBoolean("candyCaneSwordFunHouse", true);
        }
      }

      case 188 -> {
        // The Infiltrationist

        // Once you're inside the frat house, it's a simple
        // matter of making your way down to the basement and
        // retrieving Caronch's dentures from the frat boys'
        // ridiculous trophy case.

        if (ChoiceManager.lastDecision == 3 && text.contains("ridiculous trophy case")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.HOT_WING, -3));
        }
      }

      case 189 -> {
        // O Cap'm, My Cap'm
        if (ChoiceManager.lastDecision == 3) {
          QuestDatabase.setQuestIfBetter(Quest.NEMESIS, "step26");
        }
      }

      case 191 -> {
        // Chatterboxing
        if (ChoiceManager.lastDecision == 2
            && text.contains("find a valuable trinket that looks promising")) {
          BanishManager.banishMonster("chatty pirate", BanishManager.Banisher.CHATTERBOXING, true);
        }
      }

      case 237,
          // Big Merv's Protein Shakes
          238,
          // Suddenly Salad!
          239 -> {
        // Sizzling Weasel On a Stick
        if (ChoiceManager.lastDecision == 1 && text.contains("You gain")) {
          // You spend 20 hobo nickels
          AdventureResult cost = ItemPool.get(ItemPool.HOBO_NICKEL, -20);
          ResultProcessor.processResult(cost);

          // You gain 5 fullness
          KoLCharacter.setFullness(KoLCharacter.getFullness() + 5);
        }
      }

      case 242,
          // Arthur Finn's World-Record Homebrew Stout
          243,
          // Mad Jack's Corn Squeezery
          244 -> {
        // Bathtub Jimmy's Gin Mill
        if (ChoiceManager.lastDecision == 1 && text.contains("You gain")) {
          // You spend 20 hobo nickels
          AdventureResult cost = ItemPool.get(ItemPool.HOBO_NICKEL, -20);
          ResultProcessor.processResult(cost);

          // You gain 5 drunkenness.  This will be set
          // when we refresh the charpane.
        }
      }

      case 271,
          // Tattoo Shop
          274 -> {
        // Tattoo Redux
        if (ChoiceManager.lastDecision == 1) {
          Matcher matcher = TATTOO_PATTERN.matcher(request.responseText);
          if (matcher.find()) {
            int tattoo = StringUtilities.parseInt(matcher.group(1));
            AdventureResult cost = ItemPool.get(ItemPool.HOBO_NICKEL, -20 * tattoo);
            ResultProcessor.processResult(cost);
          }
        }
      }

      case 298 -> {
        // In the Shade

        // You carefully plant the packet of seeds, sprinkle it
        // with gooey green algae, wait a few days, and then
        // you reap what you sow. Sowed. Sew?

        if (ChoiceManager.lastDecision == 1 && text.contains("you reap what you sow")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.SEED_PACKET, -1));
          ResultProcessor.processResult(ItemPool.get(ItemPool.GREEN_SLIME, -1));
        }
      }

      case 346,
          // Soup For You
          347,
          // Yes, Soup For You...
          348,
          // Souped Up
          351 -> // Beginner's Luck
      QuestDatabase.setQuestIfBetter(Quest.PRIMORDIAL, QuestDatabase.STARTED);

      case 349 -> {
        // The Primordial Directive
        // You swam upward, into a brighter and warmer part of the soup
        if (ChoiceManager.lastDecision == 1
            && text.contains("a brighter and warmer part of the soup")) {
          QuestDatabase.setQuestIfBetter(Quest.PRIMORDIAL, "step1");
        }
      }

      case 350 -> {
        // Soupercharged
        if (ChoiceManager.lastDecision == 1 && text.contains("You've fixed me all up")) {
          Preferences.setInteger("aminoAcidsUsed", 0);
          QuestDatabase.setQuestProgress(Quest.PRIMORDIAL, QuestDatabase.FINISHED);
        }
      }

      case 354 -> // You Can Never Be Too Rich or Too in the Future
      ResultProcessor.processResult(ItemPool.get(ItemPool.INDIGO_PARTY_INVITATION, -1));

      case 355 -> // I'm on the Hunt, I'm After You
      ResultProcessor.processResult(ItemPool.get(ItemPool.VIOLET_HUNT_INVITATION, -1));

      case 357 -> // Painful, Circuitous Logic
      ResultProcessor.processResult(ItemPool.get(ItemPool.MECHA_MAYHEM_CLUB_CARD, -1));

      case 358 -> // Brings All the Boys to the Blue Yard
      ResultProcessor.processResult(ItemPool.get(ItemPool.BLUE_MILK_CLUB_CARD, -1));

      case 362 -> // A Bridge Too Far
      ResultProcessor.processResult(ItemPool.get(ItemPool.SPACEFLEET_COMMUNICATOR_BADGE, -1));

      case 363 -> // Does This Bug You? Does This Bug You?
      ResultProcessor.processResult(ItemPool.get(ItemPool.SMUGGLER_SHOT_FIRST_BUTTON, -1));

      case 373 -> {
        // Choice 373 is Northern Gate

        // Krakrox plugged the small stone block he found in
        // the basement of the abandoned building into the hole
        // on the left side of the gate

        if (text.contains("Krakrox plugged the small stone block")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.SMALL_STONE_BLOCK, -1));
        }

        // Krakrox plugged the little stone block he had found
        // in the belly of the giant snake into the hole on the
        // right side of the gate

        else if (text.contains("Krakrox plugged the little stone block")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.LITTLE_STONE_BLOCK, -1));
        }
      }

      case 376 -> {
        // Choice 376 is Ancient Temple

        // You fit the two halves of the stone circle together,
        // and slot them into the depression on the door. After
        // a moment's pause, a low rumbling becomes audible,
        // and then the stone slab lowers into the ground. The
        // temple is now open to you, if you are ready to face
        // whatever lies inside.

        if (text.contains("two halves of the stone circle")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.HALF_STONE_CIRCLE, -1));
          ResultProcessor.processResult(ItemPool.get(ItemPool.STONE_HALF_CIRCLE, -1));
        }
      }

      case 389 -> {
        // Choice 389 is The Unbearable Supremeness of Being

        // "Of course I understand," Jill says, in fluent
        // English. "I learned your language in the past five
        // minutes. I know where the element is, but we'll have
        // to go offworld to get it. Meet me at the Desert
        // Beach Spaceport." And with that, she gives you a
        // kiss and scampers off. Homina-homina.

        if (text.contains("Homina-homina")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.SUPREME_BEING_GLOSSARY, -1));
          QuestDatabase.setQuestProgress(Quest.FUTURE, "step1");
        }
      }

      case 390 -> {
        // Choice 390 is A Winning Pass

        // "Um, okay," you say, and stomp off to kill some more time.

        if (text.contains("to kill some more time")) {
          QuestDatabase.setQuestProgress(Quest.FUTURE, "step2");
        }
      }

      case 391 -> {
        // Choice 391 is OMG KAWAIII

        // "Damn it!" you say, and chase after her.

        if (text.contains("and chase after her")) {
          QuestDatabase.setQuestProgress(Quest.FUTURE, "step3");
        }
      }

      case 392 -> {
        // Choice 392 is The Elements of Surprise . . .

        // And as the two of you walk toward the bed, you sense
        // your ancestral memories pulling you elsewhere, ever
        // elsewhere, because your ancestral memories are
        // total, absolute jerks.

        if (text.contains("total, absolute jerks")) {
          EquipmentManager.discardEquipment(ItemPool.RUBY_ROD);
          ResultProcessor.processResult(ItemPool.get(ItemPool.ESSENCE_OF_HEAT, -1));
          ResultProcessor.processResult(ItemPool.get(ItemPool.ESSENCE_OF_KINK, -1));
          ResultProcessor.processResult(ItemPool.get(ItemPool.ESSENCE_OF_COLD, -1));
          ResultProcessor.processResult(ItemPool.get(ItemPool.ESSENCE_OF_STENCH, -1));
          ResultProcessor.processResult(ItemPool.get(ItemPool.ESSENCE_OF_FRIGHT, -1));
          ResultProcessor.processResult(ItemPool.get(ItemPool.ESSENCE_OF_CUTE, -1));
          ResultProcessor.processResult(ItemPool.get(ItemPool.SECRET_FROM_THE_FUTURE, 1));
          QuestDatabase.setQuestProgress(Quest.FUTURE, QuestDatabase.FINISHED);
        }
      }

      case 393 -> {
        // The Collector
        if (ChoiceManager.lastDecision == 1) {
          for (int i = ItemPool.GREEN_PEAWEE_MARBLE; i <= ItemPool.BIG_BUMBOOZER_MARBLE; ++i) {
            ResultProcessor.processResult(ItemPool.get(i, -1));
          }
        }
      }

      case 394 -> {
        // Hellevator Music
        // Parse response
        Matcher matcher = HELLEVATOR_PATTERN.matcher(text);
        if (!matcher.find()) {
          break;
        }
        String floor = matcher.group(1);
        for (int mcd = 0; mcd < FLOORS.length; ++mcd) {
          if (floor.equals(FLOORS[mcd])) {
            String message = "Setting monster level to " + mcd;
            RequestLogger.printLine(message);
            RequestLogger.updateSessionLog(message);
            break;
          }
        }
      }

      case 413, 414, 415, 416, 417, 418 -> HaciendaManager.parseRoom(
          ChoiceManager.lastChoice, ChoiceManager.lastDecision, text);

      case 440 -> {
        // Puttin' on the Wax
        if (ChoiceManager.lastDecision == 1) {
          HaciendaManager.parseRecording(urlString, text);
        }
      }

      case 443 -> {
        // Chess Puzzle
        if (ChoiceManager.lastDecision == 1) {
          // Option 1 is "Play"
          RabbitHoleManager.parseChessMove(urlString, text);
        }
      }

      case 450 -> {
        // The Duchess' Cottage
        if (ChoiceManager.lastDecision == 1
            && text.contains("Delectable and pulchritudinous!")) { // Option 1 is Feed the Duchess
          ResultProcessor.processItem(ItemPool.BEAUTIFUL_SOUP, -1);
          ResultProcessor.processItem(ItemPool.LOBSTER_QUA_GRILL, -1);
          ResultProcessor.processItem(ItemPool.MISSING_WINE, -1);
          ResultProcessor.processItem(ItemPool.WALRUS_ICE_CREAM, -1);
          ResultProcessor.processItem(ItemPool.HUMPTY_DUMPLINGS, -1);
        }
      }

      case 457 -> {
        // Oh, No! Five-Oh!
        int count = InventoryManager.getCount(ItemPool.ORQUETTES_PHONE_NUMBER);
        if (ChoiceManager.lastDecision == 1 && count > 0) {
          ResultProcessor.processItem(ItemPool.ORQUETTES_PHONE_NUMBER, -count);
          ResultProcessor.processItem(ItemPool.KEGGER_MAP, -1);
        }
      }

      case 460,
          461,
          462,
          463,
          464,
          465,
          467,
          468,
          469,
          470,
          472,
          473,
          474,
          475,
          476,
          477,
          478,
          479,
          480,
          481,
          482,
          483,
          484 -> // Space Trip
      ArcadeRequest.postChoiceSpaceTrip(
          request, ChoiceManager.lastChoice, ChoiceManager.lastDecision);

      case 471 -> // DemonStar
      ArcadeRequest.postChoiceDemonStar(request, ChoiceManager.lastDecision);

      case 485 -> // Fighters Of Fighting
      ArcadeRequest.postChoiceFightersOfFighting(request, ChoiceManager.lastDecision);

      case 486 -> // Dungeon Fist!
      ArcadeRequest.postChoiceDungeonFist(request, ChoiceManager.lastDecision);

      case 488, 489, 490, 491 -> // Meteoid
      ArcadeRequest.postChoiceMeteoid(
          request, ChoiceManager.lastChoice, ChoiceManager.lastDecision);

      case 502 -> {
        // Arboreal Respite
        if (ChoiceManager.lastDecision == 5) {
          Preferences.setBoolean("_candyCaneSwordSpookyForest", true);
        }
      }

      case 523 -> {
        // Death Rattlin'
        if (ChoiceManager.lastDecision == 5) {
          Preferences.setBoolean("candyCaneSwordDefiledCranny", true);
          // You jam your candy cane sword into the hole and flick it around.  You hear moans and
          // groans and grunts.  Seems like you easily defeated some tiny horrors.
          //
          // Your Evilometer beeps 11 times.
          if (text.contains("Your Evilometer beeps 11 times.")) {
            CryptManager.decreaseEvilness(AdventurePool.DEFILED_CRANNY, 11);
          }
        }
      }

      case 529, 531, 532, 533, 534 -> {
        Matcher skeletonMatcher = SKELETON_PATTERN.matcher(text);
        if (skeletonMatcher.find()) {
          String message = "You defeated " + skeletonMatcher.group(1) + " skeletons";
          RequestLogger.printLine(message);
          RequestLogger.updateSessionLog(message);
        }
      }

      case 539 -> // Choice 539 is An E.M.U. for Y.O.U.
      EquipmentManager.discardEquipment(ItemPool.SPOOKY_LITTLE_GIRL);

      case 540 -> {
        // Choice 540 is Big-Time Generator - game board
        //
        // Win:
        //
        // The generator starts to hum and the well above you
        // begins to spin, slowly at first, then faster and
        // faster. The humming becomes a deep, sternum-rattling
        // thrum, a sub-audio *WHOOMP WHOOMP WHOOMPWHOOMPWHOOMP.*
        // Brilliant blue light begins to fill the well, and
        // you feel like your bones are turning to either
        // powder, jelly, or jelly with powder in it.<p>Then
        // you fall through one of those glowy-circle
        // transporter things and end up back on Grimace, and
        // boy, are they glad to see you! You're not sure where
        // one gets ticker-tape after an alien invasion, but
        // they seem to have found some.
        //
        // Lose 3 times:
        //
        // Your E.M.U.'s getting pretty beaten up from all the
        // pinballing between obstacles, and you don't like
        // your chances of getting back to the surface if you
        // try again. You manage to blast out of the generator
        // well and land safely on the surface. After that,
        // though, the E.M.U. gives an all-over shudder, a sad
        // little servo whine, and falls apart.

        if (text.contains("WHOOMP") || text.contains("a sad little servo whine")) {
          EquipmentManager.discardEquipment(ItemPool.EMU_UNIT);
          QuestDatabase.setQuestIfBetter(Quest.GENERATOR, QuestDatabase.FINISHED);
        }
      }

      case 542 -> {
        // The Now's Your Pants!  I Mean... Your Chance!

        // Then you make your way back out of the Alley,
        // clutching your pants triumphantly and trying really
        // hard not to think about how oddly chilly it has
        // suddenly become.

        // When you steal your pants, they are unequipped, you
        // gain a "you acquire" message", and they appear in
        // inventory.
        //
        // Treat this is simply discarding the pants you are
        // wearing
        if (text.contains("oddly chilly")) {
          EquipmentManager.discardEquipment(EquipmentManager.getEquipment(Slot.PANTS));
          QuestDatabase.setQuestProgress(Quest.MOXIE, "step1");
        }
      }

      case 546 -> // Interview With You
      VampOutManager.postChoiceVampOut(text);

      case 559 -> {
        // Fudge Mountain Breakdown
        if (ChoiceManager.lastDecision == 2) {
          if (text.contains("but nothing comes out")) {
            Preferences.setInteger("_fudgeWaspFights", 3);
          } else if (text.contains("trouble has taken a holiday")) {
            // The Advent Calendar hasn't been punched out enough to find fudgewasps yet
          } else {
            Preferences.increment("_fudgeWaspFights", 1);
          }
        }
      }

      case 571 -> // Choice 571 is Your Minstrel Vamps
      QuestDatabase.setQuestProgress(Quest.CLANCY, QuestDatabase.STARTED);

      case 572 -> // Choice 572 is Your Minstrel Clamps
      QuestDatabase.setQuestProgress(Quest.CLANCY, "step2");

      case 573 -> // Choice 573 is Your Minstrel Stamps
      QuestDatabase.setQuestProgress(Quest.CLANCY, "step4");

      case 576 -> // Choice 576 is Your Minstrel Camps
      QuestDatabase.setQuestProgress(Quest.CLANCY, "step6");

      case 577 -> // Choice 577 is Your Minstrel Scamp
      QuestDatabase.setQuestProgress(Quest.CLANCY, "step8");

      case 588 -> {
        // Machines!
        if (text.contains("The batbugbears around you start acting weird")) {
          BugbearManager.clearShipZone("Sonar");
        }
      }

      case 589 -> {
        // Autopsy Auturvy
        // The tweezers you used dissolve in the caustic fluid. Rats.
        if (text.contains("dissolve in the caustic fluid")) {
          ResultProcessor.processItem(ItemPool.AUTOPSY_TWEEZERS, -1);
          Preferences.increment("autopsyTweezersUsed");
        }
      }

      case 594 -> {
        // A Lost Room
        if (text.contains("You acquire")) {
          ResultProcessor.processItem(ItemPool.LOST_KEY, -1);
        }
      }

      case 595 -> {
        // Fire! I... have made... fire!
        if (text.contains("rubbing the two stupid sticks together")
            || text.contains("pile the sticks up on top of the briefcase")) {
          ResultProcessor.processItem(ItemPool.CSA_FIRE_STARTING_KIT, -1);
        }
      }

      case 599 -> {
        // A Zombie Master's Bait
        if (request.getFormField("quantity") == null) {
          return;
        }

        AdventureResult brain;
        if (ChoiceManager.lastDecision == 1) {
          brain = ItemPool.get(ItemPool.CRAPPY_BRAIN, 1);
        } else if (ChoiceManager.lastDecision == 2) {
          brain = ItemPool.get(ItemPool.DECENT_BRAIN, 1);
        } else if (ChoiceManager.lastDecision == 3) {
          brain = ItemPool.get(ItemPool.GOOD_BRAIN, 1);
        } else if (ChoiceManager.lastDecision == 4) {
          brain = ItemPool.get(ItemPool.BOSS_BRAIN, 1);
        } else {
          return;
        }

        int quantity = StringUtilities.parseInt(request.getFormField("quantity"));
        int inventoryCount = brain.getCount(KoLConstants.inventory);
        brain = brain.getInstance(-1 * Math.min(quantity, inventoryCount));

        ResultProcessor.processResult(brain);
      }

      case 603 -> {
        // Skeletons and The Closet
        if (ChoiceManager.lastDecision != 6) {
          ResultProcessor.removeItem(ItemPool.SKELETON);
        }
      }

      case 607 -> {
        // Room 237
        // Twin Peak first choice
        if (text.contains("You take a moment to steel your nerves.")) {
          int prefval = Preferences.getInteger("twinPeakProgress");
          prefval |= 1;
          Preferences.setInteger("twinPeakProgress", prefval);
        }
      }

      case 608 -> {
        // Go Check It Out!
        // Twin Peak second choice
        if (text.contains("All work and no play")) {
          int prefval = Preferences.getInteger("twinPeakProgress");
          prefval |= 2;
          Preferences.setInteger("twinPeakProgress", prefval);
        }
      }

      case 611 -> {
        // The Horror...
        // We need to detect if the choiceadv step was completed OR we got beaten up.
        // If we Flee, nothing changes
        if (ChoiceManager.lastDecision == 1) {
          if (text.contains("That's all the horror you can take")) { // AKA beaten up
            Preferences.decrement("booPeakProgress", 2, 0);
          } else {
            Preferences.decrement("booPeakProgress", 2 * abooPeakLevel, 0);
          }
          if (Preferences.getInteger("booPeakProgress") < 0) {
            Preferences.setInteger("booPeakProgress", 0);
          }
        }
      }

      case 614 -> {
        // Near the fog there is an... anvil?
        if (!text.contains("You acquire")) {
          return;
        }
        int souls =
            ChoiceManager.lastDecision == 1
                ? 3
                : ChoiceManager.lastDecision == 2
                    ? 11
                    : ChoiceManager.lastDecision == 3
                        ? 23
                        : ChoiceManager.lastDecision == 4 ? 37 : 0;
        if (souls > 0) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.MIME_SOUL_FRAGMENT, 0 - souls));
        }
      }

      case 616 -> {
        // He Is the Arm, and He Sounds Like This
        // Twin Peak third choice
        if (text.contains("You attempt to mingle")) {
          int prefval = Preferences.getInteger("twinPeakProgress");
          prefval |= 4;
          Preferences.setInteger("twinPeakProgress", prefval);
          ResultProcessor.processResult(ItemPool.get(ItemPool.JAR_OF_OIL, -1));
        }
      }

      case 617 -> {
        // Now It's Dark
        // Twin Peak fourth choice
        if (text.contains("When the lights come back")) {
          // the other three must be completed at this point.
          Preferences.setInteger("twinPeakProgress", 15);
        }
      }

      case 618 -> {
        // Cabin Fever
        if (text.contains("mercifully, the hotel explodes")) {
          // the other three must be completed at this point.
          Preferences.setInteger("twinPeakProgress", 15);
        }
      }

      case 627, 628, 629, 630, 631, 633 -> ChibiBuddyManager.postChoice(
          ChoiceManager.lastChoice, ChoiceManager.lastDecision, text);

      case 669, 670, 671 -> {
        // All New Area Unlocked messages unlock the Ground Floor but check for it specifically in
        // case future changes unlock areas with message.
        if (text.contains("New Area Unlocked") && text.contains("The Ground Floor")) {
          Preferences.setInteger("lastCastleGroundUnlock", KoLCharacter.getAscensions());
          QuestDatabase.setQuestProgress(Quest.GARBAGE, "step8");
        }
      }

      case 675 -> {
        // Melon Collie and the Infinite Lameness
        if (ChoiceManager.lastDecision == 2) {
          ResultProcessor.removeItem(ItemPool.DRUM_N_BASS_RECORD);
        }
      }

      case 679 -> // Keep On Turnin' the Wheel in the Sky
      QuestDatabase.setQuestProgress(Quest.GARBAGE, "step10");

      case 689 -> {
        // The Final Reward
        if (text.contains("claim your rightful reward")) {
          // Daily Dungeon Complete
          Preferences.setBoolean("dailyDungeonDone", true);
          Preferences.setInteger("_lastDailyDungeonRoom", 15);
        }
      }

      case 690, 691 -> {
        // The First Chest Isn't the Deepest and Second Chest
        if (ChoiceManager.lastDecision == 2) {
          Preferences.increment("_lastDailyDungeonRoom", 3);
        } else {
          Preferences.increment("_lastDailyDungeonRoom", 1);
        }
        // Second Chest only
        if (ChoiceManager.lastChoice == 691 && ChoiceManager.lastDecision == 4) {
          Preferences.setBoolean("candyCaneSwordDailyDungeon", true);
        }
      }

      case 692 -> {
        // I Wanna Be a Door
        if (text.contains("key breaks off in the lock")) {
          // Unfortunately, the key breaks off in the lock.
          ResultProcessor.processItem(ItemPool.SKELETON_KEY, -1);
        }
        if (ChoiceManager.lastDecision != 8) {
          Preferences.increment("_lastDailyDungeonRoom", 1);
        }
      }

      case 693 -> {
        // It's Almost Certainly a Trap
        if (ChoiceManager.lastDecision != 3) {
          Preferences.increment("_lastDailyDungeonRoom", 1);
        }
      }

      case 699 -> {
        // Lumber-related Pun
        if (text.contains("hand him the branch")) {
          // Marty's eyes widen when you hand him the
          // branch from the Great Tree.
          ResultProcessor.processItem(ItemPool.GREAT_TREE_BRANCH, -1);
        } else if (text.contains("hand him the rust")) {
          // At first Marty looks disappointed when you
          // hand him the rust-spotted, rotten-handled
          // axe, but after a closer inspection he gives
          // an impressed whistle.
          ResultProcessor.processItem(ItemPool.PHIL_BUNION_AXE, -1);
        } else if (text.contains("hand him the bouquet")) {
          // Marty looks delighted when you hand him the
          // bouquet of swamp roses.
          ResultProcessor.processItem(ItemPool.SWAMP_ROSE_BOUQUET, -1);
        }
      }

      case 700 -> {
        // Delirium in the Cafeterium
        Preferences.increment("_kolhsAdventures", 1);
      }

      case 703 -> {
        // Mer-kin dreadscroll
        if (text.contains("I guess you're the Mer-kin High Priest now")) {
          Preferences.setBoolean("isMerkinHighPriest", true);
          // The following is not applicable in the Sea path
          Preferences.setString("merkinQuestPath", "scholar");
          ResultProcessor.processItem(ItemPool.DREADSCROLL, -1);
        } else {
          DreadScrollManager.recordFailure(urlString, text);
        }
      }

      case 709 -> {
        // You Beat Shub to a Stub, Bub
        Preferences.setBoolean("shubJigguwattDefeated", true);
        // The following is not applicable in the Sea path
        Preferences.setString("merkinQuestPath", "done");
      }

      case 713 -> {
        // You Brought Her To Her Kn-kn-kn-kn-knees, Knees.
        Preferences.setBoolean("yogUrtDefeated", true);
        // The following is not applicable in the Sea path
        Preferences.setString("merkinQuestPath", "done");
      }
      case 717 -> {
        // Over. Over Now.
        Preferences.setString("merkinQuestPath", "done");
      }

      case 720 -> {
        // The Florist Friar's Cottage
        FloristRequest.parseResponse(urlString, text);
      }

      case 721 -> {
        // The Cabin in the Dreadsylvanian Woods
        if (ChoiceManager.lastDecision == 3) {
          // Try the Attic

          // You use your skeleton key to unlock the padlock on the attic trap door.
          // Then you use your legs to climb the ladder into the attic.
          // Then you use your stupidity to lose the skeleton key.  Crap.

          if (text.contains("lose the skeleton key")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.DREADSYLVANIAN_SKELETON_KEY, -1));
          }
        } else if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.GHOST_PENCIL, -1));
        }
      }

      case 722 -> {
        // The Kitchen in the Woods
        if (ChoiceManager.lastDecision == 2) {
          // Screw around with the flour mill
          if (text.contains("You acquire")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.OLD_DRY_BONE, -1));
          }
        }
      }

      case 723 -> {
        // What Lies Beneath (the Cabin)
        if (ChoiceManager.lastDecision == 3) {
          // Check out the lockbox
          if (text.contains("You acquire")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.REPLICA_KEY, -1));
          }
        } else if (ChoiceManager.lastDecision == 4) {
          // Stick a wax banana in the lock
          if (text.contains("You acquire")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.WAX_BANANA, -1));
          }
        }
      }

      case 725 -> {
        // Tallest Tree in the Forest
        if (ChoiceManager.lastDecision == 2) {
          // Check out the fire tower

          // You climb the rope ladder and use your skeleton key to
          // unlock the padlock on the door leading into the little room
          // at the top of the watchtower. Then you accidentally drop
          // your skeleton key and lose it in a pile of leaves. Rats.

          if (text.contains("you accidentally drop your skeleton key")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.DREADSYLVANIAN_SKELETON_KEY, -1));
          }
        } else if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.GHOST_PENCIL, -1));
        }
      }

      case 729 -> {
        // Below the Roots
        if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.GHOST_PENCIL, -1));
        }
      }

      case 730 -> {
        // Hot Coals
        if (ChoiceManager.lastDecision == 3) {
          // Melt down an old ball and chain
          if (text.contains("You acquire")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.OLD_BALL_AND_CHAIN, -1));
          }
        }
      }

      case 733 -> {
        // Dreadsylvanian Village Square
        if (ChoiceManager.lastDecision == 1) {
          // The schoolhouse

          // You try the door of the schoolhouse, but it's locked. You
          // try your skeleton key in the lock, but it works. I mean and
          // it works. But it breaks. That was the but.

          if (text.contains("But it breaks")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.DREADSYLVANIAN_SKELETON_KEY, -1));
          }
        } else if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.GHOST_PENCIL, -1));
        }
      }

      case 737 -> {
        // The Even More Dreadful Part of Town
        if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.GHOST_PENCIL, -1));
        }
      }

      case 739 -> {
        // The Tinker's. Damn.
        if (ChoiceManager.lastDecision == 2) {
          // Make a key using the wax lock impression
          if (text.contains("You acquire")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.WAX_LOCK_IMPRESSION, -1));
            ResultProcessor.processResult(ItemPool.get(ItemPool.INTRICATE_MUSIC_BOX_PARTS, -1));
          }
        } else if (ChoiceManager.lastDecision == 3) {
          // Polish the moon-amber
          if (text.contains("You acquire")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.MOON_AMBER, -1));
          }
        } else if (ChoiceManager.lastDecision == 4) {
          // Assemble a clockwork bird
          if (text.contains("You acquire")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.DREADSYLVANIAN_CLOCKWORK_KEY, -1));
            ResultProcessor.processResult(ItemPool.get(ItemPool.INTRICATE_MUSIC_BOX_PARTS, -3));
          }
        }
      }

      case 741 -> {
        // The Old Duke's Estate
        if (ChoiceManager.lastDecision == 3) {
          // Make your way to the master suite

          // You find the door to the old Duke's master bedroom and
          // unlock it with your skeleton key.

          if (text.contains("unlock it with your skeleton key")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.DREADSYLVANIAN_SKELETON_KEY, -1));
          }
        } else if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.GHOST_PENCIL, -1));
        }
      }

      case 743 -> {
        // No Quarter
        if (ChoiceManager.lastDecision == 2) {
          // Make a shepherd's pie
          if (text.contains("You acquire")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.DREAD_TARRAGON, -1));
            ResultProcessor.processResult(ItemPool.get(ItemPool.BONE_FLOUR, -1));
            ResultProcessor.processResult(ItemPool.get(ItemPool.DREADFUL_ROAST, -1));
            ResultProcessor.processResult(ItemPool.get(ItemPool.STINKING_AGARICUS, -1));
          }
        }
      }

      case 744 -> {
        // The Master Suite -- Sweet!
        if (ChoiceManager.lastDecision == 3) {
          // Mess with the loom
          if (text.contains("You acquire")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.GHOST_THREAD, -10));
          }
        }
      }

      case 745 -> {
        // This Hall is Really Great
        if (ChoiceManager.lastDecision == 1) {
          // Head to the ballroom

          // You unlock the door to the ballroom with your skeleton
          // key. You open the door, and are so impressed by the site of
          // the elegant ballroom that you drop the key down a nearby
          // laundry chute.

          if (text.contains("you drop the key")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.DREADSYLVANIAN_SKELETON_KEY, -1));
          }
        } else if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.GHOST_PENCIL, -1));
        }
      }

      case 746 -> {
        // The Belle of the Ballroom
        if (ChoiceManager.lastDecision == 2) {
          // Trip the light fantastic

          // You twirl around on the dance floor to music only you can
          // hear, your muddy skirt whirling around you filthily. You get
          // so caught up in the twirling that you drop your seed pod. It
          // breaks open, spreading weed seeds all over your skirt, which
          // immediately take root and grow.

          if (text.contains("spreading weed seeds all over your skirt")) {
            EquipmentManager.discardEquipment(ItemPool.MUDDY_SKIRT);
            EquipmentManager.setEquipment(Slot.PANTS, ItemPool.get(ItemPool.WEEDY_SKIRT, 1));
          }
        }
      }

      case 749 -> {
        // Tower Most Tall
        if (ChoiceManager.lastDecision == 1) {
          // Go to the laboratory

          // You use your skeleton key to unlock the door to the
          // laboratory. Unfortunately, the lock is electrified, and it
          // incinerates the key shortly afterwards.

          if (text.contains("it incinerates the key")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.DREADSYLVANIAN_SKELETON_KEY, -1));
          }
        } else if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.GHOST_PENCIL, -1));
        }
      }

      case 750 -> {
        // Working in the Lab, Late One Night
        if (ChoiceManager.lastDecision == 4) {
          // Use the still
          if (text.contains("You acquire")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.BLOOD_KIWI, -1));
            ResultProcessor.processResult(ItemPool.get(ItemPool.EAU_DE_MORT, -1));
          }
        }
      }

      case 753 -> {
        // The Dreadsylvanian Dungeon
        if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.GHOST_PENCIL, -1));
        }
      }

      case 762 -> {
        // Try New Extra-Strength Anvil
        // All of the options that craft something use the same ingredients
        if (text.contains("You acquire")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.COOL_IRON_INGOT, -1));
          ResultProcessor.processResult(ItemPool.get(ItemPool.WARM_FUR, -1));
        }
      }

      case 772 -> {
        // Saved by the Bell
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("_kolhsSchoolSpirited", true);
          Preferences.increment("kolhsTotalSchoolSpirited", 1);
        } else if (ChoiceManager.lastDecision == 2) {
          Preferences.setBoolean("_kolhsPoeticallyLicenced", true);
        } else if (ChoiceManager.lastDecision == 3) {
          // You walk into the Yearbook Club and collar the kid with all
          // the camera equipment from yesterday. "Let me check your
          // memory card," he says, plugging the camera into a computer.
          // "Yup! You got it! Nice work. Here's your reward -- a nice
          // new accessory for that camera! If you're interested, now we
          // need a picture of a <b>monster</b>. You up for it?"
          //
          // You walk back into the Yearbook Club, a little tentatively.
          // "All right! Let's see what you've got!" the camera kid
          // says, and plugs your camera into a computer. "Aw, man, you
          // didn't get it? Well, I'll give you another chance.  If you
          // can still get us a picture of a <b>monster</b> and bring it
          // in tomorrow, you're still in the Club."
          //
          // You poke your head into the Yearbook Club room, but the
          // camera kid's packing up all the equipment and putting it
          // away. "Sorry, gotta go," he says, "but remember, you've
          // gotta get a picture of a <b>monster</b> for tomorrow, all
          // right? We're counting on you."

          if (text.contains("You got it!")) {
            Preferences.setString("yearbookCameraTarget", "");
            Preferences.setBoolean("yearbookCameraPending", false);
            Preferences.increment("yearbookCameraUpgrades", 1, 20, false);
            if (KoLCharacter.getAscensions()
                != Preferences.getInteger("lastYearbookCameraAscension")) {
              Preferences.setInteger("lastYearbookCameraAscension", KoLCharacter.getAscensions());
              Preferences.increment("yearbookCameraAscensions", 1, 20, false);
            }
          }

          Matcher matcher = YEARBOOK_TARGET_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("yearbookCameraTarget", matcher.group(1));
          }
        } else if (ChoiceManager.lastDecision == 4) {
          Preferences.setBoolean("_kolhsCutButNotDried", true);
        } else if (ChoiceManager.lastDecision == 5) {
          Preferences.setBoolean("_kolhsIsskayLikeAnAshtray", true);
        }
        if (ChoiceManager.lastDecision != 8) {
          Preferences.increment("_kolhsSavedByTheBell", 1);
        }
      }

      case 778 -> {
        // If You Could Only See
        if (ChoiceManager.lastDecision != 6) {
          Preferences.setBoolean("_tonicDjinn", true);
          if (!text.contains("already had a wish today")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.TONIC_DJINN, -1));
          }
        }
      }

      case 780 -> {
        // Action Elevator
        if (ChoiceManager.lastDecision == 1 && text.contains("penthouse is empty now")) {
          if (Preferences.getInteger("hiddenApartmentProgress") < 7) {
            Preferences.setInteger("hiddenApartmentProgress", 7);
          }
        } else if (ChoiceManager.lastDecision == 3) {
          Preferences.setInteger("relocatePygmyLawyer", KoLCharacter.getAscensions());
        } else if (ChoiceManager.lastDecision == 4) {
          Preferences.setBoolean("candyCaneSwordApartmentBuilding", true);
        }
      }

      case 781 -> {
        // Earthbound and Down
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setInteger("hiddenApartmentProgress", 1);
          QuestDatabase.setQuestProgress(Quest.CURSES, QuestDatabase.STARTED);
        } else if (ChoiceManager.lastDecision == 2) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.MOSS_COVERED_STONE_SPHERE, -1));
          Preferences.setInteger("hiddenApartmentProgress", 8);
        } else if (ChoiceManager.lastDecision == 3) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.SIX_BALL, -1));
        }
      }

      case 783 -> {
        // Water You Dune
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setInteger("hiddenHospitalProgress", 1);
          QuestDatabase.setQuestProgress(Quest.DOCTOR, QuestDatabase.STARTED);
        } else if (ChoiceManager.lastDecision == 2) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.DRIPPING_STONE_SPHERE, -1));
          Preferences.setInteger("hiddenHospitalProgress", 8);
        } else if (ChoiceManager.lastDecision == 3) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.TWO_BALL, -1));
        }
      }

      case 785 -> {
        // Air Apparent
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setInteger("hiddenOfficeProgress", 1);
          QuestDatabase.setQuestProgress(Quest.BUSINESS, QuestDatabase.STARTED);
        } else if (ChoiceManager.lastDecision == 2) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.CRACKLING_STONE_SPHERE, -1));
          Preferences.setInteger("hiddenOfficeProgress", 8);
        } else if (ChoiceManager.lastDecision == 3) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.ONE_BALL, -1));
        } else if (ChoiceManager.lastDecision == 4) {
          Preferences.setBoolean("_candyCaneSwordOvergrownShrine", true);
        }
      }

      case 786 -> {
        // Working Holiday
        if (ChoiceManager.lastDecision == 1 && text.contains("boss's office is empty")) {
          if (Preferences.getInteger("hiddenOfficeProgress") < 7) {
            Preferences.setInteger("hiddenOfficeProgress", 7);
          }
        }
        // if you don't get the expected binder clip, don't have one, and don't have a mcclusky
        // file, you must have unlocked the boss at least
        else if (ChoiceManager.lastDecision == 2
            && !text.contains("boring binder clip")
            && InventoryManager.getCount(MCCLUSKY_FILE) == 0
            && InventoryManager.getCount(BINDER_CLIP) == 0
            && Preferences.getInteger("hiddenOfficeProgress") < 6) {
          Preferences.setInteger("hiddenOfficeProgress", 6);
        }
      }

      case 787 -> {
        // Fire when Ready
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setInteger("hiddenBowlingAlleyProgress", 1);
          QuestDatabase.setQuestProgress(Quest.SPARE, QuestDatabase.STARTED);
        } else if (ChoiceManager.lastDecision == 2) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.SCORCHED_STONE_SPHERE, -1));
          Preferences.setInteger("hiddenBowlingAlleyProgress", 8);
        } else if (ChoiceManager.lastDecision == 3) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.FIVE_BALL, -1));
        }
      }

      case 788 -> {
        // Life is Like a Cherry of Bowls
        if (ChoiceManager.lastDecision == 1
            && text.contains("without a frustrated ghost to torment")) {
          if (Preferences.getInteger("hiddenBowlingAlleyProgress") < 7) {
            Preferences.setInteger("hiddenBowlingAlleyProgress", 7);
          }
        }
        if (ChoiceManager.lastDecision == 1) {
          ResultProcessor.removeItem(ItemPool.BOWLING_BALL);
        }
        if (ChoiceManager.lastDecision == 1 || ChoiceManager.lastDecision == 2) {
          int bowlCount = Preferences.getInteger("hiddenBowlingAlleyProgress");
          if (bowlCount < 6) {
            Preferences.setInteger(
                "hiddenBowlingAlleyProgress", (bowlCount < 2 ? 2 : bowlCount + 1));
          }
        }
        if (ChoiceManager.lastDecision == 2) {
          Preferences.setBoolean("candyCaneSwordBowlingAlley", true);
        }
      }

      case 789 -> {
        // Where Does The Lone Ranger Take His Garbagester?
        if (ChoiceManager.lastDecision == 2) {
          Preferences.setInteger("relocatePygmyJanitor", KoLCharacter.getAscensions());
        }
      }

      case 793 -> {
        // Welcome to The Shore, Inc.
        if (ChoiceManager.lastDecision == 5) {
          Preferences.setBoolean("candyCaneSwordShore", true);
        }
      }

      case 801 -> {
        // A Reanimated Conversation
        if (ChoiceManager.lastDecision == 6) {
          Preferences.setInteger("reanimatorArms", 0);
          Preferences.setInteger("reanimatorLegs", 0);
          Preferences.setInteger("reanimatorSkulls", 0);
          Preferences.setInteger("reanimatorWeirdParts", 0);
          Preferences.setInteger("reanimatorWings", 0);
        }
      }

      case 805 -> {
        // A Sietch in Time
        int gnasirProgress = Preferences.getInteger("gnasirProgress");

        // Annoyingly, the option numbers change as you turn
        // things in. Therefore, we must look at response text

        if (text.contains("give the stone rose to Gnasir")) {
          ResultProcessor.removeItem(ItemPool.STONE_ROSE);
          gnasirProgress |= 1;
          Preferences.setInteger("gnasirProgress", gnasirProgress);
        } else if (text.contains("hold up the bucket of black paint")) {
          ResultProcessor.removeItem(ItemPool.BLACK_PAINT);
          gnasirProgress |= 2;
          Preferences.setInteger("gnasirProgress", gnasirProgress);
        } else if (text.contains("hand Gnasir the glass jar")) {
          ResultProcessor.removeItem(ItemPool.KILLING_JAR);
          gnasirProgress |= 4;
          Preferences.setInteger("gnasirProgress", gnasirProgress);
        }
        // You hand him the pages, and he shuffles them into their correct order and inspects them
        // carefully.
        else if (text.contains("hand him the pages")) {
          ResultProcessor.processItem(ItemPool.WORM_RIDING_MANUAL_PAGE, -15);
          gnasirProgress |= 8;
          Preferences.setInteger("gnasirProgress", gnasirProgress);
        }
      }

      case 812 -> {
        if (ChoiceManager.lastDecision == 1) {
          Matcher matcher = UNPERM_PATTERN.matcher(text);
          if (matcher.find()) {
            KoLCharacter.removeAvailableSkill(matcher.group(1));
            Preferences.increment("bankedKarma", Integer.parseInt(matcher.group(2)));
          }
        }
      }
      case 821 -> {
        // LP-ROM burner
        if (ChoiceManager.lastDecision == 1) {
          HaciendaManager.parseRecording(urlString, text);
        }
      }

      case 835 -> {
        // Barely Tales
        if (ChoiceManager.lastDecision != 0) {
          Preferences.setBoolean("_grimBuff", true);
        }
      }

      case 836 -> {
        // Adventures Who Live in Ice Houses...
        if (ChoiceManager.lastDecision == 1) {
          BanishManager.removeBanishByBanisher(BanishManager.Banisher.ICE_HOUSE);
        }
      }

      case 851 -> {
        // Shen Copperhead, Nightclub Owner
        QuestDatabase.setQuestProgress(Quest.SHEN, "step1");
        Preferences.setInteger("shenInitiationDay", KoLCharacter.getCurrentDays());
        if (Preferences.getString("shenQuestItem").isEmpty()) {
          // We didn't recognise quest text before accepting quest, so get it from quest log
          RequestThread.postRequest(new QuestLogRequest());
        }
      }

      case 852,
          // Shen Copperhead, Jerk
          853,
          // Shen Copperhead, Huge Jerk
          854 -> {
        // Shen Copperhead, World's Biggest Jerk
        QuestDatabase.advanceQuest(Quest.SHEN);
        if (ChoiceManager.lastChoice == 854) {
          Preferences.setString("shenQuestItem", "");
        } else {
          Matcher matcher = SHEN_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("shenQuestItem", matcher.group(1));
          }
        }

        // You will have exactly one of these items to ger rid of
        ResultProcessor.removeItem(ItemPool.FIRST_PIZZA);
        ResultProcessor.removeItem(ItemPool.LACROSSE_STICK);
        ResultProcessor.removeItem(ItemPool.EYE_OF_THE_STARS);
        ResultProcessor.removeItem(ItemPool.STANKARA_STONE);
        ResultProcessor.removeItem(ItemPool.MURPHYS_FLAG);
        ResultProcessor.removeItem(ItemPool.SHIELD_OF_BROOK);
      }

      case 872 -> {
        // Drawn Onward - Handle quest in Ed
        if (text.contains("Rot in a jar of dog paws!")) {
          QuestDatabase.setQuestProgress(Quest.PALINDOME, QuestDatabase.FINISHED);
          ResultProcessor.removeItem(ItemPool.ED_FATS_STAFF);
          if (InventoryManager.getCount(ItemPool.ED_EYE) == 0
              && InventoryManager.getCount(ItemPool.ED_AMULET) == 0) {
            QuestDatabase.setQuestProgress(Quest.MACGUFFIN, QuestDatabase.FINISHED);
          }
        }
      }

      case 876 -> {
        // One Simple Nightstand
        if (ChoiceManager.lastDecision == 4) {
          Preferences.setBoolean("_candyCaneSwordHauntedBedroom", true);
        }
      }

      case 882 -> {
        // Off the Rack
        if (ChoiceManager.lastDecision == 1
            && text.contains("You never know when it might come in handy.")) {
          Preferences.setInteger("lastTowelAscension", KoLCharacter.getAscensions());
        }
      }

      case 888 -> {
        // Take a Look, it's in a Book! (Rise)
        if (ChoiceManager.lastDecision == 4) {
          Preferences.setBoolean("_candyCaneSwordHauntedLibrary", true);
        }
      }

      case 890 -> {
        // Lights Out in the Storage Room
        if (text.contains("BUT AIN'T NO ONE CAN GET A STAIN OUT LIKE OLD AGNES!")
            && !Preferences.getString("nextSpookyravenElizabethRoom").equals("none")) {
          Preferences.setString("nextSpookyravenElizabethRoom", "The Haunted Laundry Room");
        }
      }

      case 891 -> {
        // Lights Out in the Laundry Room
        if (text.contains("DO YOU SEE THE STAIN UPON MY TOWEL?")
            && !Preferences.getString("nextSpookyravenElizabethRoom").equals("none")) {
          Preferences.setString("nextSpookyravenElizabethRoom", "The Haunted Bathroom");
        }
      }

      case 892 -> {
        // Lights Out in the Bathroom
        if (text.contains("THE STAIN HAS BEEN LIFTED")
            && !Preferences.getString("nextSpookyravenElizabethRoom").equals("none")) {
          Preferences.setString("nextSpookyravenElizabethRoom", "The Haunted Kitchen");
        }
      }

      case 893 -> {
        // Lights Out in the Kitchen
        if (text.contains("If You Give a Demon a Brownie")
            && !Preferences.getString("nextSpookyravenElizabethRoom").equals("none")) {
          Preferences.setString("nextSpookyravenElizabethRoom", "The Haunted Library");
        }
      }

      case 894 -> {
        // Lights Out in the Library
        if (text.contains("If You Give a Demon a Brownie")
            && !Preferences.getString("nextSpookyravenElizabethRoom").equals("none")) {
          Preferences.setString("nextSpookyravenElizabethRoom", "The Haunted Ballroom");
        }
      }

      case 895 -> {
        // Lights Out in the Ballroom
        if (text.contains("The Flowerbed of Unearthly Delights")
            && !Preferences.getString("nextSpookyravenElizabethRoom").equals("none")) {
          Preferences.setString("nextSpookyravenElizabethRoom", "The Haunted Gallery");
        }
      }

      case 896 -> {
        // Lights Out in the Gallery

        // The correct option leads to a combat with Elizabeth.
        // If you win, we will set "nextSpookyravenElizabethRoom" to "none"
      }

      case 897 -> {
        // Lights Out in the Bedroom
        if (text.contains("restock his medical kit in the nursery")
            && !Preferences.getString("nextSpookyravenStephenRoom").equals("none")) {
          Preferences.setString("nextSpookyravenStephenRoom", "The Haunted Nursery");
        }
      }

      case 898 -> {
        // Lights Out in the Nursery
        if (text.contains("This afternoon we're burying Crumbles")
            && !Preferences.getString("nextSpookyravenStephenRoom").equals("none")) {
          Preferences.setString("nextSpookyravenStephenRoom", "The Haunted Conservatory");
        }
      }

      case 899 -> {
        // Lights Out in the Conservatory
        if (text.contains("Crumbles isn't buried very deep")
            && !Preferences.getString("nextSpookyravenStephenRoom").equals("none")) {
          Preferences.setString("nextSpookyravenStephenRoom", "The Haunted Billiards Room");
        }
      }

      case 900 -> {
        // Lights Out in the Billiards Room
        if (text.contains("The wolf head has a particularly nasty expression on its face")
            && !Preferences.getString("nextSpookyravenStephenRoom").equals("none")) {
          Preferences.setString("nextSpookyravenStephenRoom", "The Haunted Wine Cellar");
        }
      }

      case 901 -> {
        // Lights Out in the Wine Cellar
        if (text.contains("Crumbles II (Wolf)")
            && !Preferences.getString("nextSpookyravenStephenRoom").equals("none")) {
          Preferences.setString("nextSpookyravenStephenRoom", "The Haunted Boiler Room");
        }
      }

      case 902 -> {
        // Lights Out in the Boiler Room
        if (text.contains("CRUMBLES II")
            && !Preferences.getString("nextSpookyravenStephenRoom").equals("none")) {
          Preferences.setString("nextSpookyravenStephenRoom", "The Haunted Laboratory");
        }
      }

      case 903 -> {
        // Lights Out in the Laboratory

        // The correct option leads to a combat with Stephen.
        // If you win, we will set "nextSpookyravenStephenRoom" to "none"
      }

      case 915 -> {
        // Choice 915 is Et Tu, Buff Jimmy?
        if (text.contains("skinny mushroom girls")) {
          QuestDatabase.setQuestProgress(Quest.JIMMY_MUSHROOM, QuestDatabase.STARTED);
        } else if (text.contains(
            "But here's a few Beach Bucks as a token of my changes in gratitude")) {
          QuestDatabase.setQuestProgress(Quest.JIMMY_MUSHROOM, QuestDatabase.FINISHED);
          ResultProcessor.processItem(ItemPool.PENCIL_THIN_MUSHROOM, -10);
        } else if (text.contains("not really into moving out of this hammock")) {
          QuestDatabase.setQuestProgress(Quest.JIMMY_CHEESEBURGER, QuestDatabase.STARTED);
          Preferences.setInteger("buffJimmyIngredients", 0);
        } else if (text.contains("So I'll just give you some Beach Bucks instead")) {
          QuestDatabase.setQuestProgress(Quest.JIMMY_CHEESEBURGER, QuestDatabase.FINISHED);
          Preferences.setInteger("buffJimmyIngredients", 0);
          ResultProcessor.processItem(ItemPool.CHEESEBURGER_RECIPE, -1);
        } else if (text.contains("sons of sons of sailors are")) {
          QuestDatabase.setQuestProgress(Quest.JIMMY_SALT, QuestDatabase.STARTED);
        } else if (text.contains("So here's some Beach Bucks instead")) {
          QuestDatabase.setQuestProgress(Quest.JIMMY_SALT, QuestDatabase.FINISHED);
          ResultProcessor.processItem(ItemPool.SAILOR_SALT, -50);
        }
      }
      case 916 -> {
        // Choice 916 is Taco Dan's Taco Stand's Taco Dan
        if (text.contains("find those receipts")) {
          QuestDatabase.setQuestProgress(Quest.TACO_DAN_AUDIT, QuestDatabase.STARTED);
        } else if (text.contains("Here's a little Taco Dan's Taco Stand gratitude for ya")) {
          QuestDatabase.setQuestProgress(Quest.TACO_DAN_AUDIT, QuestDatabase.FINISHED);
          ResultProcessor.processItem(ItemPool.TACO_DAN_RECEIPT, -10);
        } else if (text.contains("fill it up with as many cocktail drippings")) {
          QuestDatabase.setQuestProgress(Quest.TACO_DAN_COCKTAIL, QuestDatabase.STARTED);
          Preferences.setInteger("tacoDanCocktailSauce", 0);
        } else if (text.contains("sample of Taco Dan's Taco Stand's Tacoriffic Cocktail Sauce")) {
          QuestDatabase.setQuestProgress(Quest.TACO_DAN_COCKTAIL, QuestDatabase.FINISHED);
          Preferences.setInteger("tacoDanCocktailSauce", 0);
          ResultProcessor.processItem(ItemPool.TACO_DAN_SAUCE_BOTTLE, -1);
        } else if (text.contains("get enough taco fish")) {
          QuestDatabase.setQuestProgress(Quest.TACO_DAN_FISH, QuestDatabase.STARTED);
          Preferences.setInteger("tacoDanFishMeat", 0);
        } else if (text.contains("batch of those Taco Dan's Taco Stand's Taco Fish Tacos")) {
          QuestDatabase.setQuestProgress(Quest.TACO_DAN_FISH, QuestDatabase.FINISHED);
          Preferences.setInteger("tacoDanFishMeat", 0);
        }
      }
      case 917 -> {
        // Choice 917 is Do You Even Brogurt
        if (text.contains("need about ten shots of it")) {
          QuestDatabase.setQuestProgress(Quest.BRODEN_BACTERIA, QuestDatabase.STARTED);
          Preferences.setInteger("brodenBacteria", 0);
        } else if (text.contains("YOLO cup to spit the bacteria into")) {
          QuestDatabase.setQuestProgress(Quest.BRODEN_BACTERIA, QuestDatabase.FINISHED);
          Preferences.setInteger("brodenBacteria", 0);
        } else if (text.contains("loan you my sprinkle shaker to fill up")) {
          QuestDatabase.setQuestProgress(Quest.BRODEN_SPRINKLES, QuestDatabase.STARTED);
          Preferences.setInteger("brodenSprinkles", 0);
        } else if (text.contains("can sell some <i>deluxe</i> brogurts")) {
          QuestDatabase.setQuestProgress(Quest.BRODEN_SPRINKLES, QuestDatabase.FINISHED);
          Preferences.setInteger("brodenSprinkles", 0);
          ResultProcessor.processItem(ItemPool.SPRINKLE_SHAKER, -1);
        } else if (text.contains("There were like fifteen of these guys")) {
          QuestDatabase.setQuestProgress(Quest.BRODEN_DEBT, QuestDatabase.STARTED);
        } else if (text.contains("And they all had broupons, huh")) {
          QuestDatabase.setQuestProgress(Quest.BRODEN_DEBT, QuestDatabase.FINISHED);
          ResultProcessor.processItem(ItemPool.BROUPON, -15);
        }
      }
      case 918 -> {
        // Yachtzee!
        if (ChoiceManager.lastDecision == 3 && text.contains("You open the captain's door")) {
          int beads = Math.min(InventoryManager.getCount(ItemPool.MOIST_BEADS), 100);
          ResultProcessor.processResult(ItemPool.get(ItemPool.MOIST_BEADS, -beads));
        }
      }
      case 919 -> {
        // Choice 919 is Break Time!
        if (ChoiceManager.lastDecision == 1) {
          if (text.contains("You've already thoroughly")) {
            Preferences.setInteger("_sloppyDinerBeachBucks", 4);
          } else {
            Preferences.increment("_sloppyDinerBeachBucks", 1);
          }
        }
      }
      case 920 -> {
        // Choice 920 is Eraser
        if (ChoiceManager.lastDecision == 1) {
          QuestDatabase.setQuestProgress(Quest.JIMMY_MUSHROOM, QuestDatabase.UNSTARTED);
          QuestDatabase.setQuestProgress(Quest.JIMMY_CHEESEBURGER, QuestDatabase.UNSTARTED);
          QuestDatabase.setQuestProgress(Quest.JIMMY_SALT, QuestDatabase.UNSTARTED);
        } else if (ChoiceManager.lastDecision == 2) {
          QuestDatabase.setQuestProgress(Quest.TACO_DAN_AUDIT, QuestDatabase.UNSTARTED);
          QuestDatabase.setQuestProgress(Quest.TACO_DAN_COCKTAIL, QuestDatabase.UNSTARTED);
          QuestDatabase.setQuestProgress(Quest.TACO_DAN_FISH, QuestDatabase.UNSTARTED);
        } else if (ChoiceManager.lastDecision == 3) {
          QuestDatabase.setQuestProgress(Quest.BRODEN_BACTERIA, QuestDatabase.UNSTARTED);
          QuestDatabase.setQuestProgress(Quest.BRODEN_SPRINKLES, QuestDatabase.UNSTARTED);
          QuestDatabase.setQuestProgress(Quest.BRODEN_DEBT, QuestDatabase.UNSTARTED);
        }
        if (ChoiceManager.lastDecision != 4) {
          ResultProcessor.processItem(ItemPool.MIND_DESTROYER, -1);
        }
      }

      case 923 -> {
        // All Over the Map
        if (ChoiceManager.lastDecision == 5) {
          Preferences.setBoolean("candyCaneSwordBlackForest", true);
        }
      }

      case 930 -> {
        // Another Errand I Mean Quest
        if (ChoiceManager.lastDecision == 1) {
          QuestDatabase.setQuestIfBetter(Quest.CITADEL, QuestDatabase.STARTED);
        }
      }

      case 932 -> {
        // No Whammies
        if (text.contains("steel your nerves for what lies ahead")) {
          QuestDatabase.setQuestProgress(Quest.CITADEL, "step9");
        }
      }

      case 967 -> {
        // The Thunder Rolls...
        if (ChoiceManager.lastDecision != 8) {
          ResultProcessor.removeItem(ItemPool.THUNDER_THIGH);
        }
      }

      case 968 -> {
        // The Rain Falls Down With Your Help...
        if (ChoiceManager.lastDecision != 8) {
          ResultProcessor.removeItem(ItemPool.AQUA_BRAIN);
        }
      }

      case 969 -> {
        // And The Lightning Strikes...
        if (ChoiceManager.lastDecision != 8) {
          ResultProcessor.removeItem(ItemPool.LIGHTNING_MILK);
        }
      }

      case 970 -> {
        // Rainy Fax Dreams on your Wedding Day
        if (ChoiceManager.lastDecision == 1) {
          EncounterManager.ignoreSpecialMonsters();
          KoLAdventure.clearLocation();
          GenericRequest.itemMonster = "Rain Man";
        }
      }

      case 984 -> {
        // A Radio on a Beach
        // Clear quests when accepting a new one as you can only have one
        if (text.contains("your best paramilitary-sounding radio lingo")) {
          QuestDatabase.setQuestProgress(Quest.JUNGLE_PUN, QuestDatabase.UNSTARTED);
          QuestDatabase.setQuestProgress(Quest.GORE, QuestDatabase.UNSTARTED);
          QuestDatabase.setQuestProgress(Quest.CLIPPER, QuestDatabase.UNSTARTED);
        }
        // Also clear repeatable quests if there is no quest active at the radio
        if (text.contains("Maybe try again tomorrow")) {
          QuestDatabase.setQuestProgress(Quest.JUNGLE_PUN, QuestDatabase.UNSTARTED);
          QuestDatabase.setQuestProgress(Quest.GORE, QuestDatabase.UNSTARTED);
          QuestDatabase.setQuestProgress(Quest.CLIPPER, QuestDatabase.UNSTARTED);
        }
        // EVE quest started
        else if (text.contains("navigation protocol")) {
          QuestDatabase.setQuestProgress(Quest.EVE, QuestDatabase.STARTED);
          Preferences.setString("EVEDirections", "LLRLR0");
        }
        // EVE quest finished
        else if (text.contains("a tiny parachute")) {
          QuestDatabase.setQuestProgress(Quest.EVE, QuestDatabase.FINISHED);
          Preferences.resetToDefault("EVEDirections");
        }
        // Jungle Pun quest finished (start handled in ResultProcessor)
        else if (text.contains(
            "tape recorder self-destructs with a shower of sparks and a puff of smoke")) {
          EquipmentManager.discardEquipment(ItemPool.MINI_CASSETTE_RECORDER);
          ResultProcessor.removeItem(ItemPool.MINI_CASSETTE_RECORDER);
          QuestDatabase.setQuestProgress(Quest.JUNGLE_PUN, QuestDatabase.UNSTARTED);
          Preferences.resetToDefault("junglePuns");
        }
        // Gore quest finished (start handled in ResultProcessor)
        else if (text.contains("bucket came from")) {
          EquipmentManager.discardEquipment(ItemPool.GORE_BUCKET);
          ResultProcessor.removeItem(ItemPool.GORE_BUCKET);
          QuestDatabase.setQuestProgress(Quest.GORE, QuestDatabase.UNSTARTED);
          Preferences.resetToDefault("goreCollected");
        }
        // Clipper quest finished (start handled in ResultProcessor)
        else if (text.contains("return the fingernails and the clippers")) {
          ResultProcessor.removeItem(ItemPool.FINGERNAIL_CLIPPERS);
          QuestDatabase.setQuestProgress(Quest.CLIPPER, QuestDatabase.UNSTARTED);
          Preferences.resetToDefault("fingernailsClipped");
        }
        // Fake Medium quest started
        else if (text.contains("maximal discretion")) {
          QuestDatabase.setQuestProgress(Quest.FAKE_MEDIUM, QuestDatabase.STARTED);
        }
        // Fake Medium quest finished
        else if (text.contains("toss the device into the ocean")) {
          ResultProcessor.removeItem(ItemPool.ESP_COLLAR);
          QuestDatabase.setQuestProgress(Quest.FAKE_MEDIUM, QuestDatabase.FINISHED);
        }
        // Serum quest started
        else if (text.contains("wonder how many vials they want")) {
          if (InventoryManager.getCount(ItemPool.EXPERIMENTAL_SERUM_P00) >= 5) {
            QuestDatabase.setQuestProgress(Quest.SERUM, "step1");
          } else {
            QuestDatabase.setQuestProgress(Quest.SERUM, QuestDatabase.STARTED);
          }
        }
        // Serum quest finished
        else if (text.contains("drop the vials into it")) {
          ResultProcessor.processItem(ItemPool.EXPERIMENTAL_SERUM_P00, -5);
          QuestDatabase.setQuestProgress(Quest.SERUM, QuestDatabase.FINISHED);
        }
        // Smokes quest started
        else if (text.contains("acquire cigarettes")) {
          QuestDatabase.setQuestProgress(Quest.SMOKES, QuestDatabase.STARTED);
        }
        // Smokes quest finished
        else if (text.contains("cigarettes with a grappling gun")) {
          QuestDatabase.setQuestProgress(Quest.SMOKES, QuestDatabase.FINISHED);
        }
        // Out of Order quest finished
        else if (text.contains("takes your nifty new watch")) {
          EquipmentManager.discardEquipment(ItemPool.GPS_WATCH);
          ResultProcessor.removeItem(ItemPool.GPS_WATCH);
          ResultProcessor.removeItem(ItemPool.PROJECT_TLB);
          QuestDatabase.setQuestProgress(Quest.OUT_OF_ORDER, QuestDatabase.FINISHED);
        }
        // Can't parse quest due to static so visit quest log
        else {
          RequestThread.postRequest(new QuestLogRequest());
        }
      }

      case 986 -> {
        // Control Panel
        if (ChoiceManager.lastDecision >= 1 && ChoiceManager.lastDecision <= 9) {
          Preferences.setBoolean("_controlPanelUsed", true);
          if (!text.contains("minimum of 24 hours")) {
            Preferences.increment("controlPanelOmega", 11, 100, false);
          }
        }
      }
      case 987 -> {
        // The Post-Apocalyptic Survivor Encampment
        if (!text.contains("accept your donation")) {
          break;
        }
        int qty = -1;
        if (urlString.contains("giveten")) {
          qty = -10;
        }
        // Declare the pattern here instead of globally because this is available infrequently
        Pattern encampmentPattern = Pattern.compile("whichfood=(\\d+)");
        Matcher encampmentMatcher = encampmentPattern.matcher(urlString);
        if (encampmentMatcher.find()) {
          int encampmentId = StringUtilities.parseInt(encampmentMatcher.group(1));
          ResultProcessor.processItem(encampmentId, qty);
        }
      }

      case 988 -> {
        // The Containment Unit
        String containment = Preferences.getString("EVEDirections");
        if (containment.length() != 6) {
          break;
        }
        if (text.contains("another pair of doors")) {
          int progress = StringUtilities.parseInt(containment.substring(5, 6));
          if (progress < 0 && progress > 4) {
            break;
          }
          progress++;
          Preferences.setString("EVEDirections", containment.substring(0, 5) + progress);
        } else {
          Preferences.setString("EVEDirections", containment.substring(0, 5) + "0");
        }
      }

      case 991 -> {
        // Build a Crimbot!
        if (ChoiceManager.lastDecision == 1) {
          ResultProcessor.removeItem(ItemPool.CRIMBONIUM_FUEL_ROD);
        }
      }

      case 993 -> {
        // Tales of Spelunking
        if (ChoiceManager.lastDecision == 1) {
          KoLCharacter.enterLimitmode(LimitMode.SPELUNKY);
        }
      }

      case 994 -> {
        // Hide a gift!
        if (text.contains("You hide")) {
          HashMap<Integer, Integer> idMap = new HashMap<>(3);
          HashMap<Integer, Integer> qtyMap = new HashMap<>(3);
          int index;
          int id;
          int giftQty;

          Matcher idMatcher = ITEMID_PATTERN.matcher(urlString);
          while (idMatcher.find()) {
            index = StringUtilities.parseInt(idMatcher.group(1));
            if (index < 1) continue;
            id = StringUtilities.parseInt(idMatcher.group(2));
            if (id < 1) continue;
            idMap.put(index, id);
          }

          Matcher qtyMatcher = QTY_PATTERN.matcher(urlString);
          while (qtyMatcher.find()) {
            index = StringUtilities.parseInt(qtyMatcher.group(1));
            if (index < 1) continue;
            giftQty = StringUtilities.parseInt(qtyMatcher.group(2));
            if (giftQty < 1) continue;
            qtyMap.put(index, giftQty);
          }

          for (int i = 1; i <= 3; i++) {
            Integer itemId = idMap.get(i);
            Integer giftQuantity = qtyMap.get(i);
            if (itemId == null || giftQuantity == null) continue;
            ResultProcessor.processResult(ItemPool.get(itemId, -giftQuantity));
          }
          ResultProcessor.removeItem(ItemPool.SNEAKY_WRAPPING_PAPER);
        }
      }

      case 998 -> {
        if (text.contains("confiscate your deuces")) {
          int removeDeuces = ChoiceManager.lastDecision - 1;
          ResultProcessor.processResult(ItemPool.get(ItemPool.SLEEVE_DEUCE, removeDeuces));
          break;
        }
        if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.removeItem(ItemPool.POCKET_ACE);
        }
      }

      case 999 -> {
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("_shrubDecorated", true);
          Pattern topperPattern = Pattern.compile("topper=(\\d)");
          Pattern lightsPattern = Pattern.compile("lights=(\\d)");
          Pattern garlandPattern = Pattern.compile("garland=(\\d)");
          Pattern giftPattern = Pattern.compile("gift=(\\d)");
          int decoration;

          Matcher matcher = topperPattern.matcher(urlString);
          if (matcher.find()) {
            decoration = StringUtilities.parseInt(matcher.group(1));
            switch (decoration) {
              case 1 -> Preferences.setString("shrubTopper", "Muscle");
              case 2 -> Preferences.setString("shrubTopper", "Mysticality");
              case 3 -> Preferences.setString("shrubTopper", "Moxie");
            }
          }

          matcher = lightsPattern.matcher(urlString);
          if (matcher.find()) {
            decoration = StringUtilities.parseInt(matcher.group(1));
            switch (decoration) {
              case 1 -> Preferences.setString("shrubLights", "prismatic");
              case 2 -> Preferences.setString("shrubLights", "Hot");
              case 3 -> Preferences.setString("shrubLights", "Cold");
              case 4 -> Preferences.setString("shrubLights", "Stench");
              case 5 -> Preferences.setString("shrubLights", "Spooky");
              case 6 -> Preferences.setString("shrubLights", "Sleaze");
            }
          }

          matcher = garlandPattern.matcher(urlString);
          if (matcher.find()) {
            decoration = StringUtilities.parseInt(matcher.group(1));
            switch (decoration) {
              case 1 -> Preferences.setString("shrubGarland", "HP");
              case 2 -> Preferences.setString("shrubGarland", "PvP");
              case 3 -> Preferences.setString("shrubGarland", "blocking");
            }
          }

          matcher = giftPattern.matcher(urlString);
          if (matcher.find()) {
            decoration = StringUtilities.parseInt(matcher.group(1));
            switch (decoration) {
              case 1 -> Preferences.setString("shrubGifts", "yellow");
              case 2 -> Preferences.setString("shrubGifts", "meat");
              case 3 -> Preferences.setString("shrubGifts", "gifts");
            }
          }
        }
      }

      case 1002 -> {
        // Temple of the Legend in the Hidden City - Handle quest in Ed
        if (text.contains("The spectre nods emphatically")) {
          QuestDatabase.setQuestProgress(Quest.WORSHIP, QuestDatabase.FINISHED);
          ResultProcessor.removeItem(ItemPool.ED_AMULET);
          if (InventoryManager.getCount(ItemPool.ED_EYE) == 0
              && InventoryManager.getCount(ItemPool.ED_FATS_STAFF) == 0) {
            QuestDatabase.setQuestProgress(Quest.MACGUFFIN, QuestDatabase.FINISHED);
          }
        }
      }

      case 1003 -> // Test Your Might And Also Test Other Things
      SorceressLairManager.parseContestBooth(ChoiceManager.lastDecision, text);

      case 1005, // 'Allo
          1008, // Pooling Your Resources
          1011 -> // Of Mouseholes and Manholes
      SorceressLairManager.parseMazeTrap(ChoiceManager.lastChoice, text);

      case 1013 -> // Mazel Tov!
      // Then you both giggle and head through the exit at the same time.
      QuestDatabase.setQuestProgress(Quest.FINAL, "step5");

      case 1015 -> // The Mirror in the Tower has the View that is True
      QuestDatabase.setQuestProgress(Quest.FINAL, "step10");

      case 1022 -> // Meet Frank
      // Frank bobs his head toward the hedge maze in front of you.
      QuestDatabase.setQuestProgress(Quest.FINAL, "step4");

      case 1023 -> { // Like a Bat Into Hell
        if (ChoiceManager.lastDecision == 1) {
          KoLCharacter.setLimitMode(LimitMode.ED);
        }
      }

      case 1027 -> { // The End of the Tale of Spelunking
        if (ChoiceManager.lastDecision == 1) {
          // Remove all virtual items from inventory/tally
          SpelunkyRequest.resetItems();
        }
      }

      case 1028, // A Shop
          1029, // An Old Clay Pot
          1030, // It's a Trap!  A Dart Trap.
          1031, // A Tombstone
          1032, // It's a Trap!  A Tiki Trap.
          1033, // A Big Block of Ice
          1034, // A Landmine
          1035, // A Crate
          1036, // Idolatry
          1037, // It's a Trap!  A Smashy Trap.
          1038, // A Wicked Web
          1039, // A Golden Chest
          1040, // It's Lump. It's Lump.
          1041, // Spelunkrifice
          1045 -> // Hostile Work Environment
      SpelunkyRequest.parseChoice(ChoiceManager.lastChoice, text, ChoiceManager.lastDecision);

      case 1042 -> // Pick a Perk
      SpelunkyRequest.upgrade(ChoiceManager.lastDecision);

      case 1044 -> {
        // The Gates of Hell
        if (text.contains("unlock the padlock")) {
          SpelunkyRequest.unlock("Hell", "Hell");
        }
      }

      case 1049 -> {
        // Tomb of the Unknown Your Class Here
        if (text.contains("Also in this room is a ghost")) {
          QuestDatabase.setQuestProgress(Quest.NEMESIS, "step1");
        } else if (text.contains("You acquire")) {
          AscensionClass ascensionClass = KoLCharacter.getAscensionClass();
          int starterWeaponId = ascensionClass == null ? -1 : ascensionClass.getStarterWeapon();
          ResultProcessor.processItem(starterWeaponId, -1);
          QuestDatabase.setQuestProgress(Quest.NEMESIS, "step4");
        }
      }

      case 1052 -> {
        // Underworld Body Shop
        Matcher skillidMatcher = URL_SKILLID_PATTERN.matcher(urlString);
        if (skillidMatcher.find()) {
          int cost =
              switch (StringUtilities.parseInt(skillidMatcher.group(1))) {
                case 30 -> 5;
                case 31, 36, 39, 40, 43, 44 -> 10;
                case 32 -> 15;
                case 33, 37, 38, 41, 42, 45, 48 -> 20;
                case 34 -> 25;
                case 28, 29, 35, 46 -> 30;
                default -> 0;
              };
          ResultProcessor.processResult(ItemPool.get(ItemPool.KA_COIN, -cost));
        }
      }

      case 1053 -> // The Servants' Quarters
      EdServantData.manipulateServants(request, text);

      case 1056 -> {
        // Now It's Dark
        // Twin Peak fourth choice
        if (text.contains("When the lights come back")) {
          // the other three must be completed at this point.
          Preferences.setInteger("twinPeakProgress", 15);
        }
      }

      case 1057 -> {
        // A Stone Shrine
        if (text.contains("shatter the")) {
          KoLCharacter.setHippyStoneBroken(true);
        }
      }

      case 1059 -> {
        // Helping Make Ends Meat
        if (text.contains("excitedly takes the check")) {
          QuestDatabase.setQuestProgress(Quest.MEATSMITH, QuestDatabase.FINISHED);
          ResultProcessor.removeItem(ItemPool.MEATSMITH_CHECK);
        } else if (text.contains("skeleton store is right next door")
            || text.contains("I'll be here")) {
          QuestDatabase.setQuestProgress(Quest.MEATSMITH, QuestDatabase.STARTED);
          Preferences.setBoolean("skeletonStoreAvailable", true);
        }
      }

      case 1060 -> {
        // Temporarily Out of Skeletons
        if (text.contains("it snaps off")) {
          ResultProcessor.removeItem(ItemPool.SKELETON_KEY);
        }
      }

      case 1061 -> {
        // Heart of Madness
        if (ChoiceManager.lastDecision == 1) {
          QuestDatabase.setQuestIfBetter(Quest.ARMORER, "step1");
        }
        if (text.contains("place the popular part")) {
          ResultProcessor.removeItem(ItemPool.POPULAR_PART);
          Preferences.setBoolean("popularTartUnlocked", true);
        }
      }

      case 1062 -> {
        // Lots of Options
        if (ChoiceManager.lastDecision == 6) {
          Preferences.setBoolean("_candyCaneSwordOvergrownLot", true);
        }
      }

      case 1064 -> {
        // The Doctor is Out.  Of Herbs.
        if (ChoiceManager.lastDecision == 1) {
          QuestDatabase.setQuestProgress(Quest.DOC, QuestDatabase.STARTED);
          Preferences.setBoolean("overgrownLotAvailable", true);
        } else if (ChoiceManager.lastDecision == 2) {
          QuestDatabase.setQuestProgress(Quest.DOC, QuestDatabase.FINISHED);
          ResultProcessor.processResult(ItemPool.get(ItemPool.FRAUDWORT, -3));
          ResultProcessor.processResult(ItemPool.get(ItemPool.SHYSTERWEED, -3));
          ResultProcessor.processResult(ItemPool.get(ItemPool.SWINDLEBLOSSOM, -3));
          HPRestoreItemList.updateHealthRestored();
          MPRestoreItemList.updateManaRestored();
        }
      }

      case 1065 -> {
        // Lending a Hand (and a Foot)
        if (ChoiceManager.lastDecision == 1) {
          QuestDatabase.setQuestProgress(Quest.ARMORER, QuestDatabase.STARTED);
          Preferences.setBoolean("madnessBakeryAvailable", true);
        } else if (ChoiceManager.lastDecision == 3) {
          QuestDatabase.setQuestIfBetter(Quest.ARMORER, QuestDatabase.STARTED);
        }
      }

      case 1066 -> {
        // Employee Assignment Kiosk
        if (text.contains("Performance Review:  Sufficient")) {
          EquipmentManager.discardEquipment(ItemPool.TRASH_NET);
          ResultProcessor.removeItem(ItemPool.TRASH_NET);
          Preferences.setInteger("dinseyFilthLevel", 0);
          QuestDatabase.setQuestProgress(Quest.FISH_TRASH, QuestDatabase.UNSTARTED);
        } else if (text.contains("Performance Review:  Unobjectionable")) {
          ResultProcessor.processItem(ItemPool.TOXIC_GLOBULE, -20);
          QuestDatabase.setQuestProgress(Quest.GIVE_ME_FUEL, QuestDatabase.UNSTARTED);
        } else if (text.contains("Performance Review:  Bearable")) {
          Preferences.setInteger("dinseyNastyBearsDefeated", 0);
          QuestDatabase.setQuestProgress(Quest.NASTY_BEARS, QuestDatabase.UNSTARTED);
        } else if (text.contains("Performance Review:  Acceptable")) {
          Preferences.setInteger("dinseySocialJusticeIProgress", 0);
          QuestDatabase.setQuestProgress(Quest.SOCIAL_JUSTICE_I, QuestDatabase.UNSTARTED);
        } else if (text.contains("Performance Review:  Fair")) {
          Preferences.setInteger("dinseySocialJusticeIIProgress", 0);
          QuestDatabase.setQuestProgress(Quest.SOCIAL_JUSTICE_II, QuestDatabase.UNSTARTED);
        } else if (text.contains("Performance Review:  Average")) {
          EquipmentManager.discardEquipment(ItemPool.LUBE_SHOES);
          ResultProcessor.removeItem(ItemPool.LUBE_SHOES);
          QuestDatabase.setQuestProgress(Quest.SUPER_LUBER, QuestDatabase.UNSTARTED);
        } else if (text.contains("Performance Review:  Adequate")) {
          Preferences.setInteger("dinseyTouristsFed", 0);
          ResultProcessor.removeItem(ItemPool.DINSEY_REFRESHMENTS);
          QuestDatabase.setQuestProgress(Quest.WORK_WITH_FOOD, QuestDatabase.UNSTARTED);
        } else if (text.contains("Performance Review:  Tolerable")) {
          EquipmentManager.discardEquipment(ItemPool.MASCOT_MASK);
          ResultProcessor.removeItem(ItemPool.MASCOT_MASK);
          Preferences.setInteger("dinseyFunProgress", 0);
          QuestDatabase.setQuestProgress(Quest.ZIPPITY_DOO_DAH, QuestDatabase.UNSTARTED);
        } else if (text.contains("weren't kidding about the power")) {
          if (InventoryManager.getCount(ItemPool.TOXIC_GLOBULE) >= 20) {
            QuestDatabase.setQuestProgress(Quest.GIVE_ME_FUEL, "step1");
          } else {
            QuestDatabase.setQuestProgress(Quest.GIVE_ME_FUEL, QuestDatabase.STARTED);
          }
        } else if (text.contains("anatomical diagram of a nasty bear")) {
          QuestDatabase.setQuestProgress(Quest.NASTY_BEARS, QuestDatabase.STARTED);
        } else if (text.contains("lists all of the sexist aspects of the ride")) {
          QuestDatabase.setQuestProgress(Quest.SOCIAL_JUSTICE_I, QuestDatabase.STARTED);
        } else if (text.contains("ideas are all themselves so racist")) {
          QuestDatabase.setQuestProgress(Quest.SOCIAL_JUSTICE_II, QuestDatabase.STARTED);
        } else if (text.contains("box of snacks issues forth")) {
          Preferences.setInteger("dinseyTouristsFed", 0);
          QuestDatabase.setQuestProgress(Quest.WORK_WITH_FOOD, QuestDatabase.STARTED);
        }
      }

      case 1067 -> {
        // Maint Misbehavin'
        if (text.contains("throw a bag of garbage into it")) {
          ResultProcessor.processItem(ItemPool.GARBAGE_BAG, -1);
          Preferences.setBoolean("_dinseyGarbageDisposed", true);
        }
      }

      case 1076 -> {
        // Mayo Minder&trade;
        switch (ChoiceManager.lastDecision) {
          case 1 -> Preferences.setString("mayoMinderSetting", "Mayonex");
          case 2 -> Preferences.setString("mayoMinderSetting", "Mayodiol");
          case 3 -> Preferences.setString("mayoMinderSetting", "Mayostat");
          case 4 -> Preferences.setString("mayoMinderSetting", "Mayozapine");
          case 5 -> Preferences.setString("mayoMinderSetting", "Mayoflex");
          case 6 -> Preferences.setString("mayoMinderSetting", "");
        }
      }

      case 1080 -> {
        // Bagelmat-5000
        if (text.contains("shove a wad of dough into the slot")) {
          ResultProcessor.removeItem(ItemPool.DOUGH);
        }
        if (ChoiceManager.lastDecision == 2) {
          Preferences.setBoolean("_candyCaneSwordMadnessBakery", true);
        }
      }

      case 1081 -> {
        // Assault and Baguettery
        if (ChoiceManager.lastDecision == 1
            || ChoiceManager.lastDecision == 2
            || ChoiceManager.lastDecision == 3) {
          ResultProcessor.removeItem(ItemPool.MAGICAL_BAGUETTE);
        }
      }

      case 1084 -> {
        // The Popular Machine
        if (text.contains("popular tart springs")) {
          ResultProcessor.removeItem(ItemPool.DOUGH);
          ResultProcessor.removeItem(ItemPool.STRAWBERRY);
          ResultProcessor.removeItem(ItemPool.ENCHANTED_ICING);
        }
      }

      case 1085, 1086 -> {
        // The Deck of Every Card
        DeckOfEveryCardRequest.postChoice1(text);
      }

      case 1087 -> {
        // The Dark and Dank and Sinister Cave Entrance
        if (text.contains("stumpy-legged mushroom creatures")) {
          QuestDatabase.setQuestProgress(Quest.NEMESIS, "step12");
        }
      }

      case 1088 -> {
        // Rubble, Rubble, Toil and Trouble
        if (text.contains("BOOOOOOM!")) {
          ResultProcessor.processItem(ItemPool.FIZZING_SPORE_POD, -6);
          QuestDatabase.setQuestProgress(Quest.NEMESIS, "step15");
        }
      }

      case 1103 -> {
        {
          int result =
              NumberologyManager.numberology(StringUtilities.parseInt(request.getFormField("num")));
          if (NumberologyManager.numberologyPrize(result).startsWith("fight")) {
            KoLAdventure.clearLocation();
          }
        }
      }

      case 1118 -> {
        // X-32-F Combat Training Snowman Control Console
        switch (ChoiceManager.lastDecision) {
          case 1 -> Preferences.setString("snojoSetting", "MUSCLE");
          case 2 -> Preferences.setString("snojoSetting", "MYSTICALITY");
          case 3 -> Preferences.setString("snojoSetting", "MOXIE");
          case 4 -> Preferences.setString("snojoSetting", "TOURNAMENT");
        }
      }

      case 1120, 1121, 1122, 1123 -> // Some Assembly Required
      VYKEACompanionData.assembleCompanion(
          ChoiceManager.lastChoice, ChoiceManager.lastDecision, text);

      case 1133 -> {
        // Batfellow Begins
        if (ChoiceManager.lastDecision == 1) {
          KoLCharacter.enterLimitmode(LimitMode.BATMAN);
        }
      }

      case 1137 -> // Bat-Suit Upgrades
      BatManager.batSuitUpgrade(ChoiceManager.lastDecision, text);

      case 1138 -> // Bat-Sedan Upgrades
      BatManager.batSedanUpgrade(ChoiceManager.lastDecision, text);

      case 1139 -> // Bat-Cavern Upgrades
      BatManager.batCavernUpgrade(ChoiceManager.lastDecision, text);

      case 1140 -> {
        // Casing the Conservatory
        if (ChoiceManager.lastDecision == 4) {
          ResultProcessor.removeItem(ItemPool.GLOB_OF_BAT_GLUE);
        } else if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.removeItem(ItemPool.FINGERPRINT_DUSTING_KIT);
          ResultProcessor.removeItem(ItemPool.FINGERPRINT_DUSTING_KIT);
          ResultProcessor.removeItem(ItemPool.FINGERPRINT_DUSTING_KIT);
        }
      }

      case 1141 -> {
        // Researching the Reservoir
        if (ChoiceManager.lastDecision == 4) {
          ResultProcessor.removeItem(ItemPool.BAT_AID_BANDAGE);
        } else if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.removeItem(ItemPool.ULTRACOAGULATOR);
          ResultProcessor.removeItem(ItemPool.ULTRACOAGULATOR);
          ResultProcessor.removeItem(ItemPool.ULTRACOAGULATOR);
        }
      }

      case 1142 -> {
        // Combing the Cemetery
        if (ChoiceManager.lastDecision == 4) {
          ResultProcessor.removeItem(ItemPool.BAT_BEARING);
        } else if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.removeItem(ItemPool.EXPLODING_KICKBALL);
          ResultProcessor.removeItem(ItemPool.EXPLODING_KICKBALL);
          ResultProcessor.removeItem(ItemPool.EXPLODING_KICKBALL);
        }
      }

      case 1143 -> {
        // Searching the Sewers
        if (ChoiceManager.lastDecision == 4) {
          ResultProcessor.removeItem(ItemPool.BAT_OOMERANG);
        }
      }

      case 1144 -> {
        // Assessing the Asylum
        if (ChoiceManager.lastDecision == 4) {
          ResultProcessor.removeItem(ItemPool.BAT_O_MITE);
        }
      }

      case 1145 -> {
        // Looking over the Library
        if (ChoiceManager.lastDecision == 4) {
          ResultProcessor.removeItem(ItemPool.BAT_JUTE);
        }
      }

      case 1146 -> {
        // Considering the Clock Factory
        if (ChoiceManager.lastDecision == 4) {
          ResultProcessor.removeItem(ItemPool.EXPLODING_KICKBALL);
        }
      }

      case 1147 -> {
        // Frisking the Foundry
        if (ChoiceManager.lastDecision == 4) {
          ResultProcessor.removeItem(ItemPool.ULTRACOAGULATOR);
        }
      }

      case 1148 -> {
        // Taking Stock of the Trivia Company
        if (ChoiceManager.lastDecision == 4) {
          ResultProcessor.removeItem(ItemPool.FINGERPRINT_DUSTING_KIT);
        }
      }

      case 1176 -> {
        // Go West, Young Adventurer!
        if (ChoiceManager.lastDecision == 3) {
          // Snake Oilers start with extra
          Preferences.setInteger("awolMedicine", 3);
          Preferences.setInteger("awolVenom", 3);
        }
      }

      case 1182 -> {
        // Play against the Witchess Pieces
        if (ChoiceManager.lastDecision == 1) {
          KoLAdventure.clearLocation();
          GenericRequest.itemMonster = "Your Witchess Set";
        }
      }

      case 1190 -> {
        // The Oracle
        if (ChoiceManager.lastDecision == 2) {
          ResultProcessor.removeItem(ItemPool.NO_SPOON);
          QuestDatabase.setQuestProgress(Quest.ORACLE, QuestDatabase.UNSTARTED);
          Preferences.increment("sourceEnlightenment");
          Preferences.setString("sourceOracleTarget", "");
        } else if (ChoiceManager.lastDecision <= 3) {
          QuestDatabase.setQuestProgress(Quest.ORACLE, QuestDatabase.STARTED);
          Matcher matcher = ORACLE_QUEST_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("sourceOracleTarget", matcher.group(1));
          }
        }
      }

      case 1191 -> {
        // Source Terminal
        request.setHasResult(false);
        if (ChoiceManager.lastDecision != 1) {
          break;
        }
        String input = request.getFormField("input");
        if (input == null) {
          break;
        }
        handleSourceTerminal(input, text);
      }

      case 1195 -> {
        // Spinning Your Time-Spinner
        if (ChoiceManager.lastDecision == 3) {
          Preferences.increment("_timeSpinnerMinutesUsed");
        } else if (ChoiceManager.lastDecision == 4) {
          Preferences.increment("_timeSpinnerMinutesUsed", 2);
        }
      }

      case 1196 -> {
        // Travel to a Recent Fight
        if (ChoiceManager.lastDecision == 1 && !urlString.contains("monid=0")) {
          Preferences.increment("_timeSpinnerMinutesUsed", 3);
          EncounterManager.ignoreSpecialMonsters();
        }
      }

      case 1215 -> {
        // Setting the Clock
        Preferences.setBoolean("_gingerbreadClockVisited", true);

        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("_gingerbreadClockAdvanced", true);
          Preferences.increment("_gingerbreadCityTurns");
        }
      }

      case 1217 -> {
        // Sweet Synthesis
        SweetSynthesisRequest.postChoice1(urlString, text);
      }

      case 1222 -> {
        // The Tunnel of L.O.V.E.
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("_loveTunnelUsed", true);
        }
      }

      case 1229 -> {
        // L.O.V. Exit

        Matcher matcher = LOV_LOGENTRY_PATTERN.matcher(text);
        if (matcher.find()) {
          String message = "Your log entry: " + matcher.group(1);
          RequestLogger.printLine(message);
          RequestLogger.updateSessionLog(message);
        }
      }

      case 1231 -> {
        // Gummi-Memories, In the Corner of Your Mind
        if (ChoiceManager.lastDecision == 1) {
          ResultProcessor.removeItem(ItemPool.GUMMY_MEMORY);
          Preferences.increment("noobDeferredPoints", 5);
        }
      }

      case 1234 -> {
        // Spacegate Vaccinator
        //
        // option 1 = Rainbow Vaccine
        // option 2 = Broad-Spectrum Vaccine
        // option 3 = Emotional Vaccine
        //
        // You can unlock it (by turning in enough research) or
        // Select it (if previously unlocked).
        //
        if (text.contains("New vaccine unlocked!")) {
          Preferences.setBoolean("spacegateVaccine" + ChoiceManager.lastDecision, true);
        } else if (text.contains("You acquire an effect")) {
          Preferences.setBoolean("_spacegateVaccine", true);
        }
      }

      case 1235 -> {
        // Spacegate Terminal
        QuestManager.parseSpacegateTerminal(text, true);
      }

      case 1246 -> {
        // Land Ho
        if (ChoiceManager.lastDecision == 1) {
          parseLanguageFluency(text, "spacePirateLanguageFluency");
        }
      }

      case 1247 -> {
        // Half the Ship it used to Be
        if (ChoiceManager.lastDecision == 1) {
          if (text.contains("You acquire an item")) {
            Preferences.setInteger("spacePirateLanguageFluency", 0);
          } else {
            parseLanguageFluency(text, "spacePirateLanguageFluency");
          }
        }
      }

      case 1248 -> {
        // Paradise Under a Strange Sun
        if (text.contains("You acquire an item")) {
          ResultProcessor.removeItem(ItemPool.SPACE_PIRATE_TREASURE_MAP);
        }
      }

      case 1249 -> {
        // That's No Monolith, it's a Monolith
        if (ChoiceManager.lastDecision == 1) {
          parseLanguageFluency(text, "procrastinatorLanguageFluency");
          ResultProcessor.removeItem(ItemPool.MURDERBOT_DATA_CORE);
        }
      }

      case 1250 -> {
        // I'm Afraid It's Terminal
        if (ChoiceManager.lastDecision == 1) {
          if (text.contains("You acquire an item")) {
            Preferences.setInteger("procrastinatorLanguageFluency", 0);
          }
        }
      }

      case 1251 -> {
        // Curses, A Hex
        if (text.contains("You acquire an item")) {
          ResultProcessor.removeItem(ItemPool.PROCRASTINATOR_LOCKER_KEY);
        }
      }

      case 1252 -> {
        // Time Enough at Last
        if (ChoiceManager.lastDecision == 1) {
          // You get a Space Baby children's book which
          // will grants spaceBabyLanguageFluency +10
          // when read
        }
      }

      case 1253 -> {
        // Mother May I
        if (ChoiceManager.lastDecision == 1) {
          if (text.contains("You acquire an item")) {
            Preferences.setInteger("spaceBabyLanguageFluency", 0);
          }
        }
      }

      case 1254 -> {
        // Please Baby Baby Please
        if (text.contains("You acquire an item")) {
          ResultProcessor.removeItem(ItemPool.SPACE_BABY_BAWBAW);
        }
      }

      case 1260 -> {
        // A Strange Panel
        if (ChoiceManager.lastDecision < 1 || ChoiceManager.lastDecision > 3) {
          break;
        }
        if (text.contains("10 casualties")
            || text.contains("10 crew")
            || text.contains("10 minions")
            || text.contains("10 ski")
            || text.contains("10 members")
            || text.contains("ten techs")
            || text.contains("10 soldiers")) {
          Preferences.increment("_villainLairProgress", 10);
        } else if (text.contains("5 casualties")
            || text.contains("5 souls")
            || text.contains("5 minions")
            || text.contains("five minions")
            || text.contains("group of ski")
            || text.contains("5 members")
            || text.contains("five people")
            || text.contains("five of us")) {
          Preferences.increment("_villainLairProgress", 5);
        } else {
          Preferences.decrement("_villainLairProgress", 7);
        }
        Preferences.setBoolean("_villainLairColorChoiceUsed", true);
      }

      case 1261 -> {
        // Which Door?
        if (ChoiceManager.lastDecision == 1) {
          if (text.contains("drop 1000")) {
            Preferences.increment("_villainLairProgress", 10);
            Preferences.setBoolean("_villainLairDoorChoiceUsed", true);
          }
        } else if (ChoiceManager.lastDecision == 2) {
          if (text.contains("insert the key")) {
            int itemId = -1;
            String key = Preferences.getString("_villainLairKey");
            if (key.equals("boris")) {
              itemId = ItemPool.BORIS_KEY;
            } else if (key.equals("jarlsberg")) {
              itemId = ItemPool.JARLSBERG_KEY;
            }
            if (key.equals("pete")) {
              itemId = ItemPool.SNEAKY_PETE_KEY;
            }
            ResultProcessor.removeItem(itemId);
            Preferences.increment("_villainLairProgress", 15);
            Preferences.setBoolean("_villainLairDoorChoiceUsed", true);
          }
        } else if (ChoiceManager.lastDecision == 3) {
          Preferences.decrement("_villainLairProgress", 13);
          Preferences.setBoolean("_villainLairDoorChoiceUsed", true);
        }
      }

      case 1262 -> {
        // What Setting?
        if (ChoiceManager.lastDecision < 1 || ChoiceManager.lastDecision > 3) {
          break;
        }
        if (text.contains("20 of the")
            || text.contains("20 minions")
            || text.contains("20 or so")
            || text.contains("20 soldiers")) {
          Preferences.increment("_villainLairProgress", 20);
        } else if (text.contains("10 or so")
            || text.contains("10 injured")
            || text.contains("10 patrol-sicles")
            || text.contains("10 soldiers")) {
          Preferences.increment("_villainLairProgress", 10);
        } else if (text.contains("15 aquanats")
            || text.contains("15 reserve")
            || text.contains("15 previously")
            || text.contains("15 Soldiers")) {
          Preferences.decrement("_villainLairProgress", 15);
        }
        Preferences.setBoolean("_villainLairSymbologyChoiceUsed", true);
      }

      case 1266 -> {
        // The Hostler
        if (ChoiceManager.lastDecision < 5) {
          Pattern pattern = Pattern.compile("You rent(|ed) the (.*?)!");
          Matcher matcher = pattern.matcher(text);
          if (matcher.find()) {
            String horse = matcher.group(2);
            Preferences.setString("_horsery", horse);
            String message = "Chose the " + horse;
            RequestLogger.printLine(message);
            RequestLogger.updateSessionLog(message);
            String setting =
                horse.equals("crazy horse")
                    ? "_horseryCrazyName"
                    : horse.equals("dark horse")
                        ? "_horseryDarkName"
                        : horse.equals("normal horse")
                            ? "_horseryNormalName"
                            : horse.equals("pale horse") ? "_horseryPaleName" : null;
            if (setting != null) {
              String name = Preferences.getString(setting);
              Preferences.setString("_horseryCurrentName", name);
            }
          }
        } else if (ChoiceManager.lastDecision == 5) {
          Preferences.setString("_horsery", "");
          Preferences.setString("_horseryCurrentName", "");
          String message = "Returned your horse";
          RequestLogger.printLine(message);
          RequestLogger.updateSessionLog(message);
        }
      }

      case 1270 -> {
        // Pantagramming
        // The item that we get has a procedurally-generated name
        request.setHasResult(false);
        PantogramRequest.parseResponse(urlString, text);
      }

      case 1271 -> // Mummery
      MummeryRequest.parseResponse(ChoiceManager.lastDecision, text);

      case 1275 -> {
        // Rummaging through the Garbage
        if (ChoiceManager.lastDecision >= 1 && ChoiceManager.lastDecision <= 5) {
          // Remove all of the items before parsing the newly-received one
          EquipmentManager.removeEquipment(ItemPool.DECEASED_TREE);
          ResultProcessor.removeItem(ItemPool.DECEASED_TREE);
          EquipmentManager.removeEquipment(ItemPool.BROKEN_CHAMPAGNE);
          ResultProcessor.removeItem(ItemPool.BROKEN_CHAMPAGNE);
          EquipmentManager.removeEquipment(ItemPool.TINSEL_TIGHTS);
          ResultProcessor.removeItem(ItemPool.TINSEL_TIGHTS);
          EquipmentManager.removeEquipment(ItemPool.WAD_OF_TAPE);
          ResultProcessor.removeItem(ItemPool.WAD_OF_TAPE);
          EquipmentManager.removeEquipment(ItemPool.MAKESHIFT_GARBAGE_SHIRT);
          ResultProcessor.removeItem(ItemPool.MAKESHIFT_GARBAGE_SHIRT);
          if (!Preferences.getBoolean("_garbageItemChanged")) {
            Preferences.setInteger("garbageTreeCharge", 1000);
            Preferences.setInteger("garbageChampagneCharge", 11);
            Preferences.setInteger("garbageShirtCharge", 37);
          }
          Preferences.setBoolean("_garbageItemChanged", true);
        }
        // Do some parsing of needles/wine/scraps here
        Matcher matcher = DECEASED_TREE_PATTERN.matcher(text);
        if (matcher.find()) {
          Preferences.setInteger("garbageTreeCharge", StringUtilities.parseInt(matcher.group(1)));
        }
        matcher = BROKEN_CHAMPAGNE_PATTERN.matcher(text);
        if (matcher.find()) {
          Preferences.setInteger(
              "garbageChampagneCharge", StringUtilities.parseInt(matcher.group(1)));
        }
        matcher = GARBAGE_SHIRT_PATTERN.matcher(text);
        if (matcher.find()) {
          Preferences.setInteger("garbageShirtCharge", StringUtilities.parseInt(matcher.group(1)));
        }
      }

      case 1277 -> {
        // Extra, Extra
        if (ChoiceManager.lastDecision >= 1
            && ChoiceManager.lastDecision <= 5
            && text.contains("You acquire")) {
          ResultProcessor.removeItem(ItemPool.BURNING_NEWSPAPER);
        }
      }

      case 1280 -> {
        // Welcome to FantasyRealm
        if (ChoiceManager.lastDecision != 6) {
          Preferences.setInteger("_frHoursLeft", 5);
          StringBuffer unlocks = new StringBuffer();
          unlocks.append("The Bandit Crossroads,");
          if (Preferences.getBoolean("frMountainsUnlocked")) {
            unlocks.append("The Towering Mountains,");
          }
          if (Preferences.getBoolean("frWoodUnlocked")) {
            unlocks.append("The Mystic Wood,");
          }
          if (Preferences.getBoolean("frSwampUnlocked")) {
            unlocks.append("The Putrid Swamp,");
          }
          if (Preferences.getBoolean("frVillageUnlocked")) {
            unlocks.append("The Cursed Village,");
          }
          if (Preferences.getBoolean("frCemetaryUnlocked")) {
            unlocks.append("The Sprawling Cemetery,");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
        }
      }

      case 1281 -> {
        // You'll See You at the Crossroads
        StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
        if (ChoiceManager.lastDecision != 6) {
          Preferences.decrement("_frHoursLeft");
          StringUtilities.singleStringReplace(unlocks, "The Bandit Crossroads,", "");
        }
        if (ChoiceManager.lastDecision == 1) {
          unlocks.append("The Towering Mountains,");
        }
        if (ChoiceManager.lastDecision == 2) {
          unlocks.append("The Mystic Wood,");
        }
        if (ChoiceManager.lastDecision == 3) {
          unlocks.append("The Putrid Swamp,");
        }
        if (ChoiceManager.lastDecision == 4) {
          unlocks.append("The Cursed Village,");
        }
        if (ChoiceManager.lastDecision == 5) {
          unlocks.append("The Sprawling Cemetery,");
        }
        Preferences.setString("_frAreasUnlocked", unlocks.toString());
      }

      case 1282 -> {
        // Out of Range
        StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
        if (ChoiceManager.lastDecision != 11) {
          Preferences.decrement("_frHoursLeft");
          StringUtilities.singleStringReplace(unlocks, "The Towering Mountains,", "");
        }
        if (ChoiceManager.lastDecision == 1) {
          unlocks.append("The Old Rubee Mine,");
          ResultProcessor.removeItem(ItemPool.FR_KEY);
        }
        if (ChoiceManager.lastDecision == 2) {
          unlocks.append("The Foreboding Cave,");
        }
        if (ChoiceManager.lastDecision == 3) {
          unlocks.append("The Master Thief's Chalet,");
        }
        if (ChoiceManager.lastDecision == 4) {
          ResultProcessor.removeItem(ItemPool.FR_DRUIDIC_ORB);
        }
        if (ChoiceManager.lastDecision == 5) {
          unlocks.append("The Ogre Chieftain's Keep,");
        }
        if (ChoiceManager.lastDecision == 10) {
          Preferences.increment("_frButtonsPressed");
        }
        Preferences.setString("_frAreasUnlocked", unlocks.toString());
      }

      case 1283 -> {
        // Where Wood You Like to Go
        StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
        if (ChoiceManager.lastDecision != 11) {
          Preferences.decrement("_frHoursLeft");
          StringUtilities.singleStringReplace(unlocks, "The Mystic Wood,", "");
        }
        if (ChoiceManager.lastDecision == 1) {
          unlocks.append("The Faerie Cyrkle,");
        }
        if (ChoiceManager.lastDecision == 2) {
          unlocks.append("The Druidic Campsite,");
        }
        if (ChoiceManager.lastDecision == 3) {
          unlocks.append("The Ley Nexus,");
        }
        if (ChoiceManager.lastDecision == 10) {
          Preferences.increment("_frButtonsPressed");
        }
        Preferences.setString("_frAreasUnlocked", unlocks.toString());
      }

      case 1284 -> {
        // Swamped with Leisure
        StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
        if (ChoiceManager.lastDecision != 11) {
          Preferences.decrement("_frHoursLeft");
          StringUtilities.singleStringReplace(unlocks, "The Putrid Swamp,", "");
        }
        if (ChoiceManager.lastDecision == 1) {
          unlocks.append("Near the Witch's House,");
        }
        if (ChoiceManager.lastDecision == 2) {
          unlocks.append("The Troll Fortress,");
          ResultProcessor.removeItem(ItemPool.FR_KEY);
        }
        if (ChoiceManager.lastDecision == 3) {
          unlocks.append("The Dragon's Moor,");
        }
        if (ChoiceManager.lastDecision == 10) {
          Preferences.increment("_frButtonsPressed");
        }
        Preferences.setString("_frAreasUnlocked", unlocks.toString());
      }

      case 1285 -> {
        // It Takes a Cursed Village
        StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
        if (ChoiceManager.lastDecision != 11) {
          Preferences.decrement("_frHoursLeft");
          StringUtilities.singleStringReplace(unlocks, "The Cursed Village,", "");
        }
        if (ChoiceManager.lastDecision == 1) {
          unlocks.append("The Evil Cathedral,");
        }
        if (ChoiceManager.lastDecision == 2) {
          unlocks.append("The Cursed Village Thieves' Guild,");
        }
        if (ChoiceManager.lastDecision == 3) {
          unlocks.append("The Archwizard's Tower,");
        }
        if (ChoiceManager.lastDecision == 6) {
          ResultProcessor.removeItem(ItemPool.FR_DRAGON_ORE);
        }
        if (ChoiceManager.lastDecision == 7) {
          ResultProcessor.removeItem(ItemPool.FR_ARREST_WARRANT);
        }
        if (ChoiceManager.lastDecision == 10) {
          Preferences.increment("_frButtonsPressed");
        }
        Preferences.setString("_frAreasUnlocked", unlocks.toString());
      }

      case 1286 -> {
        // Resting in Peace
        StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
        if (ChoiceManager.lastDecision != 11) {
          Preferences.decrement("_frHoursLeft");
          StringUtilities.singleStringReplace(unlocks, "The Sprawling Cemetery,", "");
        }
        if (ChoiceManager.lastDecision == 1) {
          unlocks.append("The Labyrinthine Crypt,");
        }
        if (ChoiceManager.lastDecision == 2) {
          unlocks.append("The Barrow Mounds,");
        }
        if (ChoiceManager.lastDecision == 3) {
          unlocks.append("Duke Vampire's Chateau,");
        }
        if (ChoiceManager.lastDecision == 10) {
          Preferences.increment("_frButtonsPressed");
        }
        Preferences.setString("_frAreasUnlocked", unlocks.toString());
      }

      case 1288 -> {
        // What's Yours is Yours
        StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
        if (ChoiceManager.lastDecision != 6) {
          Preferences.decrement("_frHoursLeft");
          StringUtilities.singleStringReplace(unlocks, "The Old Rubee Mine,", "");
        }
        Preferences.setString("_frAreasUnlocked", unlocks.toString());
      }

      case 1289 -> {
        // A Warm Place
        StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
        if (ChoiceManager.lastDecision != 6) {
          Preferences.decrement("_frHoursLeft");
          StringUtilities.singleStringReplace(unlocks, "The Foreboding Cave,", "");
        }
        if (ChoiceManager.lastDecision == 3) {
          unlocks.append("The Lair of the Phoenix,");
        }
        Preferences.setString("_frAreasUnlocked", unlocks.toString());
      }

      case 1290 -> {
        // The Cyrkle Is Compleat
        StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
        if (ChoiceManager.lastDecision != 6) {
          Preferences.decrement("_frHoursLeft");
          StringUtilities.singleStringReplace(unlocks, "The Faerie Cyrkle,", "");
        }
        if (ChoiceManager.lastDecision == 3) {
          unlocks.append("The Spider Queen's Lair,");
        }
        Preferences.setString("_frAreasUnlocked", unlocks.toString());
      }

      case 1291 -> {
        // Dudes, Where's My Druids?
        StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
        if (ChoiceManager.lastDecision != 6) {
          Preferences.decrement("_frHoursLeft");
          StringUtilities.singleStringReplace(unlocks, "The Druidic Campsite,", "");
        }
        if (ChoiceManager.lastDecision == 2) {
          ResultProcessor.removeItem(ItemPool.FR_TAINTED_MARSHMALLOW);
        }
        Preferences.setString("_frAreasUnlocked", unlocks.toString());
      }

      case 1292 -> {
        // Witch One You Want?
        StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
        if (ChoiceManager.lastDecision != 6) {
          Preferences.decrement("_frHoursLeft");
          StringUtilities.singleStringReplace(unlocks, "Near the Witch's House,", "");
        }
        if (ChoiceManager.lastDecision == 2) {
          ResultProcessor.removeItem(ItemPool.FR_PURPLE_MUSHROOM);
        }
        Preferences.setString("_frAreasUnlocked", unlocks.toString());
      }

      case 1293 -> {
        // Altared States
        StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
        if (ChoiceManager.lastDecision != 6) {
          Preferences.decrement("_frHoursLeft");
          StringUtilities.singleStringReplace(unlocks, "The Evil Cathedral,", "");
        }
        Preferences.setString("_frAreasUnlocked", unlocks.toString());
      }

      case 1294 -> {
        // Neither a Barrower Nor a Lender Be
        StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
        if (ChoiceManager.lastDecision != 6) {
          Preferences.decrement("_frHoursLeft");
          StringUtilities.singleStringReplace(unlocks, "The Barrow Mounds,", "");
        }
        if (ChoiceManager.lastDecision == 3) {
          unlocks.append("The Ghoul King's Catacomb,");
        }
        Preferences.setString("_frAreasUnlocked", unlocks.toString());
      }

      case 1295 -> {
        // Honor Among You
        StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
        if (ChoiceManager.lastDecision != 6) {
          Preferences.decrement("_frHoursLeft");
          StringUtilities.singleStringReplace(unlocks, "The Cursed Village Thieves' Guild,", "");
        }
        Preferences.setString("_frAreasUnlocked", unlocks.toString());
      }

      case 1296 -> {
        // For Whom the Bell Trolls
        StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
        if (ChoiceManager.lastDecision != 6) {
          Preferences.decrement("_frHoursLeft");
          StringUtilities.singleStringReplace(unlocks, "The Troll Fortress,", "");
        }
        if (ChoiceManager.lastDecision == 3) {
          ResultProcessor.removeItem(ItemPool.FR_CHESWICKS_NOTES);
        }
        Preferences.setString("_frAreasUnlocked", unlocks.toString());
      }

      case 1297 -> {
        // Stick to the Crypt
        StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
        if (ChoiceManager.lastDecision != 6) {
          Preferences.decrement("_frHoursLeft");
          StringUtilities.singleStringReplace(unlocks, "The Labyrinthine Crypt,", "");
        }
        Preferences.setString("_frAreasUnlocked", unlocks.toString());
      }

      case 1298 -> {
        // The "Phoenix"
        StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
        if (ChoiceManager.lastDecision != 6) {
          StringUtilities.singleStringReplace(unlocks, "The Lair of the Phoenix,", "");
        }
        if (ChoiceManager.lastDecision == 1) {
          ResultProcessor.removeItem(ItemPool.FR_HOLY_WATER);
        }
        Preferences.setString("_frAreasUnlocked", unlocks.toString());
      }

      case 1299 -> {
        // Stop Dragon Your Feet
        StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
        if (ChoiceManager.lastDecision != 6) {
          StringUtilities.singleStringReplace(unlocks, "The Dragon's Moor,", "");
        }
        Preferences.setString("_frAreasUnlocked", unlocks.toString());
      }

      case 1300 -> {
        // Just Vamping
        StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
        if (ChoiceManager.lastDecision != 6) {
          StringUtilities.singleStringReplace(unlocks, "Duke Vampire's Chateau,", "");
        }
        Preferences.setString("_frAreasUnlocked", unlocks.toString());
      }

      case 1301 -> {
        // Now You've Spied Her
        StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
        if (ChoiceManager.lastDecision != 6) {
          StringUtilities.singleStringReplace(unlocks, "The Spider Queen's Lair,", "");
        }
        Preferences.setString("_frAreasUnlocked", unlocks.toString());
      }

      case 1302 -> {
        // Don't Be Arch
        StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
        if (ChoiceManager.lastDecision != 6) {
          StringUtilities.singleStringReplace(unlocks, "The Archwizard's Tower,", "");
        }
        if (ChoiceManager.lastDecision == 1) {
          ResultProcessor.removeItem(ItemPool.FR_CHARGED_ORB);
        }
        Preferences.setString("_frAreasUnlocked", unlocks.toString());
      }

      case 1303 -> {
        // Ley Lady Ley
        StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
        if (ChoiceManager.lastDecision != 6) {
          StringUtilities.singleStringReplace(unlocks, "The Ley Nexus,", "");
        }
        if (ChoiceManager.lastDecision == 1) {
          ResultProcessor.removeItem(ItemPool.FR_CHESWICKS_COMPASS);
        }
        Preferences.setString("_frAreasUnlocked", unlocks.toString());
      }

      case 1304 -> {
        // He Is the Ghoul King, He Can Do Anything
        StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
        if (ChoiceManager.lastDecision != 6) {
          StringUtilities.singleStringReplace(unlocks, "The Ghoul King's Catacomb,", "");
        }
        Preferences.setString("_frAreasUnlocked", unlocks.toString());
      }

      case 1305 -> {
        // The Brogre's Progress
        StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
        if (ChoiceManager.lastDecision != 6) {
          StringUtilities.singleStringReplace(unlocks, "The Ogre Chieftain's Keep,", "");
        }
        if (ChoiceManager.lastDecision == 1) {
          ResultProcessor.removeItem(ItemPool.FR_POISONED_SMORE);
        }
        Preferences.setString("_frAreasUnlocked", unlocks.toString());
      }

      case 1307 -> {
        // It Takes a Thief
        StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
        if (ChoiceManager.lastDecision != 6) {
          StringUtilities.singleStringReplace(unlocks, "The Master Thief's Chalet,", "");
        }
        if (ChoiceManager.lastDecision == 1) {
          ResultProcessor.removeItem(ItemPool.FR_NOTARIZED_WARRANT);
        }
        Preferences.setString("_frAreasUnlocked", unlocks.toString());
      }

      case 1312 -> {
        // Choose a Soundtrack
        if (!text.contains("decide not to change the station")) {
          String songChosen =
              switch (ChoiceManager.lastDecision) {
                case 1 -> "Eye of the Giger";
                case 2 -> "Food Vibrations";
                case 3 -> "Remainin' Alive";
                case 4 -> "These Fists Were Made for Punchin'";
                case 5 -> "Total Eclipse of Your Meat";
                default -> "";
              };
          if (!songChosen.equals("")) {
            if (!KoLCharacter.hasSkill(SkillPool.SING_ALONG)) {
              KoLCharacter.addAvailableSkill(SkillPool.SING_ALONG);
            }
            if (!Preferences.getString("boomBoxSong").equals(songChosen)) {
              Preferences.setString("boomBoxSong", songChosen);
              Preferences.decrement("_boomBoxSongsLeft");
              String message = "Setting soundtrack to " + songChosen;
              RequestLogger.printLine(message);
              RequestLogger.updateSessionLog(message);
            }
          } else {
            if (KoLCharacter.hasSkill(SkillPool.SING_ALONG)) {
              KoLCharacter.removeAvailableSkill(SkillPool.SING_ALONG);
            }
            if (!Preferences.getString("boomBoxSong").equals("")) {
              Preferences.setString("boomBoxSong", "");
              String message = "Switching soundtrack off";
              RequestLogger.printLine(message);
              RequestLogger.updateSessionLog(message);
            }
          }
        }
      }

      case 1313, // Bastille Battalion
          1314, // Bastille Battalion (Master of None)
          1315, // Castle vs. Castle
          1316, // GAME OVER
          1317, // A Hello to Arms (Battalion)
          1318, // Defensive Posturing
          1319 -> // Cheese Seeking Behavior
      BastilleBattalionManager.postChoice1(urlString, request);

      case 1322 -> {
        // The Beginning of the Neverend
        boolean hard = KoLCharacter.hasEquipped(ItemPool.PARTY_HARD_T_SHIRT);
        Preferences.setBoolean("_partyHard", hard);
        if (ChoiceManager.lastDecision == 1) {
          // Decided to quest
          Preferences.setInteger("encountersUntilNEPChoice", 7);

          String quest = Preferences.getString("_questPartyFairQuest");
          if (quest.equals("booze") || quest.equals("food")) {
            QuestDatabase.setQuestProgress(Quest.PARTY_FAIR, QuestDatabase.STARTED);
            Preferences.setString("_questPartyFairProgress", "");
          } else {
            QuestDatabase.setQuestProgress(Quest.PARTY_FAIR, "step1");
            switch (quest) {
              case "woots":
                Preferences.setInteger("_questPartyFairProgress", 10);
                break;
              case "partiers":
                if (hard) {
                  Preferences.setInteger("_questPartyFairProgress", 100);
                } else {
                  Preferences.setInteger("_questPartyFairProgress", 50);
                }
                break;
              case "dj":
                if (hard) {
                  Preferences.setInteger("_questPartyFairProgress", 10000);
                } else {
                  Preferences.setInteger("_questPartyFairProgress", 5000);
                }
                break;
              case "trash":
                // The amount isn't known, so check quest log
                (new GenericRequest("questlog.php?which=1")).run();
                break;
            }
          }
        } else if (ChoiceManager.lastDecision == 2) {
          // Decided to party
          Preferences.setString("_questPartyFair", "");
          Preferences.setString("_questPartyFairQuest", "");
          Preferences.setString("_questPartyFairProgress", "");
        }
      }

      case 1323 -> {
        // All Done!
        QuestDatabase.setQuestProgress(Quest.PARTY_FAIR, QuestDatabase.FINISHED);
        Preferences.setString("_questPartyFairQuest", "");
        Preferences.setString("_questPartyFairProgress", "");
      }

      case 1324 -> {
        // It Hasn't Ended, It's Just Paused
        // Decision 5 is followed by a fight, which will decrement the free turns available itself
        if (ChoiceManager.lastDecision != 5) {
          Preferences.decrement("encountersUntilNEPChoice", 1, 0);
          Preferences.increment("_neverendingPartyFreeTurns", 1, 10, false);
        }
      }

      case 1325 -> {
        // A Room With a View...  Of a Bed
        if (ChoiceManager.lastDecision == 3) {
          // Removes 30% of current partiers
          int current = Preferences.getInteger("_questPartyFairProgress");
          Preferences.setInteger(
              "_questPartyFairProgress", current - (int) Math.floor(current * 0.3));
          ResultProcessor.removeItem(ItemPool.JAM_BAND_BOOTLEG);
        } else if (ChoiceManager.lastDecision == 4) {
          // On dj quest (choice number guessed)
          Matcher matcher = SAFE_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.decrement(
                "_questPartyFairProgress", StringUtilities.parseInt(matcher.group(1)), 0);
            if (Preferences.getInteger("_questPartyFairProgress") < 1) {
              QuestDatabase.setQuestProgress(Quest.PARTY_FAIR, "step2");
            }
          }
        } else if (ChoiceManager.lastDecision == 5) {
          // On woots quest
          Preferences.increment("_questPartyFairProgress", 20, 100, false);
          if (Preferences.getInteger("_questPartyFairProgress") == 100) {
            QuestDatabase.setQuestProgress(Quest.PARTY_FAIR, "step2");
          }
          ResultProcessor.removeItem(ItemPool.VERY_SMALL_RED_DRESS);
        }
      }

      case 1326 -> {
        // Gone Kitchin'
        if (ChoiceManager.lastDecision == 3) {
          Matcher matcher = GERALDINE_PATTERN.matcher(text);
          if (matcher.find()) {
            int itemCount = StringUtilities.parseInt(matcher.group(1));
            int itemId = ItemDatabase.getItemIdFromDescription(matcher.group(2));
            Preferences.setString("_questPartyFairProgress", itemCount + " " + itemId);
            if (InventoryManager.getCount(itemId) >= itemCount) {
              QuestDatabase.setQuestProgress(Quest.PARTY_FAIR, "step2");
            }
          }
          QuestDatabase.setQuestIfBetter(Quest.PARTY_FAIR, "step1");
        } else if (ChoiceManager.lastDecision == 4) {
          String pref = Preferences.getString("_questPartyFairProgress");
          String itemIdString = null;
          int position = pref.indexOf(" ");
          if (position > 0) {
            itemIdString = pref.substring(position);
            if (itemIdString != null) {
              ResultProcessor.processItem(StringUtilities.parseInt(itemIdString), -10);
            }
          }
          QuestDatabase.setQuestIfBetter(Quest.PARTY_FAIR, QuestDatabase.FINISHED);
          Preferences.setString("_questPartyFairQuest", "");
          Preferences.setString("_questPartyFairProgress", "");
        } else if (ChoiceManager.lastDecision == 5) {
          Matcher matcher = TRASH_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.decrement(
                "_questPartyFairProgress", StringUtilities.parseInt(matcher.group(1)), 0);
          }
        }
      }

      case 1327 -> {
        // Forward to the Back
        if (ChoiceManager.lastDecision == 3) {
          Matcher matcher = GERALD_PATTERN.matcher(text);
          if (matcher.find()) {
            int itemCount = StringUtilities.parseInt(matcher.group(1));
            int itemId = ItemDatabase.getItemIdFromDescription(matcher.group(2));
            Preferences.setString("_questPartyFairProgress", itemCount + " " + itemId);
            if (InventoryManager.getCount(itemId) >= itemCount) {
              QuestDatabase.setQuestProgress(Quest.PARTY_FAIR, "step2");
            }
          }
          QuestDatabase.setQuestIfBetter(Quest.PARTY_FAIR, "step1");
        } else if (ChoiceManager.lastDecision == 4) {
          String pref = Preferences.getString("_questPartyFairProgress");
          String itemIdString = null;
          int position = pref.indexOf(" ");
          if (position > 0) {
            itemIdString = pref.substring(position);
            if (itemIdString != null) {
              int itemCount = Preferences.getBoolean("_partyHard") ? 20 : 10;
              ResultProcessor.processItem(StringUtilities.parseInt(itemIdString), -itemCount);
            }
          }
          QuestDatabase.setQuestProgress(Quest.PARTY_FAIR, QuestDatabase.FINISHED);
          Preferences.setString("_questPartyFairQuest", "");
          Preferences.setString("_questPartyFairProgress", "");
        } else if (ChoiceManager.lastDecision == 5) {
          // Removes 20% of current partiers
          int current = Preferences.getInteger("_questPartyFairProgress");
          Preferences.setInteger(
              "_questPartyFairProgress", current - (int) Math.floor(current * 0.2));
          ResultProcessor.removeItem(ItemPool.PURPLE_BEAST_ENERGY_DRINK);
        }
      }

      case 1328 -> {
        // Basement Urges
        if (ChoiceManager.lastDecision == 4) {
          // On woots quest
          Preferences.increment("_questPartyFairProgress", 20, 100, false);
          if (Preferences.getInteger("_questPartyFairProgress") == 100) {
            QuestDatabase.setQuestProgress(Quest.PARTY_FAIR, "step2");
          }
          ResultProcessor.removeItem(ItemPool.ELECTRONICS_KIT);
        }
      }

      case 1329 -> {
        // Latte Shop
        // The item that we get has a procedurally-generated name
        request.setHasResult(false);
        LatteRequest.parseResponse(urlString, text);
      }

      case 1331 -> {
        // Daily Loathing Ballot
        if (ChoiceManager.lastDecision == 1 && !text.contains("must vote for a candidate")) {
          ModifierList modList = new ModifierList();
          Matcher matcher = URL_VOTE_PATTERN.matcher(urlString);
          while (matcher.find()) {
            int vote = StringUtilities.parseInt(matcher.group(1)) + 1;
            String pref = Preferences.getString("_voteLocal" + vote);
            ModifierList addModList = ModifierDatabase.splitModifiers(pref);
            for (ModifierValue modifier : addModList) {
              modList.addToModifier(modifier);
            }
          }
          Preferences.setString("_voteModifier", modList.toString());
          String message = "You have cast your votes";
          RequestLogger.printLine(message);
          RequestLogger.updateSessionLog(message);
        }
      }

      case 1332 -> // government requisition form
      ResultProcessor.removeItem(ItemPool.GOVERNMENT_REQUISITION_FORM);

      case 1333 -> {
        // Canadian cabin
        if (ChoiceManager.lastDecision == 2) {
          ResultProcessor.removeItem(ItemPool.MOOSEFLANK);
        } else if (ChoiceManager.lastDecision == 3) {
          ResultProcessor.processItem(ItemPool.WALRUS_BLUBBER, -10);
        } else if (ChoiceManager.lastDecision == 4) {
          ResultProcessor.processItem(ItemPool.TINY_BOMB, -10);
        }
      }

      case 1334 -> {
        // Boxing Daycare (Lobby)
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("_daycareNap", true);
        } else if (ChoiceManager.lastDecision == 2
            && text.contains("only allowed one spa treatment")) {
          Preferences.setBoolean("_daycareSpa", true);
        }
      }

      case 1335 -> {
        // Boxing Day Spa
        if (ChoiceManager.lastDecision != 5) {
          Preferences.setBoolean("_daycareSpa", true);
        }
      }

      case 1336 -> {
        // Boxing Daycare
        String message1 = null;
        String message2 = null;
        if (ChoiceManager.lastDecision == 1) {
          Matcher matcher = DAYCARE_RECRUIT_PATTERN.matcher(text);
          if (matcher.find()) {
            message1 = "Activity: Recruit toddlers";
            message2 = "You have recruited " + matcher.group(1) + " toddlers";
            Preferences.increment("_daycareRecruits");
          }
        } else if (ChoiceManager.lastDecision == 2) {
          Matcher matcher = DAYCARE_EQUIPMENT_PATTERN.matcher(text);
          if (matcher.find()) {
            AdventureResult effect = EffectPool.get(EffectPool.BOXING_DAY_BREAKFAST, 0);
            boolean haveBreakfast = effect.getCount(KoLConstants.activeEffects) > 0;
            String countString = matcher.group(1);
            int equipment = StringUtilities.parseInt(countString.replaceAll(",", ""));
            Preferences.setInteger("daycareLastScavenge", equipment / (haveBreakfast ? 2 : 1));

            message1 = "Activity: Scavenge for gym equipment";
            message2 = "You have found " + countString + " pieces of gym equipment";
            Preferences.increment("_daycareGymScavenges");
          }
        } else if (ChoiceManager.lastDecision == 3) {
          if (text.contains("new teacher joins the staff")) {
            message1 = "Activity: Hire an instructor";
            message2 = "You have hired a new instructor";
          }
          Matcher matcher = DAYCARE_ITEM_PATTERN.matcher(text);
          if (matcher.find()) {
            String itemName = matcher.group(1);
            int itemCount = StringUtilities.parseInt(matcher.group(2).replaceAll(",", ""));
            ResultProcessor.processItem(ItemDatabase.getItemId(itemName), -itemCount);
          }
        } else if (ChoiceManager.lastDecision == 4) {
          if (text.contains("step into the ring")) {
            message1 = "Activity: Spar";
            Preferences.setBoolean("_daycareFights", true);
          }
        }
        if (message1 != null) {
          RequestLogger.printLine(message1);
          RequestLogger.updateSessionLog(message1);
        }
        if (message2 != null) {
          RequestLogger.printLine(message2);
          RequestLogger.updateSessionLog(message2);
        }
      }

      case 1340 -> {
        // Is There A Doctor In The House?
        if (ChoiceManager.lastDecision == 1) {
          if (Preferences.getString("doctorBagQuestItem").isEmpty()) {
            // We didn't recognise quest text, so get it from quest log
            RequestThread.postRequest(new QuestLogRequest());
          }
          int itemId = ItemDatabase.getItemId(Preferences.getString("doctorBagQuestItem"));

          if (itemId > 0) {
            if (InventoryManager.getCount(itemId) > 0) {
              QuestDatabase.setQuestProgress(Quest.DOCTOR_BAG, "step1");
            } else {
              QuestDatabase.setQuestProgress(Quest.DOCTOR_BAG, QuestDatabase.STARTED);
            }
          } else {
            QuestDatabase.setQuestProgress(Quest.DOCTOR_BAG, QuestDatabase.STARTED);
          }
        } else {
          QuestDatabase.setQuestProgress(Quest.DOCTOR_BAG, QuestDatabase.UNSTARTED);
          Preferences.setString("doctorBagQuestItem", "");
          Preferences.setString("doctorBagQuestLocation", "");
        }
      }

      case 1341 -> {
        // A Pound of Cure
        if (ChoiceManager.lastDecision == 1) {
          String itemName = Preferences.getString("doctorBagQuestItem");
          if (!itemName.equals("")) {
            ResultProcessor.processItem(ItemDatabase.getItemId(itemName), -1);
          } else {
            // Don't know item to remove so refresh inventory instead
            ApiRequest.updateInventory();
          }
          QuestDatabase.setQuestProgress(Quest.DOCTOR_BAG, QuestDatabase.UNSTARTED);
          Preferences.setString("doctorBagQuestItem", "");
          Preferences.setString("doctorBagQuestLocation", "");
          if (text.contains("One of the five green lights")) {
            Preferences.setInteger("doctorBagQuestLights", 1);
          } else if (text.contains("second of the five green lights")) {
            Preferences.setInteger("doctorBagQuestLights", 2);
          } else if (text.contains("third of the five green lights")) {
            Preferences.setInteger("doctorBagQuestLights", 3);
          } else if (text.contains("fourth of the five green lights")) {
            Preferences.setInteger("doctorBagQuestLights", 4);
          } else if (text.contains("lights go dark again")) {
            Preferences.setInteger("doctorBagQuestLights", 0);
          }
          if (text.contains("bag has been permanently upgraded")) {
            Preferences.increment("doctorBagUpgrades");
          }
        }
      }

      case 1342 -> {
        // Torpor
        if (ChoiceManager.lastDecision == 2) {
          // You can learn or forget Vampyre skills
          for (int i = 10; i < 39; i++) {
            if (urlString.contains("sk[]=" + i) && !KoLCharacter.hasSkill(24000 + i)) {
              String skillName = SkillDatabase.getSkillName(24000 + i);
              KoLCharacter.addAvailableSkill(skillName);
              String message = "You have learned " + skillName;
              RequestLogger.printLine(message);
              RequestLogger.updateSessionLog(message);
            }
            if (!urlString.contains("sk[]=" + i) && KoLCharacter.hasSkill(24000 + i)) {
              String skillName = SkillDatabase.getSkillName(24000 + i);
              KoLCharacter.removeAvailableSkill(skillName);
              String message = "You have forgotten " + skillName;
              RequestLogger.printLine(message);
              RequestLogger.updateSessionLog(message);
            }
          }
        }
      }

      case 1347 -> // Groggy's Tavern
      Preferences.setString(
          "_pirateRealmCrewmate",
          Preferences.getString("_pirateRealmCrewmate" + ChoiceManager.lastDecision));

      case 1348 -> // Seaside Curios
      // The item ids are in order from 10190 to 10195
      Preferences.setString(
          "_pirateRealmCurio", String.valueOf(10189 + ChoiceManager.lastDecision));

      case 1349 -> { // Dishonest Ed's Ships
        Preferences.setString(
            "_pirateRealmShip",
            switch (ChoiceManager.lastDecision) {
              case 1 -> "Rigged Frigate";
              case 2 -> "Intimidating Galleon";
              case 3 -> "Speedy Caravel";
              case 4 -> "Swift Clipper";
              case 5 -> "Menacing Man o' War";
              default -> "";
            });
        Preferences.setInteger(
            "_pirateRealmShipSpeed",
            switch (ChoiceManager.lastDecision) {
              case 1, 2 -> 7;
              case 3 -> 6;
              case 4 -> 4;
              case 5 -> 9;
              default -> 0;
            });
      }

      case 1352, //  Island #1, Who Are You?
          1353, //  What's Behind Island #2?
          1354 -> { //  Third Island's the Charm
        // Advance quest progress
        QuestManager.setPirateRealmIslandQuestProgress(ChoiceManager.lastChoice - 1352, 0);
        // Reset per-island flags
        Preferences.setInteger("_pirateRealmIslandMonstersDefeated", 0);
        Preferences.setInteger("_pirateRealmSailingTurns", 0);
        Preferences.setBoolean("_pirateRealmWindicleUsed", false);
      }

      case 1355 -> //  Land Ho!
      // Step 3 -> 4, 8 -> 9 or 13 -> 14
      QuestManager.setPirateRealmIslandQuestProgress(2);

      case 1360 -> { // Like Shops in the Night
        if (ChoiceManager.lastDecision == 5 && text.contains("You gain 500 gold")) {
          // Sell them the cursed compass
          Preferences.setBoolean("_pirateRealmSoldCompass", true);
          // Remove from equipment (including checkpoints)
          if (EquipmentManager.discardEquipment(ItemPool.CURSED_COMPASS) == Slot.NONE) {
            // Remove from inventory
            ResultProcessor.removeItem(ItemPool.CURSED_COMPASS);
          }
        }
      }

      case 1362 -> { // Stormy Weather
        // Try to gain some extra distance
        if (ChoiceManager.lastDecision == 2) {
          if (text.contains("you manage to outsail the storm")) {
            // This is already incremented in postChoice0, but successful sailing increments it one
            // further.
            var turns = Preferences.increment("_pirateRealmSailingTurns", 1);
            // Since the value is already incremented, we could have already reached the island. The
            // subquest progress
            // function is smart enough to handle this, however.
            if (turns >= Preferences.getInteger("_pirateRealmShipSpeed")) {
              QuestManager.setPirateRealmIslandQuestProgress(1);
            }
            Preferences.increment("pirateRealmStormsEscaped", 1, 10, false);
          }
        }
      }

      case 1364 -> { // An Opportunity for Dastardly Do
        // Attack them
        if (ChoiceManager.lastDecision == 1) {
          if (text.contains("blast them to bits")) {
            Preferences.increment("pirateRealmShipsDestroyed", 1, 10, false);
          }
        }
      }

      case 1365 -> { // A Sea Monster!
        if (ChoiceManager.lastDecision == 1) {
          if (text.contains("plush sea serpent")) {
            Preferences.setBoolean("pirateRealmUnlockedPlushie", true);
          }
        }
      }

      case 1369, // The Battle (Island) Is Won
          1370, // Skull's Well That Ends Skull
          1371, // The Key Takeaway
          1385 -> // Just Desserts
      QuestDatabase.setQuestIfBetter(Quest.PIRATEREALM, 6);

      case 1372 -> { // You Can See Clearly Now
        Preferences.setBoolean("pirateRealmUnlockedRhum", true);
        QuestDatabase.setQuestIfBetter(Quest.PIRATEREALM, 6);
      }

      case 1375 -> { // A Close Shave
        Preferences.setBoolean("pirateRealmUnlockedShavingCream", true);
        QuestDatabase.setQuestIfBetter(Quest.PIRATEREALM, 11);
      }

      case 1376, // Your Empire of Dirt
          1377 -> // A Dreaded Sunny Day
      QuestDatabase.setQuestIfBetter(Quest.PIRATEREALM, 11);

      case 1379 -> { // The Tiki Craze Is Over
        if (text.contains("Island Drinkin' skillbook")) {
          Preferences.setBoolean("pirateRealmUnlockedTikiSkillbook", true);
          QuestDatabase.setQuestIfBetter(Quest.PIRATEREALM, 16);
        }
      }

      case 1380 -> { // Temple's Grand End
        Preferences.setBoolean("pirateRealmUnlockedTattoo", true);
        QuestDatabase.setQuestIfBetter(Quest.PIRATEREALM, 16);
      }

      case 1383 -> { // Parole
        Preferences.setBoolean("pirateRealmUnlockedThirdCrewmate", true);
        QuestDatabase.setQuestIfBetter(Quest.PIRATEREALM, 11);
      }

      case 1384 -> { // The Calm After the Storm
        Preferences.setBoolean("pirateRealmUnlockedAnemometer", true);
        QuestDatabase.setQuestIfBetter(Quest.PIRATEREALM, 16);
      }

      case 1386 -> SaberRequest.parseUpgrade(urlString, text);

      case 1387 -> SaberRequest.parseForce(urlString, text);

      case 1388 -> {
        BeachManager.parseBeachHeadCombing(text);
        BeachManager.parseCombUsage(urlString, text);
      }

      case 1391 -> {
        // Rationing out Destruction
        //    choice.php?whichchoice=1391&option=1&pwd&tossid=10321

        Matcher tossMatcher = TOSSID_PATTERN.matcher(urlString);
        if (!tossMatcher.find()) {
          break;
        }
        int itemId = StringUtilities.parseInt(tossMatcher.group(1));
        ResultProcessor.processItem(itemId, -1);

        String army = null;
        String property = null;
        String consumable = null;
        int casualties = 0;

        if (casualties == 0) {
          Matcher fratMatcher = FRAT_RATIONING_PATTERN.matcher(text);
          if (fratMatcher.find()) {
            army = "frat boys";
            property = "fratboysDefeated";
            consumable = fratMatcher.group(1);
            casualties = StringUtilities.parseInt(fratMatcher.group(2));
          }
        }
        if (casualties == 0) {
          Matcher hippyMatcher = HIPPY_RATIONING_PATTERN.matcher(text);
          if (hippyMatcher.find()) {
            army = "hippies";
            property = "hippiesDefeated";
            consumable = hippyMatcher.group(1);
            casualties = StringUtilities.parseInt(hippyMatcher.group(2));
          }
        }

        Preferences.increment(property, casualties);

        String message = "You defeated " + casualties + " " + army + " with some " + consumable;
        RequestLogger.printLine(message);
        RequestLogger.updateSessionLog(message);
      }

      case 1392 -> DecorateTentRequest.parseDecoration(urlString, text);

      case 1394 -> {
        // Send up a Smoke Signal
        if (ChoiceManager.lastDecision == 1
            && text.contains("You send a smoky message to the sky.")) {
          // Remove from inventory
          ResultProcessor.removeItem(ItemPool.CAMPFIRE_SMOKE);
        }
      }

      case 1396 -> {
        // Adjusting Your Fish
        // choice.php?pwd&whichchoice=1396&option=1&cat=fish
        if (urlString.contains("option=1") && urlString.contains("cat=")) {
          Pattern pattern = Pattern.compile("cat=([^&]*)");
          Matcher matcher = pattern.matcher(urlString);
          if (matcher.find()) {
            String phylum = matcher.group(1);
            String fixed = phylum.equals("merkin") ? "mer-kin" : phylum;
            TrackManager.track(fixed, Tracker.RED_SNAPPER);
            Preferences.setInteger("redSnapperProgress", 0);
          }
        }
      }

      case 1399 -> {
        // New Favorite Bird?
        // Auto correct, in case we cast this outside of KoLmafia
        Preferences.setInteger("_birdsSoughtToday", 6);
        if (ChoiceManager.lastDecision == 1) {
          String bird = Preferences.getString("_birdOfTheDay");
          Preferences.setString("yourFavoriteBird", bird);
          ResponseTextParser.learnSkill("Visit your Favorite Bird");
          DebugDatabase.readEffectDescriptionText(EffectPool.BLESSING_OF_YOUR_FAVORITE_BIRD);
        }
      }

      case 1406 -> {
        // Drippy House on the Prairie
        // 1 = Explore the House
        // Even though the house doesn't have a door, you check under the mat for a key anyway.
        // You don't find one, but you <i>do</i> find a little puddle of those Driplet things
        // Jeremy told you about.
        // 1 = Keep Exploring
        // Just inside the door of the house, you discover a colony of nasty bat-looking things
        // nesting in the rafters.
        // In one of the side rooms of the house, you find a giant spiral shell stuck to the wall.
        //  You pry it loose -- Jeremy will probably want to see this.
        // In the house's kitchen, you find a bucket underneath a dripping sink pipe. The
        // oddly-solid drops make a -thunk-... -thunk-... -thunk-... sound as they fall.
        // In the backyard of the house, you notice a strange fruit tree you hadn't seen before.
        // It's maybe... plums? Kinda hard to tell, what with everything being made out of the
        // same gross stuff.
        // In one of the back rooms, you find a workbench covered in drippy woodworking supplies.
        // Underneath a shattered bed frame in one of the back rooms, you find a trap door in the
        // floor. You press your ear to it and hear some... Thing... slithering around underneath
        // it.
        // 2 = Dislodge some bats
        // You flush some of the vile bat-things out of the rafters and into the nearby forest.
        // No way that'll come back to bite you in the ass!
        // 3 = Check the bucket under the sink
        // You look in the bucket and find a few Driplets.  You collect them and put the bucket
        // back under the sink.
        // 4 - Pick a nasty fruit
        // You pluck the least nasty-looking plum(?) from the tree and pocket it.
        // 5 - Check out the woodworking bench
        // You use the tools to carve your truncheon into a sharp stake.
        // 6 - Go down to the basement
        // 9 = Leave
        if (text.contains("vile bat-things")) {
          Preferences.increment("drippyBatsUnlocked", 7);
        }
        if (text.contains("sharp stake")) {
          EquipmentManager.discardEquipment(ItemPool.DRIPPY_TRUNCHEON);
        }

        // Since this choice appears on a schedule - the 16th
        // adventure in The Dripping Trees and then every 15
        // turns thereafter - "fix" the adventure count as needed.
        int advs = Preferences.getInteger("drippingTreesAdventuresSinceAscension");
        if (advs < 16) {
          Preferences.setInteger("drippingTreesAdventuresSinceAscension", 16);
          advs = 16;
        }
        int mod = (advs - 1) % 15;
        if (mod != 0) {
          Preferences.increment("drippingTreesAdventuresSinceAscension", 15 - mod);
        }
      }

      case 1407 -> {
        // Mushroom District Costume Shop

        String costume = "none";
        if (text.contains("You slip into something a little more carpentable")) {
          costume = "muscle";
        } else if (text.contains("You let down your guard and put on the gardener costume")) {
          costume = "mysticality";
        } else if (text.contains("Todge holds out a tutu and you jump into it")) {
          costume = "moxie";
        } else {
          break;
        }

        int cost = Preferences.getInteger("plumberCostumeCost");
        ResultProcessor.processItem(ItemPool.COIN, -cost);
        Preferences.increment("plumberCostumeCost", 50);
        Preferences.setString("plumberCostumeWorn", costume);
      }

      case 1408 -> {
        // Mushroom District Badge Shop
        // If successful, deduct coins.

        if (text.contains("You acquire a skill")) {
          int cost = Preferences.getInteger("plumberBadgeCost");
          ResultProcessor.processItem(ItemPool.COIN, -cost);
          Preferences.increment("plumberBadgeCost", 25);
        }
      }

      case 1410 -> {
        // The Mushy Center
        int mushroomLevel = 1; // Tomorrow's mushroom
        switch (ChoiceManager.lastDecision) {
          case 1 ->
          // Fertilize the mushroom
          mushroomLevel = Preferences.increment("mushroomGardenCropLevel", 1, 11, false);
          case 2 ->
          // Pick the mushroom
          Preferences.setInteger("mushroomGardenCropLevel", 1);
        }
        CampgroundRequest.clearCrop();
        CampgroundRequest.setCampgroundItem(new Mushroom(mushroomLevel));
        Preferences.setBoolean("_mushroomGardenVisited", true);
      }

      case 1411 -> {
        // The Hall in the Hall
        switch (ChoiceManager.lastDecision) {
          case 1 -> {
            Preferences.setBoolean("_drippingHallDoor1", true);
            // If you acquire a drippy org, count it
            if (text.contains("drippy orb")) {
              Preferences.increment("drippyOrbsClaimed");
            } else {
              int known = Preferences.getInteger("drippyOrbsClaimed");
              int min = KoLCharacter.estimatedPoolSkill() / 20;
              Preferences.setInteger("drippyOrbsClaimed", Math.max(known, min));
            }
          }
          case 2 -> Preferences.setBoolean("_drippingHallDoor2", true);
          case 3 -> Preferences.setBoolean("_drippingHallDoor3", true);
          case 4 -> {
            Preferences.setBoolean("_drippingHallDoor4", true);

            // If you acquire drippy pilsner, uses a drippy stein
            if (text.contains("drippy pilsner")) {
              ResultProcessor.processItem(ItemPool.DRIPPY_STEIN, -1);
            }
          }
        }

        // This only advances upon defeating a dripping reveler
        // or encountering this choice
        int advs = Preferences.increment("drippingHallAdventuresSinceAscension");

        // Since this choice appears on a schedule - the 12th
        // adventure in The Dripping Hall and then every 12
        // turns thereafter - "fix" the adventure count as needed.
        if (advs < 12) {
          Preferences.setInteger("drippingHallAdventuresSinceAscension", 12);
          advs = 12;
        }
        int mod = advs % 12;
        if (mod != 0) {
          Preferences.increment("drippingHallAdventuresSinceAscension", 12 - mod);
        }
      }

      case 1412 -> {
        // Guzzlr Client Selection
        switch (ChoiceManager.lastDecision) {
          case 1 -> {
            // Abandon
            Preferences.setBoolean("_guzzlrQuestAbandoned", true);

            Preferences.setString("guzzlrQuestBooze", "");
            Preferences.setString("guzzlrQuestClient", "");
            Preferences.setString("guzzlrQuestLocation", "");
            Preferences.setString("guzzlrQuestTier", "");
            QuestDatabase.setQuestProgress(Quest.GUZZLR, QuestDatabase.UNSTARTED);
          }
          case 2, 3, 4 -> {
            Matcher locationMatcher = GUZZLR_LOCATION_PATTERN.matcher(text);
            Matcher boozeMatcher = DESCID_PATTERN.matcher(text);

            String tier = "";

            if (locationMatcher.find()) {
              tier = locationMatcher.group(1).toLowerCase();
              Preferences.setString("guzzlrQuestClient", locationMatcher.group(2));
              Preferences.setString("guzzlrQuestLocation", locationMatcher.group(4));
            }

            // If we didn't capture from the ouput we can determine from the choice
            if (tier.equals("")) {
              tier =
                  ChoiceManager.lastDecision == 2
                      ? "bronze"
                      : ChoiceManager.lastDecision == 3 ? "gold" : "platinum";
            }

            // Remember the tier of the current quest
            Preferences.setString("guzzlrQuestTier", tier);

            // Increment the number of gold or platinum deliveres STARTED today
            if (!tier.equals("bronze")) {
              Preferences.increment(
                  "_guzzlr" + StringUtilities.toTitleCase(tier) + "Deliveries", 1);
            }

            if (boozeMatcher.find()) {
              int itemId = ItemDatabase.getItemIdFromDescription(boozeMatcher.group(1));
              Preferences.setString("guzzlrQuestBooze", ItemDatabase.getItemName(itemId));
            }

            if (Preferences.getString("guzzlrQuestBooze").isEmpty()
                || Preferences.getString("guzzlrQuestLocation").isEmpty()) {
              RequestThread.postRequest(new QuestLogRequest());
            }

            int itemId = ItemDatabase.getItemId(Preferences.getString("guzzlrQuestBooze"));

            if (itemId > 0 && itemId != ItemPool.GUZZLR_COCKTAIL_SET) {
              if (InventoryManager.getCount(itemId) > 0) {
                QuestDatabase.setQuestProgress(Quest.GUZZLR, "step1");
              } else {
                QuestDatabase.setQuestProgress(Quest.GUZZLR, QuestDatabase.STARTED);
              }
            } else {
              QuestDatabase.setQuestProgress(Quest.GUZZLR, QuestDatabase.STARTED);
            }
          }
        }
      }

      case 1414 -> Preferences.setBoolean("lockPicked", true);

      case 1418 -> {
        // So Cold
        if (ChoiceManager.lastDecision == 1) {
          KoLCharacter.usableFamiliar(FamiliarPool.MELODRAMEDARY).loseExperience();
          Preferences.setBoolean("_entauntaunedToday", true);
        }
      }

      case 1420 -> {
        // Cargo Cultist Shorts
        if (ChoiceManager.lastDecision == 1) {
          CargoCultistShortsRequest.parsePocketPick(urlString, text);
        }
      }

      case 1435 -> {
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("mappingMonsters", false);
        }
      }
      case 1437 -> {
        // Configuring the retro cape washing instructions
        String instructions =
            ChoiceManager.lastDecision == 2
                ? "hold"
                : ChoiceManager.lastDecision == 3
                    ? "thrill"
                    : ChoiceManager.lastDecision == 4
                        ? "kiss"
                        : ChoiceManager.lastDecision == 5 ? "kill" : null;

        if (instructions != null) {
          Preferences.setString("retroCapeWashingInstructions", instructions);
          ItemDatabase.setCapeSkills();
        }
      }
      case 1438 -> {
        // Configuring the retro cape superhero
        String hero =
            ChoiceManager.lastDecision == 1
                ? "vampire"
                : ChoiceManager.lastDecision == 2
                    ? "heck"
                    : ChoiceManager.lastDecision == 3 ? "robot" : null;

        if (hero != null) {
          Preferences.setString("retroCapeSuperhero", hero);
          ItemDatabase.setCapeSkills();
        }
      }
      case 1442 -> {
        if (text.contains("You fill out all the appropriate forms")) {
          ResultProcessor.removeItem(ItemPool.GOVERNMENT_FOOD_SHIPMENT);
        }
      }
      case 1443 -> {
        if (text.contains("You fill out all the appropriate forms")) {
          ResultProcessor.removeItem(ItemPool.GOVERNMENT_BOOZE_SHIPMENT);
        }
      }
      case 1444 -> {
        if (text.contains("You fill out all the appropriate forms")) {
          ResultProcessor.removeItem(ItemPool.GOVERNMENT_CANDY_SHIPMENT);
        }
      }

      case 1445, // Reassembly Station
          1447 -> // Statbot 5000
      YouRobotManager.postChoice1(urlString, request);

      case 1448 -> {
        if (text.contains("You acquire an item:")) {
          Matcher stalkMatcher = Pattern.compile("pp=(\\d+)").matcher(urlString);
          if (stalkMatcher.find()) {
            int pp = Integer.parseInt(stalkMatcher.group(1)) - 1;
            String[] stalkStatus = Preferences.getString("_pottedPowerPlant").split(",");
            stalkStatus[pp] = "0";
            Preferences.setString("_pottedPowerPlant", String.join(",", stalkStatus));
          }
        }
      }
      case 1449 -> {
        switch (ChoiceManager.lastDecision) {
          case 1 -> Preferences.setString("backupCameraMode", "ml");
          case 2 -> Preferences.setString("backupCameraMode", "meat");
          case 3 -> Preferences.setString("backupCameraMode", "init");
          case 4 -> Preferences.setBoolean("backupCameraReverserEnabled", true);
          case 5 -> Preferences.setBoolean("backupCameraReverserEnabled", false);
        }
      }
      case 1451 -> {
        // Fire Captain Hagnk
        if (ChoiceManager.lastDecision == 1) {
          Matcher locationIdMatcher = Pattern.compile("zid=([^&]*)").matcher(urlString);
          if (locationIdMatcher.find()) {
            int zid = StringUtilities.parseInt(locationIdMatcher.group(1));
            KoLAdventure location =
                AdventureDatabase.getAdventureByURL("adventure.php?snarfblat=" + zid);
            WildfireCampRequest.reduceFireLevel(location);
          }
        } else if (ChoiceManager.lastDecision == 3) {
          if (text.contains("Hagnk takes your fire extinguisher")) {
            Preferences.setInteger("_fireExtinguisherCharge", 100);
            Preferences.setBoolean("_fireExtinguisherRefilled", true);
          }
        }
        WildfireCampRequest.parseCaptain(text);
      }
      case 1452 -> {
        // Sprinkler Joe
        if (ChoiceManager.lastDecision == 1 && text.contains("raindrop.gif")) {
          Preferences.setBoolean("wildfireSprinkled", true);
        }
      }
      case 1453 -> {
        // Fracker Dan
        if (ChoiceManager.lastDecision == 1 && text.contains("raindrop.gif")) {
          Preferences.setBoolean("wildfireFracked", true);
        }
      }
      case 1454 -> {
        // Cropduster Dusty
        if (ChoiceManager.lastDecision == 1 && text.contains("raindrop.gif")) {
          Preferences.setBoolean("wildfireDusted", true);
        }
      }
      case 1455 -> {
        // Cold Medicine Cabinet
        if (ChoiceManager.lastDecision != 6) {
          if (ChoiceManager.lastDecision == 1) {
            Preferences.increment("_coldMedicineEquipmentTaken", 1, 2, false);
          }

          Preferences.increment("_coldMedicineConsults", 1, 5, false);
          Preferences.setInteger("_nextColdMedicineConsult", KoLCharacter.getTurnsPlayed() + 20);
        }
      }

      case 1467, 1468, 1469, 1470, 1471, 1472, 1473, 1474, 1475 -> // June cleaver
      JuneCleaverManager.parseChoice(urlString);
      case 1481 -> JurassicParkaCommand.parseChoice(ChoiceManager.lastDecision);

      case 1505 -> {
        // Sing!
        // This should not fail without intentionally submitting a bad option.
        if (text.contains("You sing:")) {
          AdventureResult item = ChoiceManager.lastItemUsed;
          if (item == null) {
            // Unexpected
            return;
          }
          int itemId = item.getItemId();
          // Remove the microphone you used from inventory
          // Add the more-used version to inventory
          switch (itemId) {
            case ItemPool.LOATHING_IDOL_MICROPHONE:
              ResultProcessor.processItem(ItemPool.LOATHING_IDOL_MICROPHONE_75, 1);
              break;
            case ItemPool.LOATHING_IDOL_MICROPHONE_75:
              ResultProcessor.processItem(ItemPool.LOATHING_IDOL_MICROPHONE_50, 1);
              break;
            case ItemPool.LOATHING_IDOL_MICROPHONE_50:
              ResultProcessor.processItem(ItemPool.LOATHING_IDOL_MICROPHONE_25, 1);
              break;
            case ItemPool.LOATHING_IDOL_MICROPHONE_25:
              break;
            default:
              // Should not get here unless UseItemRequest did not parse the item.
              return;
          }
          ResultProcessor.processItem(itemId, -1);
        }
      }
        // Burning Leaves
      case 1510 -> {
        String leaves = request.getFormField("leaves");
        // Redirects to fight.php also run through postChoice, but those are handled elsewhere
        // in GenericRequest.checkChoiceRedirection
        if (leaves != null) BurningLeavesRequest.logLeavesBurned(leaves);
      }
      case 1512, 1513 -> {
        if (ChoiceManager.lastDecision == 1 && text.contains("You press the button.")) {
          Preferences.setString(
              "_automatedFutureSide", ChoiceManager.lastChoice == 1512 ? "solenoids" : "bearings");
          Preferences.increment("_automatedFutureManufactures", 1, 11, false);
        }
      }
      case 1517 -> {
        // Mimic DNA Bank
        switch (ChoiceManager.lastDecision) {
          case 1 -> {
            if (text.contains("You donate your egg to science.")) {
              ResultProcessor.processItem(ItemPool.MIMIC_EGG, -1);
              updateMimicMonsters(urlString, -1);
              Preferences.increment("_mimicEggsDonated", 1, 3, false);
            }
          }
          case 2 -> {
            if (text.contains("pops into a backroom")) {
              Preferences.increment("_mimicEggsObtained", 1, 11, false);
              updateMimicMonsters(urlString, 1);
              KoLCharacter.getFamiliar().addNonCombatExperience(-100);
            } else if (text.contains("can't extract")) {
              Preferences.setInteger("_mimicEggsObtained", 11);
            }
          }
        }
      }
      case 1518 -> {
        // Black and White Apron Meal Kit
        if (text.contains("You cook and quickly consume your")) {
          ResultProcessor.processItem(ItemPool.BLACK_AND_WHITE_APRON_MEAL_KIT, -1);
          if (Preferences.getInteger("bwApronMealsEaten") >= 0) {
            // Known starting point.
            Preferences.increment("bwApronMealsEaten");
          }
        }
      }
      case 1534 -> {
        // Clan Photo Booth - Get your photo taken
        if (ChoiceManager.lastDecision != 6 && text.contains("You select")) {
          Preferences.increment("_photoBoothEffects");
        }
      }
      case 1535 -> {
        // Clan Photo Booth - Borrow a prop
        if (ChoiceManager.lastDecision != 12 && text.contains("You grab your prop")) {
          Preferences.increment("_photoBoothEquipment");
        }
      }
      case 1555 -> {
        // Specimen Preparation Bench
        if (ChoiceManager.lastDecision == 1) {
          if (text.contains("You inject the viscous liquid")) {
            KoLCharacter.getFamiliar().addNonCombatExperience(20);
            Preferences.increment("zootSpecimensPrepared", 1, 11, false);
          }
        }
      }
      case 1558 -> {
        // Foreseeing Peril
        // This choice shows items given to others but will never actually give you an item.
        request.setHasResult(false);

        if (text.contains("You've already seen too much peril.")) {
          Preferences.setInteger("_perilsForeseen", 3);
        }

        if (text.contains("Sorry, you can't foresee your own peril.")) {
          KoLmafia.updateDisplay(
              KoLConstants.MafiaState.ERROR, "Sorry, you can't foresee your own peril.");
          break;
        }

        if (text.contains(
            "That player is already suffering the peril of hardcore or ronin, no need to add more.")) {
          KoLmafia.updateDisplay(
              KoLConstants.MafiaState.ERROR,
              "That player is already suffering the peril of hardcore or ronin, no need to add more.");
          break;
        }

        if (text.contains("You gaze into your Peridot and foresee a horrible future")) {
          Preferences.increment("_perilsForeseen", 1, 3, false);
          break;
        }
      }
    }
  }

  // <td align="center" valign="middle"><a href="choice.php?whichchoice=810&option=1&slot=7&pwd=xxx"
  // style="text-decoration:none"><img alt='Toybot (Level 3)' title='Toybot (Level 3)' border=0
  // src='http://images.kingdomofloathing.com/otherimages/crimbotown/krampus_toybot.gif' /></a></td>
  private static final Pattern URL_SLOT_PATTERN = Pattern.compile("slot=(\\d+)");
  private static final Pattern BOT_PATTERN = Pattern.compile("<td.*?<img alt='([^']*)'.*?</td>");

  private static String findKRAMPUSBot(final String urlString, final String responseText) {
    Matcher slotMatcher = URL_SLOT_PATTERN.matcher(urlString);
    if (!slotMatcher.find()) {
      return null;
    }
    String slotString = slotMatcher.group(0);
    Matcher botMatcher = BOT_PATTERN.matcher(responseText);
    while (botMatcher.find()) {
      if (botMatcher.group(0).contains(slotString)) {
        return botMatcher.group(1);
      }
    }
    return null;
  }

  private static final Pattern MONSTER_ID_PATTERN = Pattern.compile("mid=(\\d+)");

  public static void updateMimicMonsters(final String urlString, final int increment) {
    var matcher = MONSTER_ID_PATTERN.matcher(urlString);
    if (!matcher.find()) return;
    int mid = Integer.parseInt(matcher.group(1));
    updateMimicMonsters(mid, increment);
  }

  public static void updateMimicMonsters(final int monsterId, final int increment) {
    var mid = String.valueOf(monsterId);

    // Parse the string into a map
    var map =
        Arrays.stream(Preferences.getString("mimicEggMonsters").split(","))
            .map(pair -> pair.split(":"))
            .filter(pair -> pair.length == 2)
            .map(pair -> Map.entry(pair[0], Integer.parseInt(pair[1])))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    // Update the map
    map.compute(mid, (k, v) -> (v == null ? 0 : v) + increment);

    // Encode the map back into a string, removing any monsters for whom we have fewer than one eggs
    var updated =
        map.entrySet().stream()
            .filter(e -> e.getValue() > 0)
            .map(e -> e.getKey() + ":" + e.getValue())
            .collect(Collectors.joining(","));

    Preferences.setString("mimicEggMonsters", updated);
  }

  private static final Pattern BENCH_WARRANT_PATTERN =
      Pattern.compile("creep <font color=blueviolet><b>(\\d+)</b></font> of them");
  private static final Pattern BENCH_WARRANT_CANDY_PATTERN =
      Pattern.compile("scare off <font color=blueviolet><b>(\\d+)</b></font> protestors");
  private static final Pattern FOG_PATTERN = Pattern.compile("<font.*?><b>(.*?)</b></font>");
  private static final Pattern LOV_EXIT_PATTERN =
      Pattern.compile("a sign above it that says <b>(.*?)</b>");
  private static final Pattern LOV_LOGENTRY_PATTERN = Pattern.compile("you scrawl <b>(.*?)</b>");
  private static final Pattern LYNYRD_PATTERN =
      Pattern.compile(
          "(?:scare|group of|All) <b>(\\d+)</b> (?:of the protesters|protesters|of them)");
  private static final Pattern POOL_SKILL_PATTERN = Pattern.compile("(\\d+) Pool Skill</b>");
  private static final Pattern PINK_WORD_PATTERN =
      Pattern.compile(
          "scrawled in lipstick on a cocktail napkin:  <b><font color=pink>(.*?)</font></b>");
  private static final Pattern STILL_PATTERN =
      Pattern.compile("toss (.*?) cocktail onions into the still");
  private static final Pattern TIME_SPINNER_PATTERN = Pattern.compile("have (\\d+) minute");
  private static final Pattern TIME_SPINNER_MEDALS_PATTERN =
      Pattern.compile("memory of earning <b>(\\d+) medal");
  private static final Pattern VACCINE_PATTERN =
      Pattern.compile("option value=(\\d+).*?class=button type=submit value=\"([^\"]*)");
  private static final Pattern WALFORD_PATTERN =
      Pattern.compile("\\(Walford's bucket filled by (\\d+)%\\)");
  private static final Pattern SUMMON_WAVE_PATTERN =
      Pattern.compile("sweep it down and point at (.*?).  A huge wave rises from the sea");

  private static final Pattern MAYAM_SYMBOLS =
      Pattern.compile("<img data-pos=\"\\d\" class=\"(used)?\"? alt=\"([^\\s]+)\\s");

  public static void parseMayamCalendar(final String text) {
    var symbolMatcher = MAYAM_SYMBOLS.matcher(text);

    var yams = 0;
    var used = new ArrayList<String>();

    while (symbolMatcher.find()) {
      var shouldAdd = symbolMatcher.group(1) != null;
      var symbol = symbolMatcher.group(2).toLowerCase();

      if (symbol.equals("yam")) symbol += (++yams);
      if (shouldAdd) used.add(symbol);
    }

    Preferences.setString("_mayamSymbolsUsed", String.join(",", used));
  }

  public static void postChoice2(final String urlString, final GenericRequest request) {
    String text = request.responseText;

    switch (ChoiceManager.lastChoice) {
      case 3 -> {
        // The Oracle Will See You Now
        if (text.contains("actually a book")) {
          Preferences.setInteger("lastPlusSignUnlock", KoLCharacter.getAscensions());
        }
      }

      case 7 -> {
        // How Depressing

        if (ChoiceManager.lastDecision == 1) {
          EquipmentManager.discardEquipment(ItemPool.SPOOKY_GLOVE);
        }
      }

      case 21 -> {
        // Under the Knife
        if (ChoiceManager.lastDecision == 1 && text.contains("anaesthetizes you")) {
          Preferences.increment("sexChanges", 1);
          Preferences.setBoolean("_sexChanged", true);
          KoLCharacter.setGender(
              text.contains("in more ways than one") ? Gender.FEMALE : Gender.MALE);
          ConcoctionDatabase.setRefreshNeeded(false);
        }
      }

      case 48,
          49,
          50,
          51,
          52,
          53,
          54,
          55,
          56,
          57,
          58,
          59,
          60,
          61,
          62,
          63,
          64,
          65,
          66,
          67,
          68,
          69,
          70 -> // Choices in the Violet Fog
      VioletFogManager.mapChoice(ChoiceManager.lastChoice, ChoiceManager.lastDecision, text);

      case 71 -> {
        // A Journey to the Center of Your Mind
        String tripZone =
            switch (ChoiceManager.lastDecision) {
              case 1 -> "Bad Trip";
              case 2 -> "Mediocre Trip";
              case 3 -> "Great Trip";
              default -> "";
            };

        // We are now in a pseudo LimitMode
        Preferences.setString("currentAstralTrip", tripZone);
        KoLCharacter.setLimitMode(LimitMode.ASTRAL);
      }

      case 73 -> {
        // Don't Fence Me In
        if (ChoiceManager.lastDecision == 3) {
          if (text.contains("you pick") || text.contains("you manage")) {
            Preferences.increment("_whiteRiceDrops", 1);
          }
        }
      }

      case 89 -> {
        if (ChoiceManager.lastDecision == 4) {
          TurnCounter.startCounting(10, "Garden Banished loc=*", "wolfshield.gif");
        }
      }

      case 105 -> {
        if (ChoiceManager.lastDecision == 3) {
          checkGuyMadeOfBees(request);
        }
      }

      case 112 -> {
        // Please, Hammer
        if (ChoiceManager.lastDecision == 1 && KoLmafia.isAdventuring()) {
          InventoryManager.retrieveItem(ItemPool.get(ItemPool.HAROLDS_HAMMER, 1));
        }
      }

      case 125 -> {
        // No visible means of support
        if (ChoiceManager.lastDecision == 3) {
          QuestDatabase.setQuestProgress(Quest.WORSHIP, "step3");
        }
      }

      case 132 -> {
        // Let's Make a Deal!
        if (ChoiceManager.lastDecision == 2) {
          QuestDatabase.setQuestProgress(Quest.PYRAMID, "step1");
        }
      }

      case 162 -> {
        // Between a Rock and Some Other Rocks
        if (KoLmafia.isAdventuring()
            && !EquipmentManager.isWearingOutfit(OutfitPool.MINING_OUTFIT)
            && !KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.EARTHEN_FIST))) {
          QuestManager.unlockGoatlet();
        }
      }

      case 197,
          // Somewhat Higher and Mostly Dry
          198,
          // Disgustin' Junction
          199 -> {
        // The Former or the Ladder
        if (ChoiceManager.lastDecision == 1) {
          checkDungeonSewers(request);
        }
      }

      case 200,
          // Enter The Hoboverlord
          201,
          // Home, Home in the Range
          202,
          // Bumpity Bump Bump
          203,
          // Deep Enough to Dive
          204,
          // Welcome To You!
          205 -> {
        // Van, Damn

        // Stop for Hobopolis bosses
        if (ChoiceManager.lastDecision == 2 && KoLmafia.isAdventuring()) {
          KoLmafia.updateDisplay(
              MafiaState.PENDING, hobopolisBossName(ChoiceManager.lastChoice) + " waits for you.");
        }
      }

      case 276 -> {
        // The Gong Has Been Bung
        String form =
            switch (ChoiceManager.lastDecision) {
              case 1 -> "Roach";
              case 2 -> "Mole";
              case 3 -> "Bird";
              default -> "";
            };

        // We are now in a pseudo LimitMode
        Preferences.setString("currentLlamaForm", form);
        // This will look at the property and set actual LimitMode
        KoLCharacter.setLimitMode(LimitMode.NONE);
      }

      case 277 -> {
        // Welcome Back!
        Preferences.setString("currentLlamaForm", "");
        KoLCharacter.setLimitMode(LimitMode.NONE);
      }

      case 299 -> {
        // Down at the Hatch
        if (ChoiceManager.lastDecision == 1) {
          // The first time you take option 1, you
          // release Big Brother. Subsequent times, you
          // release other creatures.
          if (Preferences.getBoolean("bigBrotherRescued")) {
            Preferences.setInteger("_lastFitzsimmonsHatch", KoLCharacter.getTurnsPlayed());
          }
          QuestDatabase.setQuestIfBetter(Quest.SEA_MONKEES, "step2");
          Preferences.setBoolean("bigBrotherRescued", true);
          ConcoctionDatabase.setRefreshNeeded(false);
        }
      }

      case 302,
          303,
          // You've Hit Bottom (Pastamancer, Sauceror)
          306,
          // Not a Micro Fish (Seal Clubber, Turtle Tamer)
          307,
          // Ode to the Sea (Disco Bandit)
          308 -> // Boxing the Juke (Accordion Thief)
      QuestDatabase.setQuestIfBetter(Quest.SEA_MONKEES, "step5");

      case 304 -> {
        // A Vent Horizon

        // "You conjure some delicious batter from the core of
        // the thermal vent. It pops and sizzles as you stick
        // it in your sack."

        if (text.contains("pops and sizzles")) {
          Preferences.increment("tempuraSummons", 1);
        }
      }

      case 309 -> {
        // Barback

        // "You head down the tunnel into the cave, and manage
        // to find another seaode. Sweet! I mean... salty!"

        if (text.contains("salty!")) {
          Preferences.increment("seaodesFound", 1);
        }
      }

      case 326 -> {
        // Showdown

        if (ChoiceManager.lastDecision == 2 && KoLmafia.isAdventuring()) {
          KoLmafia.updateDisplay(MafiaState.ABORT, "Mother Slime waits for you.");
        }
      }

      case 330 -> {
        // A Shark's Chum
        if (ChoiceManager.lastDecision == 1) {
          Preferences.increment("poolSharkCount", 1);
        }
      }

      case 360 -> WumpusManager.takeChoice(ChoiceManager.lastDecision, text);

      case 441 -> {
        // The Mad Tea Party

        // I'm sorry, but there's a very strict dress code for
        // this party

        if (ChoiceManager.lastDecision == 1 && !text.contains("very strict dress code")) {
          Preferences.setBoolean("_madTeaParty", true);
        }
      }

      case 442 -> {
        // A Moment of Reflection
        if (ChoiceManager.lastDecision == 5) {
          // Option 5 is Chess Puzzle
          RabbitHoleManager.parseChessPuzzle(text);
        }

        if (ChoiceManager.lastDecision != 6) {
          // Option 6 does not consume the map. Others do.
          ResultProcessor.processItem(ItemPool.REFLECTION_OF_MAP, -1);
        }
      }

      case 508 -> {
        // Pants-Gazing
        if (text.contains("You acquire an effect")) {
          Preferences.increment("_gapBuffs", 1);
        }
      }

      case 517 -> // Mr. Alarm, I presarm
      QuestDatabase.setQuestIfBetter(Quest.PALINDOME, "step3");

      case 518 -> {
        // Clear and Present Danger

        // Stop for Hobopolis bosses
        if (ChoiceManager.lastDecision == 2 && KoLmafia.isAdventuring()) {
          KoLmafia.updateDisplay(
              MafiaState.PENDING, hobopolisBossName(ChoiceManager.lastChoice) + " waits for you.");
        }
      }

      case 524 -> {
        // The Adventures of Lars the Cyberian
        if (text.contains("Skullhead's Screw")) {
          // You lose the book if you receive the reward.
          // I don't know if that's always the result of
          // the same choice option
          ResultProcessor.processItem(ItemPool.LARS_THE_CYBERIAN, -1);
        }
      }

      case 548 -> {
        // Behind Closed Doors
        if (ChoiceManager.lastDecision == 2 && KoLmafia.isAdventuring()) {
          KoLmafia.updateDisplay(MafiaState.ABORT, "The Necbromancer waits for you.");
        }
      }

      case 549 -> {
        // Dark in the Attic
        if (text.contains("The silver pellets tear through the sorority werewolves")) {
          ResultProcessor.processItem(ItemPool.SILVER_SHOTGUN_SHELL, -1);
          RequestLogger.printLine("You took care of a bunch of werewolves.");
        } else if (text.contains("quietly sneak away")) {
          RequestLogger.printLine("You need a silver shotgun shell to kill werewolves.");
        } else if (text.contains("a loose shutter")) {
          RequestLogger.printLine("All the werewolves have been defeated.");
        } else if (text.contains("crank up the volume on the boombox")) {
          RequestLogger.printLine("You crank up the volume on the boombox.");
        } else if (text.contains("a firm counterclockwise twist")) {
          RequestLogger.printLine("You crank down the volume on the boombox.");
        }
      }

      case 550 -> {
        // The Unliving Room
        if (text.contains("you pull out the chainsaw blades")) {
          ResultProcessor.processItem(ItemPool.CHAINSAW_CHAIN, -1);
          RequestLogger.printLine("You took out a bunch of zombies.");
        } else if (text.contains("a wet tearing noise")) {
          RequestLogger.printLine("You need a chainsaw chain to kill zombies.");
        } else if (text.contains("a bloody tangle")) {
          RequestLogger.printLine("All the zombies have been defeated.");
        } else if (text.contains("the skeletons collapse into piles of loose bones")) {
          ResultProcessor.processItem(ItemPool.FUNHOUSE_MIRROR, -1);
          RequestLogger.printLine("You made short work of some skeletons.");
        } else if (text.contains("couch in front of the door")) {
          RequestLogger.printLine("You need a funhouse mirror to kill skeletons.");
        } else if (text.contains("just coats")) {
          RequestLogger.printLine("All the skeletons have been defeated.");
        } else if (text.contains("close the windows")) {
          RequestLogger.printLine("You close the windows.");
        } else if (text.contains("open the windows")) {
          RequestLogger.printLine("You open the windows.");
        }
      }

      case 551 -> {
        // Debasement
        if (text.contains("the vampire girls shriek")) {
          RequestLogger.printLine("You slew some vampires.");
        } else if (text.contains("gets back in her coffin")) {
          RequestLogger.printLine("You need to equip plastic vampire fangs to kill vampires.");
        } else if (text.contains("they recognize you")) {
          RequestLogger.printLine("You have already killed some vampires.");
        } else if (text.contains("crank up the fog machine")) {
          RequestLogger.printLine("You crank up the fog machine.");
        } else if (text.contains("turn the fog machine way down")) {
          RequestLogger.printLine("You crank down the fog machine.");
        }
      }

      case 553 -> {
        // Relocked and Reloaded
        if (text.contains("You melt")) {
          int item =
              switch (ChoiceManager.lastDecision) {
                case 1 -> ItemPool.MAXWELL_HAMMER;
                case 2 -> ItemPool.TONGUE_BRACELET;
                case 3 -> ItemPool.SILVER_CHEESE_SLICER;
                case 4 -> ItemPool.SILVER_SHRIMP_FORK;
                case 5 -> ItemPool.SILVER_PATE_KNIFE;
                default -> 0;
              };
          if (item > 0) {
            ResultProcessor.processItem(item, -1);
          }
        }
      }

      case 558 -> {
        // Tool Time
        if (text.contains("You acquire an item")) {
          int amount = 3 + ChoiceManager.lastDecision;
          ResultProcessor.processItem(ItemPool.LOLLIPOP_STICK, -amount);
        }
      }

      case 560 -> // Foreshadowing Demon!
      QuestDatabase.setQuest(
          Quest.CLUMSINESS,
          ChoiceManager.lastDecision == 1 ? QuestDatabase.STARTED : QuestDatabase.UNSTARTED);

      case 561 -> {
        // You Must Choose Your Destruction!
        QuestDatabase.setQuest(Quest.CLUMSINESS, "step1");
        Preferences.setString(
            "clumsinessGroveBoss",
            ChoiceManager.lastDecision == 1 ? "The Thorax" : "The Bat in the Spats");
      }

      case 563 -> {
        // A Test of Your Mettle
        if (ChoiceManager.lastDecision == 1) {
          String nextBoss =
              InventoryManager.getCount(ItemPool.VANITY_STONE) > 0
                  ? "The Thorax"
                  : "The Bat in the Spats";
          Preferences.setString("clumsinessGroveBoss", nextBoss);
          QuestDatabase.setQuest(Quest.CLUMSINESS, "step3");
        }
      }

      case 564 -> // A Maelstrom of Trouble
      QuestDatabase.setQuest(
          Quest.MAELSTROM,
          ChoiceManager.lastDecision == 1 ? QuestDatabase.STARTED : QuestDatabase.UNSTARTED);

      case 565 -> {
        // To Get Groped or Get Mugged?
        QuestDatabase.setQuest(Quest.MAELSTROM, "step1");
        Preferences.setString(
            "maelstromOfLoversBoss",
            ChoiceManager.lastDecision == 1 ? "The Terrible Pinch" : "Thug 1 and Thug 2");
      }

      case 566 -> {
        // A Choice to be Made
        if (ChoiceManager.lastDecision == 1) {
          String nextBoss =
              InventoryManager.getCount(ItemPool.JEALOUSY_STONE) > 0
                  ? "The Terrible Pinch"
                  : "Thug 1 and Thug 2";
          Preferences.setString("maelstromOfLoversBoss", nextBoss);
          QuestDatabase.setQuest(Quest.MAELSTROM, "step3");
        }
      }

      case 567 -> // You May Be on Thin Ice
      QuestDatabase.setQuest(
          Quest.GLACIER,
          ChoiceManager.lastDecision == 1 ? QuestDatabase.STARTED : QuestDatabase.UNSTARTED);

      case 568 -> {
        // Some Sounds Most Unnerving
        QuestDatabase.setQuest(Quest.GLACIER, "step1");
        Preferences.setString(
            "glacierOfJerksBoss",
            ChoiceManager.lastDecision == 1 ? "Mammon the Elephant" : "The Large-Bellied Snitch");
      }

      case 569 -> {
        // One More Demon to Slay
        if (ChoiceManager.lastDecision == 1) {
          String nextBoss =
              InventoryManager.getCount(ItemPool.GLUTTONOUS_STONE) > 0
                  ? "Mammon the Elephant"
                  : "The Large-Bellied Snitch";
          Preferences.setString("glacierOfJerksBoss", nextBoss);
          QuestDatabase.setQuest(Quest.GLACIER, "step3");
        }
      }

      case 578 -> // End of the Boris Road
      handleAfterAvatar(ChoiceManager.lastDecision);

      case 579 -> {
        // Such Great Heights
        if (ChoiceManager.lastDecision == 3
            && Preferences.getInteger("lastTempleAdventures") != KoLCharacter.getAscensions()) {
          Preferences.setInteger("lastTempleAdventures", KoLCharacter.getAscensions());
          // Also resets your Mayam Calendar symbols, if you have used any.
          Preferences.setString("_mayamSymbolsUsed", "");
        }
      }

      case 581 -> {
        // Such Great Depths
        if (ChoiceManager.lastDecision == 2) {
          Preferences.setBoolean("_templeHiddenPower", true);
        }
      }

      case 584 -> {
        // Unconfusing Buttons
        if (Preferences.getInteger("lastTempleButtonsUnlock") != KoLCharacter.getAscensions()) {
          Preferences.setInteger("lastTempleButtonsUnlock", KoLCharacter.getAscensions());
        }
        if (InventoryManager.getCount(ItemPool.NOSTRIL_OF_THE_SERPENT) > 0) {
          ResultProcessor.processItem(ItemPool.NOSTRIL_OF_THE_SERPENT, -1);
        }
        if (ChoiceManager.lastDecision == 4) {
          QuestDatabase.setQuestIfBetter(Quest.WORSHIP, "step2");
        }
      }

      case 595 -> // Fire! I... have made... fire!
      Preferences.setBoolean("_fireStartingKitUsed", true);

      case 597 -> {
        // Pick a Part
        Preferences.setBoolean("_gnomePart", true);
        ConcoctionDatabase.refreshConcoctionsNow();
      }

      case 602 -> {
        // Behind the Gash
        // This is a multi-part choice adventure, and we only want to handle the last choice
        if (text.contains("you shout into the blackness")) {
          handleAfterAvatar(ChoiceManager.lastDecision);
        }
      }

      case 613 -> {
        // Behind the door there is a fog
        if (ChoiceManager.lastDecision == 1) {
          Matcher fogMatcher = FOG_PATTERN.matcher(text);
          if (fogMatcher.find()) {
            String message = "Message: \"" + fogMatcher.group(1) + "\"";
            RequestLogger.printLine(message);
            RequestLogger.updateSessionLog(message);
          }
        }
      }

      case 640 -> // Tailor the Snow Suit
      SnowsuitCommand.setStateFromDecision(ChoiceManager.lastDecision);

      case 641 -> {
        // Stupid Pipes.
        if (ChoiceManager.lastDecision == 2 && text.contains("flickering pixel")) {
          Preferences.setBoolean("flickeringPixel1", true);
        }
      }

      case 642 -> {
        // You're Freaking Kidding Me
        if (ChoiceManager.lastDecision == 2 && text.contains("flickering pixel")) {
          Preferences.setBoolean("flickeringPixel2", true);
        }
      }

      case 644 -> {
        // Snakes.
        if (ChoiceManager.lastDecision == 2 && text.contains("flickering pixel")) {
          Preferences.setBoolean("flickeringPixel3", true);
        }
      }

      case 645 -> {
        // So... Many... Skulls...
        if (ChoiceManager.lastDecision == 2 && text.contains("flickering pixel")) {
          Preferences.setBoolean("flickeringPixel4", true);
        }
      }

      case 647 -> {
        // A Stupid Dummy. Also, a Straw Man.
        if (ChoiceManager.lastDecision == 2 && text.contains("flickering pixel")) {
          Preferences.setBoolean("flickeringPixel5", true);
        }
      }

      case 648 -> {
        // Slings and Arrows
        if (ChoiceManager.lastDecision == 2 && text.contains("flickering pixel")) {
          Preferences.setBoolean("flickeringPixel6", true);
        }
      }

      case 650 -> {
        // This Is Your Life. Your Horrible, Horrible Life.
        if (ChoiceManager.lastDecision == 2 && text.contains("flickering pixel")) {
          Preferences.setBoolean("flickeringPixel7", true);
        }
      }

      case 651 -> {
        // The Wall of Wailing
        if (ChoiceManager.lastDecision == 2 && text.contains("flickering pixel")) {
          Preferences.setBoolean("flickeringPixel8", true);
        }
      }

      case 677 -> {
        // Copper Feel
        if (ChoiceManager.lastDecision == 1) {
          ResultProcessor.removeItem(ItemPool.MODEL_AIRSHIP);
        }
      }

      case 682 -> // Now Leaving Jarlsberg, Population You
      handleAfterAvatar(ChoiceManager.lastDecision);

      case 696 -> {
        // Stick a Fork In It
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("maraisDarkUnlock", true);
        } else if (ChoiceManager.lastDecision == 2) {
          Preferences.setBoolean("maraisWildlifeUnlock", true);
        }
      }

      case 697 -> {
        // Sophie's Choice
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("maraisCorpseUnlock", true);
        } else if (ChoiceManager.lastDecision == 2) {
          Preferences.setBoolean("maraisWizardUnlock", true);
        }
      }

      case 698 -> {
        // From Bad to Worst
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("maraisBeaverUnlock", true);
        } else if (ChoiceManager.lastDecision == 2) {
          Preferences.setBoolean("maraisVillageUnlock", true);
        }
      }

      case 704 -> { // Playing the Catalog Card
        DreadScrollManager.handleLibrary(text);
        postChoiceCatalog(text, ChoiceManager.lastDecision);
      }

      case 774 -> {
        // Opening up the Folder Holder

        // Choice 1 is adding a folder.
        if (ChoiceManager.lastDecision == 1
            && text.contains("You carefully place your new folder in the holder")) {
          // Figure out which one it was from the URL
          String id = request.getFormField("folder");
          AdventureResult folder = EquipmentRequest.idToFolder(id);
          ResultProcessor.removeItem(folder.getItemId());
        }

        // Choice 2 is removing a folder. Since the folder is
        // destroyed, it does not go back to inventory.

        // Set all folder slots from the response text
        EquipmentRequest.parseFolders(text);
      }

      case 786 -> {
        if (ChoiceManager.lastDecision == 2) {
          ResultProcessor.autoCreate(ItemPool.MCCLUSKY_FILE);
        }
      }

      case 798 -> {
        // Hippy Talkin'
        if (text.contains("Point me at the landfill")) {
          QuestDatabase.setQuestProgress(Quest.HIPPY, QuestDatabase.STARTED);
        }
      }

      case 810 -> {
        if (ChoiceManager.lastDecision == 2) {
          ResultProcessor.processItem(ItemPool.WARBEAR_WHOSIT, -100);
        } else if (ChoiceManager.lastDecision == 4 && text.contains("You upgrade the robot!")) {
          String bot = findKRAMPUSBot(request.getURLString(), text);
          int cost =
              (bot == null) ? 0 : bot.contains("Level 2") ? 250 : bot.contains("Level 3") ? 500 : 0;
          if (cost != 0) {
            ResultProcessor.processItem(ItemPool.WARBEAR_WHOSIT, -cost);
          }
        }
      }

      case 829,
          // We all wear masks
          822, // The Prince's Ball (In the Restroom)
          823, // The Prince's Ball (On the Dance Floor)
          824, // The Prince's Ball (The Kitchen)
          825, // The Prince's Ball (On the Balcony)
          826, // The Prince's Ball (The Lounge)
          827, // The Prince's Ball (At the Canapés Table)
          // stepmother
          830, // Cooldown
          832, // Shower Power
          833, // Vendi, Vidi, Vici
          834, // Back Room Dealings
          // wolf
          831, // Intrusion
          837, // On Purple Pond
          838, // General Mill
          839, // The Sounds of the Undergrounds
          840, // Hop on Rock Pops
          841, // Building, Structure, Edifice
          842, // The Gingerbread Warehouse
          // witch
          844, // The Portal to Horrible Parents
          845, // Rumpelstiltskin's Workshop
          846, // Bartering for the Future of Innocent Children
          847, // Pick Your Poison
          848, // Where the Magic Happens
          849, // The Practice
          850 -> // World of Bartercraft
      // gnome
      GrimstoneManager.postChoice2(text);

      case 854 -> {
        // Shen Copperhead, World's Biggest Jerk
        if (InventoryManager.hasItem(ItemPool.COPPERHEAD_CHARM)
            && InventoryManager.hasItem(ItemPool.COPPERHEAD_CHARM_RAMPANT)) {
          ResultProcessor.autoCreate(ItemPool.TALISMAN);
        }
      }

      case 855 -> {
        // Behind the 'Stache
        String hazard =
            switch (ChoiceManager.lastDecision) {
              case 1, 5 -> "gong";
              case 2 -> "ice";
              case 3 -> "lantern";
              default -> Preferences.getString("copperheadClubHazard");
            };
        Preferences.setString("copperheadClubHazard", hazard);
        if (ChoiceManager.lastDecision == 5) {
          Preferences.setBoolean("candyCaneSwordCopperheadClub", true);
        }
      }

      case 856 -> {
        // This Looks Like a Good Bush for an Ambush
        Matcher lynyrdMatcher = LYNYRD_PATTERN.matcher(text);
        if (lynyrdMatcher.find()) {
          int protestersScared = StringUtilities.parseInt(lynyrdMatcher.group(1));
          Preferences.increment("zeppelinProtestors", protestersScared);
          RequestLogger.printLine("Scared off " + protestersScared + " protesters");
        }
      }

      case 857 -> {
        // Bench Warrant
        Matcher benchWarrantMatcher = BENCH_WARRANT_PATTERN.matcher(text);
        if (benchWarrantMatcher.find()) {
          int protestersCreeped = StringUtilities.parseInt(benchWarrantMatcher.group(1));
          Preferences.increment("zeppelinProtestors", protestersCreeped);
          RequestLogger.printLine("Creeped out " + protestersCreeped + " protesters");
        } else {
          benchWarrantMatcher = BENCH_WARRANT_CANDY_PATTERN.matcher(text);
          if (benchWarrantMatcher.find()) {
            int protestersCreeped = StringUtilities.parseInt(benchWarrantMatcher.group(1));
            Preferences.increment("zeppelinProtestors", protestersCreeped);
            RequestLogger.printLine("Scared off " + protestersCreeped + " protesters");
          }
        }
      }

      case 858 -> {
        // Fire Up Above
        if (text.contains("three nearest protesters")) {
          Preferences.increment("zeppelinProtestors", 3);
          RequestLogger.printLine("Soaked 3 protesters");
        } else if (text.contains("Flamin' Whatshisname")) {
          Preferences.increment("zeppelinProtestors", 10);
          ResultProcessor.processItem(ItemPool.FLAMIN_WHATSHISNAME, -1);
          RequestLogger.printLine("Set fire to 10 protesters");
        }
      }

      case 860 -> {
        // Another Tired Retread
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setString("peteMotorbikeTires", "Racing Slicks");
        } else if (ChoiceManager.lastDecision == 2) {
          Preferences.setString("peteMotorbikeTires", "Spiky Tires");
        } else if (ChoiceManager.lastDecision == 3) {
          Preferences.setString("peteMotorbikeTires", "Snow Tires");
        }
      }
      case 861 -> {
        // Station of the Gas
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setString("peteMotorbikeGasTank", "Large Capacity Tank");
          KoLCharacter.setDesertBeachAvailable();
        } else if (ChoiceManager.lastDecision == 2) {
          Preferences.setString("peteMotorbikeGasTank", "Extra-Buoyant Tank");
          Preferences.setInteger("lastIslandUnlock", KoLCharacter.getAscensions());
        } else if (ChoiceManager.lastDecision == 3) {
          Preferences.setString("peteMotorbikeGasTank", "Nitro-Burnin' Funny Tank");
        }
      }
      case 862 -> {
        // Me and Cinderella Put It All Together
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setString("peteMotorbikeHeadlight", "Ultrabright Yellow Bulb");
        } else if (ChoiceManager.lastDecision == 2) {
          Preferences.setString("peteMotorbikeHeadlight", "Party Bulb");
        } else if (ChoiceManager.lastDecision == 3) {
          Preferences.setString("peteMotorbikeHeadlight", "Blacklight Bulb");
        }
      }
      case 863 -> {
        // Endowing the Cowling
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setString("peteMotorbikeCowling", "Ghost Vacuum");
        } else if (ChoiceManager.lastDecision == 2) {
          Preferences.setString("peteMotorbikeCowling", "Rocket Launcher");
        } else if (ChoiceManager.lastDecision == 3) {
          Preferences.setString("peteMotorbikeCowling", "Sweepy Red Light");
        }
      }
      case 864 -> {
        // Diving into the Mufflers
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setString("peteMotorbikeMuffler", "Extra-Loud Muffler");
        } else if (ChoiceManager.lastDecision == 2) {
          Preferences.setString("peteMotorbikeMuffler", "Extra-Quiet Muffler");
        } else if (ChoiceManager.lastDecision == 3) {
          Preferences.setString("peteMotorbikeMuffler", "Extra-Smelly Muffler");
        }
      }
      case 865 -> {
        // Ayy, Sit on It
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setString("peteMotorbikeSeat", "Massage Seat");
        } else if (ChoiceManager.lastDecision == 2) {
          Preferences.setString("peteMotorbikeSeat", "Deep Seat Cushions");
        } else if (ChoiceManager.lastDecision == 3) {
          Preferences.setString("peteMotorbikeSeat", "Sissy Bar");
        }
      }

      case 869 -> // End of Pete Road
      handleAfterAvatar(ChoiceManager.lastDecision);

      case 875 -> {
        // Welcome To Our ool Table
        Matcher poolSkillMatcher = POOL_SKILL_PATTERN.matcher(text);
        if (poolSkillMatcher.find()) {
          Preferences.increment("poolSkill", StringUtilities.parseInt(poolSkillMatcher.group(1)));
        }
      }

      case 918 -> {
        // Yachtzee!
        Preferences.setInteger("encountersUntilYachtzeeChoice", 19);
        if (text.contains("Ultimate Mind Destroyer")) {
          var date = DateTimeManager.getArizonaDateTime();
          Preferences.setString(
              "umdLastObtained", date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
      }

      case 921 -> // We'll All Be Flat
      QuestDatabase.setQuestProgress(Quest.MANOR, "step1");

      case 928 -> {
        // The Blackberry Cobbler
        if (ChoiceManager.lastDecision != 6) {
          ResultProcessor.processItem(ItemPool.BLACKBERRY, -3);
        }
      }

      case 929 -> {
        // Control Freak
        if (ChoiceManager.lastDecision == 1 && text.contains("wooden wheel disintegrating")) {
          ResultProcessor.processItem(ItemPool.CRUMBLING_WHEEL, -1);
          PyramidRequest.advancePyramidPosition();
        } else if (ChoiceManager.lastDecision == 2
            && text.contains("snap the ratchet onto the peg")) {
          ResultProcessor.processItem(ItemPool.TOMB_RATCHET, -1);
          PyramidRequest.advancePyramidPosition();
        }
      }

      case 940 -> {
        // Let Your Fists Do The Walking
        if (ChoiceManager.lastDecision == 6) {
          ResultProcessor.processItem(ItemPool.WHITE_PAGE, 1);
        }
      }

      case 974 -> {
        // Around The World
        Matcher pinkWordMatcher = PINK_WORD_PATTERN.matcher(text);
        if (pinkWordMatcher.find()) {
          String pinkWord = pinkWordMatcher.group(1);
          String message =
              "Bohemian Party Pink Word found: "
                  + pinkWord
                  + " in clan "
                  + ClanManager.getClanName(false)
                  + ".";
          RequestLogger.printHtml(
              "<font color=\"blue\">"
                  + StringUtilities.getEntityEncode(message, false)
                  + "</font>");
          RequestLogger.updateSessionLog(message);
        }
      }

      case 975 -> {
        // Crazy Still After All These Years
        Matcher stillMatcher = STILL_PATTERN.matcher(text);
        if (stillMatcher.find()) {
          ResultProcessor.processItem(
              ItemPool.COCKTAIL_ONION, -StringUtilities.parseInt(stillMatcher.group(1)));
        }
      }

      case 977 -> {
        // [Chariot Betting]
        if (ChoiceManager.lastDecision < 12) {
          ResultProcessor.processItem(ItemPool.CHRONER, -10);
        }
      }

      case 981 -> {
        // [back room]
        if (ChoiceManager.lastDecision < 12) {
          ResultProcessor.processItem(ItemPool.MERCURY_BLESSING, -1);
        }
      }

      case 982, 983 -> {
        // Playing Dice With Romans & The 99-Centurion Store
        if (ChoiceManager.lastDecision < 6) {
          ResultProcessor.processItem(ItemPool.CHRONER, -1);
        }
      }

      case 1042 -> // Pick a Perk
      KoLmafia.resetAfterLimitmode();

      case 1054 -> {
        // Returning the MacGuffin
        QuestDatabase.setQuestProgress(Quest.WAREHOUSE, QuestDatabase.FINISHED);
        if (ChoiceManager.lastDecision == 1) {
          ResultProcessor.processItem(ItemPool.ED_HOLY_MACGUFFIN, -1);
        }
      }

      case 1055 -> {
        // Returning the MacGuffin
        KoLCharacter.liberateKing();
        handleAfterAvatar(ChoiceManager.lastDecision);
      }

      case 1062 -> {
        // Lots of Options
        if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.processItem(ItemPool.BOOZE_MAP, -1);
        }
      }

      case 1073 -> {
        // This Ride Is Like... A Rollercoaster Baby Baby
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("dinseyRollercoasterNext", false);
        }
        if (text.contains("lubricating every inch of the tracks")) {
          QuestDatabase.setQuestProgress(Quest.SUPER_LUBER, "step2");
        }
      }

      case 1089 -> { // Community Service
        if (text.contains("You acquire")) {
          String quest =
              switch (ChoiceManager.lastDecision) {
                case 1 -> "Donate Blood";
                case 2 -> "Feed The Children";
                case 3 -> "Build Playground Mazes";
                case 4 -> "Feed Conspirators";
                case 5 -> "Breed More Collies";
                case 6 -> "Reduce Gazelle Population";
                case 7 -> "Make Sausage";
                case 8 -> "Be a Living Statue";
                case 9 -> "Make Margaritas";
                case 10 -> "Clean Steam Tunnels";
                case 11 -> "Coil Wire";
                default -> null;
              };
          if (quest != null) {
            String current = Preferences.getString("csServicesPerformed");
            if (current.equals("")) {
              Preferences.setString("csServicesPerformed", quest);
            } else {
              Preferences.setString("csServicesPerformed", current + "," + quest);
            }
          }
        }
      }

      case 1090 -> {
        // The Towering Inferno Discotheque
        if (ChoiceManager.lastDecision > 1) {
          Preferences.setBoolean("_infernoDiscoVisited", true);
        }
      }

      case 1091 -> {
        // Choice 1091 is The Floor Is Yours
        if (text.contains("You acquire")) {
          switch (ChoiceManager.lastDecision) {
            case 1 -> ResultProcessor.processResult(ItemPool.get(ItemPool.GOLD_1970, -1));
            case 2 -> ResultProcessor.processResult(
                ItemPool.get(ItemPool.NEW_AGE_HEALING_CRYSTAL, -1));
            case 3 -> ResultProcessor.processResult(ItemPool.get(ItemPool.EMPTY_LAVA_BOTTLE, -1));
            case 5 -> ResultProcessor.processResult(
                ItemPool.get(ItemPool.GLOWING_NEW_AGE_CRYSTAL, -1));
            case 6 -> {
              ResultProcessor.processResult(ItemPool.get(ItemPool.CRYSTALLINE_LIGHT_BULB, -1));
              ResultProcessor.processResult(ItemPool.get(ItemPool.INSULATED_GOLD_WIRE, -1));
              ResultProcessor.processResult(ItemPool.get(ItemPool.HEAT_RESISTANT_SHEET_METAL, -1));
            }
          }
        }
      }

      case 1092 -> {
        // Choice 1092 is Dyer Maker
        if (text.contains("You acquire")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.VISCOUS_LAVA_GLOBS, -1));
        }
      }

      case 1093 -> {
        // The WLF Bunker

        // A woman in an orange denim jumpsuit emerges from a
        // hidden trap door, smiles, and hands you a coin in
        // exchange for your efforts.

        if (text.contains("hands you a coin")) {
          String index = String.valueOf(ChoiceManager.lastDecision);
          int itemId = Preferences.getInteger("_volcanoItem" + index);
          int count = Preferences.getInteger("_volcanoItemCount" + index);
          if (itemId > 0 && count > 0) {
            ResultProcessor.processResult(ItemPool.get(itemId, -count));
          }
          Preferences.setBoolean("_volcanoItemRedeemed", true);
          Preferences.setInteger("_volcanoItem1", 0);
          Preferences.setInteger("_volcanoItem2", 0);
          Preferences.setInteger("_volcanoItem3", 0);
          Preferences.setInteger("_volcanoItemCount1", 0);
          Preferences.setInteger("_volcanoItemCount2", 0);
          Preferences.setInteger("_volcanoItemCount3", 0);
        }
      }

      case 1100 -> {
        // Pray to the Barrel God
        if (ChoiceManager.lastDecision <= 4) {
          Preferences.setBoolean("_barrelPrayer", true);
          ConcoctionDatabase.refreshConcoctions();
        }
      }

      case 1101 -> {
        // It's a Barrel Smashing Party!
        if (ChoiceManager.lastDecision == 2) {
          // We're smashing 100 barrels
          // The results don't say which barrels are being smashed, but it seems to happen in item
          // order
          int count = 100;
          int itemId = ItemPool.LITTLE_FIRKIN;
          while (count > 0 && itemId <= ItemPool.BARNACLED_BARREL) {
            int smashNumber = Math.min(count, InventoryManager.getCount(itemId));
            if (smashNumber > 0) {
              ResultProcessor.processResult(ItemPool.get(itemId, -smashNumber));
              count -= smashNumber;
            }
            itemId++;
          }
          break;
        }
        int itemId = extractIidFromURL(request.getURLString());
        String name = ItemDatabase.getItemName(itemId);
        if (name != null) {
          ResultProcessor.removeItem(itemId);
        }
      }

      case 1103 -> {
        // Doing the Maths
        if (!text.contains("Try again")) {
          Preferences.increment("_universeCalculated");
        }
      }

      case 1104 -> {
        // Tree Tea
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("_pottedTeaTreeUsed", true);
        }
      }

      case 1105 -> {
        // Specifici Tea
        if (request.getURLString().contains("itemid")) {
          Preferences.setBoolean("_pottedTeaTreeUsed", true);
        }
      }

      case 1110 -> {
        // Spoopy
        if (ChoiceManager.lastDecision == 5) {
          if (text.contains("You board up the doghouse")) {
            Preferences.setBoolean("doghouseBoarded", true);
          } else if (text.contains("You unboard-up the doghouse")) {
            Preferences.setBoolean("doghouseBoarded", false);
          }
        }
      }

      case 1114 -> {
        // Walford Rusley, Bucket Collector
        if (ChoiceManager.lastDecision == 1) {
          QuestDatabase.setQuestProgress(Quest.BUCKET, QuestDatabase.UNSTARTED);
          Preferences.setInteger("walfordBucketProgress", 0);
          Preferences.setString("walfordBucketItem", "");
        } else if (ChoiceManager.lastDecision < 5) {
          QuestDatabase.setQuestProgress(Quest.BUCKET, QuestDatabase.STARTED);
          Preferences.setInteger("walfordBucketProgress", 0);
          Preferences.setBoolean("_walfordQuestStartedToday", true);
          if (text.contains("Bucket of balls")) {
            Preferences.setString("walfordBucketItem", "balls");
          } else if (text.contains("bucket with blood")) {
            Preferences.setString("walfordBucketItem", "blood");
          } else if (text.contains("Bolts, mainly")) {
            Preferences.setString("walfordBucketItem", "bolts");
          } else if (text.contains("bucket of chicken")) {
            Preferences.setString("walfordBucketItem", "chicken");
          } else if (text.contains("Here y'go -- chum")) {
            Preferences.setString("walfordBucketItem", "chum");
          } else if (text.contains("fill that with ice")) {
            Preferences.setString("walfordBucketItem", "ice");
          } else if (text.contains("fill it up with milk")) {
            Preferences.setString("walfordBucketItem", "milk");
          } else if (text.contains("bucket of moonbeams")) {
            Preferences.setString("walfordBucketItem", "moonbeams");
          } else if (text.contains("bucket with rain")) {
            Preferences.setString("walfordBucketItem", "rain");
          }
        }
      }

      case 1115 -> {
        // VYKEA!
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("_VYKEACafeteriaRaided", true);
        } else if (ChoiceManager.lastDecision == 3) {
          Matcher WalfordMatcher = WALFORD_PATTERN.matcher(text);
          if (WalfordMatcher.find()) {
            Preferences.increment(
                "walfordBucketProgress", StringUtilities.parseInt(WalfordMatcher.group(1)));
            if (Preferences.getInteger("walfordBucketProgress") >= 100) {
              QuestDatabase.setQuestProgress(Quest.BUCKET, "step2");
            }
          }
        } else if (ChoiceManager.lastDecision == 4) {
          Preferences.setBoolean("_VYKEALoungeRaided", true);
        }
      }

      case 1116 -> {
        // All They Got Inside is Vacancy (and Ice)
        if (ChoiceManager.lastDecision == 3) {
          Matcher WalfordMatcher = WALFORD_PATTERN.matcher(text);
          if (WalfordMatcher.find()) {
            Preferences.increment(
                "walfordBucketProgress", StringUtilities.parseInt(WalfordMatcher.group(1)));
            if (Preferences.getInteger("walfordBucketProgress") >= 100) {
              QuestDatabase.setQuestProgress(Quest.BUCKET, "step2");
            }
          }
        } else if (ChoiceManager.lastDecision == 5) {
          Preferences.setBoolean("_iceHotelRoomsRaided", true);
        }
      }

      case 1119 -> {
        // Shining Mauve Backwards In Time
        Preferences.setInteger("encountersUntilDMTChoice", 49);
        if (ChoiceManager.lastDecision == 4
            && Preferences.getInteger("lastDMTDuplication") != KoLCharacter.getAscensions()) {
          Preferences.setInteger("lastDMTDuplication", KoLCharacter.getAscensions());
        }
      }

      case 1134 -> {
        // Batfellow Ends
        // (choosing to exit)
        if (ChoiceManager.lastDecision == 1) {
          KoLCharacter.setLimitMode(LimitMode.NONE);
        }
      }

      case 1168 -> // Batfellow Ends
      // (from running out of time)
      KoLCharacter.setLimitMode(LimitMode.NONE);

      case 1171 -> { // LT&T Office
        if (ChoiceManager.lastDecision < 4) {
          QuestDatabase.setQuestProgress(Quest.TELEGRAM, QuestDatabase.STARTED);
        }
      }

      case 1172 -> { // The Investigation Begins
        QuestDatabase.setQuestProgress(Quest.TELEGRAM, "step1");
        Preferences.setInteger("lttQuestStageCount", 0);
      }

      case 1173 -> { // The Investigation Continues
        QuestDatabase.setQuestProgress(Quest.TELEGRAM, "step2");
        Preferences.setInteger("lttQuestStageCount", 0);
      }

      case 1174 -> { // The Investigation Continues
        QuestDatabase.setQuestProgress(Quest.TELEGRAM, "step3");
        Preferences.setInteger("lttQuestStageCount", 0);
      }

      case 1175 -> { // The Investigation Thrillingly Concludes!
        QuestDatabase.setQuestProgress(Quest.TELEGRAM, "step4");
        Preferences.setInteger("lttQuestStageCount", 0);
      }

      case 1180 -> // Back to the East
      handleAfterAvatar(ChoiceManager.lastDecision);

      case 1188 -> { // The Call is Coming from Outside the Simulation
        if (ChoiceManager.lastDecision == 1) {
          // Skill learned
          Preferences.decrement("sourceEnlightenment", 1, 0);
        }
      }

      case 1191 -> // Source Terminal
      request.setHasResult(true);

      case 1198 -> {
        // Play a Time Prank
        if (ChoiceManager.lastDecision == 1 && text.contains("paradoxical time copy")) {
          Preferences.increment("_timeSpinnerMinutesUsed");
        }
      }

      case 1199 -> {
        // The Far Future
        if (text.contains("item appears in the replicator")
            || text.contains("convoluted nature of time-travel")) {
          Preferences.setBoolean("_timeSpinnerReplicatorUsed", true);
          break;
        }
        Matcher medalMatcher = TIME_SPINNER_MEDALS_PATTERN.matcher(text);
        if (medalMatcher.find()) {
          Preferences.setInteger(
              "timeSpinnerMedals", StringUtilities.parseInt(medalMatcher.group(1)));
        }
      }

      case 1202 -> {
        // Noon in the Civic Center

        // You knock the column over and collect the sprinkles from its shattered remains.
        // A booming voice from behind you startles you. "YOU ARE IN CONTEMPT OF COURT!"
        if (text.contains("CONTEMPT OF COURT!")) {
          Preferences.setBoolean("_gingerbreadColumnDestroyed", true);
          break;
        }

        // You bribe the clerk and he lets you into the office with a sneer. And a key. The key was
        // probably the important part.
        if (text.contains("bribe the clerk")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -1000));
          break;
        }

        // He squints at you and pushes a briefcase across the table.
        if (text.contains("briefcase full of sprinkles")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.GINGERBREAD_BLACKMAIL_PHOTOS, -1));
          Preferences.setBoolean("gingerBlackmailAccomplished", true);
          break;
        }
      }

      case 1203 -> {
        // Midnight in Civic Center
        // You step into the library and spend a few hours studying law. It's surprisingly
        // difficult! You gain a new respect for lawyers.
        // Haha no you don't.
        if (text.contains("few hours studying law")) {
          Preferences.increment("gingerLawChoice");
          break;
        }

        // You pay the counterfeiter to make you a fake version of Gingerbread City to pawn off on
        // some rube as the real thing
        if (text.contains("fake version of Gingerbread City")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -300));
          break;
        }

        // You quickly melt the lock on the cell and the criminal inside thanks you as he runs off
        // into the night.
        // "Hey," you shout after him, "you forgot your..." but he's already gone.
        // Oh well. He almost certainly stole this thing, anyway.
        if (text.contains("melt the lock on the cell")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.CREME_BRULEE_TORCH, -1));
          break;
        }

        // You insert your sprinkles and buy your cigarettes.
        if (text.contains("buy your cigarettes")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -5));
          break;
        }

        // You feed the treat to the puppy and he immediately becomes a loyal friend to you. Dogs
        // are so easy!
        if (text.contains("Dogs are so easy")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.GINGERBREAD_DOG_TREAT, -1));
          break;
        }
      }

      case 1204 -> {
        // Noon at the Train Station
        // You pull the lever and hear a rumbling from underneath you as the sewer gets much larger.
        // Now there's more room for the alligators!
        if (text.contains("more room for the alligators")) {
          Preferences.setBoolean("_gingerBiggerAlligators", true);
          break;
        }

        // You can't make heads or tails of it
        if (text.contains("can't make heads or tails of it")) {
          Preferences.increment("gingerTrainScheduleStudies");
        }
        // You're starting to get a feel for it, but it's still really confusing
        else if (text.contains("starting to get a feel for it")) {
          Preferences.increment("gingerTrainScheduleStudies");
          if (Preferences.getInteger("gingerTrainScheduleStudies") < 4) {
            Preferences.setInteger("gingerTrainScheduleStudies", 4);
          }
        }
        // You're starting to get a handle on how it all works.
        else if (text.contains("starting to get a handle")) {
          Preferences.increment("gingerTrainScheduleStudies");
          if (Preferences.getInteger("gingerTrainScheduleStudies") < 7) {
            Preferences.setInteger("gingerTrainScheduleStudies", 7);
          }
        }
        // You think you've got a pretty good understanding of it at this point.
        else if (text.contains("pretty good understanding")) {
          Preferences.increment("gingerTrainScheduleStudies");
          if (Preferences.getInteger("gingerTrainScheduleStudies") < 10) {
            Preferences.setInteger("gingerTrainScheduleStudies", 10);
          }
        }
        // What next?
      }

      case 1205 -> {
        // Midnight at the Train Station
        if (text.contains("provide a great workout")) {
          Preferences.increment("gingerMuscleChoice");
        }

        if (text.contains("new line to the subway system")) {
          Preferences.setBoolean("gingerSubwayLineUnlocked", true);
        }

        if (text.contains("what looks like a sweet roll")) {
          Preferences.increment("gingerDigCount");
        } else if (text.contains("piece of rock candy")) {
          Preferences.increment("gingerDigCount");
          if (Preferences.getInteger("gingerDigCount") < 4) {
            Preferences.setInteger("gingerDigCount", 4);
          }
        } else if (text.contains("sugar raygun")) {
          Preferences.setInteger("gingerDigCount", 7);
          ResultProcessor.processResult(ItemPool.get(ItemPool.TEETHPICK, -1));
        }
      }

      case 1206 -> {
        // Noon in the Industrial Zone
        // You buy the tool.
        if (!text.contains("buy the tool")) {
          break;
        }

        switch (ChoiceManager.lastDecision) {
          case 1 ->
          // creme brulee torch
          ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -25));
          case 2 ->
          // candy crowbar
          ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -50));
          case 3 ->
          // candy screwdriver
          ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -100));
          case 4 ->
          // teethpick
          ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -1000));
        }
      }

      case 1207 -> {
        // Midnight in the Industrial Zone
        // You can't afford a tattoo.
        if (ChoiceManager.lastDecision == 3 && !text.contains("can't afford a tattoo")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -100000));
        }
      }

      case 1208 -> {
        // Upscale Noon
        if (text.contains("drop off the negatives")) {
          Preferences.setBoolean("gingerNegativesDropped", true);
          ResultProcessor.processResult(ItemPool.get(ItemPool.FRUIT_LEATHER_NEGATIVE, -1));
          break;
        }

        if (!text.contains("You acquire an item")) {
          break;
        }

        switch (ChoiceManager.lastDecision) {
          case 1 ->
          // gingerbread dog treat
          ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -200));
          case 2 ->
          // pumpkin spice candle
          ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -150));
          case 3 ->
          // gingerbread spice latte
          ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -50));
          case 4 ->
          // gingerbread trousers
          ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -500));
          case 5 ->
          // gingerbread waistcoat
          ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -500));
          case 6 ->
          // gingerbread tophat
          ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -500));
          case 8 ->
          // gingerbread blackmail photos
          Preferences.setBoolean("gingerNegativesDropped", false);
        }
      }

      case 1210 -> {
        // Civic Planning Office
        // You move the policy to the front of the drawer, and by the time you leave the office
        // they've already enacted it!
        if (!text.contains("they've already enacted it")) {
          break;
        }

        switch (ChoiceManager.lastDecision) {
          case 1 -> Preferences.setBoolean("gingerRetailUnlocked", true);
          case 2 -> Preferences.setBoolean("gingerSewersUnlocked", true);
          case 3 -> Preferences.setBoolean("gingerExtraAdventures", true);
          case 4 -> Preferences.setBoolean("gingerAdvanceClockUnlocked", true);
        }
      }

      case 1212 -> {
        // Seedy Seedy Seedy
        if (!text.contains("You acquire an item")) {
          break;
        }

        if (ChoiceManager.lastDecision == 1) {
          // gingerbread pistol
          ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -300));
        } else if (ChoiceManager.lastDecision == 3) {
          // gingerbread beer
          ResultProcessor.processResult(ItemPool.get(ItemPool.GINGERBREAD_MUG, -1));
        }
      }

      case 1214 -> {
        // The Gingerbread Gallery
        if (!text.contains("You acquire an item")) {
          break;
        }

        if (ChoiceManager.lastDecision == 1) {
          // high-end ginger wine
        } else if (ChoiceManager.lastDecision == 2) {
          // chocolate sculpture
          ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -300));
        } else if (ChoiceManager.lastDecision == 3) {
          // Pop Art: a Guide
          ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -1000));
        } else if (ChoiceManager.lastDecision == 4) {
          // No Hats as Art
          ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -1000));
        }
      }

      case 1218 -> {
        // Wax On
        if (text.contains("You acquire an item")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.WAX_GLOB, -1));
        }
      }

      case 1219 -> {
        // Approach the Jellyfish
        // You acquire an item: sea jelly
        // You think it'd be best to leave them alone for the rest of the day.
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("_seaJellyHarvested", true);
        }
      }

      case 1232 -> // Finally Human
      handleAfterAvatar(ChoiceManager.lastDecision);

      case 1264 -> { // Meteor Metal Machinations
        if (ChoiceManager.lastDecision >= 1 && ChoiceManager.lastDecision <= 6) {
          // Exchanging for equipment
          ResultProcessor.removeItem(ItemPool.METAL_METEOROID);
        }
      }

      case 1267 -> {
        // Rubbed it the Right Way
        String wish = request.getFormField("wish");
        GenieRequest.postChoice(text, wish);
      }

      case 1272 -> {
        // R&D
        int letterId = extractIidFromURL(urlString);
        String letterName = ItemDatabase.getItemName(letterId);
        if (letterName != null) {
          // Turning in letter
          String message = "handed over " + letterName;
          RequestLogger.printLine(message);
          RequestLogger.updateSessionLog(message);
          ResultProcessor.removeItem(letterId);
        }
      }

      case 1273 -> {
        // R&D
        if (text.contains("slide the weird key into the weird lock")) {
          ResultProcessor.removeItem(ItemPool.WAREHOUSE_KEY);
        }
      }

      case 1320 -> {
        // A Heist!
        if (ChoiceManager.lastDecision == 1) {
          Preferences.increment("_catBurglarHeistsComplete");
        }
      }

      case 1339 -> {
        // A Little Pump and Grind
        if (ChoiceManager.lastDecision == 1 && text.contains("filling counter increments")) {
          int itemId = extractIidFromURL(request.getURLString());
          int qty = extractQtyFromURL(request.getURLString());
          ResultProcessor.processResult(ItemPool.get(itemId, -qty));
        } else if (ChoiceManager.lastDecision == 2 && text.contains("You acquire an item")) {
          ResultProcessor.removeItem(ItemPool.MAGICAL_SAUSAGE_CASING);
        }
      }

      case 1344 -> // Thank You, Come Again
      handleAfterAvatar(ChoiceManager.lastDecision);

      case 1387 -> SaberRequest.postForce(urlString, text);

      case 1395 -> { // Take your Pills
        {
          if (!text.contains("day's worth of pills")) {
            // Something failed.  Not enough spleen space left, already have
            // Everything Looks Yellow active, or maybe some other failure condition
            break;
          }
          if (ChoiceManager.lastDecision >= 1 && ChoiceManager.lastDecision <= 8) {
            if (!Preferences.getBoolean("_freePillKeeperUsed")) {
              Preferences.setBoolean("_freePillKeeperUsed", true);
            } else {
              KoLCharacter.setSpleenUse(KoLCharacter.getSpleenUse() + 3);
            }
          }
          if (ChoiceManager.lastDecision == 7) {
            // *** No longer forces a semirare
          }
          if (ChoiceManager.lastDecision == 3) {
            Preferences.setBoolean("noncombatForcerActive", true);
          }
        }
      }

      case 1409 -> // Your Quest is Over
      handleAfterAvatar(ChoiceManager.lastDecision);

      case 1449 -> {
        // If you change the mode with the item equipped, you need to un-equip and re-equip it to
        // get the modifiers
        if (ChoiceManager.lastDecision >= 1 && ChoiceManager.lastDecision <= 3) {
          for (var i : SlotSet.ACCESSORY_SLOTS) {
            AdventureResult item = EquipmentManager.getEquipment(i);
            if (item != null && item.getItemId() == ItemPool.BACKUP_CAMERA) {
              RequestThread.postRequest(new EquipmentRequest(EquipmentRequest.UNEQUIP, i));
              RequestThread.postRequest(new EquipmentRequest(item, i));
            }
          }
        }
      }
      case 1452, // Sprinkler Joe
          1453, // Fracker Dan
          1454 -> // Cropduster Dusty
      WildfireCampRequest.refresh();

      case 1457 -> { // Food Lab
        if (ChoiceManager.lastDecision == 1 && text.contains("You acquire an item")) {
          ResultProcessor.processItem(ItemPool.GOOIFIED_ANIMAL_MATTER, -5);
        }
      }

      case 1458 -> { // Nog Lab
        if (ChoiceManager.lastDecision == 1 && text.contains("You acquire an item")) {
          ResultProcessor.processItem(ItemPool.GOOIFIED_VEGETABLE_MATTER, -5);
        }
      }

      case 1459 -> { // Chem Lab
        if (ChoiceManager.lastDecision == 1 && text.contains("You acquire an item")) {
          ResultProcessor.processItem(ItemPool.GOOIFIED_MINERAL_MATTER, -5);
        }
      }

      case 1460 -> { // Gift Fabrication Lab
        if (text.contains("You acquire an item")) {
          switch (ChoiceManager.lastDecision) {
            case 1 -> ResultProcessor.processItem(ItemPool.GOOIFIED_ANIMAL_MATTER, -30);
            case 2 -> ResultProcessor.processItem(ItemPool.GOOIFIED_VEGETABLE_MATTER, -30);
            case 3 -> ResultProcessor.processItem(ItemPool.GOOIFIED_MINERAL_MATTER, -30);
            case 4 -> {
              ResultProcessor.processItem(ItemPool.GOOIFIED_ANIMAL_MATTER, -15);
              ResultProcessor.processItem(ItemPool.GOOIFIED_VEGETABLE_MATTER, -15);
            }
            case 5 -> {
              ResultProcessor.processItem(ItemPool.GOOIFIED_VEGETABLE_MATTER, -15);
              ResultProcessor.processItem(ItemPool.GOOIFIED_MINERAL_MATTER, -15);
            }
            case 6 -> {
              ResultProcessor.processItem(ItemPool.GOOIFIED_MINERAL_MATTER, -15);
              ResultProcessor.processItem(ItemPool.GOOIFIED_ANIMAL_MATTER, -15);
            }
            case 7 -> {
              ResultProcessor.processItem(ItemPool.GOOIFIED_ANIMAL_MATTER, -10);
              ResultProcessor.processItem(ItemPool.GOOIFIED_VEGETABLE_MATTER, -10);
              ResultProcessor.processItem(ItemPool.GOOIFIED_MINERAL_MATTER, -10);
            }
            default -> {}
          }
        }
      }

      case 1461 -> { // Site Alpha Primary Lab
        switch (ChoiceManager.lastDecision) {
          case 1 -> Preferences.increment("primaryLabGooIntensity", 1);
          case 2 -> Preferences.decrement("primaryLabGooIntensity", 1);
          case 3 -> ResultProcessor.processItem(ItemPool.GREY_GOO_RING, -1);
          case 4 -> {
            // Do nothing
          }
          case 5 ->
          // Grab the Cheer Core
          Preferences.setBoolean("primaryLabCheerCoreGrabbed", true);
        }
      }

      case 1465 -> // No More Grey You
      handleAfterAvatar(ChoiceManager.lastDecision);

      case 1466 -> // Configure Your Unbreakable Umbrella
      UmbrellaRequest.parseUmbrella(urlString, text);

      case 1476 -> { // Stillsuit
        if (ChoiceManager.lastDecision == 1) {
          StillSuitManager.handleDrink(text);
        }
      }

      case 1483 -> {
        // Direct Autumn-Aton
        {
          int location = StringUtilities.parseInt(request.getFormField("heythereprogrammer"));
          AutumnatonManager.postChoice(ChoiceManager.lastDecision, text, location);
        }
      }

      case 1484 -> { // Conspicuous Plaque
        if (ChoiceManager.lastDecision == 1 && text.contains("All right, you're the boss.")) {
          var name = request.getFormField("name");
          Preferences.setString("speakeasyName", name);
        }
      }

      case 1486 -> { // Choose an Action During a Caboose Distraction
        if (ChoiceManager.lastDecision == 2) {
          Preferences.increment("elfGratitude", 3);
        }
      }

      case 1487 -> // A Passenger Among Passengers
      Preferences.increment("elfGratitude", 5);

      case 1489 -> { // Slagging Off
        switch (ChoiceManager.lastDecision) {
          case 1, 2 -> ResultProcessor.removeItem(ItemPool.CRIMBO_CRYSTAL_SHARDS);
        }
      }

      case 1490 -> { // Woolin' Around
        if (ChoiceManager.lastDecision >= 1 && ChoiceManager.lastDecision <= 6) {
          // Exchanging for items
          ResultProcessor.removeItem(ItemPool.GRUBBY_WOOL);
        }
      }

      case 1494 -> { // Examine S.I.T. Course Certificate
        switch (ChoiceManager.lastDecision) {
          case 1 -> {
            KoLCharacter.removeAvailableSkill(SkillPool.INSECTOLOGIST);
            KoLCharacter.removeAvailableSkill(SkillPool.CRYPTOBOTANIST);
            Preferences.setString("currentSITSkill", "Psychogeologist");
          }
          case 2 -> {
            KoLCharacter.removeAvailableSkill(SkillPool.PSYCHOGEOLOGIST);
            KoLCharacter.removeAvailableSkill(SkillPool.CRYPTOBOTANIST);
            Preferences.setString("currentSITSkill", "Insectologist");
          }
          case 3 -> {
            KoLCharacter.removeAvailableSkill(SkillPool.PSYCHOGEOLOGIST);
            KoLCharacter.removeAvailableSkill(SkillPool.INSECTOLOGIST);
            Preferences.setString("currentSITSkill", "Cryptobotanist");
          }
          default -> {
            return;
          }
        }
        // Since you can walk away from this choice, only set you
        // actually selected a course.
        Preferences.setBoolean("_sitCourseCompleted", true);
      }

      case 1496 -> // Out of the Shadows
      handleAfterAvatar(ChoiceManager.lastDecision);

      case 1497 -> // Calling Rufus
      RufusManager.parseCallResponse(text, ChoiceManager.lastDecision);

      case 1498 -> // Calling Rufus Back
      RufusManager.parseCallBackResponse(text, ChoiceManager.lastDecision);

      case 1500 -> {
        // Like a Loded Stone
        if (ChoiceManager.lastDecision == 3) {
          Preferences.setBoolean("_shadowForestLooted", true);
        }
      }

      case 1501 -> {
        // Make a Wish
        String wish = request.getFormField("wish");
        MonkeyPawRequest.postChoice(text, wish);
      }

      case 1509 -> // Adjust Jill-of-All-Trades Lighting
      LedCandleRequest.parseUpgrade(urlString, text);

      case 1510 -> {
        // Burning Leaves
        String leavesField = request.getFormField("leaves");
        int leaves = StringUtilities.parseInt(leavesField);
        BurningLeavesRequest.postChoice(text, leaves);
      }

      case 1003,
          // Test Your Might And Also Test Other Things
          // If you are in WereProfessor, option 5 is "Look to the Moon".
          // If you select this as a Mild-Mannered Professor, you change form.
          // If you are already a Savage Beast:
          //
          // You notice the moon shining brightly over the cusp of the
          // tower and feel even farther from your professorial self.
          // Garh, no!
          1520,
          // Well, come on
          // -> Lose Savage Beast and gain Mild-Mannered Professor
          1521 -> {
        // Turn and face the strange
        // -> Lose Mild-Mannered Professor and gain Savage Beast
        if (ChoiceManager.lastChoice == 1003 && ChoiceManager.lastDecision != 5) {
          break;
        }
        // In either case, access to the Tinkering Bench changes
        ConcoctionDatabase.refreshConcoctions();
        // Savage Beast modifiers might have changes
        ResultProcessor.updateSavageBeast();
        // In either direction, current modifiers have changed.
        KoLCharacter.recalculateAdjustments();
        KoLCharacter.updateStatus();
      }

      case 1522 -> {
        // The Antiscientific Method
        {
          var location = KoLAdventure.lastVisitedLocation;
          if (location != null && text.contains("smashed scientific equipment")) {
            String value = Preferences.getString("antiScientificMethod").trim();
            String name = location.getAdventureName();
            if (!value.equals("")) {
              value += "|";
            }
            value += name;
            Preferences.setString("antiScientificMethod", value);
          }
        }
      }

      case 1523 -> // Research Bench
      ResearchBenchRequest.postChoice2(urlString, text);

      case 1524 -> // Moonlighting
      handleAfterAvatar(ChoiceManager.lastDecision);

      case 1525 -> // Dart Perks
      InventoryManager.checkDartPerks();

      case 1526 -> {
        // Conduct the Band
        switch (ChoiceManager.lastDecision) {
          case 1, 2, 3 -> Preferences.setInteger(
              "nextAprilBandTurn", KoLCharacter.getTurnsPlayed() + 11);
          case 4, 5, 6, 7, 8 -> Preferences.increment("_aprilBandInstruments", 1, 2, false);
        }
      }

      case 1527 -> {
        // Consider the Calendar
        parseMayamCalendar(text);
        if (text.contains("times without using an adventure")) {
          Preferences.increment("_mayamRests", 5);
        }
        if (text.contains("looks more experienced")) {
          KoLCharacter.getFamiliar().addNonCombatExperience(100);
        }
      }

      case 1532 -> {
        // Chatting with your Burly Bodyguard
        var monsterIdString = request.getFormField("bgid");
        if (text.contains("You set off to find a monster with a specific bodyguard to challenge.")
            && monsterIdString != null) {
          try {
            var monsterName = MonsterDatabase.getMonsterName(Integer.parseInt(monsterIdString));
            Preferences.setInteger("bodyguardCharge", 0);
            Preferences.setString("bodyguardChatMonster", monsterName);
          } catch (NumberFormatException e) {
            break;
          }
        }
      }

      case 1544 -> {
        // Devil some Candy
        if (text.contains("You place your candy in the deviler")) {
          Preferences.increment("_candyEggsDeviled");
          String item = request.getFormField("a");
          try {
            ResultProcessor.removeItem(Integer.parseInt(item));
          } catch (NumberFormatException e) {
            break;
          }
        }
      }

      case 1551 -> {
        // Hashing with your vice
        if (text.contains("You crush the schematic into little bits of checksum.")) {
          String item = request.getFormField("iid");
          try {
            ResultProcessor.removeItem(Integer.parseInt(item));
          } catch (NumberFormatException e) {
            break;
          }
        }
      }

      case 1553 -> {
        // Hybridization Chamber
        if (ChoiceManager.lastDecision == 1 && text.contains("<span class='guts'>Grafting")) {
          var famId = request.getFormField("fam");
          // if this is our current familiar, remove it and the item it holds
          var familiar = KoLCharacter.getFamiliar();
          if (Integer.parseInt(famId) == familiar.getId()) {
            familiar.setItem(EquipmentRequest.UNEQUIP);
            KoLCharacter.setFamiliar(FamiliarData.NO_FAMILIAR);
          }
          // grab our graft info and our new level
          ApiRequest.updateStatus();
          // refresh again because level is wrong the first time
          ApiRequest.updateStatus(true);
        }
      }

      case 1554 -> // We'll Return to Our Home, Bathed in Rays of Gold
      handleAfterAvatar(ChoiceManager.lastDecision);

      case 1560 -> {
        // A Cooler Yeti Conversation
        switch (ChoiceManager.lastDecision) {
          case 2 -> {
            Preferences.setBoolean("_coolerYetiAdventures", true);
            Preferences.setString("coolerYetiMode", "adventures");
          }
          case 3 -> Preferences.setString("coolerYetiMode", "effect");
          case 4 -> Preferences.setString("coolerYetiMode", "bar");
          case 5 -> Preferences.setString("coolerYetiMode", "stats");
        }
      }

      case 1561 -> {
        // Request Supply Drop
        String req = request.getFormField("request");
        AlliedRadioRequest.postChoice(text, false, req);
      }

      case 1562 -> {
        // Time is a Möbius Strip
        if (text.contains("stock certificate")) {
          Preferences.setInteger("stockCertificateTurn", KoLCharacter.getTurnsPlayed());
        }
        // update Paradoxicity from charpane
        // eventually we're going to want to do some parsing to e.g. figure out which choices are
        // available, but this is much simpler
        RequestThread.postRequest(new CharPaneRequest());
      }

      case 1563 -> {
        // Request Supply Drop
        String req = request.getFormField("request");
        AlliedRadioRequest.postChoice(text, true, req);
      }

      case 1565 -> {
        // The Council of Loathing
        if (text.contains(
            "You free King Ralph, signalling a triumphant end to your submaritime adventure")) {
          KoLCharacter.liberateKing();
        }
      }

      case 1566 -> {
        // Summon a Wave
        if (ChoiceManager.lastDecision == 1) {
          Matcher waveMatcher = SUMMON_WAVE_PATTERN.matcher(text);
          if (waveMatcher.find()) {
            Preferences.setString("_seadentWaveZone", waveMatcher.group(1));
          }
        }
      }
    }
  }

  private static void handleAfterAvatar(final int decision) {
    String newClass = "Unknown";
    switch (ChoiceManager.lastDecision) {
      case 1 -> newClass = "Seal Clubber";
      case 2 -> newClass = "Turtle Tamer";
      case 3 -> newClass = "Pastamancer";
      case 4 -> newClass = "Sauceror";
      case 5 -> newClass = "Disco Bandit";
      case 6 -> newClass = "Accordion Thief";
    }

    String message = "Now walking on the " + newClass + " road.";

    KoLmafia.updateDisplay(message);
    RequestLogger.updateSessionLog(message);

    KoLmafia.resetAfterAvatar();
  }

  private static final Pattern BOOMBOX_PATTERN = Pattern.compile("you can do <b>(\\d+)</b> more");
  private static final Pattern BOOMBOX_SONG_PATTERN =
      Pattern.compile("&quot;(.*?)&quot;( \\(Keep playing\\)|)");
  private static final Pattern CASE_PATTERN = Pattern.compile("\\((\\d+) more case");
  private static final Pattern CHAMBER_PATTERN = Pattern.compile("Chamber <b>#(\\d+)</b>");
  private static final Pattern CRIMBOT_CHASSIS_PATTERN =
      Pattern.compile("base chassis is the (.*?),");
  private static final Pattern CRIMBOT_ARM_PATTERN =
      Pattern.compile("(?:My arm is the|</i> equipped with a) (.*?),");
  private static final Pattern CRIMBOT_PROPULSION_PATTERN =
      Pattern.compile(
          "(?:provided by a|am mobilized by an|equipped with a pair of|move via) (.*?),");
  private static final Pattern DAYCARE_PATTERN =
      Pattern.compile(
          "(?:Looks like|Probably around) (.*?) pieces in all. (.*?) toddlers are training with (.*?) instructor");
  private static final Pattern DAYCARE_RECRUITS_PATTERN =
      Pattern.compile("<font color=blue><b>\\[(.*?) Meat\\]</b></font>");
  private static final Pattern DAYCARE_INSTRUCTOR_ITEM_PATTERN =
      Pattern.compile(
          "<input  class=button type=submit value=\"Hire an instructor \"></td><td valign=center><font color=blue><b>\\[(\\d*) (.*?)\\]</b>");
  private static final Pattern DINSEY_ROLLERCOASTER_PATTERN =
      Pattern.compile("rollercoaster is currently set to (.*?) Mode");
  private static final Pattern DINSEY_PIRATE_PATTERN =
      Pattern.compile("'Updated Pirate' is (lit|dark)");
  private static final Pattern DINSEY_TEACUP_PATTERN =
      Pattern.compile("'Current Teacup Spin Rate' points to (\\d+),000 RPM");
  private static final Pattern DINSEY_SLUICE_PATTERN =
      Pattern.compile("'Sluice Swishers' is currently in the (.*?) position");
  private static final Pattern DOCTOR_BAG_PATTERN =
      Pattern.compile("We've received a report of a patient (.*?), in (.*?)\\.");
  private static final Pattern EARLY_DAYCARE_PATTERN =
      Pattern.compile("mostly empty. (.*?) toddlers are training with (.*?) instructor");
  private static final Pattern ED_RETURN_PATTERN =
      Pattern.compile("Return to the fight! \\((\\d+) Ka\\)");
  private static final Pattern EDPIECE_PATTERN =
      Pattern.compile("<p>The crown is currently adorned with a golden (.*?).<center>");
  private static final Pattern ENLIGHTENMENT_PATTERN =
      Pattern.compile("achieved <b>(\\d+)</b> enlightenment");
  private static final Pattern GUZZLR_TIER_PATTERN =
      Pattern.compile("You have completed ([0-9,]+) (Bronze|Gold|Platinum) Tier deliveries");
  private static final Pattern GUZZLR_QUEST_PATTERN =
      Pattern.compile("<p>You are currently tasked with taking a (.*?) to (.*?) in (.*?)\\.<p>");
  private static final Pattern ICEHOUSE_PATTERN =
      Pattern.compile("perfectly-preserved (.*?), right");
  private static final Pattern MAYO_MINDER_PATTERN =
      Pattern.compile("currently loaded up with packets of (.*?)<p>");
  private static final Pattern MOTORBIKE_TIRES_PATTERN = Pattern.compile("<b>Tires:</b> (.*?)?\\(");
  private static final Pattern MOTORBIKE_GASTANK_PATTERN =
      Pattern.compile("<b>Gas Tank:</b> (.*?)?\\(");
  private static final Pattern MOTORBIKE_HEADLIGHT_PATTERN =
      Pattern.compile("<b>Headlight:</b> (.*?)?\\(");
  private static final Pattern MOTORBIKE_COWLING_PATTERN =
      Pattern.compile("<b>Cowling:</b> (.*?)?\\(");
  private static final Pattern MOTORBIKE_MUFFLER_PATTERN =
      Pattern.compile("<b>Muffler:</b> (.*?)?\\(");
  private static final Pattern MOTORBIKE_SEAT_PATTERN = Pattern.compile("<b>Seat:</b> (.*?)?\\(");
  // Looks like your order for a <muffin type> muffin is not yet ready.
  public static final Pattern MUFFIN_TYPE_PATTERN =
      Pattern.compile("Looks like your order for a (.*? muffin) is not yet ready");
  private static final Pattern MUSHROOM_COSTUME_PATTERN =
      Pattern.compile(
          "<form.*?name=option value=(\\d).*?type=submit value=\"(.*?) Costume\".*?>(\\d+) coins<.*?</form>");
  private static final Pattern MUSHROOM_BADGE_PATTERN =
      Pattern.compile("Current cost: (\\d+) coins.");
  private static final Pattern OMEGA_PATTERN =
      Pattern.compile("<br>Current power level: (\\d+)%</td>");
  private static final Pattern RADIO_STATIC_PATTERN =
      Pattern.compile("<p>(?!(?:<form|</center>))(.+?)(?=<[^i</])");
  private static final Pattern REANIMATOR_ARM_PATTERN = Pattern.compile("(\\d+) arms??<br>");
  private static final Pattern REANIMATOR_LEG_PATTERN = Pattern.compile("(\\d+) legs??<br>");
  private static final Pattern REANIMATOR_SKULL_PATTERN = Pattern.compile("(\\d+) skulls??<br>");
  private static final Pattern REANIMATOR_WEIRDPART_PATTERN =
      Pattern.compile("(\\d+) weird random parts??<br>");
  private static final Pattern REANIMATOR_WING_PATTERN = Pattern.compile("(\\d+) wings??<br>");
  private static final Pattern RED_SNAPPER_PATTERN =
      Pattern.compile("guiding you towards: <b>(.*?)</b>.  You've found <b>(\\d+)</b> of them");
  private static final Pattern SAUSAGE_PATTERN =
      Pattern.compile(
          "grinder needs (.*?) of the (.*?) required units of filling to make a sausage.  Your grinder reads \\\"(\\d+)\\\" units.");
  private static final Pattern SHEN_PATTERN =
      Pattern.compile(
          "(?:Bring me|artifact known only as) <b>(.*?)</b>, hidden away for centuries");
  private static final Pattern SNOJO_CONSOLE_PATTERN = Pattern.compile("<b>(.*?) MODE</b>");
  private static final Pattern TRICK_OR_TREAT_PATTERN =
      Pattern.compile(
          "<img (class='faded')? src='.*?/trickortreat/(?:house_)?(starhouse|[ld])(?:\\d+)?.gif'");
  private static final Pattern VOTE_PATTERN =
      Pattern.compile(
          "<label><input .*? value=\\\"(\\d)\\\" class=\\\"locals\\\" /> (.*?)<br /><span .*? color: blue\\\">(.*?)</span><br /></label>");
  public static final Pattern VOTE_SPEECH_PATTERN =
      Pattern.compile(
          "<p><input type='radio' name='g' value='(\\d+)' /> <b>(.*?)</b>(.*?)<br><blockquote>(.*?)</blockquote>");
  private static final Pattern WLF_PATTERN =
      Pattern.compile(
          "<form action=choice.php>.*?<b>(.*?)</b>.*?descitem\\((.*?)\\).*?>(.*?)<.*?name=option value=([\\d]*).*?</form>",
          Pattern.DOTALL);
  private static final Pattern WLF_COUNT_PATTERN = Pattern.compile(".*? \\(([\\d]+)\\)$");

  public static final Map<Quest, String> conspiracyQuestMessages = new HashMap<>();

  static {
    conspiracyQuestMessages.put(
        Quest.CLIPPER,
        "&quot;Attention any available operative. Attention any available operative. A reward has been posted for DNA evidence gathered from Lt. Weirdeaux's subjects inside Site 15. The DNA is to be gathered via keratin extraction. Message repeats.&quot;");
    conspiracyQuestMessages.put(
        Quest.EVE,
        "&quot;Attention Operative 01-A-A. General Sitterson reports a... situation involving experiment E-V-E-6. Military intervention has been requested. Message repeats.&quot;");
    conspiracyQuestMessages.put(
        Quest.FAKE_MEDIUM,
        "&quot;Attention Operative EC-T-1. An outside client has expressed interest in the acquisition of an ESP suppression collar from the laboratory. Operationally significant sums of money are involved. Message repeats.&quot;");
    conspiracyQuestMessages.put(
        Quest.GORE,
        "&quot;Attention any available operative. Attention any available operative. Laboratory overseer General Sitterson reports unacceptable levels of environmental gore. Several elevator shafts are already fully clogged, limiting staff mobility, and several surveillance camera lenses have been rendered opaque, placing the validity of experimental data at risk. Immediate janitorial assistance is requested. Message repeats.&quot;");
    conspiracyQuestMessages.put(
        Quest.JUNGLE_PUN,
        "&quot;Attention any available operative. Attention any available operative. The director of Project Buena Vista has posted a significant bounty for the collection of jungle-related puns. Repeat: Jungle-related puns. Non-jungle puns or jungle non-puns will not be accepted. Non-jungle non-puns, by order of the director, are to be rewarded with summary execution. Message repeats.&quot;");
    conspiracyQuestMessages.put(
        Quest.OUT_OF_ORDER,
        "&quot;Attention Operative QZ-N-0. Colonel Kurzweil at Jungle Interior Camp 4 reports the theft of Project T. L. B. materials. Requests immediate assistance. Is confident that it has not yet been removed from the jungle. Message repeats.&quot;");
    conspiracyQuestMessages.put(
        Quest.SERUM,
        "&quot;Attention Operative 21-B-M. Emergency deployment orders have been executed due to a shortage of experimental serum P-00. Repeat: P Zero Zero. Lt. Weirdeaux is known to have P-00 manufacturing facilities inside the Site 15 mansion. Message repeats.&quot;");
    conspiracyQuestMessages.put(
        Quest.SMOKES,
        "&quot;Attention Operative 00-A-6. Colonel Kurzweil at Jungle Interior Camp 4 reports that they have run out of smokes. Repeat: They have run out of smokes. Requests immediate assistance. Message repeats.&quot;");
  }

  public static void visitChoice(final GenericRequest request) {
    String text = request.responseText;
    switch (ChoiceManager.lastChoice) {
      case 4 -> {
        // Finger-Lickin'... Death.
        if (KoLCharacter.hasEquipped(CANDY_CANE_SWORD) && !text.contains("Teach them a lesson")) {
          Preferences.setBoolean("_candyCaneSwordSouthOfTheBorder", true);
        }
      }

      case 109 -> {
        // Dumpster Diving
        if (KoLCharacter.hasEquipped(CANDY_CANE_SWORD) && !text.contains("candy sword")) {
          Preferences.setBoolean("_candyCaneSwordBackAlley", true);
        }
      }

      case 127 -> {
        // No sir, away! A papaya war is on!
        if (KoLCharacter.hasEquipped(CANDY_CANE_SWORD) && !text.contains("candy cane sword")) {
          Preferences.setBoolean("_candyCaneSwordPalindome", true);
        }
      }

      case 139 -> {
        // Bait and Switch
        if (KoLCharacter.hasEquipped(CANDY_CANE_SWORD) && !text.contains("candy cane sword")) {
          Preferences.setBoolean("candyCaneSwordWarHippyBait", true);
        }
      }

      case 140 -> {
        // The Thin Tie-Dyed Line
        if (KoLCharacter.hasEquipped(CANDY_CANE_SWORD) && !text.contains("Threaten a passerby")) {
          Preferences.setBoolean("candyCaneSwordWarHippyLine", true);
        }
      }

      case 143 -> {
        // Catching Some Zetas
        if (KoLCharacter.hasEquipped(CANDY_CANE_SWORD) && !text.contains("candy cane sword")) {
          Preferences.setBoolean("candyCaneSwordWarFratZetas", true);
        }
      }

      case 144 -> {
        // One Less Room Than In That Movie
        if (KoLCharacter.hasEquipped(CANDY_CANE_SWORD) && !text.contains("Pry open the hatch")) {
          Preferences.setBoolean("candyCaneSwordWarFratRoom", true);
        }
      }

      case 151 -> {
        // Adventurer, $1.99
        if (KoLCharacter.hasEquipped(CANDY_CANE_SWORD) && !text.contains("your cane")) {
          Preferences.setBoolean("candyCaneSwordFunHouse", true);
        }
      }

      case 360 -> // Wumpus Hunt
      WumpusManager.visitChoice(text);

      case 440 -> // Puttin' on the Wax
      HaciendaManager.preRecording(text);

      case 443 -> // A Chess Puzzle
      RabbitHoleManager.parseChessPuzzle(text);

      case 460 -> // Space Trip
      ArcadeRequest.visitSpaceTripChoice(text);

      case 471 -> // DemonStar
      ArcadeRequest.visitDemonStarChoice(text);

      case 485 -> // Fighters Of Fighting
      ArcadeRequest.visitFightersOfFightingChoice(text);

      case 486 -> // DungeonFist!
      ArcadeRequest.visitDungeonFistChoice(text);

      case 488 -> // Meteoid
      ArcadeRequest.visitMeteoidChoice(text);

      case 496,
          // Crate Expectations
          509,
          // Of Course!
          510,
          // Those Who Came Before You
          511,
          // If it's Tiny, is it Still a Mansion?
          512,
          // Hot and Cold Running Rats
          513,
          // Staring Down the Barrel
          514,
          // 1984 Had Nothing on This Cellar
          515,
          // A Rat's Home...
          1000,
          // Everything in Moderation
          1001 -> // Hot and Cold Dripping Rats
      TavernRequest.postTavernVisit(request);

      case 502 -> {
        // Arboreal Respite
        if (KoLCharacter.hasEquipped(CANDY_CANE_SWORD) && !text.contains("candy cane sword")) {
          Preferences.setBoolean("_candyCaneSwordSpookyForest", true);
        }
      }

      case 523 -> {
        // Death Rattlin'
        if (KoLCharacter.hasEquipped(CANDY_CANE_SWORD) && !text.contains("candy cane sword")) {
          Preferences.setBoolean("candyCaneSwordDefiledCranny", true);
        }
      }

      case 537 -> // Play Porko!
      SpaaaceRequest.visitPorkoChoice(text);

      case 540 -> // Big-Time Generator
      SpaaaceRequest.visitGeneratorChoice(text);

      case 570 -> GameproManager.parseGameproMagazine(text);

      case 627, 628, 629, 630, 631, 633 -> ChibiBuddyManager.visit(ChoiceManager.lastChoice, text);

      case 641 -> {
        // Stupid Pipes.
        if (!text.contains("Dive Down")
            && KoLCharacter.getElementalResistanceLevels(Element.HOT) >= 25) {
          Preferences.setBoolean("flickeringPixel1", true);
        }
      }

      case 642 -> {
        // You're Freaking Kidding Me
        if (!text.contains("Wait a minute...")
            && KoLCharacter.getAdjustedMuscle() >= 500
            && KoLCharacter.getAdjustedMysticality() >= 500
            && KoLCharacter.getAdjustedMoxie() >= 500) {
          Preferences.setBoolean("flickeringPixel2", true);
        }
      }

      case 644 -> {
        // Snakes.
        if (!text.contains("Tie the snakes in a knot.") && KoLCharacter.getAdjustedMoxie() >= 300) {
          Preferences.setBoolean("flickeringPixel3", true);
        }
      }

      case 645 -> {
        // So... Many... Skulls...
        if (!text.contains("You fear no evil")
            && KoLCharacter.getElementalResistanceLevels(Element.SPOOKY) >= 25) {
          Preferences.setBoolean("flickeringPixel4", true);
        }
      }

      case 647 -> {
        // A Stupid Dummy. Also, a Straw Man.

        // *** unspaded
        if (!text.contains("Graaaaaaaaargh!") && KoLCharacter.currentBonusDamage() >= 1000) {
          Preferences.setBoolean("flickeringPixel5", true);
        }
      }

      case 648 -> {
        // Slings and Arrows
        // *** Yes, there supposed to be two spaces there.
        if (!text.contains("Arrows?  Ha.") && KoLCharacter.getCurrentHP() >= 1000) {
          Preferences.setBoolean("flickeringPixel6", true);
        }
      }

      case 650 -> {
        // This Is Your Life. Your Horrible, Horrible Life.
        if (!text.contains("Then watch it again with the commentary on!")
            && KoLCharacter.getCurrentMP() >= 1000) {
          Preferences.setBoolean("flickeringPixel7", true);
        }
      }

      case 651 -> {
        // The Wall of Wailing
        if (!text.contains("Make the tide resist you")
            && KoLCharacter.currentPrismaticDamage() >= 60) {
          Preferences.setBoolean("flickeringPixel8", true);
        }
      }

      case 658 -> // Debasement
      ResultProcessor.processItem(ItemPool.GOLD_PIECE, -30);

      case 689 -> // The Final Reward
      Preferences.setInteger("_lastDailyDungeonRoom", 14);

      case 690 -> // The First Chest Isn't the Deepest
      Preferences.setInteger("_lastDailyDungeonRoom", 4);

      case 691 -> {
        // Second Chest
        Preferences.setInteger("_lastDailyDungeonRoom", 9);
        if (KoLCharacter.hasEquipped(CANDY_CANE_SWORD) && !text.contains("candy cane sword")) {
          Preferences.setBoolean("candyCaneSwordDailyDungeon", true);
        }
      }

      case 692, 693 -> {
        // I Wanna Be a Door and It's Almost Certainly a Trap
        Matcher chamberMatcher = CHAMBER_PATTERN.matcher(text);
        if (chamberMatcher.find()) {
          int round = StringUtilities.parseInt(chamberMatcher.group(1));
          Preferences.setInteger("_lastDailyDungeonRoom", round - 1);
        }
      }

      case 704 -> // Playing the Catalog Card
      visitCatalog(text);

      case 705 -> // Halls Passing in the Night
      ResultProcessor.processItem(ItemPool.MERKIN_HALLPASS, -1);

      case 764 -> {
        // The Machine

        // You approach The Machine, and notice that the
        // capacitor you're carrying fits perfectly into an
        // obviously empty socket on the base of it. You plug
        // it in, and The Machine whirs ominously to life.......

        if (text.contains("You plug it in")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.SKULL_CAPACITOR, -1));
        }
      }

      case 774 -> {
        // Opening up the Folder Holder

        String option = request.getFormField("forceoption");
        if (option != null) {
          ChoiceManager.lastDecision = StringUtilities.parseInt(option);
        }
      }

      case 780 -> {
        // Action Elevator
        if (KoLCharacter.hasEquipped(CANDY_CANE_SWORD) && !text.contains("Climb on top")) {
          Preferences.setBoolean("candyCaneSwordApartmentBuilding", true);
        }
      }

      case 781 -> {
        // Earthbound and Down
        if (!text.contains("option value=1")) {
          if (Preferences.getInteger("hiddenApartmentProgress") == 0) {
            Preferences.setInteger("hiddenApartmentProgress", 1);
          }
        }
      }

      case 783 -> {
        // Water You Dune
        if (!text.contains("option value=1")) {
          if (Preferences.getInteger("hiddenHospitalProgress") == 0) {
            Preferences.setInteger("hiddenHospitalProgress", 1);
          }
        }
      }

      case 785 -> {
        // Air Apparent
        if (!text.contains("option value=1")) {
          if (Preferences.getInteger("hiddenOfficeProgress") == 0) {
            Preferences.setInteger("hiddenOfficeProgress", 1);
          }
        }
        if (KoLCharacter.hasEquipped(CANDY_CANE_SWORD) && !text.contains("candy cane sword")) {
          Preferences.setBoolean("_candyCaneSwordOvergrownShrine", true);
        }
      }

      case 787 -> {
        // Fire When Ready
        if (!text.contains("option value=1")) {
          if (Preferences.getInteger("hiddenBowlingAlleyProgress") == 0) {
            Preferences.setInteger("hiddenBowlingAlleyProgress", 1);
          }
        }
      }

      case 788 -> {
        // Life is Like a Cherry of Bowls
        if (KoLCharacter.hasEquipped(CANDY_CANE_SWORD) && !text.contains("candy cane sword")) {
          Preferences.setBoolean("candyCaneSwordBowlingAlley", true);
        }
      }

      case 791 -> // Legend of the Temple in the Hidden City
      Preferences.setInteger("zigguratLianas", 1);

      case 793 -> {
        // Welcome to The Shore, Inc.
        if (KoLCharacter.hasEquipped(CANDY_CANE_SWORD) && !text.contains("Fencing Competition")) {
          Preferences.setBoolean("candyCaneSwordShore", true);
        }
      }

      case 798 -> {
        // Hippy Talkin'
        if (text.contains("You should totally keep it!")) {
          QuestDatabase.setQuestProgress(Quest.HIPPY, QuestDatabase.FINISHED);
          Preferences.setInteger("lastIslandUnlock", KoLCharacter.getAscensions());
        }
      }

      case 801 -> {
        {
          // A Reanimated Conversation
          Matcher matcher = REANIMATOR_ARM_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger("reanimatorArms", StringUtilities.parseInt(matcher.group(1)));
          } else {
            Preferences.setInteger("reanimatorArms", 0);
          }
          matcher = REANIMATOR_LEG_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger("reanimatorLegs", StringUtilities.parseInt(matcher.group(1)));
          } else {
            Preferences.setInteger("reanimatorLegs", 0);
          }
          matcher = REANIMATOR_SKULL_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger("reanimatorSkulls", StringUtilities.parseInt(matcher.group(1)));
          } else {
            Preferences.setInteger("reanimatorSkulls", 0);
          }
          matcher = REANIMATOR_WEIRDPART_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger(
                "reanimatorWeirdParts", StringUtilities.parseInt(matcher.group(1)));
          } else {
            Preferences.setInteger("reanimatorWeirdParts", 0);
          }
          matcher = REANIMATOR_WING_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger("reanimatorWings", StringUtilities.parseInt(matcher.group(1)));
          } else {
            Preferences.setInteger("reanimatorWings", 0);
          }
        }
      }

      case 804 -> {
        {
          var state =
              TRICK_OR_TREAT_PATTERN
                  .matcher(text)
                  .results()
                  .map(
                      m -> {
                        var faded = m.group(1) != null;
                        var type = m.group(2).substring(0, 1);
                        return faded ? type : type.toUpperCase();
                      })
                  .collect(Collectors.joining());

          Preferences.setString("_trickOrTreatBlock", state);
        }
      }

      case 836 -> {
        {
          // Adventures Who Live in Ice Houses...
          Matcher matcher = ICEHOUSE_PATTERN.matcher(text);
          if (matcher.find()) {
            String icehouseMonster = matcher.group(1);
            BanishManager.banishMonster(icehouseMonster, BanishManager.Banisher.ICE_HOUSE, false);
          }
        }
      }

      case 822, // The Prince's Ball (In the Restroom)
          823, // The Prince's Ball (On the Dance Floor)
          824, // The Prince's Ball (The Kitchen)
          825, // The Prince's Ball (On the Balcony)
          826, // The Prince's Ball (The Lounge)
          827, // The Prince's Ball (At the Canapés Table)
          // stepmother
          829,
          // We all wear masks
          830, // Cooldown
          832, // Shower Power
          833, // Vendi, Vidi, Vici
          834, // Back Room Dealings
          // wolf
          831, // Intrusion
          837, // On Purple Pond
          838, // General Mill
          839, // The Sounds of the Undergrounds
          840, // Hop on Rock Pops
          841, // Building, Structure, Edifice
          842, // The Gingerbread Warehouse
          // witch
          844, // The Portal to Horrible Parents
          845, // Rumpelstiltskin's Workshop
          846, // Bartering for the Future of Innocent Children
          847, // Pick Your Poison
          848, // Where the Magic Happens
          849, // The Practice
          850 -> // World of Bartercraft
      // gnome
      GrimstoneManager.visitChoice(text);

      case 851 -> {
        {
          // Shen Copperhead, Nightclub Owner
          Matcher matcher = SHEN_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("shenQuestItem", matcher.group(1));
          }
        }
      }

      case 855 -> {
        // Behind the 'Stache
        if (KoLCharacter.hasEquipped(CANDY_CANE_SWORD) && !text.contains("and knock over")) {
          Preferences.setBoolean("candyCaneSwordCopperheadClub", true);
        }
      }

      case 871 -> {
        {
          // inspecting Motorbike
          Matcher matcher = MOTORBIKE_TIRES_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("peteMotorbikeTires", matcher.group(1).trim());
          }
          matcher = MOTORBIKE_GASTANK_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("peteMotorbikeGasTank", matcher.group(1).trim());
            if (Preferences.getString("peteMotorbikeGasTank").equals("Large Capacity Tank")) {
              KoLCharacter.setDesertBeachAvailable();
            } else if (Preferences.getString("peteMotorbikeGasTank").equals("Extra-Buoyant Tank")) {
              Preferences.setInteger("lastIslandUnlock", KoLCharacter.getAscensions());
            }
          }
          matcher = MOTORBIKE_HEADLIGHT_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("peteMotorbikeHeadlight", matcher.group(1).trim());
          }
          matcher = MOTORBIKE_COWLING_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("peteMotorbikeCowling", matcher.group(1).trim());
          }
          matcher = MOTORBIKE_MUFFLER_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("peteMotorbikeMuffler", matcher.group(1).trim());
          }
          matcher = MOTORBIKE_SEAT_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("peteMotorbikeSeat", matcher.group(1).trim());
          }
        }
      }

      case 876 -> {
        // One Simple Nightstand
        if (KoLCharacter.hasEquipped(CANDY_CANE_SWORD) && !text.contains("candy cane sword")) {
          Preferences.setBoolean("_candyCaneSwordHauntedBedroom", true);
        }
      }

      case 888 -> {
        // Take a Look, it's in a Book! (Rise)
        if (KoLCharacter.hasEquipped(CANDY_CANE_SWORD) && !text.contains("Sword Canes")) {
          Preferences.setBoolean("_candyCaneSwordHauntedLibrary", true);
        }
      }

      case 890, // Lights Out in the Storage Room
          891, // Lights Out in the Laundry Room
          892, // Lights Out in the Bathroom
          893, // Lights Out in the Kitchen
          894, // Lights Out in the Library
          895, // Lights Out in the Ballroom
          896, // Lights Out in the Gallery
          897, // Lights Out in the Bedroom
          898, // Lights Out in the Nursery
          899, // Lights Out in the Conservatory
          900, // Lights Out in the Billiards Room
          901, // Lights Out in the Wine Cellar
          902, // Lights Out in the Boiler Room
          903 -> { // Lights Out in the Laboratory
        // Remove the counter if it exists so a new one can be made
        // as soon as the next adventure is started
        TurnCounter.stopCounting("Spookyraven Lights Out");
        Preferences.setInteger("lastLightsOutTurn", KoLCharacter.getTurnsPlayed());
      }

      case 923 -> {
        // All Over the Map
        if (KoLCharacter.hasEquipped(CANDY_CANE_SWORD) && !text.contains("candy cane sword")) {
          Preferences.setBoolean("candyCaneSwordBlackForest", true);
        }
      }

      case 930 -> {
        // Another Errand I Mean Quest

        // Upon turning in the White Citadel Satisfaction Satchel, "paco" takes
        // it and gives us a lucky rabbit's foot. No further choice processing.
        if (text.contains("<b>lucky rabbit's foot</b>")) {
          ResultProcessor.processItem(ItemPool.CITADEL_SATCHEL, -1);
          QuestDatabase.setQuestProgress(Quest.CITADEL, QuestDatabase.FINISHED);
        }
      }

      case 984 -> {
        if (!text.contains("Awaiting mission")) {

          Matcher staticMatcher = RADIO_STATIC_PATTERN.matcher(text);

          String snippet = ".*";

          while (staticMatcher.find()) {
            String section = staticMatcher.group(1);

            if (section.contains("You turn the biggest knob on the radio")) {
              continue;
            }

            for (String part : section.split("&lt;.*?&gt;")) {
              if (part.startsWith("&lt;") || part.length() < 3) {
                continue;
              }

              part = part.replaceAll("\\.* *$", "");
              part = part.replace("  ", " ");
              snippet += Pattern.quote(part) + ".*";
            }
          }

          Iterator<Entry<Quest, String>> iterator = conspiracyQuestMessages.entrySet().iterator();

          Quest todaysQuest = null;

          while (iterator.hasNext()) {
            Map.Entry<Quest, String> entry = iterator.next();

            if (Pattern.matches(snippet, entry.getValue())) {
              if (todaysQuest == null) {
                todaysQuest = entry.getKey();
              } else {
                // Multiple matches
                todaysQuest = null;
                break;
              }
            }
          }

          if (todaysQuest != null) {
            Preferences.setString("_questESp", todaysQuest.getPref());
          }
        }
      }

      case 986 -> {
        // Control Panel
        Preferences.setBoolean(
            "controlPanel1", !text.contains("All-Ranchero FM station: VOLUNTARY"));
        Preferences.setBoolean(
            "controlPanel2", !text.contains("&pi; sleep-hypnosis generators: OFF"));
        Preferences.setBoolean(
            "controlPanel3", !text.contains("Simian Ludovico Wednesdays: CANCELLED"));
        Preferences.setBoolean(
            "controlPanel4", !text.contains("Monkey food safety protocols: OBEYED"));
        Preferences.setBoolean("controlPanel5", !text.contains("Shampoo Dispensers: CHILD-SAFE"));
        Preferences.setBoolean("controlPanel6", !text.contains("Assemble-a-Bear kiosks: CLOSED"));
        Preferences.setBoolean("controlPanel7", !text.contains("Training algorithm: ROUND ROBIN"));
        Preferences.setBoolean(
            "controlPanel8", !text.contains("Re-enactment supply closet: LOCKED"));
        Preferences.setBoolean("controlPanel9", !text.contains("Thermostat setting: 76 DEGREES"));
        Matcher omegaMatcher = OMEGA_PATTERN.matcher(text);
        if (omegaMatcher.find()) {
          Preferences.setInteger(
              "controlPanelOmega", StringUtilities.parseInt(omegaMatcher.group(1)));
        }
        if (text.contains("Omega device activated")) {
          Preferences.setInteger("controlPanelOmega", 0);
          QuestDatabase.setQuestProgress(Quest.EVE, QuestDatabase.UNSTARTED);
          QuestDatabase.setQuestProgress(Quest.FAKE_MEDIUM, QuestDatabase.UNSTARTED);
          QuestDatabase.setQuestProgress(Quest.SERUM, QuestDatabase.UNSTARTED);
          QuestDatabase.setQuestProgress(Quest.SMOKES, QuestDatabase.UNSTARTED);
          QuestDatabase.setQuestProgress(Quest.OUT_OF_ORDER, QuestDatabase.UNSTARTED);
        }
      }

      case 1002 -> // Legend of the Temple in the Hidden City
      Preferences.setInteger("zigguratLianas", 1);

      case 1003 -> // Test Your Might And Also Test Other Things
      SorceressLairManager.parseContestBooth(0, text);

      case 1005, // 'Allo
          1006, // One Small Step For Adventurer
          1007, // Twisty Little Passages, All Hedge
          1008, // Pooling Your Resources
          1009, // Good Ol' 44% Duck
          1010, // Another Day, Another Fork
          1011, // Of Mouseholes and Manholes
          1012, // The Last Temptation
          1013 -> // Mazel Tov!
      SorceressLairManager.visitChoice(ChoiceManager.lastChoice, text);

      case 1023, 1024 -> {
        {
          // Like a Bat into Hell
          // Like a Bat out of Hell
          Matcher matcher = ED_RETURN_PATTERN.matcher(text);
          if (matcher.find()) {
            int cost = StringUtilities.parseInt(matcher.group(1));
            int defeats = 3 + (int) (Math.log(cost) / Math.log(2));
            Preferences.setInteger("_edDefeats", defeats);
          }
        }
      }

      case 1025 -> {
        {
          // Reconfigure your Mini-Crimbot
          Matcher matcher = CRIMBOT_CHASSIS_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("crimbotChassis", matcher.group(1));
          }
          matcher = CRIMBOT_ARM_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("crimbotArm", matcher.group(1));
          }
          matcher = CRIMBOT_PROPULSION_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("crimbotPropulsion", matcher.group(1));
          }
        }
      }

      case 1049 -> { // Tomb of the Unknown Your Class Here
        if (text.contains("The Epic Weapon's yours")) {
          QuestDatabase.setQuestProgress(Quest.NEMESIS, "step3");
        }
      }

      case 1051 -> // The Book of the Undying
      EdBaseRequest.inspectBook(text);

      case 1053 -> { // The Servants' Quarters
        EdBaseRequest.inspectServants(text);
        EdServantData.inspectServants(text);
      }

      case 1062 -> {
        // Lots of Options
        if (KoLCharacter.hasEquipped(CANDY_CANE_SWORD) && !text.contains("candy cane sword")) {
          Preferences.setBoolean("_candyCaneSwordOvergrownLot", true);
        }
      }

      case 1063 -> {
        {
          // Adjust your 'Edpiece
          Matcher matcher = EDPIECE_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("edPiece", matcher.group(1).trim());
          }
        }
      }

      case 1068 -> {
        {
          // Barf Mountain Breakdown
          Matcher matcher = DINSEY_ROLLERCOASTER_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("dinseyRollercoasterStats", matcher.group(1).trim());
          }
          Preferences.setBoolean(
              "dinseyRapidPassEnabled", text.contains("Disable Rapid-Pass System"));
        }
      }

      case 1069 -> {
        {
          // The Pirate Bay
          Matcher matcher = DINSEY_PIRATE_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setBoolean("dinseyGarbagePirate", (matcher.group(1).equals("lit")));
          }
        }
      }

      case 1070 -> {
        {
          // In Your Cups
          Matcher matcher = DINSEY_TEACUP_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("dinseyToxicMultiplier", matcher.group(1).trim());
          }
          Preferences.setBoolean(
              "dinseySafetyProtocolsLoose", text.contains("protocols seem pretty loose"));
        }
      }

      case 1071 -> {
        {
          // Gator Gamer
          Matcher matcher = DINSEY_SLUICE_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("dinseyGatorStenchDamage", matcher.group(1).trim());
          }
          Preferences.setBoolean("dinseyAudienceEngagement", text.contains("High Engagement Mode"));
        }
      }

      case 1075 -> {
        {
          // Mmmmmmayonnaise
          TurnCounter.stopCounting("Mmmmmmayonnaise window begin");
          TurnCounter.stopCounting("Mmmmmmayonnaise window end");
        }
      }

      case 1076 -> {
        {
          // Mayo Minder&trade;
          Matcher matcher = MAYO_MINDER_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("mayoMinderSetting", matcher.group(1).trim());
          } else {
            Preferences.setString("mayoMinderSetting", "");
          }
        }
      }

      case 1080 -> {
        // Bagelmat-5000
        if (KoLCharacter.hasEquipped(CANDY_CANE_SWORD) && !text.contains("candy cane sword")) {
          Preferences.setBoolean("_candyCaneSwordMadnessBakery", true);
        }
      }

      case 1087 -> // The Dark and Dank and Sinister Cave Entrance
      QuestDatabase.setQuestIfBetter(Quest.NEMESIS, "step11");

      case 1088 -> // Rubble, Rubble, Toil and Trouble
      QuestDatabase.setQuestIfBetter(Quest.NEMESIS, "step13");

      case 1093 -> {
        {
          // The WLF Bunker

          // There is no choice if this happens, but we recognise title in visitChoice()
          // You enter the bunker, but the speaker is silent. You've already done your day's work,
          // soldier!
          if (text.contains("the speaker is silent")) {
            Preferences.setBoolean("_volcanoItemRedeemed", true);
            Preferences.setInteger("_volcanoItem1", 0);
            Preferences.setInteger("_volcanoItem2", 0);
            Preferences.setInteger("_volcanoItem3", 0);
            Preferences.setInteger("_volcanoItemCount1", 0);
            Preferences.setInteger("_volcanoItemCount2", 0);
            Preferences.setInteger("_volcanoItemCount3", 0);
          } else {
            // On the other hand, if there IS a choice on the page,
            // it will be whichchoice=1093 asking to redeem items

            Preferences.setBoolean("_volcanoItemRedeemed", false);

            Matcher matcher = WLF_PATTERN.matcher(text);
            while (matcher.find()) {
              // String challenge = matcher.group( 1 );
              String descid = matcher.group(2);
              int itemId = ItemDatabase.getItemIdFromDescription(descid);
              if (itemId != -1) {
                String itemName = matcher.group(3).trim();
                Matcher countMatcher = WLF_COUNT_PATTERN.matcher(itemName);
                int count =
                    countMatcher.find() ? StringUtilities.parseInt(countMatcher.group(1)) : 1;
                String index = matcher.group(4);
                Preferences.setInteger("_volcanoItem" + index, itemId);
                Preferences.setInteger("_volcanoItemCount" + index, count);
              }
            }
          }
        }
      }

      case 1100 -> {
        // Pray to the Barrel God
        if (text.contains("You already prayed to the Barrel god today")) {
          Preferences.setBoolean("_barrelPrayer", true);
        } else {
          if (!text.contains("barrel lid shield")) {
            Preferences.setBoolean("prayedForProtection", true);
          }
          if (!text.contains("barrel hoop earring")) {
            Preferences.setBoolean("prayedForGlamour", true);
          }
          if (!text.contains("bankruptcy barrel")) {
            Preferences.setBoolean("prayedForVigor", true);
          }
        }
      }

      case 1110 -> // Spoopy
      Preferences.setBoolean("doghouseBoarded", !text.contains("Board up the doghouse"));

      case 1118 -> {
        {
          // X-32-F Combat Training Snowman Control Console
          Matcher matcher = SNOJO_CONSOLE_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("snojoSetting", matcher.group(1).trim());
          } else {
            Preferences.setString("snojoSetting", "");
          }
        }
      }

      case 1181 -> {
        // Witchess Set
        if (!text.contains("Examine the shrink ray")) {
          Preferences.setInteger("_witchessFights", 5);
        }
      }

      case 1188 -> {
        {
          // The Call is Coming from Outside the Simulation
          Matcher matcher = ENLIGHTENMENT_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger(
                "sourceEnlightenment", StringUtilities.parseInt(matcher.group(1)));
          }
        }
      }

      case 1191 -> // Source Terminal
      request.setHasResult(false);

      case 1193 -> {
        {
          // The Precinct
          Matcher matcher = CASE_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger(
                "_detectiveCasesCompleted", 3 - StringUtilities.parseInt(matcher.group(1)));
          }
        }
      }

      case 1195 -> {
        {
          // Spinning Your Time-Spinner
          Matcher matcher = TIME_SPINNER_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger(
                "_timeSpinnerMinutesUsed", 10 - StringUtilities.parseInt(matcher.group(1)));
          }
        }
      }

      case 1202, // Noon in the Civic Center
          1203, // Midnight in Civic Center
          1204, // Noon at the Train Station
          1205, // Midnight at the Train Station
          1206, // Noon in the Industrial Zone
          1207, // Midnight in the Industrial Zone
          1208, // Upscale Noon
          1209 -> { // Upscale Midnight
        {
          Preferences.increment("_gingerbreadCityTurns");
        }
      }

      case 1229 -> {
        {
          // L.O.V. Exit

          // As you are about to leave the station, you notice a
          // data entry pad and a sign above it that says <WORD>.
          // Huh, that's odd.

          Matcher matcher = LOV_EXIT_PATTERN.matcher(text);
          if (matcher.find()) {
            String message = "L.O.V. Exit word: " + matcher.group(1);
            RequestLogger.printLine(message);
            RequestLogger.updateSessionLog(message);
          }
        }
      }

      case 1234 -> {
        {
          // Spacegate Vaccinator

          Matcher matcher = VACCINE_PATTERN.matcher(text);

          while (matcher.find()) {
            String setting = "spacegateVaccine" + matcher.group(1);
            String button = matcher.group(2);
            if (button.startsWith("Select Vaccine")) {
              Preferences.setBoolean(setting, true);
            } else if (button.startsWith("Unlock Vaccine")) {
              Preferences.setBoolean(setting, false);
            }
          }
        }
      }

      case 1259 -> { // LI-11 HQ
        if (text.contains("LI-11 HQ")) {
          Pattern capitalPattern = Pattern.compile("<td><b>(.*?)</b>(.*?)</td></tr>");
          boolean bondAdv = false,
              bondWpn = false,
              bondInit = false,
              bondDR = false,
              bondHP = false,
              bondItem2 = false,
              bondStat = false,
              bondDrunk1 = false,
              bondBooze = false,
              bondSymbols = false,
              bondDrunk2 = false,
              bondJetpack = false,
              bondMartiniTurn = false,
              bondMeat = false,
              bondItem1 = false,
              bondMus1 = false,
              bondMys1 = false,
              bondMox1 = false,
              bondBeach = false,
              bondBeat = false,
              bondMartiniDelivery = false,
              bondMus2 = false,
              bondMys2 = false,
              bondMox2 = false,
              bondStealth = false,
              bondMartiniPlus = false,
              bondBridge = false,
              bondWar = false,
              bondMPregen = false,
              bondWeapon2 = false,
              bondItem3 = false,
              bondStealth2 = false,
              bondSpleen = false,
              bondStat2 = false,
              bondDesert = false,
              bondHoney = false;
          Matcher matcher = capitalPattern.matcher(text);
          while (matcher.find()) {
            if (matcher.group(2).contains("Active") || matcher.group(2).contains("Connected")) {
              switch (matcher.group(1)) {
                case "Super-Accurate Spy Watch" -> bondAdv = true;
                case "Razor-Sharp Tie" -> bondWpn = true;
                case "Jet-Powered Skis" -> bondInit = true;
                case "Kevlar-Lined Pants" -> bondDR = true;
                case "Injected Nanobots" -> bondHP = true;
                case "Sticky Climbing Gloves" -> bondItem2 = true;
                case "Retinal Knowledge HUD" -> bondStat = true;
                case "Belt-Implanted Still" -> bondDrunk1 = true;
                case "Alcohol Absorbent Underwear" -> bondBooze = true;
                case "Universal Symbology Guide" -> bondSymbols = true;
                case "Soberness Injection Pen" -> bondDrunk2 = true;
                case "Short-Range Jetpack" -> bondJetpack = true;
                case "Invisible Meat Car, the Vanish" -> bondStealth = true;
                case "Portable Pocket Bridge" -> bondBridge = true;
                case "Static-Inducing, Bug-Shorting Underpants" -> bondMPregen = true;
                case "Exotic Bartender, Barry L. Eagle" -> bondMartiniTurn = true;
                case "Renowned Meat Thief, Ivanna Cuddle" -> bondMeat = true;
                case "Master Art Thief, Sly Richard" -> bondItem1 = true;
                case "Personal Trainer, Debbie Dallas" -> bondMus1 = true;
                case "Rocket Scientist, Crimbo Jones" -> bondMys1 = true;
                case "Licensed Masseur, Oliver Closehoff" -> bondMox1 = true;
                case "Professional Cabbie, Rock Hardy" -> bondBeach = true;
                case "Fellow Spy, Daisy Duke" -> bondBeat = true;
                case "Fellow Spy, Prince O'Toole" -> bondMartiniDelivery = true;
                case "Personal Kinesiologist, Doctor Kittie" -> bondMus2 = true;
                case "Computer Hacker, Mitt Jobs" -> bondMys2 = true;
                case "Spa Owner, Fatima Jiggles" -> bondMox2 = true;
                case "Exotic Olive Procurer, Ben Dover" -> bondMartiniPlus = true;
                case "Trained Sniper, Felicity Snuggles" -> bondWar = true;
                case "Martial Arts Trainer, Jaques Trappe" -> bondWeapon2 = true;
                case "Electromagnetic Ring" -> bondItem3 = true;
                case "Robo-Spleen" -> bondSpleen = true;
                case "Universal GPS" -> bondDesert = true;
                case "Mission Controller, Maeby Moneypenny" -> bondStealth2 = true;
                case "Sage Advisor, London McBrittishman" -> bondStat2 = true;
                case "True Love, Honey Potts" -> bondHoney = true;
              }
            }
          }
          Preferences.setBoolean("bondAdv", bondAdv);
          Preferences.setBoolean("bondWpn", bondWpn);
          Preferences.setBoolean("bondInit", bondInit);
          Preferences.setBoolean("bondDA", bondDR);
          Preferences.setBoolean("bondHP", bondHP);
          Preferences.setBoolean("bondItem2", bondItem2);
          Preferences.setBoolean("bondStat", bondStat);
          Preferences.setBoolean("bondDrunk1", bondDrunk1);
          Preferences.setBoolean("bondBooze", bondBooze);
          Preferences.setBoolean("bondSymbols", bondSymbols);
          Preferences.setBoolean("bondDrunk2", bondDrunk2);
          Preferences.setBoolean("bondJetpack", bondJetpack);
          Preferences.setBoolean("bondStealth", bondStealth);
          Preferences.setBoolean("bondMartiniTurn", bondMartiniTurn);
          Preferences.setBoolean("bondMeat", bondMeat);
          Preferences.setBoolean("bondItem1", bondItem1);
          Preferences.setBoolean("bondMus1", bondMus1);
          Preferences.setBoolean("bondMys1", bondMys1);
          Preferences.setBoolean("bondMox1", bondMox1);
          Preferences.setBoolean("bondBeach", bondBeach);
          Preferences.setBoolean("bondBeat", bondBeat);
          Preferences.setBoolean("bondMartiniDelivery", bondMartiniDelivery);
          Preferences.setBoolean("bondMus2", bondMus2);
          Preferences.setBoolean("bondMys2", bondMys2);
          Preferences.setBoolean("bondMox2", bondMox2);
          Preferences.setBoolean("bondMartiniPlus", bondMartiniPlus);
          Preferences.setBoolean("bondBridge", bondBridge); // need to handle when this is selected
          Preferences.setBoolean("bondWar", bondWar);
          Preferences.setBoolean("bondMPregen", bondMPregen);
          Preferences.setBoolean("bondWeapon2", bondWeapon2);
          Preferences.setBoolean("bondItem3", bondItem3);
          Preferences.setBoolean("bondStealth2", bondStealth2);
          Preferences.setBoolean("bondSpleen", bondSpleen);
          Preferences.setBoolean("bondStat2", bondStat2);
          Preferences.setBoolean("bondDesert", bondDesert); // Do something with this
          Preferences.setBoolean("bondHoney", bondHoney);
          if (bondBeach) {
            KoLCharacter.setDesertBeachAvailable();
          }
          KoLCharacter.recalculateAdjustments();
          KoLCharacter.updateStatus();
        }
      }

      case 1266 -> {
        {
          // The Hostler
          Preferences.setBoolean("horseryAvailable", true);

          // <td valign=top class=small><b>Drab Teddy</b> the Normal Horse<P>
          // <td valign=top class=small><b>Surreptitious Mantilla</b> the Dark Horse<P>
          // <td valign=top class=small><b>Wacky Biggles</b> the Crazy Horse<P>
          // <td valign=top class=small><b>Frightful Twiggy</b> the Pale Horse<P>

          // Save the horse names so we can recognize them in combat
          Pattern pattern =
              Pattern.compile("<td valign=top class=small><b>([^<]+)</b> the ([^ ]+) Horse<P>");
          Matcher matcher = pattern.matcher(text);
          while (matcher.find()) {
            String name = matcher.group(1);
            String type = matcher.group(2);
            String setting =
                type.equals("Crazy")
                    ? "_horseryCrazyName"
                    : type.equals("Dark")
                        ? "_horseryDarkName"
                        : type.equals("Normal")
                            ? "_horseryNormalName"
                            : type.equals("Pale") ? "_horseryPaleName" : null;
            if (setting != null) {
              Preferences.setString(setting, name);
            }
          }

          pattern =
              Pattern.compile(
                  "Gives you\\s+([+-]\\d+)% Muscle, ([+-]\\d+)% Mysticality, and ([+-]\\d+)%");
          matcher = pattern.matcher(text);
          if (matcher.find()) {
            Preferences.setString("_horseryCrazyMus", matcher.group(1));
            Preferences.setString("_horseryCrazyMys", matcher.group(2));
            Preferences.setString("_horseryCrazyMox", matcher.group(3));
          }

          if (!text.contains("name=option value=1")) {
            Preferences.setString("_horsery", "normal horse");
          } else if (!text.contains("name=option value=2")) {
            Preferences.setString("_horsery", "dark horse");
          } else if (!text.contains("name=option value=3")) {
            Preferences.setString("_horsery", "crazy horse");
          } else if (!text.contains("name=option value=4")) {
            Preferences.setString("_horsery", "pale horse");
          } else {
            Preferences.setString("_horsery", "");
          }
        }
      }

      case 1267 -> // Rubbed it the Right Way
      GenieRequest.visitChoice(text);

      case 1308 -> {
        {
          // place.php?whichplace=monorail&action=monorail_downtown
          //
          // On a Downtown Train
          //
          // Everything you do at this location uses this choice
          // adventure #. As you select options, you stay in the
          // same choice, but the available options change.
          //
          // We must deduce what is happening by looking at the
          // response text

          // Looks like your order for a <muffin type> muffin is not yet ready.
          Matcher muffinMatcher = MUFFIN_TYPE_PATTERN.matcher(text);
          if (muffinMatcher.find()) {
            Preferences.setString("muffinOnOrder", muffinMatcher.group(1));
          }
          // "Sorry, you placed your order a lifetime ago, so we had to throw out the actual baked
          // good. Here's your earthenware cookware.
          else if (text.contains("you placed your order a lifetime ago")) {
            Preferences.setString("muffinOnOrder", "none");
          }
          // You spot your order from the other day, neatly labelled on the pickup shelf.
          else if (text.contains("You spot your order from the other day")) {
            Preferences.setString("muffinOnOrder", "none");
          }
          // (If we have an earthenware muffin tin and the "order" buttons are present)
          else if (text.contains("Order a blueberry muffin")) {
            Preferences.setString("muffinOnOrder", "none");
          }

          // "Excellent! Here's your muffin tin! Stop by any time to order a muffin!"
          if (text.contains("Here's your muffin tin!")) {
            ResultProcessor.processItem(ItemPool.SHOVELFUL_OF_EARTH, -10);
            ResultProcessor.processItem(ItemPool.HUNK_OF_GRANITE, -10);
          }
        }
      }

      case 1309 -> {
        // We will either now, or in the past, have had Favored By Lyle
        Preferences.setBoolean("_lyleFavored", true);
        if (KoLCharacter.hasEquipped(CANDY_CANE_SWORD)) {
          Preferences.setBoolean("_candyCaneSwordLyle", true);
        }
      }

      case 1312 -> {
        {
          // Choose a Soundtrack
          Matcher matcher = BOOMBOX_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("_boomBoxSongsLeft", matcher.group(1));
          }
          Preferences.setString("boomBoxSong", "");
          matcher = BOOMBOX_SONG_PATTERN.matcher(text);
          while (matcher.find()) {
            if (matcher.group(2) != null && matcher.group(2).contains("Keep playing")) {
              Preferences.setString("boomBoxSong", matcher.group(1));
            }
          }
        }
      }

      case 1313, // Bastille Battalion
          1314, // Bastille Battalion (Master of None)
          1315, // Castle vs. Castle
          1316, // GAME OVER
          1317, // A Hello to Arms (Battalion)
          1318, // Defensive Posturing
          1319 -> // Cheese Seeking Behavior
      BastilleBattalionManager.visitChoice(request);

      case 1322 -> {
        // The Beginning of the Neverend
        if (text.contains("talk to him and help him get more booze")) {
          Preferences.setString("_questPartyFairQuest", "booze");
        } else if (text.contains("Think you can help me clean the place up?")) {
          Preferences.setString("_questPartyFairQuest", "trash");
        } else if (text.contains(
            "helping her with whatever problem she's having with the snacks")) {
          Preferences.setString("_questPartyFairQuest", "food");
        } else if (text.contains("megawoots right now")) {
          Preferences.setString("_questPartyFairQuest", "woots");
        } else if (text.contains("taking up a collection from the guests")) {
          Preferences.setString("_questPartyFairQuest", "dj");
        } else if (text.contains("all of the people to leave")) {
          Preferences.setString("_questPartyFairQuest", "partiers");
        }
      }

      case 1324 -> // It Hasn't Ended, It's Just Paused
      Preferences.setInteger("encountersUntilNEPChoice", 7);

      case 1329 -> // Latte Shop
      LatteRequest.parseVisitChoice(text);

      case 1331 -> {
        {
          // Daily Loathing Ballot
          Matcher localMatcher = VOTE_PATTERN.matcher(text);
          while (localMatcher.find()) {
            int voteValue = StringUtilities.parseInt(localMatcher.group(1)) + 1;
            String voteMod = ModifierDatabase.parseModifier(localMatcher.group(3));
            if (voteMod != null) {
              Preferences.setString("_voteLocal" + voteValue, voteMod);
            }
          }

          Matcher platformMatcher = VOTE_SPEECH_PATTERN.matcher(text);

          int count = 1;

          while (platformMatcher.find()) {
            String party = platformMatcher.group(3);
            String speech = platformMatcher.group(4);

            String monsterName = parseVoteSpeech(party, speech);

            if (monsterName != null) {
              Preferences.setString("_voteMonster" + count, monsterName);
            }

            count++;
          }
        }
      }

      case 1336 -> {
        {
          // Boxing Daycare
          Matcher matcher = DAYCARE_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("daycareEquipment", matcher.group(1).replaceAll(",", ""));
            Preferences.setString("daycareToddlers", matcher.group(2).replaceAll(",", ""));
            String instructors = matcher.group(3);
            if (instructors.equals("an")) {
              instructors = "1";
            }
            Preferences.setString("daycareInstructors", instructors);
          } else {
            matcher = EARLY_DAYCARE_PATTERN.matcher(text);
            if (matcher.find()) {
              Preferences.setString("daycareToddlers", matcher.group(1).replaceAll(",", ""));
              String instructors = matcher.group(2);
              if (instructors.equals("an")) {
                instructors = "1";
              }
              Preferences.setString("daycareInstructors", instructors);
            }
          }
          Matcher recruitsToday = DAYCARE_RECRUITS_PATTERN.matcher(text);
          if (recruitsToday.find()) {
            Preferences.setInteger(
                "_daycareRecruits", (recruitsToday.group(1).replaceAll(",", "")).length() - 3);
          }
          Matcher instructorItem = DAYCARE_INSTRUCTOR_ITEM_PATTERN.matcher(text);
          if (instructorItem.find()) {
            int quantity = Integer.parseInt(instructorItem.group(1).replaceAll(",", ""));
            Preferences.setInteger("daycareInstructorItemQuantity", quantity);
            Preferences.setInteger(
                "daycareInstructorItem", ItemDatabase.getItemId(instructorItem.group(2), quantity));
          }
          // *** Update _daycareScavenges
        }
      }

      case 1339 -> {
        {
          // A Little Pump and Grind
          Matcher matcher = SAUSAGE_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger(
                "_sausagesMade",
                StringUtilities.parseInt(matcher.group(2).replaceAll(",", "")) / 111 - 1);
            Preferences.setString("sausageGrinderUnits", matcher.group(3));
          }
        }
      }

      case 1340 -> {
        {
          // Is There A Doctor In The House?
          Matcher matcher = DOCTOR_BAG_PATTERN.matcher(text);
          if (matcher.find()) {
            String malady = matcher.group(1);
            String item = "";
            if (malady.contains("tropical heatstroke")) {
              item = "palm-frond fan";
            } else if (malady.contains("archaic cough")) {
              item = "antique bottle of cough syrup";
            } else if (malady.contains("broken limb")) {
              item = "cast";
            } else if (malady.contains("low vim and vigor")) {
              item = "Doc Galaktik's Vitality Serum";
            } else if (malady.contains("bad clams")) {
              item = "anti-anti-antidote";
            } else if (malady.contains("criss-cross laceration")) {
              item = "plaid bandage";
            } else if (malady.contains("knocked out by a random encounter")) {
              item = "phonics down";
            } else if (malady.contains("Thin Blood Syndrome")) {
              item = "red blood cells";
            } else if (malady.contains("a blood shortage")) {
              item = "bag of pygmy blood";
            }
            Preferences.setString("doctorBagQuestItem", item);
            Preferences.setString("doctorBagQuestLocation", matcher.group(2));
          }
        }
      }

      case 1347 -> { // Groggy's Tavern
        {
          var choices = ChoiceUtilities.parseChoices(text);
          choices.forEach(
              (choice, crewmate) ->
                  Preferences.setString(
                      "_pirateRealmCrewmate" + choice, crewmate.replaceFirst("^the ", "")));
          // Correct knowledge of third crewmate unlock if necessary.
          Preferences.setBoolean("pirateRealmUnlockedThirdCrewmate", choices.size() >= 3);
        }
      }

      case 1348 -> { // Seaside Curios
        {
          var choices = ChoiceUtilities.parseChoices(text);
          Preferences.setBoolean("pirateRealmUnlockedAnemometer", choices.containsKey(4));
          Preferences.setBoolean("pirateRealmUnlockedFlag", choices.containsKey(5));
          Preferences.setBoolean("pirateRealmUnlockedSpyglass", choices.containsKey(6));
        }
      }

      case 1349 -> { // Dishonest Ed's Ships
        {
          var choices = ChoiceUtilities.parseChoices(text);
          Preferences.setBoolean("pirateRealmUnlockedClipper", choices.containsKey(4));
          Preferences.setBoolean("pirateRealmUnlockedManOWar", choices.containsKey(5));
        }
      }

      case 1388 -> {
        BeachManager.parseCombUsage(text);
        BeachManager.parseBeachMap(text);
      }

      case 1396 -> {
        {
          // Adjusting Your Fish
          Matcher matcher = RED_SNAPPER_PATTERN.matcher(text);
          if (matcher.find()) {
            Phylum phylum = Phylum.find(matcher.group(1));
            int progress = StringUtilities.parseInt(matcher.group(2));
            TrackManager.track(phylum.toString(), Tracker.RED_SNAPPER);
            Preferences.setInteger("redSnapperProgress", progress);
          }
        }
      }

      case 1407 -> {
        {
          // Mushroom District Costume Shop
          Matcher matcher = MUSHROOM_COSTUME_PATTERN.matcher(text);
          int cost = 0;
          boolean carpenter = false;
          boolean gardener = false;
          boolean ballerina = false;
          while (matcher.find()) {
            String costume = matcher.group(2);
            cost = StringUtilities.parseInt(matcher.group(3));
            switch (costume) {
              case "Carpenter" -> carpenter = true;
              case "Gardener" -> gardener = true;
              case "Ballerina" -> ballerina = true;
            }
          }
          String wearing =
              !carpenter ? "muscle" : !gardener ? "mysticality" : !ballerina ? "moxie" : "none";
          Preferences.setInteger("plumberCostumeCost", cost);
          Preferences.setString("plumberCostumeWorn", wearing);
        }
      }

      case 1408 -> {
        {
          // Mushroom District Badge Shop
          Matcher matcher = MUSHROOM_BADGE_PATTERN.matcher(text);
          int cost = 0;
          if (matcher.find()) {
            cost = StringUtilities.parseInt(matcher.group(1));
          }
          Preferences.setInteger("plumberBadgeCost", cost);
        }
      }

      case 1410 -> {
        {
          // The Mushy Center

          // The mushroom in your garden is now large enough to walk around inside.  Also there's a
          // door on it, which is convenient for that purpose.

          int mushroomMessageLevel =
              text.contains("walk around inside")
                  ? 11
                  : text.contains("immense mushroom")
                      ? 5
                      : text.contains("giant mushroom")
                          ? 4
                          : text.contains("bulky mushroom")
                              ? 3
                              : text.contains("plump mushroom")
                                  ? 2
                                  : text.contains("decent-sized mushroom") ? 1 : 0;

          // mushgrow5.gif is used for both the immense and colossal mushroom

          int mushroomImageLevel =
              text.contains("mushgrow5.gif")
                  ? 5
                  : text.contains("mushgrow4.gif")
                      ? 4
                      : text.contains("mushgrow3.gif")
                          ? 3
                          : text.contains("mushgrow2.gif")
                              ? 2
                              : text.contains("mushgrow1.gif") ? 1 : 0;

          int mushroomLevel = Math.max(mushroomMessageLevel, mushroomImageLevel);

          // After level 5, you can continue fertilizing for an unknown number
          // of days before you get the ultimate mushroom.

          // If we have fertilized the garden through KoLmafia,
          // this is the number of days we have fertilized.
          int currentLevel = Preferences.getInteger("mushroomGardenCropLevel");

          // If we have not fertilized consistently through KoLmafia, correct it here
          int newLevel = Math.max(mushroomLevel, currentLevel);

          Preferences.setInteger("mushroomGardenCropLevel", newLevel);
          CampgroundRequest.clearCrop();
          CampgroundRequest.setCampgroundItem(new Mushroom(newLevel));
        }
      }
      case 1412 -> {
        Matcher tierMatcher = GUZZLR_TIER_PATTERN.matcher(text);
        while (tierMatcher.find()) {
          Preferences.setInteger(
              "guzzlr" + tierMatcher.group(2) + "Deliveries",
              StringUtilities.parseInt(tierMatcher.group(1)));
        }

        if (text.contains("You are currently tasked with")) {
          Matcher alreadyMatcher = GUZZLR_QUEST_PATTERN.matcher(text);
          if (alreadyMatcher.find()) {
            String booze = alreadyMatcher.group(1);
            Preferences.setString(
                "guzzlrQuestBooze",
                booze.equals("special personalized cocktail") ? "Guzzlr cocktail set" : booze);
            Preferences.setString("guzzlrQuestClient", alreadyMatcher.group(2));
            Preferences.setString("guzzlrQuestLocation", alreadyMatcher.group(3));
          }

          int itemId = ItemDatabase.getItemId(Preferences.getString("guzzlrQuestBooze"));

          if (itemId > 0) {
            if (InventoryManager.getCount(itemId) > 0) {
              QuestDatabase.setQuestProgress(Quest.GUZZLR, "step1");
            } else {
              QuestDatabase.setQuestProgress(Quest.GUZZLR, QuestDatabase.STARTED);
            }
          } else {
            QuestDatabase.setQuestProgress(Quest.GUZZLR, QuestDatabase.STARTED);
          }

          Preferences.setBoolean(
              "_guzzlrQuestAbandoned",
              (ChoiceUtilities.findChoiceDecisionIndex("Abandon Client", text).equals("0")));
        } else {
          // If we have unlocked Gold Tier but cannot accept one, we must have already accepted
          // three.
          boolean unlockedGoldTier = Preferences.getInteger("guzzlrBronzeDeliveries") >= 5;
          if (unlockedGoldTier
              && ChoiceUtilities.findChoiceDecisionIndex("Gold Tier", text).equals("0")) {
            Preferences.setInteger("_guzzlrGoldDeliveries", 3);
          }

          // If we have unlocked Platinum Tier but cannot accept one, we must have already accepted
          // one.
          boolean unlockedPlatinumTier = Preferences.getInteger("guzzlrGoldDeliveries") >= 5;
          if (unlockedPlatinumTier
              && ChoiceUtilities.findChoiceDecisionIndex("Platinum Tier", text).equals("0")) {
            Preferences.setInteger("_guzzlrPlatinumDeliveries", 1);
          }
        }
      }

      case 1420 -> // Cargo Cultist Shorts
      CargoCultistShortsRequest.parseAvailablePockets(text);

      case 1425 -> // Oh Yeah!
      Preferences.setInteger("lastCartographyFratHouse", KoLCharacter.getAscensions());

      case 1427 -> //  The Hidden Junction
      Preferences.setInteger("lastCartographyGuanoJunction", KoLCharacter.getAscensions());

      case 1428 -> //  Your Neck of the Woods
      Preferences.setInteger("lastCartographyDarkNeck", KoLCharacter.getAscensions());

      case 1429 -> //  No Nook Unknown
      Preferences.setInteger("lastCartographyDefiledNook", KoLCharacter.getAscensions());

      case 1430 -> //  Ghostly Memories
      Preferences.setInteger("lastCartographyBooPeak", KoLCharacter.getAscensions());

      case 1431 -> //  Here There Be Giants
      Preferences.setInteger("lastCartographyCastleTop", KoLCharacter.getAscensions());

      case 1432 -> //  Mob Maptality
      Preferences.setInteger("lastCartographyZeppelinProtesters", KoLCharacter.getAscensions());

      case 1433 -> //  Sneaky, Sneaky (Frat Warrior Fatigues)
      Preferences.setInteger("lastCartographyFratHouseVerge", KoLCharacter.getAscensions());

      case 1434 -> //  Sneaky, Sneaky (War Hippy Fatigues)
      Preferences.setInteger("lastCartographyHippyCampVerge", KoLCharacter.getAscensions());

      case 1436 -> // Billards Room Options
      Preferences.setInteger("lastCartographyHauntedBilliards", KoLCharacter.getAscensions());

      case 1445, // Reassembly Station
          1447 -> // Statbot 5000
      YouRobotManager.visitChoice(request);

      case 1448 -> {
        {
          String[] stalkStatus = new String[7];
          Matcher stalks =
              Pattern.compile(
                      "<button.*?name=\"pp\" value=\"(\\d+)\".*?>\\s+<img.*?src=\".*?/otherimages/powerplant/(\\d+)\\.png\"")
                  .matcher(text);
          while (stalks.find()) {
            int pp = Integer.parseInt(stalks.group(1)) - 1;
            stalkStatus[pp] = stalks.group(2);
          }
          Preferences.setString("_pottedPowerPlant", String.join(",", stalkStatus));
        }
      }
      case 1449 -> {
        {
          String setting =
              (!text.contains("Warning Beep"))
                  ? "ml"
                  : (!text.contains("Infrared Spectrum"))
                      ? "meat"
                      : (!text.contains("Maximum Framerate")) ? "init" : "";

          Preferences.setString("backupCameraMode", setting);
          Preferences.setBoolean("backupCameraReverserEnabled", text.contains("Disable Reverser"));
        }
      }
      case 1455 -> {
        CampgroundRequest.setCurrentWorkshedItem(ItemPool.COLD_MEDICINE_CABINET);
        Matcher consultations = Pattern.compile("You have <b>(\\d)</b> consul").matcher(text);
        if (consultations.find()) {
          int remaining = Integer.parseInt(consultations.group(1));
          Preferences.setInteger("_coldMedicineConsults", 5 - remaining);
        }
        var equipmentCount = text.contains("ice crown") ? 0 : text.contains("frozen jeans") ? 1 : 2;
        Preferences.setInteger("_coldMedicineEquipmentTaken", equipmentCount);
      }
      case 1462 -> CrystalBallManager.parsePonder(text);
      case 1463 -> LocketManager.parseMonsters(text);
      case 1476 -> StillSuitManager.parseChoice(text);
      case 1483 -> AutumnatonManager.visitChoice(text);
      case 1484 -> { // Conspicuous Plaque
        var pattern = Pattern.compile("The plaque currently reads: <b>(.*?)</b>");
        var matcher = pattern.matcher(text);
        if (matcher.find()) {
          Preferences.setString("speakeasyName", matcher.group(1));
        }
      }
      case 1485 -> { // Play with your train
        CampgroundRequest.setCurrentWorkshedItem(ItemPool.MODEL_TRAIN_SET);
        TrainsetManager.visitChoice(text);
      }
      case 1491 -> // Strange Stalagmite(s)
      Preferences.setBoolean("_strangeStalagmiteUsed", true);

      case 1497 -> // Calling Rufus
      RufusManager.parseCall(text);

      case 1498 -> // Calling Rufus Back
      RufusManager.parseCallBack(text);

      case 1499 -> // The Shadow Labyrinth
      RufusManager.handleShadowRiftNC(1499, text);

      case 1500 -> // Like a Loded Stone
      RufusManager.handleShadowRiftNC(1500, text);

      case 1501 -> // Make a Wish
      MonkeyPawRequest.visitChoice(text);

      case 1510 -> // Burning Leaves
      BurningLeavesRequest.visitChoice(text);

      case 1512, 1513 -> {
        if (text.contains("don't even think about pressing that button")) {
          Preferences.setString(
              "_automatedFutureSide", ChoiceManager.lastChoice == 1512 ? "bearings" : "solenoids");
        } else if (text.contains("You've already pushed the button eleven times today")) {
          Preferences.setString(
              "_automatedFutureSide", ChoiceManager.lastChoice == 1512 ? "solenoids" : "bearings");
          Preferences.setInteger("_automatedFutureManufactures", 11);
        }
      }

      case 1517 -> {
        Matcher eggsSpawned = Pattern.compile("(\\d+)/11 eggs spawned today").matcher(text);
        if (eggsSpawned.find()) {
          int spawned = Integer.parseInt(eggsSpawned.group(1));
          Preferences.setInteger("_mimicEggsObtained", spawned);
        }
        Matcher eggsDonated = Pattern.compile("(\\d+)/3 donations made for the day").matcher(text);
        if (eggsDonated.find()) {
          int donated = Integer.parseInt(eggsDonated.group(1));
          Preferences.setInteger("_mimicEggsDonated", donated);
        }
      }

      case 1523 -> // Research Bench
      ResearchBenchRequest.visitChoice(text);

      case 1527 -> // Consider the Calendar
      parseMayamCalendar(text);

      case 1537 -> { // TakerSpace
        CampgroundRequest.setCurrentWorkshedItem(ItemPool.TAKERSPACE_LETTER_OF_MARQUE);

        // visiting the choice for the first time delivers the daily supplies
        Preferences.setBoolean("_takerSpaceSuppliesDelivered", true);
        Matcher supplies =
            Pattern.compile(
                    "<b>Current Supplies:</b><br>(\\d+) stolen spices<br>(\\d+) robbed rums<br>(\\d+) absconded-with anchors?<br>(\\d+) misappropriated mainmasts<br>(\\d+) snatched silk<br>(\\d+) gaffled gold<br>")
                .matcher(text);
        if (supplies.find()) {
          Preferences.setInteger("takerSpaceSpice", Integer.parseInt(supplies.group(1)));
          Preferences.setInteger("takerSpaceRum", Integer.parseInt(supplies.group(2)));
          Preferences.setInteger("takerSpaceAnchor", Integer.parseInt(supplies.group(3)));
          Preferences.setInteger("takerSpaceMast", Integer.parseInt(supplies.group(4)));
          Preferences.setInteger("takerSpaceSilk", Integer.parseInt(supplies.group(5)));
          Preferences.setInteger("takerSpaceGold", Integer.parseInt(supplies.group(6)));
          ConcoctionDatabase.refreshConcoctions();
        }
      }

      case 1545 -> // Cyberzone 1 Half-Way
      Preferences.setInteger("_cyberZone1Turns", 10);
      case 1546 -> // Cyberzone 1 Final
      Preferences.setInteger("_cyberZone1Turns", 20);
      case 1547 -> // Cyberzone 2 Half-Way
      Preferences.setInteger("_cyberZone2Turns", 10);
      case 1548 -> // Cyberzone 2 Final
      Preferences.setInteger("_cyberZone2Turns", 20);
      case 1549 -> // Cyberzone 3 Half-Way
      Preferences.setInteger("_cyberZone3Turns", 10);
      case 1550 -> // Cyberzone 3 Final
      Preferences.setInteger("_cyberZone3Turns", 20);
      case 1555 -> { // Specimen Preparation Bench
        Matcher benchUsed = Pattern.compile(" You have done so (\\d+) time").matcher(text);
        if (benchUsed.find()) {
          int spawned = Integer.parseInt(benchUsed.group(1));
          Preferences.setInteger("zootSpecimensPrepared", spawned);
        }
      }
      case 1556 -> // Leprecondo
      LeprecondoManager.visit(text);
      case 1557 -> // Peering Through Your Peridot
      Preferences.setString(
          "_perilLocations",
          (locations) ->
              Stream.concat(
                      Stream.of(KoLAdventure.lastVisitedLocation.getAdventureId()),
                      Arrays.stream(locations.split(",")))
                  .filter(Predicate.not(String::isBlank))
                  .map(StringUtilities::parseInt)
                  .sorted()
                  .distinct()
                  .map(i -> Integer.toString(i))
                  .collect(Collectors.joining(",")));
      case 1558 -> { // Foreseeing Peril
        if (text.contains("You've already foreseen enough peril today.")) {
          Preferences.setInteger("_perilsForeseen", 3);
          break;
        }
        Matcher matcher =
            Pattern.compile("You can foresee peril (\\d+) more times? today").matcher(text);
        if (matcher.find()) {
          Preferences.setInteger("_perilsForeseen", 3 - Integer.parseInt(matcher.group(1)));
        }
      }

      case 1560 -> {
        // A Cooler Yeti Conversation
        if (text.contains("Make my next drink impossibly cold")) {
          Preferences.setBoolean("_coolerYetiAdventures", false);
        } else {
          Preferences.setBoolean("_coolerYetiAdventures", true);
        }

        if (!text.contains("He's busy with")) {
          Preferences.setString("coolerYetiMode", "");
        } else if (text.contains("He's busy with a cooler")) {
          Preferences.setString("coolerYetiMode", "adventures");
        } else if (text.contains("He's busy with an ice cup")) {
          Preferences.setString("coolerYetiMode", "effect");
        } else if (text.contains("He's busy with his bar")) {
          Preferences.setString("coolerYetiMode", "bar");
        } else if (text.contains("He's busy with a flawless ice cube")) {
          Preferences.setString("coolerYetiMode", "stats");
        }
      }

      case 1561 -> // Request Supply Drop
      AlliedRadioRequest.visitChoice(text);

      case 1562 -> { // Time is a Möbius Strip
        Preferences.setInteger("_lastMobiusStripTurn", KoLCharacter.getTurnsPlayed());
        Preferences.increment("_mobiusStripEncounters", 1);
      }
    }
  }

  public static final String parseVoteSpeech(final String party, final String speech) {
    if (party.contains("Pork Elf Historical Preservation Party")) {
      if (speech.contains("strict curtailing of unnatural modern technologies")) {
        return "government bureaucrat";
      } else if (speech.contains("reintroduce Pork Elf DNA")) {
        return "terrible mutant";
      } else if (speech.contains("kingdom-wide seance")) {
        return "angry ghost";
      } else if (speech.contains("very interested in snakes")) {
        return "annoyed snake";
      } else if (speech.contains("lots of magical lard")) {
        return "slime blob";
      }
    } else if (party.contains("Clan Ventrilo")) {
      if (speech.contains("bringing this blessing to the entire population")) {
        return "slime blob";
      } else if (speech.contains("see your deceased loved ones again")) {
        return "angry ghost";
      } else if (speech.contains("stronger and more vigorous")) {
        return "terrible mutant";
      } else if (speech.contains("implement healthcare reforms")) {
        return "government bureaucrat";
      } else if (speech.contains("flavored drink in a tube")) {
        return "annoyed snake";
      }
    } else if (party.contains("Bureau of Efficient Government")) {
      if (speech.contains("graveyards are a terribly inefficient use of space")) {
        return "angry ghost";
      } else if (speech.contains("strictly enforced efficiency laws")) {
        return "government bureaucrat";
      } else if (speech.contains("distribute all the medications for all known diseases ")) {
        return "terrible mutant";
      } else if (speech.contains("introduce an influx of snakes")) {
        return "annoyed snake";
      } else if (speech.contains("releasing ambulatory garbage-eating slimes")) {
        return "slime blob";
      }
    } else if (party.contains("Scions of Ich'Xuul'kor")) {
      if (speech.contains("increase awareness of our really great god")) {
        return "terrible mutant";
      } else if (speech.contains("hunt these evil people down")) {
        return "government bureaucrat";
      } else if (speech.contains("sound of a great hissing")) {
        return "annoyed snake";
      } else if (speech.contains("make things a little bit more like he's used to")) {
        return "slime blob";
      } else if (speech.contains("kindness energy")) {
        return "angry ghost";
      }
    } else if (party.contains("Extra-Terrific Party")) {
      if (speech.contains("wondrous chemical")) {
        return "terrible mutant";
      } else if (speech.contains("comprehensive DNA harvesting program")) {
        return "government bureaucrat";
      } else if (speech.contains("mining and refining processes begin")) {
        return "slime blob";
      } else if (speech.contains("warp engines will not destabilize")) {
        return "angry ghost";
      } else if (speech.contains("breeding pair of these delightful creatures")) {
        return "annoyed snake";
      }
    }

    return null;
  }

  public static int findBooPeakLevel(String decisionText) {
    if (decisionText == null) {
      return 0;
    }
    return switch (decisionText) {
      case "Ask the Question",
          "Talk to the Ghosts",
          "I Wanna Know What Love Is",
          "Tap Him on the Back",
          "Avert Your Eyes",
          "Approach a Raider",
          "Approach the Argument",
          "Approach the Ghost",
          "Approach the Accountant Ghost",
          "Ask if He's Lost" -> 1;
      case "Enter the Crypt",
          "Try to Talk Some Sense into Them",
          "Put Your Two Cents In",
          "Talk to the Ghost",
          "Tell Them What Werewolves Are",
          "Scream in Terror",
          "Check out the Duel",
          "Watch the Fight",
          "Approach and Reproach",
          "Talk Back to the Robot" -> 2;
      case "Go down the Steps",
          "Make a Suggestion",
          "Tell Them About True Love",
          "Scold the Ghost",
          "Examine the Pipe",
          "Say What?",
          "Listen to the Lesson",
          "Listen in on the Discussion",
          "Point out the Malefactors",
          "Ask for Information" -> 3;
      case "Hurl Some Spells of Your Own",
          "Take Command",
          "Lose Your Patience",
          "Fail to Stifle a Sneeze",
          "Ask for Help",
          "Ask How Duskwalker Basketball Is Played, Against Your Better Judgment",
          "Knights in White Armor, Never Reaching an End",
          "Own up to It",
          "Approach the Poor Waifs",
          "Look Behind You" -> 4;
      case "Read the Book",
          "Join the Conversation",
          "Speak of the Pompatus of Love",
          "Ask What's Going On",
          "Interrupt the Rally",
          "Ask What She's Doing Up There",
          "Point Out an Unfortunate Fact",
          "Try to Talk Sense",
          "Ask for Directional Guidance",
          "What?" -> 5;
      default -> 0;
    };
  }

  private static void checkGuyMadeOfBees(final GenericRequest request) {
    KoLCharacter.ensureUpdatedGuyMadeOfBees();
    Preferences.increment("guyMadeOfBeesCount", 1, 5, true);

    String text = request.responseText;
    String urlString = request.getPath();

    if (urlString.startsWith("fight.php")) {
      if (text.contains("guy made of bee pollen")) {
        // Record that we beat the guy made of bees.
        Preferences.setBoolean("guyMadeOfBeesDefeated", true);
      }
    } else if (urlString.startsWith("choice.php") && text.contains("that ship is sailed")) {
      // For some reason, we didn't notice when we
      // beat the guy made of bees. Record it now.
      Preferences.setBoolean("guyMadeOfBeesDefeated", true);
    }
  }

  private static void handleSourceTerminal(final String input, String text) {
    if (input.startsWith("educate")) {
      int successIndex = text.lastIndexOf("active skills");
      int failIndex = text.lastIndexOf("missing educate");
      int listIndex = text.lastIndexOf("usage: educate [target file]");

      if (listIndex > successIndex && listIndex > failIndex) {
        int startIndex = text.lastIndexOf("available targets:");
        int endIndex = text.lastIndexOf("&gt;");
        if (startIndex == -1 || endIndex == -1) {
          // this shouldn't happen...
          return;
        }
        text = text.substring(startIndex, endIndex);
        Pattern EDUCATE_PATTERN = Pattern.compile("rel=\"educate (.*?).edu\"");
        StringBuilder knownString = new StringBuilder();

        Matcher matcher = EDUCATE_PATTERN.matcher(text);
        while (matcher.find()) {
          if (knownString.length() > 0) {
            knownString.append(",");
          }
          knownString.append(matcher.group(1)).append(".edu");
        }
        Preferences.setString("sourceTerminalEducateKnown", knownString.toString());
        return;
      }

      if (failIndex > successIndex) return;

      String skill = input.substring(7).trim();

      if (Preferences.getString("sourceTerminalChips").contains("DRAM")) {
        Preferences.setString(
            "sourceTerminalEducate1", Preferences.getString("sourceTerminalEducate2"));
        Preferences.setString("sourceTerminalEducate2", skill);
      } else {
        Preferences.setString("sourceTerminalEducate1", skill);
      }
    } else if (input.startsWith("enhance")) {
      int successIndex = text.lastIndexOf("You acquire an effect");
      int badInputIndex = text.lastIndexOf("missing enhance");
      int limitIndex = text.lastIndexOf("enhance limit exceeded");
      int listIndex = text.lastIndexOf("usage: enhance [target file]");

      if (listIndex > limitIndex && listIndex > badInputIndex && listIndex > successIndex) {
        int startIndex = text.lastIndexOf("available targets:");
        int endIndex = text.lastIndexOf("&gt;");
        if (startIndex == -1 || endIndex == -1) {
          // this shouldn't happen...
          return;
        }
        text = text.substring(startIndex, endIndex);
        Pattern ENHANCE_PATTERN = Pattern.compile("rel=\"enhance (.*?).enh\"");
        StringBuilder knownString = new StringBuilder();

        Matcher matcher = ENHANCE_PATTERN.matcher(text);
        while (matcher.find()) {
          if (knownString.length() > 0) {
            knownString.append(",");
          }
          knownString.append(matcher.group(1) + ".enh");
        }
        Preferences.setString("sourceTerminalEnhanceKnown", knownString.toString());
        return;
      }

      if (limitIndex > badInputIndex && limitIndex > successIndex) {
        String chips = Preferences.getString("sourceTerminalChips");
        int limit = 1 + (chips.contains("CRAM") ? 1 : 0) + (chips.contains("SCRAM") ? 1 : 0);
        Preferences.setInteger("_sourceTerminalEnhanceUses", limit);
        return;
      }

      if (badInputIndex > successIndex) return;

      int startIndex = text.lastIndexOf("You acquire");
      int endIndex = text.lastIndexOf("&gt;");
      if (startIndex == -1 || endIndex == -1) {
        // this shouldn't happen...
        return;
      }
      text = text.substring(startIndex, endIndex);
      Pattern ENHANCE_PATTERN =
          Pattern.compile("acquire an effect: (.*?) \\(duration: (\\d+) Adventures\\)</div>");

      Matcher matcher = ENHANCE_PATTERN.matcher(text);
      if (matcher.find()) {
        String message =
            "You acquire an effect: " + matcher.group(1) + " (" + matcher.group(2) + ")";
        RequestLogger.printLine(message);
        RequestLogger.updateSessionLog(message);
        // Refresh status manually since KoL doesn't trigger it
        ApiRequest.updateStatus(true);
      }

      Preferences.increment("_sourceTerminalEnhanceUses");
    } else if (input.startsWith("enquiry")) {
      int successIndex = text.lastIndexOf("enquiry mode set:");
      int failIndex = text.lastIndexOf("missing enquiry target");
      int listIndex = text.lastIndexOf("usage: enquiry [target file]");

      if (listIndex > failIndex && listIndex > successIndex) {
        int startIndex = text.lastIndexOf("available targets:");
        int endIndex = text.lastIndexOf("&gt;");
        if (startIndex == -1 || endIndex == -1) {
          // this shouldn't happen...
          return;
        }
        text = text.substring(startIndex, endIndex);
        Pattern ENQUIRY_PATTERN = Pattern.compile("rel=\"enquiry (.*?).enq\"");
        StringBuilder knownString = new StringBuilder();

        Matcher matcher = ENQUIRY_PATTERN.matcher(text);
        while (matcher.find()) {
          if (knownString.length() > 0) {
            knownString.append(",");
          }
          knownString.append(matcher.group(1) + ".enq");
        }
        Preferences.setString("sourceTerminalEnquiryKnown", knownString.toString());
        return;
      }

      if (failIndex > successIndex) return;

      int beginIndex = successIndex + 18;
      int endIndex = text.indexOf("</div>", beginIndex);
      Preferences.setString("sourceTerminalEnquiry", text.substring(beginIndex, endIndex));
    } else if (input.startsWith("extrude")) {
      int acquire = text.lastIndexOf("You acquire");
      int invalid = text.lastIndexOf("Invalid");
      int insufficient = text.lastIndexOf("Insufficient");
      int confirm = text.lastIndexOf("to confirm");
      int exceeded = text.lastIndexOf("limits exceeded");
      int listIndex = text.lastIndexOf("usage: extrude [target file]");

      if (listIndex > acquire
          && listIndex > invalid
          && listIndex > insufficient
          && listIndex > confirm
          && listIndex > exceeded) {
        int startIndex = text.lastIndexOf("available targets:");
        int endIndex = text.lastIndexOf("&gt;");
        if (startIndex == -1 || endIndex == -1) {
          // this shouldn't happen...
          return;
        }
        text = text.substring(startIndex, endIndex);
        Pattern EXTRUDE_PATTERN = Pattern.compile("rel=\"extrude (.*?).ext\"");
        StringBuilder knownString = new StringBuilder();

        Matcher matcher = EXTRUDE_PATTERN.matcher(text);
        while (matcher.find()) {
          if (knownString.length() > 0) {
            knownString.append(",");
          }
          knownString.append(matcher.group(1) + ".ext");
        }
        Preferences.setString("sourceTerminalExtrudeKnown", knownString.toString());
        return;
      }

      if (exceeded > acquire
          && exceeded > invalid
          && exceeded > insufficient
          && exceeded > confirm) {
        Preferences.setInteger("_sourceTerminalExtrudes", 3);
        return;
      }

      if (invalid > acquire || insufficient > acquire || confirm > acquire) return;

      // Creation must have succeeded
      String message = "";
      if (input.contains("food")) {
        ResultProcessor.processResult(ItemPool.get(ItemPool.BROWSER_COOKIE));
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_ESSENCE, -10));
        message = "You acquire an item: browser cookie";
      } else if (input.contains("booze")) {
        ResultProcessor.processResult(ItemPool.get(ItemPool.HACKED_GIBSON));
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_ESSENCE, -10));
        message = "You acquire an item: hacked gibson";
      } else if (input.contains("goggles")) {
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_SHADES));
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_ESSENCE, -100));
        message = "You acquire an item: Source shades";
      } else if (input.contains("gram")) {
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_TERMINAL_GRAM_CHIP));
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_ESSENCE, -100));
        message = "You acquire an item: Source terminal GRAM chip";
      } else if (input.contains("pram")) {
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_TERMINAL_PRAM_CHIP));
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_ESSENCE, -100));
        message = "You acquire an item: Source terminal PRAM chip";
      } else if (input.contains("spam")) {
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_TERMINAL_SPAM_CHIP));
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_ESSENCE, -100));
        message = "You acquire an item: Source terminal SPAM chip";
      } else if (input.contains("cram")) {
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_TERMINAL_CRAM_CHIP));
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_ESSENCE, -1000));
        message = "You acquire an item: Source terminal CRAM chip";
      } else if (input.contains("dram")) {
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_TERMINAL_DRAM_CHIP));
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_ESSENCE, -1000));
        message = "You acquire an item: Source terminal DRAM chip";
      } else if (input.contains("tram")) {
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_TERMINAL_TRAM_CHIP));
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_ESSENCE, -1000));
        message = "You acquire an item: Source terminal TRAM chip";
      } else if (input.contains("familiar")) {
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOFTWARE_BUG));
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_ESSENCE, -10000));
        message = "You acquire an item: software bug";
      }
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
      Preferences.increment("_sourceTerminalExtrudes");
    } else if (input.startsWith("status")) {
      int startIndex = text.lastIndexOf("Installed Hardware");
      int endIndex = text.lastIndexOf("&gt;");
      if (startIndex == -1 || endIndex == -1) {
        // this shouldn't happen...
        return;
      }
      text = text.substring(startIndex, endIndex);
      Pattern CHIPS_PATTERN =
          Pattern.compile(
              "PRAM chips installed: (\\d+).*?GRAM chips installed: (\\d+).*?SPAM chips installed: (\\d+)");
      Pattern CHIP_PATTERN = Pattern.compile("<div>(.*?) chip installed");
      StringBuilder chipString = new StringBuilder();

      Matcher matcher = CHIPS_PATTERN.matcher(text);
      if (matcher.find()) {
        Preferences.setInteger("sourceTerminalPram", StringUtilities.parseInt(matcher.group(1)));
        Preferences.setInteger("sourceTerminalGram", StringUtilities.parseInt(matcher.group(2)));
        Preferences.setInteger("sourceTerminalSpam", StringUtilities.parseInt(matcher.group(3)));
      }

      text = text.substring(text.indexOf("reduced"));
      matcher = CHIP_PATTERN.matcher(text);
      while (matcher.find()) {
        if (chipString.length() > 0) {
          chipString.append(",");
        }
        chipString.append(matcher.group(1));
      }
      Preferences.setString("sourceTerminalChips", chipString.toString());
    }
  }

  private static String hobopolisBossName(final int choice) {
    return switch (choice) {
      case 200 ->
      // Enter The Hoboverlord
      "Hodgman";
      case 201 ->
      // Home, Home in the Range
      "Ol' Scratch";
      case 202 ->
      // Bumpity Bump Bump
      "Frosty";
      case 203 ->
      // Deep Enough to Dive
      "Oscus";
      case 204 ->
      // Welcome To You!
      "Zombo";
      case 205 ->
      // Van, Damn
      "Chester";
      case 518 ->
      // Clear and Present Danger
      "Uncle Hobo";
      default -> "nobody";
    };
  }

  private static void checkDungeonSewers(final GenericRequest request) {
    // Somewhat Higher and Mostly Dry
    // Disgustin' Junction
    // The Former or the Ladder

    String text = request.responseText;
    int explorations = 0;

    int dumplings = InventoryManager.getAccessibleCount(ItemPool.DUMPLINGS);
    int wads = InventoryManager.getAccessibleCount(ItemPool.SEWER_WAD);
    int oozeo = InventoryManager.getAccessibleCount(ItemPool.OOZE_O);
    int oil = InventoryManager.getAccessibleCount(ItemPool.OIL_OF_OILINESS);
    int umbrella = InventoryManager.getAccessibleCount(ItemPool.GATORSKIN_UMBRELLA);

    // You steel your nerves and descend into the darkened tunnel.
    if (!text.contains("You steel your nerves and descend into the darkened tunnel.")) {
      return;
    }

    // *** CODE TESTS ***

    // You flip through your code binder, and figure out that one
    // of the glyphs is code for 'shortcut', while the others are
    // the glyphs for 'longcut' and 'crewcut', respectively. You
    // head down the 'shortcut' tunnel.

    if (text.contains("'crewcut'")) {
      explorations += 1;
    }

    // You flip through your code binder, and gain a basic
    // understanding of the sign: "This ladder just goes in a big
    // circle. If you climb it you'll end up back where you
    // started." You continue down the tunnel, instead.

    if (text.contains("in a big circle")) {
      explorations += 3;
    }

    // You consult your binder and translate the glyphs -- one of
    // them says "This way to the Great Egress" and the other two
    // are just advertisements for Amalgamated Ladderage, Inc. You
    // head toward the Egress.

    if (text.contains("Amalgamated Ladderage")) {
      explorations += 5;
    }

    // *** ITEM TESTS ***

    // "How about these?" you ask, offering the fish some of your
    // unfortunate dumplings.
    if (text.contains("some of your unfortunate dumplings")) {
      // Remove unfortunate dumplings from inventory
      ResultProcessor.processItem(ItemPool.DUMPLINGS, -1);
      ++explorations;
      dumplings = InventoryManager.getAccessibleCount(ItemPool.DUMPLINGS);
      if (dumplings <= 0) {
        RequestLogger.printLine("That was your last unfortunate dumplings.");
      }
    }

    // Before you can ask him what kind of tribute he wants, you
    // see his eyes light up at the sight of your sewer wad.
    if (text.contains("the sight of your sewer wad")) {
      // Remove sewer wad from inventory
      ResultProcessor.processItem(ItemPool.SEWER_WAD, -1);
      ++explorations;
      wads = InventoryManager.getAccessibleCount(ItemPool.SEWER_WAD);
      if (wads <= 0) {
        RequestLogger.printLine("That was your last sewer wad.");
      }
    }

    // He finds a bottle of Ooze-O, and begins giggling madly. He
    // uncorks the bottle, takes a drink, and passes out in a heap.
    if (text.contains("He finds a bottle of Ooze-O")) {
      // Remove bottle of Ooze-O from inventory
      ResultProcessor.processItem(ItemPool.OOZE_O, -1);
      ++explorations;
      oozeo = InventoryManager.getAccessibleCount(ItemPool.OOZE_O);
      if (oozeo <= 0) {
        RequestLogger.printLine("That was your last bottle of Ooze-O.");
      }
    }

    // You grunt and strain, but you can't manage to get between
    // the bars. In a flash of insight, you douse yourself with oil
    // of oiliness (it takes three whole bottles to cover your
    // entire body) and squeak through like a champagne cork. Only
    // without the bang, and you're not made out of cork, and
    // champagne doesn't usually smell like sewage. Anyway. You
    // continue down the tunnel.
    if (text.contains("it takes three whole bottles")) {
      // Remove 3 bottles of oil of oiliness from inventory
      ResultProcessor.processItem(ItemPool.OIL_OF_OILINESS, -3);
      ++explorations;
      oil = InventoryManager.getAccessibleCount(ItemPool.OIL_OF_OILINESS);
      if (oil < 3) {
        RequestLogger.printLine("You have less than 3 bottles of oil of oiliness left.");
      }
    }

    // Fortunately, your gatorskin umbrella allows you to pass
    // beneath the sewagefall without incident. There's not much
    // left of the umbrella, though, and you discard it before
    // moving deeper into the tunnel.
    if (text.contains("your gatorskin umbrella allows you to pass")) {
      // Unequip gatorskin umbrella and discard it.

      ++explorations;
      AdventureResult item = ItemPool.get(ItemPool.GATORSKIN_UMBRELLA, 1);
      Slot slot = Slot.WEAPON;
      if (KoLCharacter.hasEquipped(item, Slot.WEAPON)) {
        slot = Slot.WEAPON;
      } else if (KoLCharacter.hasEquipped(item, Slot.OFFHAND)) {
        slot = Slot.OFFHAND;
      }

      EquipmentManager.setEquipment(slot, EquipmentRequest.UNEQUIP);

      AdventureResult.addResultToList(KoLConstants.inventory, item);
      ResultProcessor.processItem(ItemPool.GATORSKIN_UMBRELLA, -1);
      umbrella = InventoryManager.getAccessibleCount(item);
      if (umbrella > 0) {
        RequestThread.postRequest(new EquipmentRequest(item, slot));
      } else {
        RequestLogger.printLine("That was your last gatorskin umbrella.");
      }
    }

    // *** GRATE ***

    // Further into the sewer, you encounter a halfway-open grate
    // with a crank on the opposite side. What luck -- looks like
    // somebody else opened this grate from the other side!

    if (text.contains("somebody else opened this grate")) {
      explorations += 5;
    }

    // Now figure out how to say what happened. If the player wants
    // to stop if runs out of test items, generate an ERROR and
    // list the missing items in the status message. Otherwise,
    // simply tell how many explorations were accomplished.

    AdventureResult result =
        AdventureResult.tallyItem("sewer tunnel explorations", explorations, false);
    AdventureResult.addResultToList(KoLConstants.tally, result);

    MafiaState state = MafiaState.CONTINUE;
    String message = "+" + explorations + " Explorations";

    if (Preferences.getBoolean("requireSewerTestItems")) {
      String missing = "";
      String comma = "";

      if (dumplings < 1) {
        missing = missing + comma + "unfortunate dumplings";
        comma = ", ";
      }
      if (wads < 1) {
        missing = missing + comma + "sewer wad";
        comma = ", ";
      }
      if (oozeo < 1) {
        missing = missing + comma + "bottle of Ooze-O";
        comma = ", ";
      }
      if (oil < 1) {
        missing = missing + comma + "oil of oiliness";
        comma = ", ";
      }
      if (umbrella < 1) {
        missing = missing + comma + "gatorskin umbrella";
        comma = ", ";
      }
      if (!missing.equals("")) {
        state = MafiaState.ERROR;
        message += ", NEED: " + missing;
      }
    }

    KoLmafia.updateDisplay(state, message);
  }

  public static String choiceDescription(final int choice, final int decision) {
    // If we have spoilers for this choice, use that
    Spoilers spoilers = ChoiceAdventures.choiceSpoilers(choice, null);
    if (spoilers != null) {
      ChoiceOption spoiler =
          ChoiceAdventures.choiceSpoiler(choice, decision, spoilers.getOptions());
      if (spoiler != null) {
        return spoiler.toString();
      }
    }

    // If we didn't find a spoiler, use KoL's label for the option
    Map<Integer, String> choices = ChoiceUtilities.parseChoices(ChoiceManager.lastResponseText);
    String desc = choices.get(decision);

    // If we still can't find it, throw up our hands
    return (desc == null) ? "unknown" : desc;
  }

  public static int getAdventuresUsed(final String urlString) {
    int choice = ChoiceUtilities.extractChoiceFromURL(urlString);
    int decision = ChoiceUtilities.extractOptionFromURL(urlString);
    switch (choice) {
      case 929 -> {
        // Control Freak
        if (decision == 5) {
          return PyramidRequest.lowerChamberTurnsUsed();
        }
      }
      case 1085, 1086 -> {
        // Pick a Card
        return DeckOfEveryCardRequest.getAdventuresUsed(urlString);
      }
      case 1099 -> {
        // The Barrel Full of Barrels
        //
        // choice 1: A barrel
        // choice 2: Turn Crank (1)
        // choice 3: Exit
        //
        // choice.php?whichchoice=1099&pwd&option=1&slot=00
        //     slots: <ROW><COLUMN> from 00 - 22
        //
        // Turning the crank costs a turn.
        // Smashing a barrel does not - unless it contains a mimic
        //
        // Assume that only option 3 - Exit - is guaranteed to not cost a turn.
        return decision == 3 ? 0 : 1;
      }
      case 1388 -> {
        // Comb the Beach
        return BeachCombRequest.getAdventuresUsed(urlString);
      }
      case 1463 -> {
        // Combat Lover's Locket
        return LocketRequest.getAdventuresUsed(urlString);
      }
    }
    return 0;
  }

  public static final boolean registerRequest(final String urlString) {
    int choice = ChoiceUtilities.extractChoiceFromURL(urlString);
    int decision = ChoiceUtilities.extractOptionFromURL(urlString);
    if (choice != 0) {
      switch (choice) {
        case 443 -> {
          // Chess Puzzle
          return RabbitHoleManager.registerChessboardRequest(urlString);
        }

        case 460,
            461,
            462,
            463,
            464,
            465,
            467,
            468,
            469,
            470,
            472,
            473,
            474,
            475,
            476,
            477,
            478,
            479,
            480,
            481,
            482,
            483,
            484,
            // Space Trip
            471,
            // DemonStar
            485,
            // Fighters Of Fighting
            486,
            // Dungeon Fist!
            488,
            489,
            490,
            491 -> {
          // Meteoid
          return true;
        }

        case 535, // Deep Inside Ronald
            536 -> { // Deep Inside Grimace
          return true;
        }

        case 546 -> { // Interview With You
          return true;
        }

        case 1003, // Test Your Might And Also Test Other Things
            1015, // The Mirror in the Tower has the View that is True
            1020, // Closing Ceremony
            1021, // Meet Frank
            1022, // Meet Frank
            1005, // 'Allo
            1006, // One Small Step For Adventurer
            1007, // Twisty Little Passages, All Hedge
            1008, // Pooling Your Resources
            1009, // Good Ol' 44% Duck
            1010, // Another Day, Another Fork
            1011, // Of Mouseholes and Manholes
            1012, // The Last Temptation
            1013 -> { // Mazel Tov!
          return SorceressLairManager.registerChoice(choice, urlString);
        }

        case 1053 -> { // The Servants' Quarters
          return EdServantData.registerRequest(urlString);
        }

        case 1063 -> { // Adjust your 'Edpiece
          var state = EdPieceCommand.getStateFromDecision(decision);

          if (state != null) {
            RequestLogger.updateSessionLog();
            RequestLogger.updateSessionLog("edpiece " + state);
          }

          return true;
        }

        case 1101 -> { // It's a Barrel Smashing Party!
          if (decision == 2) {
            // We're smashing 100 barrels
            // The results don't say which barrels are being smashed, but it seems to happen in
            // item order
            int count = 100;
            int itemId = ItemPool.LITTLE_FIRKIN;
            RequestLogger.updateSessionLog("smashing 100 barrels");
            while (count > 0 && itemId <= ItemPool.BARNACLED_BARREL) {
              int smashNumber = Math.min(count, InventoryManager.getCount(itemId));
              String name = ItemDatabase.getItemName(itemId);
              if (smashNumber > 0 && name != null) {
                RequestLogger.updateSessionLog("smash " + smashNumber + " " + name);
                count -= smashNumber;
              }
              itemId++;
            }
            return true;
          }
          int itemId = extractIidFromURL(urlString);
          String name = ItemDatabase.getItemName(itemId);
          if (name != null) {
            RequestLogger.updateSessionLog("smash " + name);
            return true;
          }
        }

        case 1181 -> { // Your Witchess Set
          String desc = ChoiceManager.choiceDescription(choice, decision);
          RequestLogger.updateSessionLog("Took choice " + choice + "/" + decision + ": " + desc);
          return true;
        }

        case 1182 -> { // Play against the Witchess Pieces
          // These will redirect to a fight. The encounter will suffice.
          if (decision >= 1 && decision <= 7) {
            String desc = "Play against the Witchess pieces";
            RequestLogger.updateSessionLog("Took choice " + choice + "/" + decision + ": " + desc);
          }
          return true;
        }

        case 1313, // Bastille Battalion
            1314, // Bastille Battalion (Master of None)
            1315, // Castle vs. Castle
            1316, // GAME OVER
            1317, // A Hello to Arms (Battalion)
            1318, // Defensive Posturing
            1319 -> { // Cheese Seeking Behavior
          return BastilleBattalionManager.registerRequest(urlString);
        }

        case 1334, // Boxing Daycare (Lobby)
            1335, // Boxing Daycare Spa
            1336 -> { // Boxing Daycare
          // Special logging done elsewhere
          return true;
        }

        case 1339 -> { // A Little Pump and Grind
          if (decision == 1) {
            int itemId = extractIidFromURL(urlString);
            int qty = extractQtyFromURL(urlString);
            String name = ItemDatabase.getItemName(itemId);
            if (name != null) {
              RequestLogger.updateSessionLog("grinding " + qty + " " + name);
              return true;
            }
          }
          return true;
        }

        case 1352, //  Island #1, Who Are You?
            1353, //  What's Behind Island #2?
            1354 -> { //  Third Island's the Charm
          // Get the island name here so that we can set the location name accordingly.
          // Other choicey things should happen in postChoiceX
          String desc = ChoiceManager.choiceDescription(choice, decision);
          RequestLogger.updateSessionLog("Took choice " + choice + "/" + decision + ": " + desc);
          if (desc != null && !desc.equals("Decide Later")) {
            Preferences.setString("_lastPirateRealmIsland", desc);
          }
          return true;
        }

        case 1388 -> { // Comb the Beach
          return BeachCombRequest.registerRequest(urlString);
        }

        case 1445, // Reassembly Station
            1447 -> { // Statbot 5000
          return YouRobotManager.registerRequest(urlString);
        }

        case 1483 -> { // Direct Autumn-Aton
          return AutumnatonManager.registerRequest(urlString);
        }

        case 1523 -> { // Research Bench
          return ResearchBenchRequest.registerRequest(urlString);
        }
      }

      if (decision != 0) {
        // Figure out which decision we took
        String desc = ChoiceManager.choiceDescription(choice, decision);
        RequestLogger.updateSessionLog("Took choice " + choice + "/" + decision + ": " + desc);
      }
    } else if (decision == 0) {
      // forceoption=0 will redirect to the real choice.
      // Don't bother logging it.
      return true;
    }

    // By default, we log the url of any choice we take
    RequestLogger.updateSessionLog(urlString);

    switch (choice) {
      case 1195 -> {
        if (decision == 3) {
          KoLAdventure.clearLocation();
          GenericRequest.itemMonster = "Time-Spinner";

          RequestLogger.registerLocation("Way Back in Time");
        }
        return true;
      }
      case 1196 -> {
        if (ChoiceManager.lastDecision == 1 && !urlString.contains("monid=0")) {
          KoLAdventure.clearLocation();
          GenericRequest.itemMonster = "Time-Spinner";

          RequestLogger.registerLocation("A Recent Fight");
        }
        return true;
      }
    }

    // Special cases
    if (choice == 879 && decision == 3) {
      // One Rustic Nightstand
      //
      // Option 3 redirects to a fight with the remains of a
      // jilted mistress. Unlike other such redirections,
      // this takes a turn.
      RequestLogger.registerLocation("The Haunted Bedroom");
    }

    return true;
  }

  public static final void registerDeferredChoice(final int choice, final String encounter) {
    switch (choice) {
      case 123 -> // At Least It's Not Full Of Trash
      RequestLogger.registerLocation("The Hidden Temple");

      case 125 -> { // No Visible Means of Support
        // The tiles took a turn to get here
        ResultProcessor.processAdventuresUsed(1);
        RequestLogger.registerLocation("The Hidden Temple");
      }

      case 437 -> // Flying In Circles
      RequestLogger.registerLocation("The Nemesis' Lair");

      case 620, // A Blow Is Struck!
          621, // Hold the Line!
          622, // The Moment of Truth
          634 -> // Goodbye Fnord
      // These all arise out of a multifight, rather than by
      // visiting a location.
      RequestLogger.registerLastLocation();

      case 1005, // 'Allo
          1006, // One Small Step For Adventurer
          1007, // Twisty Little Passages, All Hedge
          1008, // Pooling Your Resources
          1009, // Good Ol' 44% Duck
          1010, // Another Day, Another Fork
          1011, // Of Mouseholes and Manholes
          1012, // The Last Temptation
          1013 -> { // Mazel Tov!
        // This is chain of choices that either immediately
        // follow a fight or the previous choice, either of
        // which takes a turn (unlike normal choice chains)
        String location = "The Hedge Maze (Room " + (choice - 1004) + ")";
        RequestLogger.registerLocation(location);
      }

      case 1018, // Bee Persistent
          1019 -> { // Bee Rewarded
        // Getting here took a turn
        ResultProcessor.processAdventuresUsed(1);
        RequestLogger.registerLocation("The Black Forest");
      }

      case 1223, // L.O.V. Entrance
          1224, // L.O.V. Equipment Room
          1225, // L.O.V. Engine Room
          1226, // L.O.V. Emergency Room
          1227, // L.O.V. Elbow Room
          1228 -> // L.O.V. Emporium
      // This is chain of choices that either immediately
      // follow a fight or the previous choice.
      RequestLogger.registerLastLocation();

      case 1310 -> { // Granted a Boon
        {
          // Boon after fight, location is currently null, so don't log under that name
          RequestLogger.registerLocation("God Lobster");
        }
      }

      case 1334, // Boxing Daycare (Lobby)
          1335, // Boxing Daycare Spa
          1336 -> // Boxing Daycare
      RequestLogger.registerLocation("Boxing Daycare");
    }
  }

  private static final Pattern FLUENCY_PATTERN = Pattern.compile("Fluency is now (\\d+)%");

  public static void parseLanguageFluency(final String text, final String setting) {
    Matcher m = FLUENCY_PATTERN.matcher(text);
    if (m.find()) {
      Preferences.setInteger(setting, StringUtilities.parseInt(m.group(1)));
    }
  }

  // as you improve your vocabulary, these options get reordered. Hence, store which is which.
  // <input type=hidden name=option value=1><input  class=button type=submit value="AF531.55:
  // Underwater Knitting&nbsp;on Five Sand Dollars a Day">
  private static final Pattern CATALOG_OPTION_PATTERN =
      Pattern.compile(
          "<input type=hidden name=option value=(\\d+)><input +class=button type=submit value=\"([A-Z]{2}\\d{3}\\.\\d{2}):");

  private static void visitCatalog(String text) {
    var choices = merkinCatalogChoicesPrefToList();
    var newChoices = new ArrayList<MerkinCatalogChoice>();
    Matcher m = CATALOG_OPTION_PATTERN.matcher(text);
    while (m.find()) {
      int choice = Integer.parseInt(m.group(1));
      String desc = m.group(2);
      var existing = choices.stream().filter(x -> x.identifier.equals(desc)).findFirst();
      if (existing.isPresent()) {
        var actual = existing.get();
        newChoices.add(new MerkinCatalogChoice(desc, choice, actual.spoiler));
      } else {
        newChoices.add(new MerkinCatalogChoice(desc, choice, "unknown"));
      }
    }
    if (!newChoices.isEmpty()) {
      saveMerkinCatalogChoicePref(newChoices);
    }
  }

  private static void postChoiceCatalog(String text, int decision) {
    var isStat = text.contains("You learn something from it");
    var lst = merkinCatalogChoicesPrefToList();
    var relevantOption = lst.stream().filter(x -> x.choice == decision).findFirst();
    relevantOption.ifPresent(actual -> actual.spoiler = isStat ? "stats" : "clue");
    saveMerkinCatalogChoicePref(lst);
  }

  // YH702.74:1:stats
  // spoiler is "unknown", "stats" or "clue"
  public static class MerkinCatalogChoice {
    public String identifier;
    public int choice;
    public String spoiler;

    public MerkinCatalogChoice(String identifier, int choice, String spoiler) {
      this.identifier = identifier;
      this.choice = choice;
      this.spoiler = spoiler;
    }
  }

  public static ArrayList<MerkinCatalogChoice> merkinCatalogChoicesPrefToList() {
    ArrayList<MerkinCatalogChoice> lst = new ArrayList<>();
    var pref = Preferences.getString("merkinCatalogChoices");
    var cards = pref.split(",");
    for (var card : cards) {
      var segments = card.split(":");
      if (segments.length < 3) {
        continue;
      }
      var num = Integer.parseInt(segments[1]);
      var choice = new MerkinCatalogChoice(segments[0], num, segments[2]);
      lst.add(choice);
    }
    return lst;
  }

  private static void saveMerkinCatalogChoicePref(ArrayList<MerkinCatalogChoice> lst) {
    String value =
        lst.stream()
            .map(rec -> rec.identifier + ":" + rec.choice + ":" + rec.spoiler)
            .sorted()
            .collect(Collectors.joining(","));
    Preferences.setString("merkinCatalogChoices", value);
  }

  public static boolean canWalkFromChoice(int choice) {
    switch (choice) {
      case 570: // GameInformPowerDailyPro Walkthru
      case 603: // Skeletons and The Closet
      case 627: // ChibiBuddy&trade; (on)
      case 632: // Add an E-Mail Address
      case 633: // ChibiBuddy&trade; (off)
      case 664: // The Crackpot Mystic's Shed
      case 720: // The Florist Friar's Cottage
      case 767: // Tales of Dread
      case 769: // The Super-Secret Canadian Mind-Control Device
      case 770: // The Institute for Canadian Studies
      case 774: // Opening up the Folder Holder
      case 792: // The Degrassi Knoll Gym
      case 793: // Welcome to The Shore, Inc.
      case 801: // A Reanimated Conversation
      case 804: // Trick or Treat!
      case 810: // K.R.A.M.P.U.S. facility
      case 812: // The Unpermery
      case 821: // LP-ROM burner
      case 835: // Barely Tales
      case 836: // Adventures Who Live in Ice Houses...
      case 844: // The Portal to Horrible Parents
      case 845: // Rumpelstiltskin's Workshop
      case 859: // Upping your grade
      case 867: // Sneaky Peterskills
      case 870: // Hair Today
      case 871: // inspecting Motorbike
      case 872: // Drawn Onward
      case 922: // Summoning Chamber
      case 929: // Control Freak
      case 984: // A Radio on a Beach
      case 985: // The Odd Jobs Board
      case 986: // Control Panel
      case 987: // The Post-Apocalyptic Survivor Encampment
      case 991: // Build a Crimbot!
      case 994: // Hide a Gift!
      case 999: // Shrubberatin'
      case 1003: // Contest Booth
      case 1025: // Reconfigure your Mini-Crimbot
      case 1051: // The Book of the Undying
      case 1053: // The Servants' Quarters
      case 1059: // Helping Make Ends Meat
      case 1063: // Adjust your 'Edpiece
      case 1064: // The Doctor is Out.  Of Herbs.
      case 1065: // Lending a Hand (and a Foot)
      case 1066: // Employee Assignment Kiosk
      case 1067: // Maint Misbehavin'
      case 1076: // Mayo Minder&trade;
      case 1089: // Community Service
      case 1090: // The Towering Inferno Discotheque
      case 1093: // The WLF Bunker
      case 1099: // The Barrel Full of Barrels
      case 1100: // Pray to the Barrel God
      case 1101: // It's a Barrel Smashing Party!
      case 1103: // Doing the Maths
      case 1104: // Tree Tea
      case 1105: // Specifici Tea
      case 1110: // Spoopy
      case 1114: // Walford Rusley, Bucket Collector
      case 1118: // X-32-F Combat Training Snowman Control Console
      case 1171: // LT&T Office
      case 1177: // Book of the West: Cow Punching
      case 1178: // Book of the West: Beanslinging
      case 1179: // Book of the West: Snake Oiling
      case 1181: // Witchess Set
      case 1188: // The Call is Coming from Outside the Simulation
      case 1190: // The Oracle
      case 1191: // Source Terminal
      case 1193: // The Precinct
      case 1195: // Spinning Your Time-Spinner
      case 1197: // Travel back to a Delicious Meal
      case 1217: // Sweet Synthesis
      case 1219: // Approach the Jellyfish
      case 1233: // Equipment Requisition
      case 1234: // Spacegate Vaccination Machine
      case 1235: // Spacegate Terminal
      case 1259: // LI-11 HQ
      case 1264: // Meteor Metal Machinations
      case 1266: // The Hostler
      case 1267: // Rubbed it the Right Way
      case 1270: // Pantagramming
      case 1271: // Mummery
      case 1272: // R&D
      case 1273: // The Cursed Warehouse
      case 1275: // Rummaging through the Garbage
      case 1277: // Extra, Extra
      case 1278: // Madame Zatara’s Relationship Fortune Teller
      case 1320: // A Heist!
      case 1322: // The Beginning of the Neverend
      case 1329: // Latte Shop
      case 1334: // Boxing Daycare (Lobby)
      case 1335: // Boxing Day Spa
      case 1336: // Boxing Daycare
      case 1339: // A Little Pump and Grind
      case 1389: // The Council of Exploathing
      case 1395: // Take your Pills
      case 1396: // Adjusting Your Fish
      case 1407: // Mushroom District Costume Shop
      case 1408: // Mushroom District Badge Shop
      case 1420: // What has it got in its pocketses?
      case 1437: // Setup Your knock-off retro superhero cape
      case 1438: // Setup your knock-off retro superhero cape
      case 1439: // Spread Crimbo Spirit
      case 1445: // Reassembly Station
      case 1447: // Statbot 5000
      case 1448: // Potted Power Plant
      case 1449: // Set Backup Camera Mode
      case 1451: // Fire Captain Hagnk
      case 1452: // Sprinkler Joe
      case 1453: // Fracker Dan
      case 1454: // Cropduster Dusty
      case 1455: // Cold Medicine Cabinet
      case 1457: // Food Lab
      case 1458: // Booze Lab
      case 1459: // Chem Lab
      case 1460: // Toy Lab
      case 1463: // Reminiscing About Those Monsters You Fought
      case 1466: // Configure Your Unbreakable Umbrella
      case 1476: // Stillsuit
      case 1481: // Adjust Your Parka
      case 1483: // Direct Autumn-Aton
      case 1484: // Conspicuous Plaque
      case 1485: // Play with your train
      case 1490: // Woolin' Around
      case 1493: // Treasure House
      case 1494: // Examine S.I.T. Course Certificate
      case 1495: // Get Some Training
      case 1501: // Make a Wish
      case 1509: // Adjust Jill-of-All-Trades Lighting
      case 1510: // Burning Leaves
      case 1516: // Differentiate this Egg
      case 1517: // Mimic DNA Bank
      case 1518: // Prepare your Meal
      case 1523: // Research Bench
      case 1526: // Conduct the Band
      case 1527: // Consider the Calendar
      case 1533: // Clan Photo Booth
      case 1534: // Clan Photo Booth - Get your photo taken
      case 1535: // Clan Photo Booth - Borrow a prop
      case 1536: // Clan Photo Booth - Take a group photo
      case 1537: // TakerSpace
      case 1544: // Devil some Candy
      case 1551: // Hashing with your vice
      case 1553: // Hybridization Chamber
      case 1555: // Specimen Preparation Bench
      case 1556: // Leprechaun's Condo
      case 1558: // Foreseeing Peril
      case 1561: // Request Supply Drop
      case 1563: // Request Supply Drop
        return true;

      default:
        return false;
    }
  }
}
