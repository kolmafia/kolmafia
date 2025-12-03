package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AdventureResult.AdventureLongCountResult;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.ConsumptionType;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit.Checkpoint;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.Lookup;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.DebugDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDrop;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.persistence.StandardRewardDatabase;
import net.sourceforge.kolmafia.persistence.TCRSDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ChateauRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.PlaceRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.coinmaster.HermitRequest;
import net.sourceforge.kolmafia.request.concoction.CreateItemRequest;
import net.sourceforge.kolmafia.session.TrackManager.Tracker;
import net.sourceforge.kolmafia.utilities.LockableListFactory;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ResultProcessor {
  private static final Pattern DISCARD_PATTERN = Pattern.compile("You discard your (.*?)\\.");

  private static boolean autoCrafting = false;

  public static Pattern ITEM_TABLE_PATTERN =
      Pattern.compile(
          "<table class=\"item\".*?rel=\"(.*?)\".*?title=\"(.*?)\".*?descitem\\(([\\d]*)\\).*?</table>");
  public static Pattern BOLD_NAME_PATTERN =
      Pattern.compile(
          "<b>([^<]*)</b>(?: \\((stored in Hagnk's Ancestral Mini-Storage|automatically equipped)\\))?");

  private ResultProcessor() {}

  public static String processItems(
      boolean adventureResults, final String results, final List<AdventureResult> items) {
    // Results now come in like this:
    //
    // <table class="item" style="float: none" rel="id=617&s=137&q=0&d=1&g=0&t=1&n=1&m=1&u=u">
    // <tr><td><img src="http://images.kingdomofloathing.com/itemimages/rcandy.gif"
    // alt="Angry Farmer candy" title="Angry Farmer candy" class=hand
    // onClick='descitem(893169457)'></td>
    // <td valign=center class=effect>You acquire an item: <b>Angry Farmer
    // candy</b></td></tr></table>
    //
    // Or, in haiku:
    //
    // <table class="item" style="float: none" rel="id=83&s=5&q=0&d=1&g=0&t=1&n=1&m=0&u=.">
    // <tr><td><img src="http://images.kingdomofloathing.com/itemimages/rustyshaft.gif"
    // alt="rusty metal shaft" title="rusty metal shaft" class=hand
    // onClick='descitem(228339790)'></td>
    // <td valign=center class=effect><b>rusty metal shaft</b><br>was once your foe's, is now
    // yours.<br>
    // Beaters-up, keepers.</td></tr></table>
    //
    // Pre-process all such matches and save them to a list of items.
    // Register new items.
    // Check multi-usability and plurals
    //
    // In Spelunky:
    //
    // <table><tr><td><img style='vertical-align: middle' class=hand
    // src='http://images.kingdomofloathing.com/itemimages/shotgun.gif'
    // onclick='descitem(606913715)'></td><td valign=center>You acquire an item: <b>shotgun</b>
    // (automatically equipped)</td></tr></table>
    //
    // In Daycare:
    //
    // <table><tr><td valign=center>You lose an item: </td><td><img
    // src=https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/egg.gif
    // width=30 height=30></td><td valign=center><b>hardboiled egg</b>
    // (44)</td></tr></table></span></td></tr></table>

    StringBuffer buffer = new StringBuffer();
    boolean changed = false;

    // Item names have a lot of extra adjectives in Two Crazy Random Summer
    boolean crazyRandomAdjectives = KoLCharacter.isCrazyRandomTwo();

    Matcher itemMatcher = ResultProcessor.ITEM_TABLE_PATTERN.matcher(results);
    while (itemMatcher.find()) {
      String relString = itemMatcher.group(1);
      String itemName = itemMatcher.group(2).trim();
      String descId = itemMatcher.group(3);
      Matcher boldMatcher = ResultProcessor.BOLD_NAME_PATTERN.matcher(itemMatcher.group(0));
      String boldName = boldMatcher.find() ? boldMatcher.group(1).trim() : null;
      String comment = boldName != null ? boldMatcher.group(2) : null;

      // Both itemName and boldName can have adjectives. If
      // it's a new item, we can't know the real name.

      // If we don't know this descid, it's an unknown item.
      if (ItemDatabase.getItemIdFromDescription(descId) == -1) {
        ItemDatabase.registerItem(itemName, descId, relString, boldName);
      }

      // Extract item from the relstring
      AdventureResult item = ItemDatabase.itemFromRelString(relString);
      int itemId = item.getItemId();
      int count = item.getCount();
      String name = item.getName();

      if (crazyRandomAdjectives) {
        ResultProcessor.handleCrazyRandomAdjectives(item, boldName);
      }

      // Check if multiusability conflicts with our expectations
      boolean multi = ItemDatabase.relStringMultiusable(relString);
      boolean ourMulti = ItemDatabase.isMultiUsable(itemId);
      if (multi != ourMulti) {
        String message =
            (multi)
                ? name + " is multiusable, but KoLmafia thought it was not"
                : name + " is not multiusable, but KoLmafia thought it was";

        RequestLogger.printLine(message);
        RequestLogger.updateSessionLog(message);
        ItemDatabase.registerMultiUsability(itemId, multi);
      }

      // If we got more than one, check plural name.
      // Can't do this in Two Crazy Random Summer
      if (!crazyRandomAdjectives) {
        String plural = ItemDatabase.extractItemsPlural(count, boldName);
        String ourPlural = plural == null ? null : ItemDatabase.getPluralName(itemId);
        if (plural != null && !plural.equals(ourPlural)) {
          String message = "Unexpected plural of '" + name + "' found: " + plural;
          RequestLogger.printLine(message);
          RequestLogger.updateSessionLog(message);
          ItemDatabase.registerPlural(itemId, plural);
        }
      }

      // Log it if we pickpocket something "impossible"
      if (RequestLogger.getLastURLString().contains("action=steal")) {
        MonsterData monster = MonsterStatusTracker.getLastMonster();
        for (MonsterDrop monsterDrop : monster.getItems()) {
          if (monsterDrop.item().getItemId() == itemId) {
            String message =
                switch (monsterDrop.flag()) {
                  case NO_PICKPOCKET ->
                      "Pickpocketed item " + name + " which is marked as non pickpocketable.";
                  case CONDITIONAL ->
                      "Pickpocketed item " + name + " which is marked as conditional.";
                  case FIXED -> "Pickpocketed item " + name + " which is marked as fixed chance.";
                  case STEAL_ACCORDION ->
                      "Pickpocketed item " + name + " which is marked as accordion steal.";
                  case MULTI_DROP -> "Pickpocketed item " + name + " which is marked as multidrop.";
                  default -> null;
                };
            if (message != null) {
              RequestLogger.printLine(message);
              RequestLogger.updateSessionLog(message);
            }
          }
        }
      }

      // Perform special processing, if indicated
      if (comment != null) {
        itemMatcher.appendReplacement(buffer, "");
        changed = true;
        // If the item went to Hagnk's...
        if (comment.contains("Hagnk")) {
          // move it to Hagnk's and remove from page text
          String message = "Stored in Hagnk's: " + item.toString();
          RequestLogger.printLine(message);
          if (Preferences.getBoolean("logAcquiredItems")) {
            RequestLogger.updateSessionLog(message);
          }
          AdventureResult.addResultToList(KoLConstants.storage, item);
        }
        // If the item was automatically equipped...
        else if (comment.contains("automatically equipped")) {
          // add to inventory, equip it, and remove from page text
          String acquisition = "You acquire and equip an item:";
          ResultProcessor.processItem(adventureResults, acquisition, item, null);
          EquipmentManager.autoequipItem(item);
        }
      }

      // Otherwise, add it to the list of items we found
      else if (items != null) {
        items.add(item);
      }
    }

    if (changed) {
      itemMatcher.appendTail(buffer);
      return buffer.toString();
    }

    return results;
  }

  public static LinkedList<AdventureResult> parseItems(final String results) {
    LinkedList<AdventureResult> items = new LinkedList<>();

    // Item names have a lot of extra adjectives in Two Crazy Random Summer
    boolean crazyRandomAdjectives = KoLCharacter.isCrazyRandomTwo();

    Matcher itemMatcher = ResultProcessor.ITEM_TABLE_PATTERN.matcher(results);
    while (itemMatcher.find()) {
      String relString = itemMatcher.group(1);
      String itemName = itemMatcher.group(2).trim();
      String descId = itemMatcher.group(3);
      Matcher boldMatcher = ResultProcessor.BOLD_NAME_PATTERN.matcher(itemMatcher.group(0));
      String boldName = boldMatcher.find() ? boldMatcher.group(1).trim() : null;
      boolean hagnk = boldName != null && boldMatcher.group(2) != null;

      // If we don't know this descid, it's an unknown item.
      if (ItemDatabase.getItemIdFromDescription(descId) == -1) {
        ItemDatabase.registerItem(itemName, descId, relString, boldName);
      }

      AdventureResult item = ItemDatabase.itemFromRelString(relString);
      if (crazyRandomAdjectives) {
        ResultProcessor.handleCrazyRandomAdjectives(item, boldName);
      }

      if (!hagnk) {
        items.add(item);
      }
    }

    return items;
  }

  private static void handleCrazyRandomAdjectives(final AdventureResult item, final String name) {
    // Add to TCRS data map if not already present
    int itemId = item.getItemId();
    if (TCRSDatabase.derive(itemId)) {
      TCRSDatabase.applyModifiers(itemId);
    }
  }

  // <table><tr><td><img class=hand
  // src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/breath.gif"
  // onClick='eff("7ecbd57bcb86d63be06bb6d4b8e7229f");' width=30 height=30 alt="Hot Breath"
  // title="Hot Breath"></td><td valign=center class=effect>You acquire an effect: <b>Hot
  // Breath</b><br>(duration: 5 Adventures)</td></tr></table>
  // <table><tr><td><img class=hand
  // src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/scharm.gif"
  // onClick='eff("81d92825729f8b3a913133c18e37a74c");' width=30 height=30 alt="Ancient Annoying
  // Serpent Poison" title="Ancient Annoying Serpent Poison"></td><td valign=center class=effect>You
  // acquire an intrinsic: <b>Ancient Annoying Serpent Poison</b><br></td></tr></table>
  // <table><tr><td><img class=hand
  // src="https://s3.amazonaws.com//images.kingdomofloathing.com/itemimages/milk.gif"
  // onClick='eff("225aa10e75476b0ad5fa576c89df3901");' width=30 height=30></td><td valign=center
  // class=effect>You lose some of an effect: <b>Got Milk</b> (5 Adventures)</td></tr></table>
  // <table><tr><td><img class=hand
  // src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/discoleer.gif"
  // onClick='eff("bc3d4aad3454fcd82c066ef3949749ca");' width=30 height=30></td><td valign=center
  // class=effect>You lose an effect: <b>Disco Leer</b></td></tr></table>
  // <table><tr><td><img class=hand
  // src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/scharm.gif"
  // onClick='eff("81d92825729f8b3a913133c18e37a74c");' width=30 height=30 alt="Ancient Annoying
  // Serpent Poison" title="Ancient Annoying Serpent Poison"></td><td valign=center class=effect>You
  // lose an intrinsic: <b>Ancient Annoying Serpent Poison</b><br></td></tr></table>

  // <table><tr><td><img class=hand
  // src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/lump.gif"
  // onClick='eff("87613dd8cce26a5557db77ab059bf039");' width=30 height=30 alt="Discomfited"
  // title="Discomfited"></td><td valign=center class=effect>You acquire an effect:
  // <b>Discomfited</b><br>(duration: 30 Adventures)</td></tr></table>
  // <table><tr><td><img class=hand
  // src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/strboost.gif"
  // onClick='eff("332e06d519892d99db38ab7f918b1edf");' width=30 height=30 alt="The Strength...  of
  // the Future" title="The Strength...  of the Future"></td><td valign=center class=effect>You
  // acquire an effect: <b>The Strength...  of the Future</b><br>(duration: 20
  // Adventures)</td></tr></table>

  public static Pattern BIRD_PATTERN = Pattern.compile("Blessing of the (.*)");

  public static void updateBird(int effectId, String effectName, String property) {
    // You acquire an effect: <b>Blessing of the Southern Japanese Velocity Eagle</b>
    String bird = Preferences.getString(property);
    Matcher birdMatcher = ResultProcessor.BIRD_PATTERN.matcher(effectName);
    String blessingBird = birdMatcher.find() ? birdMatcher.group(1) : bird;
    if (!bird.equals(blessingBird)) {
      Preferences.setString(property, blessingBird);
      DebugDatabase.readEffectDescriptionText(effectId);
    }
  }

  public static void updateEntauntauned() {
    DebugDatabase.readEffectDescriptionText(EffectPool.ENTAUNTAUNED);
  }

  public static void updateSavageBeast() {
    // Don't bother unless we are currently a Savage Beast.
    // We'll do this again if we transform into one
    if (KoLCharacter.isSavageBeast()) {
      DebugDatabase.effectDescriptionText(EffectPool.SAVAGE_BEAST);
    }
  }

  public static Pattern EFFECT_TABLE_PATTERN =
      Pattern.compile(
          "<table><tr><td><img[^>]*eff\\(\"(.*?)\"\\)[^>]*>.*?class=effect>(.*?)<b>(.*?)</b>(?:<br>| )?(?:\\((?:duration: )?(\\d+) Adventures?\\))?</td></tr></table>");

  public static LinkedList<AdventureResult> parseEffects(String results) {
    // Pre-process all effect matches and add them to the passed in list of effects.

    LinkedList<AdventureResult> effects = new LinkedList<>();

    Matcher effectMatcher = ResultProcessor.EFFECT_TABLE_PATTERN.matcher(results);
    while (effectMatcher.find()) {
      String descId = effectMatcher.group(1);
      // KoL bug: some Affirmations, at least, are reported with a leading space
      String effectName = effectMatcher.group(3).trim();
      int effectId = EffectDatabase.getEffectIdFromDescription(descId);

      // If we don't know this effectId, it's an unknown effect
      if (effectId == -1) {
        effectId = EffectDatabase.learnEffectId(effectName, descId);
      }

      // If the effect is "Blessing of the Bird", KoL changes
      // it to "Blessing of the XXX", where XXX is today's bird
      switch (effectId) {
        case EffectPool.BLESSING_OF_THE_BIRD ->
            updateBird(EffectPool.BLESSING_OF_THE_BIRD, effectName, "_birdOfTheDay");
        case EffectPool.BLESSING_OF_YOUR_FAVORITE_BIRD ->
            updateBird(EffectPool.BLESSING_OF_YOUR_FAVORITE_BIRD, effectName, "yourFavoriteBird");
        case EffectPool.ENTAUNTAUNED,
            EffectPool.BUZZED_ON_DISTILLATE,
            EffectPool.WINE_FORTIFIED,
            EffectPool.WINE_HOT,
            EffectPool.WINE_FRISKY,
            EffectPool.WINE_COLD,
            EffectPool.WINE_FRIENDLY,
            EffectPool.WINE_DARK,
            EffectPool.WINE_BEFOULED,
            EffectPool.CITIZEN_OF_A_ZONE,
            EffectPool.GRAFTED,
            EffectPool.MILK_OF_FAMILIAR_CRUELTY,
            EffectPool.MILK_OF_FAMILIAR_KINDNESS ->
            DebugDatabase.readEffectDescriptionText(effectId);
      }

      String acquisition = effectMatcher.group(2);
      int duration = 0;

      if (acquisition.startsWith("You lose an effect")
          || acquisition.startsWith("You lose an intrinsic")) {
        duration = 0;
      } else if (acquisition.startsWith("You acquire an intrinsic")) {
        duration = Integer.MAX_VALUE;
      } else if (acquisition.contains("lose some of an effect")) {
        duration = -StringUtilities.parseInt(effectMatcher.group(4));
      } else {
        duration = StringUtilities.parseInt(effectMatcher.group(4));
      }

      AdventureResult effect = EffectPool.get(effectId, duration);
      effects.add(effect);
    }

    return effects;
  }

  public static boolean processResults(boolean adventureResults, String results) {
    return ResultProcessor.processResults(adventureResults, results, null);
  }

  public static boolean processResults(
      boolean adventureResults, String results, List<AdventureResult> data) {

    if (data == null && RequestLogger.isDebugging()) {
      RequestLogger.updateDebugLog("Processing results...");
    }

    // If items are wrapped in a table with a "rel" string, that
    // precisely identifies what has been acquired.
    //
    // Pre-process all such matches and save them to a list of items.
    // Register new items.
    // Check multi-usability and plurals

    LinkedList<AdventureResult> items = new LinkedList<>();
    results = ResultProcessor.processItems(adventureResults, results, items);

    // Process effects similarly, saving them to a list of effects.
    // Register new effects.

    LinkedList<AdventureResult> effects = ResultProcessor.parseEffects(results);

    boolean requiresRefresh = false;

    try {
      requiresRefresh = processNormalResults(adventureResults, results, data, items, effects);
    } finally {
      if (data == null) {
        KoLmafia.applyEffects();
      }
    }

    return requiresRefresh;
  }

  private static boolean processNormalResults(
      boolean adventureResults,
      String results,
      List<AdventureResult> data,
      LinkedList<AdventureResult> items,
      LinkedList<AdventureResult> effects) {
    // Whacky, whacky KoL can insert <head> sections within the <body>
    String body = KoLConstants.HEAD_PATTERN.matcher(results).replaceAll("");
    String plainTextResult =
        KoLConstants.ANYTAG_BUT_ITALIC_PATTERN.matcher(body).replaceAll(KoLConstants.LINE_BREAK);

    if (data == null) {
      ResultProcessor.processFamiliarWeightGain(plainTextResult);
    }

    LinkedList<String> parsedResults =
        new LinkedList<>(Arrays.asList(plainTextResult.split(KoLConstants.LINE_BREAK)));
    boolean shouldRefresh = false;

    while (parsedResults.size() > 0) {
      shouldRefresh |=
          ResultProcessor.processNextResult(adventureResults, parsedResults, data, items, effects);
    }

    return shouldRefresh;
  }

  public static boolean processFamiliarWeightGain(final String results) {
    if (results.contains("gains a pound")
        ||
        // The following are Haiku results
        results.contains("gained a pound")
        || results.contains("puts on weight")
        || results.contains("gaining weight")
        ||
        // The following are Anapest results
        results.contains("just got heavier")
        || results.contains("put on some weight")) {
      KoLCharacter.incrementFamiliarWeight();

      FamiliarData familiar = KoLCharacter.getFamiliar();
      String fam1 =
          familiar.getName() + ", the " + familiar.getWeight() + " lb. " + familiar.getRace();

      String message = "Your familiar gains a pound: " + fam1;
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
      return true;
    }

    return false;
  }

  private static boolean processNextResult(
      boolean adventureResults,
      LinkedList<String> parsedResults,
      List<AdventureResult> data,
      LinkedList<AdventureResult> items,
      LinkedList<AdventureResult> effects) {
    String lastToken = parsedResults.remove();

    // Skip bogus lead necklace drops from the Baby Bugged Bugbear

    if (lastToken.equals(" Parse error (function not found) in arena.php line 2225")) {
      parsedResults.remove();
      return false;
    }

    // Skip skill acquisition - it's followed by a boldface
    // which makes the parser think it's found an item.

    if (lastToken.contains("You acquire a skill")
        || lastToken.contains("You learn a skill")
        || lastToken.contains("You gain a skill")
        || lastToken.contains("You have learned a skill")
        || lastToken.contains("You acquire a new skill")) {
      return false;
    }

    String acquisition = lastToken.trim();

    if (acquisition.startsWith("You acquire an effect")
        || acquisition.startsWith("You lose an effect")
        || acquisition.startsWith("You lose some of an effect")) {
      return ResultProcessor.processEffect(parsedResults, acquisition, data, effects);
    }

    if (acquisition.startsWith("You acquire an intrinsic")
        || acquisition.startsWith("You lose an intrinsic")) {
      return ResultProcessor.processIntrinsic(parsedResults, acquisition, data, effects);
    }

    if (acquisition.startsWith("You acquire")) {
      if (acquisition.contains("clan trophy")) {
        return false;
      }

      ResultProcessor.processItem(adventureResults, parsedResults, acquisition, data, items);
      return false;
    }

    if (lastToken.startsWith("You gain")
        || (lastToken.startsWith("You lose ") && !lastToken.startsWith("You lose an item"))
        || lastToken.startsWith("You spent ")) {
      // Chatty pirate message
      if (lastToken.startsWith("You lose your temper")) {
        return false;
      }

      // "I Refuse!" choice text
      if (lastToken.startsWith("You lose control of your legs")) {
        return false;
      }

      return ResultProcessor.processGainLoss(lastToken, data);
    }

    if (lastToken.startsWith("You discard")) {
      ResultProcessor.processDiscard(lastToken);
    }

    return false;
  }

  private static void processItem(
      boolean adventureResults,
      LinkedList<String> parsedResults,
      String acquisition,
      List<AdventureResult> data,
      LinkedList<AdventureResult> items) {
    String item = parsedResults.remove();

    if (item.equals("7 Years of Bad Luck")) {
      return;
    }

    if (item.equals("Tales of the West:  Cow Punching")) {
      // Accommodate KoL bug: extra space in name
      item = "Tales of the West: Cow Punching";
    }

    if (item.startsWith("Love Potion #")) {
      // Item name varies
      item = "Love Potion #XYZ";
    }

    if (acquisition.contains("an item")) {
      AdventureResult result = items.size() == 0 ? null : items.getFirst();

      if (result != null) {
        items.removeFirst();
        ResultProcessor.processItem(adventureResults, acquisition, result, data);
        return;
      }

      // We really shouldn't get here; all items should
      // appear in a standardized table with relstrings which
      // we parse into the "items" list

      result = ItemPool.get(item, 1);

      if (result.getItemId() == -1) {
        RequestLogger.printLine("Unrecognized item found: " + item);
      }

      boolean autoEquip =
          parsedResults.size() > 0 && parsedResults.getFirst().contains("automatically equipped");

      if (autoEquip) {
        // This happens in Spelunky
        parsedResults.remove();
        acquisition = "You acquire and equip an item:";
      }

      ResultProcessor.processItem(adventureResults, acquisition, result, data);

      if (autoEquip) {
        EquipmentManager.autoequipItem(result);
      }

      return;
    }

    if (acquisition.contains("a bounty item")) {
      // Bounty items are no longer real items
      return;
    }

    // The name of the item follows the number that appears after
    // the first index.

    int spaceIndex = item.indexOf(" ");

    String countString = "";
    String itemName;

    if (spaceIndex != -1) {
      countString = item.substring(0, spaceIndex);
      itemName = item.substring(spaceIndex).trim();
    } else {
      itemName = item;
    }

    if (!StringUtilities.isNumeric(countString)) {
      countString = "1";
      itemName = item;
    }

    AdventureResult result = items.size() == 0 ? null : items.getFirst();

    if (result != null) {
      items.removeFirst();
      ResultProcessor.processItem(adventureResults, acquisition, result, data);
      return;
    }

    int itemCount = StringUtilities.parseInt(countString);

    // If we got more than one, do substring matching. This might
    // allow recognition of an unknown (or changed) plural form.

    int itemId = ItemDatabase.getItemId(itemName, itemCount, itemCount > 1);

    if (itemId < 0) {
      RequestLogger.printLine("Unrecognized item found: " + item);
      result = AdventureResult.tallyItem(itemName, itemCount, false);
    } else {
      result = ItemPool.get(itemId, itemCount);
      if (items.size() > 0) {
        AdventureResult first = items.getFirst();
        if (first.getName().equals(result.getName())) {
          result = items.removeFirst();
        }
      }
    }

    ResultProcessor.processItem(adventureResults, acquisition, result, data);
  }

  public static void processItem(
      boolean adventureResults,
      String acquisition,
      AdventureResult result,
      List<AdventureResult> data) {
    if (data != null) {
      AdventureResult.addResultToList(data, result);
      return;
    }

    String message = acquisition + " " + result.toString();

    RequestLogger.printLine(message);
    if (Preferences.getBoolean("logAcquiredItems")) {
      RequestLogger.updateSessionLog(message);
    }

    ResultProcessor.processResult(adventureResults, result);
  }

  public static Pattern DURATION_PATTERN =
      Pattern.compile("\\((?:duration: )?(\\d+) Adventures?\\)");

  private static boolean decodedNamesEqual(String name1, String name2) {
    // Sacr&eacute; Mental
    // Sacré Mental
    // The Strength... of the Future
    // The Strength...  of the Future
    return name1.equals(name2)
        || StringUtilities.getEntityDecode(name1).equals(StringUtilities.getEntityDecode(name2));
  }

  private static boolean processEffect(
      LinkedList<String> parsedResults,
      String acquisition,
      List<AdventureResult> data,
      LinkedList<AdventureResult> effects) {
    if (data != null) {
      return false;
    }

    AdventureResult effect = effects.size() == 0 ? null : effects.getFirst();
    if (effect != null) {
      parsedResults.removeFirst();
      effects.removeFirst();
      return ResultProcessor.processEffect(false, acquisition, effect, data);
    }

    // KoL bug: some Affirmations, at least, are reported with a leading space
    String effectName = parsedResults.remove().trim();
    int effectId = EffectDatabase.getEffectId(effectName);
    int duration = 0;

    if (parsedResults.size() > 0 && parsedResults.getFirst().contains("Adventure")) {
      String lastToken = parsedResults.remove();
      Matcher m = DURATION_PATTERN.matcher(lastToken);
      if (m.find()) {
        duration = StringUtilities.parseInt(m.group(1));
      }
      if (acquisition.startsWith("You lose")) {
        duration = -duration;
      }
    }

    effect = EffectPool.get(effectId, duration);
    return ResultProcessor.processEffect(false, acquisition, effect, data);
  }

  public static boolean processEffect(
      boolean adventureResults,
      String acquisition,
      AdventureResult result,
      List<AdventureResult> data) {
    if (data != null) {
      AdventureResult.addResultToList(data, result);
      return false;
    }

    String effectName = result.getName();
    int count = result.getCount();

    String message = acquisition + " " + effectName;
    if (count != 0) {
      message += " (" + count + ")";
    }

    RequestLogger.printLine(message);
    if (Preferences.getBoolean("logStatusEffects")) {
      RequestLogger.updateSessionLog(message);
    }

    return ResultProcessor.processResult(adventureResults, result);
  }

  private static boolean processIntrinsic(
      LinkedList<String> parsedResults,
      String acquisition,
      List<AdventureResult> data,
      LinkedList<AdventureResult> effects) {
    if (data != null) {
      return false;
    }

    String effectName = parsedResults.remove().trim();
    AdventureResult effect = effects.size() == 0 ? null : effects.getFirst();

    if (effect != null && decodedNamesEqual(effectName, effect.getName())) {
      effects.removeFirst();
    }

    String message = acquisition + " " + effectName;
    RequestLogger.printLine(message);
    if (Preferences.getBoolean("logStatusEffects")) {
      RequestLogger.updateSessionLog(message);
    }

    int effectId = EffectDatabase.getEffectId(effectName);
    AdventureResult result = EffectPool.get(effectId, Integer.MAX_VALUE);

    if (message.startsWith("You lose")) {
      AdventureResult.removeResultFromList(KoLConstants.activeEffects, result);
    } else {
      KoLConstants.activeEffects.add(result);
      LockableListFactory.sort(KoLConstants.activeEffects);
    }

    return true;
  }

  public static boolean processIntrinsic(
      boolean adventureResults,
      String acquisition,
      AdventureResult result,
      List<AdventureResult> data) {
    if (data != null) {
      AdventureResult.addResultToList(data, result);
      return false;
    }

    String effectName = result.getName();
    String message = acquisition + " " + effectName;

    RequestLogger.printLine(message);
    if (Preferences.getBoolean("logStatusEffects")) {
      RequestLogger.updateSessionLog(message);
    }

    return ResultProcessor.processResult(adventureResults, result);
  }

  public static boolean processGainLoss(String lastToken, final List<AdventureResult> data) {
    // BastilleBattalionManager already handled this.
    if (lastToken.endsWith("cheese!")) {
      return true;
    }

    int periodIndex = lastToken.indexOf(".");
    if (periodIndex != -1) {
      lastToken = lastToken.substring(0, periodIndex);
    }

    int parenIndex = lastToken.indexOf("(");
    if (parenIndex != -1) {
      lastToken = lastToken.substring(0, parenIndex);
    }

    lastToken = lastToken.trim();

    if (lastToken.contains("Meat")) {
      return ResultProcessor.processMeat(lastToken, data);
    }

    if (data != null) {
      return false;
    }

    if (lastToken.startsWith("You gain a") || lastToken.startsWith("You gain some")) {
      RequestLogger.printLine(lastToken);
      if (Preferences.getBoolean("logStatGains")) {
        RequestLogger.updateSessionLog(lastToken);
      }

      // Update Hatter deed since new hats may now be equippable
      PreferenceListenerRegistry.firePreferenceChanged("(hats)");

      return true;
    }

    return ResultProcessor.processStatGain(lastToken, data);
  }

  private static boolean possibleMeatDrop(int drop, int bonus) {
    double rate = (KoLCharacter.getMeatDropPercentAdjustment() + 100 + bonus) / 100.0;
    return Math.floor(Math.ceil(drop / rate) * rate) == drop;
  }

  public static boolean processMeat(String text, boolean won, boolean nunnery) {
    AdventureResult result = ResultProcessor.parseResult(text);
    if (result == null) {
      return true;
    }

    if (won) {
      if (nunnery) {
        IslandManager.addNunneryMeat(result);
        return false;
      }

      if (SpadingManager.hasSpadingScript()) {
        int drop = result.getCount();
        if (!ResultProcessor.possibleMeatDrop(drop, 0)) {
          StringBuilder buf = new StringBuilder("Alert - possible unknown meat bonus:");
          if (KoLCharacter.currentNumericModifier(DoubleModifier.SPORADIC_MEATDROP) != 0.0f) {
            buf.append(" (sporadic!)");
          }
          if (KoLCharacter.currentNumericModifier(DoubleModifier.MEAT_BONUS) != 0.0f) {
            buf.append(" (ant tool!)");
          }
          for (int i = 1; i <= 100 && buf.length() < 80; ++i) {
            if (ResultProcessor.possibleMeatDrop(drop, i)) {
              buf.append(" +");
              buf.append(i);
            }
            if (ResultProcessor.possibleMeatDrop(drop, -i)) {
              buf.append(" -");
              buf.append(i);
            }
          }
          SpadingManager.processMeatDrop(buf.toString());
        }
      }
    }

    return ResultProcessor.processMeat(text, result, null);
  }

  public static boolean processMeat(String lastToken, List<AdventureResult> data) {
    AdventureResult result = ResultProcessor.parseResult(lastToken);
    if (result == null) {
      return true;
    }
    return ResultProcessor.processMeat(lastToken, result, data);
  }

  private static boolean processMeat(
      String lastToken, AdventureResult result, List<AdventureResult> data) {
    if (data != null) {
      AdventureResult.addResultToList(data, result);
      return false;
    }

    // KoL can tell you that you lose meat - Leprechaun theft,
    // chewing bug vendors, etc. - but you can only lose as much
    // meat as you actually have in inventory.

    long amount = result.getLongCount();
    long available = KoLCharacter.getAvailableMeat();

    if (amount < 0 && -amount > available) {
      amount = -available;
      lastToken = "You lose " + -amount + " Meat";
      result = new AdventureLongCountResult(AdventureResult.MEAT, amount);
    }

    if (amount == 0) {
      return false;
    }

    RequestLogger.printLine(lastToken);
    if (Preferences.getBoolean("logGainMessages")) {
      RequestLogger.updateSessionLog(lastToken);
    }

    return ResultProcessor.processResult(result);
  }

  public static boolean processStatGain(String lastToken, List<AdventureResult> data) {
    if (data != null) {
      return false;
    }

    AdventureResult result = ResultProcessor.parseResult(lastToken);
    if (result == null) {
      return true;
    }

    RequestLogger.printLine(lastToken);
    if (Preferences.getBoolean("logStatGains")) {
      RequestLogger.updateSessionLog(lastToken);
    }

    return ResultProcessor.processResult(result);
  }

  private static void processDiscard(String lastToken) {
    Matcher matcher = ResultProcessor.DISCARD_PATTERN.matcher(lastToken);
    if (matcher.find()) {
      AdventureResult item = ItemPool.get(matcher.group(1), -1);
      AdventureResult.addResultToList(KoLConstants.inventory, item);
      AdventureResult.addResultToList(KoLConstants.tally, item);
      switch (item.getItemId()) {
        case ItemPool.INSTANT_KARMA -> Preferences.increment("bankedKarma", 11);
      }
    }
  }

  public static AdventureResult parseResult(String result) {
    result = result.trim();

    if (RequestLogger.isDebugging()) {
      RequestLogger.updateDebugLog("Parsing result: " + result);
    }

    try {
      AdventureResult retval = AdventureResult.parseResult(result);
      // If AdventureResult could not parse it, log it
      if (retval == null) {
        String message = "Could not parse: " + StringUtilities.globalStringDelete(result, "&nbsp;");
        RequestLogger.printLine(message);
        RequestLogger.updateSessionLog(message);
      }
      return retval;
    } catch (Exception e) {
      // This should not happen. Therefore, print
      // a stack trace for debug purposes.

      StaticEntity.printStackTrace(e);
      return null;
    }
  }

  /**
   * Utility. The method used to process a result. By default, this will also add an adventure
   * result to the tally. Use this whenever the result is already known and no additional parsing is
   * needed.
   *
   * @param result Result to add to the running tally of adventure results
   */
  public static boolean processResult(AdventureResult result) {
    return ResultProcessor.processResult(false, result);
  }

  public static boolean processResult(boolean adventureResults, AdventureResult result) {
    // This should not happen, but punt if the result was null.

    if (result == null) {
      return false;
    }

    if (RequestLogger.isDebugging()) {
      RequestLogger.updateDebugLog("Processing result: " + result);
    }

    String resultName = result.getName();
    boolean shouldRefresh = false;

    // Process the adventure result in this section; if
    // it's a status effect, then add it to the recent
    // effect list. Otherwise, add it to the tally.

    if (result.isItem()) {
      AdventureResult.addResultToList(KoLConstants.tally, result);
    } else if (result.isStatusEffect()) {
      int active = result.getCount(KoLConstants.activeEffects);
      int duration = result.getCount();
      shouldRefresh |= duration > 0 ? active == 0 : active == duration;
      AdventureResult.addResultToList(KoLConstants.recentEffects, result);
    } else if (resultName.equals(AdventureResult.SUBSTATS)) {
      // Update substat delta and fullstat delta, if necessary
      int[] counts = result.getCounts();

      // Update AdventureResult.SESSION_SUBSTATS in place
      // Update AdventureResult.SESSION_FULLSTATS in place
      boolean substatChanged = false;
      boolean fullstatChanged = false;

      int count = counts[0];
      if (count != 0) {
        long stat = KoLCharacter.getTotalMuscle();
        long diff =
            KoLCharacter.calculateBasePoints(stat + count) - KoLCharacter.calculateBasePoints(stat);
        AdventureResult.SESSION_SUBSTATS[0] += count;
        AdventureResult.SESSION_FULLSTATS[0] += (int) diff;
        substatChanged = true;
        fullstatChanged |= (diff != 0);
      }

      count = counts[1];
      if (count != 0) {
        long stat = KoLCharacter.getTotalMysticality();
        long diff =
            KoLCharacter.calculateBasePoints(stat + count) - KoLCharacter.calculateBasePoints(stat);
        AdventureResult.SESSION_SUBSTATS[1] += count;
        AdventureResult.SESSION_FULLSTATS[1] += (int) diff;
        substatChanged = true;
        fullstatChanged |= (diff != 0);
      }

      count = counts[2];
      if (count != 0) {
        long stat = KoLCharacter.getTotalMoxie();
        long diff =
            KoLCharacter.calculateBasePoints(stat + count) - KoLCharacter.calculateBasePoints(stat);
        AdventureResult.SESSION_SUBSTATS[2] += count;
        AdventureResult.SESSION_FULLSTATS[2] += (int) diff;
        substatChanged = true;
        fullstatChanged |= (diff != 0);
      }

      int size = KoLConstants.tally.size();
      if (substatChanged && size > 2) {
        LockableListFactory.fireContentsChanged(KoLConstants.tally, 2, 2);
      }

      if (fullstatChanged) {
        shouldRefresh = true;
        if (size > 3) {
          LockableListFactory.fireContentsChanged(KoLConstants.tally, 3, 3);
        }
      }
    } else if (resultName.equals(AdventureResult.MEAT)) {
      AdventureResult.addResultToList(KoLConstants.tally, result);
      KoLCharacter.incrementSessionMeat(result.getLongCount());
      shouldRefresh = true;
    } else if (resultName.equals(AdventureResult.ADV)) {
      if (result.getCount() < 0) {
        TurnCounter.saveCounters();
        AdventureResult.addResultToList(KoLConstants.tally, result.getNegation());
      }
    } else if (resultName.equals(AdventureResult.CHOICE)) {
      // Don't let ignored choices delay iteration
      KoLmafia.tookChoice = true;
    }

    ResultProcessor.tallyResult(result, true);

    if (result.isItem()) {
      // Do special processing when you get certain items
      if (HermitRequest.isWorthlessItem(result.getItemId())) {
        result = HermitRequest.WORTHLESS_ITEM.getInstance(result.getCount());
      }

      ResultProcessor.gainItem(adventureResults, result);

      return false;
    }

    if (result.isStatusEffect()) {
      switch (result.getEffectId()) {
        case EffectPool.GARISH -> {
          // If you gain or lose Gar-ish, and autoGarish
          // not set, benefit of Lasagna changes
          if (!Preferences.getBoolean("autoGarish")) {
            ConcoctionDatabase.setRefreshNeeded(true);
          }
        }
        case EffectPool.HALF_ASTRAL -> {
          if (result.getCount() > 0) {
            KoLCharacter.setLimitMode(LimitMode.ASTRAL);
          }
        }
        case EffectPool.HARE_BRAINED -> {
          Preferences.setInteger("hareTurnsUsed", 30 - result.getCount());
        }
        case EffectPool.INIGOS, EffectPool.CRAFT_TEA -> {
          // If you gain or lose Inigo's or Craft Tea, what you can
          // craft changes
          ConcoctionDatabase.setRefreshNeeded(true);
        }
        case EffectPool.RECORD_HUNGER,
            EffectPool.DRUNK_AVUNCULAR,
            EffectPool.BARREL_OF_LAUGHS,
            EffectPool.BEER_BARREL_POLKA,
            EffectPool.REFINED_PALATE -> {
          // Turn generation from food and booze changes
          ConcoctionDatabase.setRefreshNeeded(true);
        }
        case EffectPool.CHILLED_TO_THE_BONE -> {
          int duration = result.getCount();
          if (duration <= 0) break;
          Preferences.setInteger("chilledToTheBone", (int) Math.pow(3, duration));
        }
        case EffectPool.A_BEASTLY_ODOR -> {
          int duration = result.getCount();
          if (duration <= 0) break;
          TrackManager.track("beast", Tracker.A_BEASTLY_ODOR);
        }
        case EffectPool.EW_THE_HUMANITY -> {
          int duration = result.getCount();
          if (duration <= 0) break;
          TrackManager.track("dude", Tracker.EW_THE_HUMANITY);
        }
      }

      return shouldRefresh;
    }

    GoalManager.updateProgress(result);

    return shouldRefresh;
  }

  public static boolean processItem(int itemId, int count) {
    return ResultProcessor.processResult(ItemPool.get(itemId, count));
  }

  public static void removeItem(int itemId) {
    AdventureResult ar = ItemPool.get(itemId, -1);
    if (KoLConstants.inventory.contains(ar)) {
      ResultProcessor.processResult(ar);
    }
  }

  public static void removeAllItems(int itemId) {
    int count = InventoryManager.getCount(itemId);
    if (count > 0) {
      AdventureResult ar = ItemPool.get(itemId, -count);
      ResultProcessor.processResult(ar);
    }
  }

  public static boolean processMeat(long amount) {
    return ResultProcessor.processResult(
        new AdventureLongCountResult(AdventureResult.MEAT, amount));
  }

  public static void processAdventuresLeft(int amount) {
    if (amount != 0) {
      KoLCharacter.setAdventuresLeft(KoLCharacter.getAdventuresLeft() + amount);
    }
  }

  public static void processAdventuresUsed(int amount) {
    if (amount != 0) {
      ResultProcessor.processResult(new AdventureResult(AdventureResult.ADV, -amount));
    }
  }

  /**
   * Processes a result received through adventuring. This places items inside of inventories and
   * lots of other good stuff.
   */
  public static final void tallyResult(
      final AdventureResult result, final boolean updateCalculatedLists) {
    // Treat the result as normal from this point forward.
    // Figure out which list the result should be added to
    // and add it to that list.

    String resultName = result.getName();
    if (resultName == null) {
      return;
    }

    if (result.isItem()) {
      // Certain items that you "gain" don't really enter inventory
      switch (result.getItemId()) {
        case ItemPool.CHATEAU_MUSCLE,
            ItemPool.CHATEAU_MYST,
            ItemPool.CHATEAU_MOXIE,
            ItemPool.CHATEAU_FAN,
            ItemPool.CHATEAU_CHANDELIER,
            ItemPool.CHATEAU_SKYLIGHT,
            ItemPool.CHATEAU_BANK,
            ItemPool.CHATEAU_JUICE_BAR -> {
          ChateauRequest.gainItem(result);
          return;
        }
      }

      AdventureResult.addResultToList(KoLConstants.inventory, result);

      if (updateCalculatedLists) {
        EquipmentManager.processResult(result);
        ConcoctionDatabase.setRefreshNeeded(result.getItemId());
      }
    } else if (resultName.equals(AdventureResult.HP)) {
      KoLCharacter.setHP(
          KoLCharacter.getCurrentHP() + result.getLongCount(),
          KoLCharacter.getMaximumHP(),
          KoLCharacter.getBaseMaxHP());
    } else if (resultName.equals(AdventureResult.MP)) {
      KoLCharacter.setMP(
          KoLCharacter.getCurrentMP() + result.getLongCount(),
          KoLCharacter.getMaximumMP(),
          KoLCharacter.getBaseMaxMP());
    } else if (resultName.equals(AdventureResult.ENERGY)) {
      KoLCharacter.setYouRobotEnergy(KoLCharacter.getYouRobotEnergy() + result.getCount());
    } else if (resultName.equals(AdventureResult.SCRAP)) {
      KoLCharacter.setYouRobotScraps(KoLCharacter.getYouRobotScraps() + result.getCount());
    } else if (resultName.equals(AdventureResult.MEAT)) {
      KoLCharacter.setAvailableMeat(KoLCharacter.getAvailableMeat() + result.getLongCount());
      if (updateCalculatedLists) {
        ConcoctionDatabase.setRefreshNeeded(false);
      }
    } else if (resultName.equals(AdventureResult.ADV)) {
      if (result.getCount() < 0) {
        AdventureResult[] effectsArray = new AdventureResult[KoLConstants.activeEffects.size()];
        KoLConstants.activeEffects.toArray(effectsArray);

        for (int i = effectsArray.length - 1; i >= 0; --i) {
          AdventureResult effect = effectsArray[i];
          int duration = effect.getCount();
          if (duration == Integer.MAX_VALUE) {
            // Intrinsic effect
          } else if (KoLCharacter.getAscensionClass() == AscensionClass.COW_PUNCHER
              && effect.getEffectId() == EffectPool.COWRRUPTION) {
            // Does not decrement
          } else if (duration + result.getCount() <= 0) {
            KoLConstants.activeEffects.remove(i);

            switch (effect.getEffectId()) {
              case EffectPool.INIGOS, EffectPool.CRAFT_TEA -> {
                // If you lose Inigo's or Craft Tea, what you can craft changes
                ConcoctionDatabase.setRefreshNeeded(true);
              }
              case EffectPool.HALF_ASTRAL -> {
                // There is no "cool down" choice adventure for leaving this,
                // unlike the various llama lama forms
                KoLCharacter.setLimitMode(LimitMode.NONE);
              }
            }
          } else {
            KoLConstants.activeEffects.set(
                i, effect.getInstance(effect.getCount() + result.getCount()));
          }
        }

        KoLCharacter.setCurrentRun(KoLCharacter.getCurrentRun() - result.getCount());
      }
    } else if (resultName.equals(AdventureResult.DRUNK)) {
      KoLCharacter.setInebriety(KoLCharacter.getInebriety() + result.getCount());
    } else if (resultName.equals(AdventureResult.FULL)) {
      KoLCharacter.setFullness(KoLCharacter.getFullness() + result.getCount());
    } else if (resultName.equals(AdventureResult.SUBSTATS)) {
      if (result.isMuscleGain()) {
        KoLCharacter.incrementTotalMuscle(result.getCount());
      } else if (result.isMysticalityGain()) {
        KoLCharacter.incrementTotalMysticality(result.getCount());
      } else if (result.isMoxieGain()) {
        KoLCharacter.incrementTotalMoxie(result.getCount());
      }
    } else if (resultName.equals(AdventureResult.PVP)) {
      KoLCharacter.setAttacksLeft(KoLCharacter.getAttacksLeft() + result.getCount());
    }
  }

  private static void gainItem(boolean adventureResults, AdventureResult result) {
    int itemId = result.getItemId();
    int count = result.getCount();

    ConcoctionDatabase.setRefreshNeeded(itemId);

    // All results, whether positive or negative, are
    // handled here.

    switch (itemId) {
      case ItemPool.FAKE_HAND -> NamedListenerRegistry.fireChange("(fakehands)");
      case ItemPool.BLACK_BARTS_BOOTY ->
          // Whether you just got Black Bart's booty or just used
          // it to get the skill, you ain't gettin' another this season.
          Preferences.setBoolean("blackBartsBootyAvailable", false);
      case ItemPool.HOLIDAY_FUN_BOOK -> Preferences.setBoolean("holidayHalsBookAvailable", false);
      case ItemPool.ANTAGONISTIC_SNOWMAN_KIT ->
          Preferences.setBoolean("antagonisticSnowmanKitAvailable", false);
      case ItemPool.MAP_TO_KOKOMO -> Preferences.setBoolean("mapToKokomoAvailable", false);
      case ItemPool.ESSENCE_OF_BEAR -> Preferences.setBoolean("essenceOfBearAvailable", false);
      case ItemPool.MANUAL_OF_NUMBEROLOGY ->
          Preferences.setBoolean("manualOfNumberologyAvailable", false);
      case ItemPool.ROM_OF_OPTIMALITY -> Preferences.setBoolean("ROMOfOptimalityAvailable", false);
      case ItemPool.SCHOOL_OF_HARD_KNOCKS_DIPLOMA -> {
        // You actual can (could) buy multiple School of Hard Knocks diplomas
        // Preferences.setBoolean( "schoolOfHardKnocksDiplomaAvailable", false );
      }
      case ItemPool.GUIDE_TO_SAFARI -> Preferences.setBoolean("guideToSafariAvailable", false);
      case ItemPool.GLITCH_ITEM -> Preferences.setBoolean("glitchItemAvailable", false);
      case ItemPool.LAW_OF_AVERAGES -> Preferences.setBoolean("lawOfAveragesAvailable", false);
      case ItemPool.UNIVERSAL_SEASONING ->
          Preferences.setBoolean("universalSeasoningAvailable", false);
      case ItemPool.BOOK_OF_IRONY -> Preferences.setBoolean("bookOfIronyAvailable", false);
      case ItemPool.MAGNIFICENT_OYSTER_EGG,
          ItemPool.BRILLIANT_OYSTER_EGG,
          ItemPool.GLISTENING_OYSTER_EGG,
          ItemPool.SCINTILLATING_OYSTER_EGG,
          ItemPool.PEARLESCENT_OYSTER_EGG,
          ItemPool.LUSTROUS_OYSTER_EGG,
          ItemPool.GLEAMING_OYSTER_EGG -> {
        if (KoLCharacter.hasEquipped(ItemPool.OYSTER_BASKET)
            && HolidayDatabase.getHoliday().contains("Oyster Egg Day")
            && adventureResults) {
          Preferences.increment("_oysterEggsFound");
        }
      }
      case ItemPool.SHADOW_SAUSAGE,
          ItemPool.SHADOW_SKIN,
          ItemPool.SHADOW_FLAME,
          ItemPool.SHADOW_BREAD,
          ItemPool.SHADOW_ICE,
          ItemPool.SHADOW_FLUID,
          ItemPool.SHADOW_GLASS,
          ItemPool.SHADOW_BRICK,
          ItemPool.SHADOW_SINEW,
          ItemPool.SHADOW_VENOM,
          ItemPool.SHADOW_NECTAR,
          ItemPool.SHADOW_STICK ->
          RufusManager.handleShadowItems(result.getName());
    }

    if (CoinmastersDatabase.isCurrency(result)) {
      NamedListenerRegistry.fireChange("(coinmaster)");
    }

    if (StandardRewardDatabase.isPulverizedStandardReward(itemId)) {
      // These are currencies in the Armory and Leggery
      NamedListenerRegistry.fireChange("(armoryandleggery)");
    }

    if (ItemDatabase.isCandyItem(itemId)) {
      NamedListenerRegistry.fireChange("(candy)");
    }

    // This might be a target of the Party Fair quest.
    if (QuestDatabase.isQuestStep(Quest.PARTY_FAIR, "step1")
        || QuestDatabase.isQuestStep(Quest.PARTY_FAIR, "step2")) {
      String quest = Preferences.getString("_questPartyFairQuest");
      if (quest.equals("booze") || quest.equals("food")) {
        String target = Preferences.getString("_questPartyFairProgress");
        String itemCountString = null;
        String itemIdString = null;
        int position = target.indexOf(" ");
        if (position > 0) {
          itemCountString = target.substring(0, position);
          itemIdString = target.substring(position);
          if (StringUtilities.parseInt(itemIdString) == itemId) {
            if (InventoryManager.getCount(itemId) >= StringUtilities.parseInt(itemCountString)) {
              QuestDatabase.setQuestProgress(Quest.PARTY_FAIR, "step2");
            } else {
              QuestDatabase.setQuestProgress(Quest.PARTY_FAIR, "step1");
            }
          }
        }
      }
    }

    // This might be a target of the Doctor, Doctor quest.
    if (QuestDatabase.isQuestStep(Quest.PARTY_FAIR, QuestDatabase.STARTED)) {
      int targetItemId = ItemDatabase.getItemId(Preferences.getString("doctorBagQuestItem"));
      if (targetItemId == itemId) {
        QuestDatabase.setQuestProgress(Quest.DOCTOR_BAG, "step1");
      }
    }

    // From here on out, only positive results are handled.
    if (count < 0) {
      return;
    }

    if (EquipmentDatabase.isHat(result)) {
      PreferenceListenerRegistry.firePreferenceChanged("(hats)");
    }

    var lookup = new Lookup(ModifierType.ITEM, itemId);
    if (ModifierDatabase.getInventorySkillProviders().contains(lookup)) {
      InventoryManager.checkSkillGrantingEquipment(itemId);
    }

    switch (itemId) {
      case ItemPool.GMOB_POLLEN -> {
        if (adventureResults) {
          // Record that we beat the guy made of bees.
          Preferences.setBoolean("guyMadeOfBeesDefeated", true);
        }
      }
      case ItemPool.ROASTED_MARSHMALLOW -> {
        // Special Yuletide adventures
        if (KoLAdventure.lastAdventureId() == AdventurePool.YULETIDE) {
          ResultProcessor.removeItem(ItemPool.MARSHMALLOW);
        }
      }
      case ItemPool.MARSHMALLOW_BOMB -> {
        // Special St Sneaky Pete's Day Yuletide adventures
        if (KoLAdventure.lastAdventureId() == AdventurePool.YULETIDE) {
          ResultProcessor.removeItem(ItemPool.GREEN_MARSHMALLOW);
        }
      }

      // Sticker weapons may have been folded from the other form
      case ItemPool.STICKER_SWORD -> ResultProcessor.removeItem(ItemPool.STICKER_CROSSBOW);
      case ItemPool.STICKER_CROSSBOW -> ResultProcessor.removeItem(ItemPool.STICKER_SWORD);
      case ItemPool.MOSQUITO_LARVA -> QuestDatabase.setQuestProgress(Quest.LARVA, "step1");
      case ItemPool.BITCHIN_MEATCAR,
          ItemPool.DESERT_BUS_PASS,
          ItemPool.PUMPKIN_CARRIAGE,
          ItemPool.TIN_LIZZIE ->
          // Desert beach unlocked
          KoLCharacter.setDesertBeachAvailable();
      case ItemPool.RUSTY_SCREWDRIVER -> QuestDatabase.setQuestProgress(Quest.UNTINKER, "step1");
      case ItemPool.JUNK_JUNK -> QuestDatabase.setQuestProgress(Quest.HIPPY, "step3");
      case ItemPool.DINGY_DINGHY, ItemPool.SKIFF, ItemPool.YELLOW_SUBMARINE ->
          // Island unlocked
          Preferences.setInteger("lastIslandUnlock", KoLCharacter.getAscensions());
      case ItemPool.TISSUE_PAPER_IMMATERIA ->
          QuestDatabase.setQuestProgress(Quest.GARBAGE, "step3");
      case ItemPool.TIN_FOIL_IMMATERIA -> QuestDatabase.setQuestProgress(Quest.GARBAGE, "step4");
      case ItemPool.GAUZE_IMMATERIA -> QuestDatabase.setQuestProgress(Quest.GARBAGE, "step5");
      case ItemPool.PLASTIC_WRAP_IMMATERIA ->
          QuestDatabase.setQuestProgress(Quest.GARBAGE, "step6");
      case ItemPool.SOCK -> {
        // If you get a S.O.C.K., you lose all the Immateria
        ResultProcessor.processItem(ItemPool.TISSUE_PAPER_IMMATERIA, -1);
        ResultProcessor.processItem(ItemPool.TIN_FOIL_IMMATERIA, -1);
        ResultProcessor.processItem(ItemPool.GAUZE_IMMATERIA, -1);
        ResultProcessor.processItem(ItemPool.PLASTIC_WRAP_IMMATERIA, -1);
        QuestDatabase.setQuestProgress(Quest.GARBAGE, "step7");
      }
      case ItemPool.BROKEN_WINGS, ItemPool.SUNKEN_EYES ->
          // Make the blackbird so you don't need to have the familiar with you
          ResultProcessor.autoCreate(ItemPool.REASSEMBLED_BLACKBIRD);
      case ItemPool.BUSTED_WINGS, ItemPool.BIRD_BRAIN ->
          // Make the Crow so you don't need to have the familiar with you
          ResultProcessor.autoCreate(ItemPool.RECONSTITUTED_CROW);
      case ItemPool.PIRATE_FLEDGES -> QuestDatabase.setQuestProgress(Quest.PIRATE, "step6");
      case ItemPool.MACGUFFIN_DIARY, ItemPool.ED_DIARY -> {
        // If you get your father's MacGuffin diary, you lose
        // your forged identification documents
        ResultProcessor.processItem(ItemPool.FORGED_ID_DOCUMENTS, -1);
        QuestDatabase.setQuestProgress(Quest.BLACK, "step3");
        // Automatically use the diary to open zones
        if (Preferences.getBoolean("autoQuest")) {
          UseItemRequest.getInstance(result).run();
        }
      }
      case ItemPool.VOLCANO_MAP -> {
        // A counter was made in case we lost the fight against the
        // final assassin, but since this dropped we won the fight
        TurnCounter.stopCounting("Nemesis Assassin window begin");
        TurnCounter.stopCounting("Nemesis Assassin window end");
        QuestDatabase.setQuestProgress(Quest.NEMESIS, "step25");
        // Automatically use the map to open zones
        if (Preferences.getBoolean("autoQuest")) {
          UseItemRequest.getInstance(result).run();
        }
      }
      case ItemPool.FIRST_PIZZA,
          ItemPool.LACROSSE_STICK,
          ItemPool.EYE_OF_THE_STARS,
          ItemPool.STANKARA_STONE,
          ItemPool.MURPHYS_FLAG,
          ItemPool.SHIELD_OF_BROOK -> {
        // Annoyingly, matter-duplicating drones can duplicate quest items
        if (InventoryManager.getCount(itemId) == 1) {
          QuestDatabase.advanceQuest(Quest.SHEN);
        }
        Preferences.setString("shenQuestItem", result.getName());
      }
      case ItemPool.PALINDROME_BOOK_2 -> {
        // If you get "2 Love Me, Vol. 2", you lose
        // the items you put on the shelves
        ResultProcessor.processItem(ItemPool.PHOTOGRAPH_OF_GOD, -1);
        ResultProcessor.processItem(ItemPool.PHOTOGRAPH_OF_RED_NUGGET, -1);
        ResultProcessor.processItem(ItemPool.PHOTOGRAPH_OF_OSTRICH_EGG, -1);
        ResultProcessor.processItem(ItemPool.PHOTOGRAPH_OF_DOG, -1);
        QuestDatabase.setQuestIfBetter(Quest.PALINDOME, "step1");
      }
      case ItemPool.WET_STUNT_NUT_STEW -> {
        // If you have been asked to get the stew, you now have it.
        if (QuestDatabase.isQuestStep(Quest.PALINDOME, "step3")) {
          QuestDatabase.setQuestProgress(Quest.PALINDOME, "step4");
        }
      }
      case ItemPool.MEGA_GEM -> {
        // If you get the Mega Gem, you lose your wet stunt nut
        // stew
        ResultProcessor.processItem(ItemPool.WET_STUNT_NUT_STEW, -1);
        QuestDatabase.setQuestIfBetter(Quest.PALINDOME, "step5");
      }
      case ItemPool.HOLY_MACGUFFIN ->
          QuestDatabase.setQuestProgress(Quest.PYRAMID, QuestDatabase.FINISHED);
      case ItemPool.CONFETTI -> {
        // If you get the confetti, you lose the Holy MacGuffin
        if (InventoryManager.getCount(ItemPool.HOLY_MACGUFFIN) > 0) {
          ResultProcessor.processItem(ItemPool.HOLY_MACGUFFIN, -1);
          QuestDatabase.setQuestProgress(Quest.PYRAMID, QuestDatabase.FINISHED);
          QuestDatabase.setQuestProgress(Quest.MANOR, QuestDatabase.FINISHED);
          QuestDatabase.setQuestProgress(Quest.WORSHIP, QuestDatabase.FINISHED);
          QuestDatabase.setQuestProgress(Quest.PALINDOME, QuestDatabase.FINISHED);
          QuestDatabase.setQuestProgress(Quest.MACGUFFIN, QuestDatabase.FINISHED);
        }
      }
      case ItemPool.MORTAR_DISSOLVING_RECIPE -> {
        QuestDatabase.setQuestIfBetter(Quest.MANOR, "step2");
        if (Preferences.getBoolean("autoQuest")) {
          boolean equipSpecs = InventoryManager.getCount(ItemPool.SPOOKYRAVEN_SPECTACLES) > 0;
          Checkpoint checkpoint = null;
          try {
            if (equipSpecs) {
              checkpoint = new Checkpoint();
              RequestThread.postRequest(
                  new EquipmentRequest(
                      ItemPool.get(ItemPool.SPOOKYRAVEN_SPECTACLES, 1), Slot.ACCESSORY3));
            }
            RequestThread.postRequest(
                UseItemRequest.getInstance(ItemPool.MORTAR_DISSOLVING_RECIPE));
          } finally {
            if (checkpoint != null) {
              checkpoint.close();
            }
          }
          boolean hasSpecs =
              equipSpecs
                  || KoLCharacter.hasEquipped(ItemPool.get(ItemPool.SPOOKYRAVEN_SPECTACLES, 1));
          KoLmafia.updateDisplay(
              "Mortar-dissolving recipe used with Lord Spookyraven's spectacles "
                  + (hasSpecs ? "" : "NOT ")
                  + "equipped.");
          // Ugly hacky fix for the above UseItemRequest for some reason not triggering the code
          // there
          if (hasSpecs) {
            Preferences.setString("spookyravenRecipeUsed", "with_glasses");
          } else {
            Preferences.setString("spookyravenRecipeUsed", "no_glasses");
          }
        }
      }
      case ItemPool.MOLYBDENUM_MAGNET ->
          // When you get the molybdenum magnet, tell quest handler
          IslandManager.startJunkyardQuest();
      case ItemPool.MOLYBDENUM_HAMMER,
          ItemPool.MOLYBDENUM_SCREWDRIVER,
          ItemPool.MOLYBDENUM_PLIERS,
          ItemPool.MOLYBDENUM_WRENCH ->
          // When you get a molybdenum item, tell quest handler
          IslandManager.resetGremlinTool();
      case ItemPool.SPOOKY_BICYCLE_CHAIN -> {
        if (adventureResults) QuestDatabase.setQuestIfBetter(Quest.BUGBEAR, "step3");
      }
      case ItemPool.RONALD_SHELTER_MAP, ItemPool.GRIMACE_SHELTER_MAP ->
          QuestDatabase.setQuestIfBetter(Quest.GENERATOR, "step1");
      case ItemPool.SPOOKY_LITTLE_GIRL -> QuestDatabase.setQuestIfBetter(Quest.GENERATOR, "step2");
      case ItemPool.EMU_UNIT -> {
        // If you get an E.M.U. Unit, you lose all the E.M.U. parts
        ResultProcessor.processItem(ItemPool.EMU_JOYSTICK, -1);
        ResultProcessor.processItem(ItemPool.EMU_ROCKET, -1);
        ResultProcessor.processItem(ItemPool.EMU_HELMET, -1);
        ResultProcessor.processItem(ItemPool.EMU_HARNESS, -1);
        QuestDatabase.setQuestIfBetter(Quest.GENERATOR, "step3");
      }
      case ItemPool.OVERCHARGED_POWER_SPHERE,
          ItemPool.EL_VIBRATO_HELMET,
          ItemPool.EL_VIBRATO_SPEAR,
          ItemPool.EL_VIBRATO_PANTS -> {
        if (adventureResults) ResultProcessor.removeItem(ItemPool.POWER_SPHERE);
      }
      case ItemPool.BROKEN_DRONE -> {
        if (adventureResults) ResultProcessor.removeItem(ItemPool.DRONE);
      }
      case ItemPool.REPAIRED_DRONE -> {
        if (adventureResults) ResultProcessor.removeItem(ItemPool.BROKEN_DRONE);
      }
      case ItemPool.AUGMENTED_DRONE -> {
        if (adventureResults) ResultProcessor.removeItem(ItemPool.REPAIRED_DRONE);
      }
      case ItemPool.TRAPEZOID -> ResultProcessor.removeItem(ItemPool.POWER_SPHERE);
      case ItemPool.CITADEL_SATCHEL -> ResultProcessor.processMeat(-300);
      case ItemPool.HAROLDS_HAMMER_HEAD, ItemPool.HAROLDS_HAMMER ->
          // Yes, they are the same quest step!
          QuestDatabase.setQuestProgress(Quest.HAMMER, QuestDatabase.STARTED);
      case ItemPool.HAROLDS_BELL -> {
        QuestDatabase.setQuestProgress(Quest.HAMMER, QuestDatabase.FINISHED);
        ResultProcessor.processItem(ItemPool.HAROLDS_HAMMER, -1);
      }
      case ItemPool.LIT_BIRTHDAY_CAKE -> {
        ResultProcessor.processItem(ItemPool.UNLIT_BIRTHDAY_CAKE, -1);
        QuestDatabase.setQuestProgress(Quest.BAKER, QuestDatabase.STARTED);
      }
      case ItemPool.UNLIT_BIRTHDAY_CAKE ->
          // Yes, they are the same quest step!
          QuestDatabase.setQuestProgress(Quest.BAKER, QuestDatabase.STARTED);
      case ItemPool.PAT_A_CAKE_PENDANT -> {
        ResultProcessor.processItem(ItemPool.LIT_BIRTHDAY_CAKE, -1);
        QuestDatabase.setQuestProgress(Quest.BAKER, QuestDatabase.FINISHED);
      }

      // These update the session results for the item swapping in
      // the Gnome's Going Postal quest.

      case ItemPool.REALLY_BIG_TINY_HOUSE ->
          ResultProcessor.processItem(ItemPool.RED_PAPER_CLIP, -1);
      case ItemPool.NONESSENTIAL_AMULET ->
          ResultProcessor.processItem(ItemPool.REALLY_BIG_TINY_HOUSE, -1);
      case ItemPool.WHITE_WINE_VINAIGRETTE ->
          ResultProcessor.processItem(ItemPool.NONESSENTIAL_AMULET, -1);
      case ItemPool.CURIOUSLY_SHINY_AX ->
          ResultProcessor.processItem(ItemPool.WHITE_WINE_VINAIGRETTE, -1);
      case ItemPool.CUP_OF_STRONG_TEA ->
          ResultProcessor.processItem(ItemPool.CURIOUSLY_SHINY_AX, -1);
      case ItemPool.MARINATED_STAKES -> ResultProcessor.processItem(ItemPool.CUP_OF_STRONG_TEA, -1);
      case ItemPool.KNOB_BUTTER -> ResultProcessor.processItem(ItemPool.MARINATED_STAKES, -1);
      case ItemPool.VIAL_OF_ECTOPLASM -> ResultProcessor.processItem(ItemPool.KNOB_BUTTER, -1);
      case ItemPool.BOOCK_OF_MAGICKS -> ResultProcessor.processItem(ItemPool.VIAL_OF_ECTOPLASM, -1);
      case ItemPool.EZ_PLAY_HARMONICA_BOOK ->
          ResultProcessor.processItem(ItemPool.BOOCK_OF_MAGICKS, -1);
      case ItemPool.FINGERLESS_HOBO_GLOVES ->
          ResultProcessor.processItem(ItemPool.EZ_PLAY_HARMONICA_BOOK, -1);
      case ItemPool.CHOMSKYS_COMICS ->
          ResultProcessor.processItem(ItemPool.FINGERLESS_HOBO_GLOVES, -1);
      case ItemPool.GNOME_DEMODULIZER -> ResultProcessor.removeItem(ItemPool.CHOMSKYS_COMICS);
      case ItemPool.SHOPPING_LIST ->
          QuestDatabase.setQuestIfBetter(Quest.MEATCAR, QuestDatabase.STARTED);
      case ItemPool.MUS_MANUAL, ItemPool.MYS_MANUAL, ItemPool.MOX_MANUAL -> {
        ResultProcessor.processItem(ItemPool.DUSTY_BOOK, -1);
        ResultProcessor.processItem(ItemPool.FERNSWARTHYS_KEY, -1);
      }
      case ItemPool.FRATHOUSE_BLUEPRINTS -> {
        ResultProcessor.processItem(ItemPool.CARONCH_MAP, -1);
        ResultProcessor.processItem(ItemPool.CARONCH_NASTY_BOOTY, -1);
        QuestDatabase.setQuestIfBetter(Quest.PIRATE, "step2");
      }
      case ItemPool.CARONCH_DENTURES -> QuestDatabase.setQuestIfBetter(Quest.PIRATE, "step3");
      case ItemPool.EXORCISED_SANDWICH -> QuestDatabase.setQuestProgress(Quest.MYST, "step1");
      case ItemPool.BIG_KNOB_SAUSAGE -> QuestDatabase.setQuestProgress(Quest.MUSCLE, "step1");
      case ItemPool.BATSKIN_BELT, ItemPool.BONERDAGON_SKULL -> {
        if (adventureResults) {
          ResultProcessor.autoCreate(ItemPool.BADASS_BELT);
        }
      }
      case ItemPool.DODECAGRAM -> {
        if (InventoryManager.getCount(ItemPool.CANDLES) > 0
            && InventoryManager.getCount(ItemPool.BUTTERKNIFE) > 0) {
          QuestDatabase.setQuestProgress(Quest.FRIAR, "step2");
        }
      }
      case ItemPool.CANDLES -> {
        if (InventoryManager.getCount(ItemPool.DODECAGRAM) > 0
            && InventoryManager.getCount(ItemPool.BUTTERKNIFE) > 0) {
          QuestDatabase.setQuestProgress(Quest.FRIAR, "step2");
        }
      }
      case ItemPool.BUTTERKNIFE -> {
        if (InventoryManager.getCount(ItemPool.DODECAGRAM) > 0
            && InventoryManager.getCount(ItemPool.CANDLES) > 0) {
          QuestDatabase.setQuestProgress(Quest.FRIAR, "step2");
        }
      }
      case ItemPool.GROARS_FUR, ItemPool.WINGED_YETI_FUR ->
          QuestDatabase.setQuestProgress(Quest.TRAPPER, "step5");
      case ItemPool.MISTY_CLOAK, ItemPool.MISTY_ROBE, ItemPool.MISTY_CAPE -> {
        if (RequestLogger.getLastURLString().contains("action=highlands_dude")) {
          QuestDatabase.setQuestProgress(Quest.TOPPING, QuestDatabase.FINISHED);
          QuestDatabase.setQuestProgress(Quest.LOL, QuestDatabase.STARTED);
          Preferences.setInteger("booPeakProgress", 0);
          Preferences.setBoolean("booPeakLit", true);
          Preferences.setInteger("twinPeakProgress", 15);
          Preferences.setFloat("oilPeakProgress", 0);
          Preferences.setBoolean("oilPeakLit", true);
        }
      }
      case ItemPool.HEMP_STRING, ItemPool.BONERDAGON_VERTEBRA ->
          ResultProcessor.autoCreate(ItemPool.BONERDAGON_NECKLACE);
      case ItemPool.COPPERHEAD_CHARM, ItemPool.COPPERHEAD_CHARM_RAMPANT -> {
        if (InventoryManager.hasItem(ItemPool.COPPERHEAD_CHARM)) {
          QuestDatabase.setQuestProgress(Quest.SHEN, QuestDatabase.FINISHED);
        }
        if (InventoryManager.hasItem(ItemPool.COPPERHEAD_CHARM_RAMPANT)) {
          QuestDatabase.setQuestProgress(Quest.RON, QuestDatabase.FINISHED);
        }
        if (InventoryManager.hasItem(ItemPool.COPPERHEAD_CHARM)
            && InventoryManager.hasItem(ItemPool.COPPERHEAD_CHARM_RAMPANT)) {
          ResultProcessor.autoCreate(ItemPool.TALISMAN);
        }
      }
      case ItemPool.TALISMAN ->
          // Having a Talisman is good enough to adventure in the Palindome.
          // Which is to say, The Palindome quest is now started.
          QuestDatabase.setQuestIfBetter(Quest.PALINDOME, QuestDatabase.STARTED);
      case ItemPool.EYE_OF_ED ->
          QuestDatabase.setQuestProgress(Quest.MANOR, QuestDatabase.FINISHED);
      case ItemPool.MCCLUSKY_FILE_PAGE5 -> ResultProcessor.autoCreate(ItemPool.MCCLUSKY_FILE);
      case ItemPool.MOSS_COVERED_STONE_SPHERE -> {
        Preferences.setInteger("hiddenApartmentProgress", 7);
        QuestDatabase.setQuestProgress(Quest.CURSES, QuestDatabase.FINISHED);
        if (QuestDatabase.isQuestFinished(Quest.DOCTOR)
            && QuestDatabase.isQuestFinished(Quest.BUSINESS)
            && QuestDatabase.isQuestFinished(Quest.SPARE)) {
          QuestDatabase.setQuestProgress(Quest.WORSHIP, "step4");
        }
      }
      case ItemPool.DRIPPING_STONE_SPHERE -> {
        Preferences.setInteger("hiddenHospitalProgress", 7);
        QuestDatabase.setQuestProgress(Quest.DOCTOR, QuestDatabase.FINISHED);
        if (QuestDatabase.isQuestFinished(Quest.CURSES)
            && QuestDatabase.isQuestFinished(Quest.BUSINESS)
            && QuestDatabase.isQuestFinished(Quest.SPARE)) {
          QuestDatabase.setQuestProgress(Quest.WORSHIP, "step4");
        }
      }
      case ItemPool.CRACKLING_STONE_SPHERE -> {
        // Lose McClusky File when you kill the Protector Spirit
        ResultProcessor.processItem(ItemPool.MCCLUSKY_FILE, -1);
        Preferences.setInteger("hiddenOfficeProgress", 7);
        QuestDatabase.setQuestProgress(Quest.BUSINESS, QuestDatabase.FINISHED);
        if (QuestDatabase.isQuestFinished(Quest.CURSES)
            && QuestDatabase.isQuestFinished(Quest.DOCTOR)
            && QuestDatabase.isQuestFinished(Quest.SPARE)) {
          QuestDatabase.setQuestProgress(Quest.WORSHIP, "step4");
        }
      }
      case ItemPool.SCORCHED_STONE_SPHERE -> {
        Preferences.setInteger("hiddenBowlingAlleyProgress", 7);
        QuestDatabase.setQuestProgress(Quest.SPARE, QuestDatabase.FINISHED);
        if (QuestDatabase.isQuestFinished(Quest.CURSES)
            && QuestDatabase.isQuestFinished(Quest.BUSINESS)
            && QuestDatabase.isQuestFinished(Quest.DOCTOR)) {
          QuestDatabase.setQuestProgress(Quest.WORSHIP, "step4");
        }
      }
      case ItemPool.ANCIENT_AMULET -> {
        // If you get the ancient amulet, you lose the 4 stone triangles, and have definitely
        // completed quest actions
        ResultProcessor.processItem(ItemPool.STONE_TRIANGLE, -4);
        Preferences.setInteger("hiddenApartmentProgress", 8);
        Preferences.setInteger("hiddenHospitalProgress", 8);
        Preferences.setInteger("hiddenOfficeProgress", 8);
        Preferences.setInteger("hiddenBowlingAlleyProgress", 8);
        QuestDatabase.setQuestProgress(Quest.WORSHIP, QuestDatabase.FINISHED);
      }
      case ItemPool.STAFF_OF_FATS ->
          QuestDatabase.setQuestProgress(Quest.PALINDOME, QuestDatabase.FINISHED);
      case ItemPool.ANCIENT_BOMB -> ResultProcessor.processItem(ItemPool.ANCIENT_BRONZE_TOKEN, -1);
      case ItemPool.CARONCH_MAP ->
          QuestDatabase.setQuestProgress(Quest.PIRATE, QuestDatabase.STARTED);
      case ItemPool.CARONCH_NASTY_BOOTY -> QuestDatabase.setQuestIfBetter(Quest.PIRATE, "step1");
      case ItemPool.BILLIARDS_KEY ->
          QuestDatabase.setQuestProgress(Quest.SPOOKYRAVEN_NECKLACE, "step1");
      case ItemPool.LIBRARY_KEY ->
          QuestDatabase.setQuestProgress(Quest.SPOOKYRAVEN_NECKLACE, "step3");
      case ItemPool.SPOOKYRAVEN_NECKLACE -> {
        Preferences.setInteger("writingDesksDefeated", 5);
        QuestDatabase.setQuestProgress(Quest.SPOOKYRAVEN_NECKLACE, "step4");
      }
      case ItemPool.DUSTY_POPPET ->
          QuestDatabase.setQuestProgress(Quest.SPOOKYRAVEN_BABIES, QuestDatabase.STARTED);
      case ItemPool.BABY_GHOSTS -> {
        QuestDatabase.setQuestProgress(Quest.SPOOKYRAVEN_BABIES, "step1");
        ResultProcessor.removeItem(ItemPool.DUSTY_POPPET);
        ResultProcessor.removeItem(ItemPool.RICKETY_ROCKING_HORSE);
        ResultProcessor.removeItem(ItemPool.ANTIQUE_JACK_IN_THE_BOX);
      }
      case ItemPool.GHOST_FORMULA -> {
        QuestDatabase.setQuestProgress(Quest.SPOOKYRAVEN_BABIES, QuestDatabase.FINISHED);
        ResultProcessor.removeItem(ItemPool.BABY_GHOSTS);
      }
      case ItemPool.NEOPRENE_SKULLCAP -> {
        if (adventureResults) {
          QuestDatabase.setQuestProgress(Quest.BAT, "step4");
        }
      }
      case ItemPool.HAND_CARVED_BOKKEN, ItemPool.HAND_CARVED_BOW, ItemPool.HAND_CARVED_STAFF -> {
        if (RequestLogger.getLastURLString().contains("action=lc_marty")) {
          QuestDatabase.setQuestProgress(Quest.SWAMP, QuestDatabase.FINISHED);
        }
      }
      case ItemPool.WORSE_HOMES_GARDENS -> {
        QuestDatabase.setQuestProgress(Quest.HIPPY, "step1");
        ConcoctionDatabase.setRefreshNeeded(false);
      }
      case ItemPool.STEEL_LIVER, ItemPool.STEEL_STOMACH, ItemPool.STEEL_SPLEEN ->
          QuestDatabase.setQuestProgress(Quest.AZAZEL, QuestDatabase.FINISHED);
      case ItemPool.BATHYSPHERE ->
          QuestDatabase.setQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.STARTED);
      case ItemPool.BUBBLIN_STONE -> QuestDatabase.setQuestIfBetter(Quest.SEA_MONKEES, "step3");
      case ItemPool.DAS_BOOT,
          ItemPool.FISHY_PIPE,
          ItemPool.FISH_MEAT_CRATE,
          ItemPool.DAMP_WALLET -> {
        if (RequestLogger.getLastURLString().contains("action=oldman_oldman")) {
          ResultProcessor.removeItem(ItemPool.DAMP_OLD_BOOT);
          QuestDatabase.setQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.FINISHED);
        }
      }
      case ItemPool.PREGNANT_FLAMING_MUSHROOM ->
          ResultProcessor.processItem(ItemPool.FLAMING_MUSHROOM, -1);
      case ItemPool.PREGNANT_FROZEN_MUSHROOM ->
          ResultProcessor.processItem(ItemPool.FROZEN_MUSHROOM, -1);
      case ItemPool.PREGNANT_STINKY_MUSHROOM ->
          ResultProcessor.processItem(ItemPool.STINKY_MUSHROOM, -1);
      case ItemPool.GRANDMAS_NOTE -> QuestDatabase.setQuestProgress(Quest.SEA_MONKEES, "step7");
      case ItemPool.GRANDMAS_MAP -> {
        ResultProcessor.processItem(ItemPool.GRANDMAS_NOTE, -1);
        ResultProcessor.processItem(ItemPool.FUCHSIA_YARN, -1);
        ResultProcessor.processItem(ItemPool.CHARTREUSE_YARN, -1);
        QuestDatabase.setQuestProgress(Quest.SEA_MONKEES, "step8");
      }
      case ItemPool.BLACK_GLASS -> QuestDatabase.setQuestProgress(Quest.SEA_MONKEES, "step12");
      case ItemPool.SMALL_STONE_BLOCK -> ResultProcessor.processItem(ItemPool.IRON_KEY, -1);
      case ItemPool.CRIMBOMINATION_CONTRAPTION -> {
        ResultProcessor.removeItem(ItemPool.WRENCH_HANDLE);
        ResultProcessor.removeItem(ItemPool.HEADLESS_BOLTS);
        ResultProcessor.removeItem(ItemPool.AGITPROP_INK);
        ResultProcessor.removeItem(ItemPool.HANDFUL_OF_WIRES);
        ResultProcessor.removeItem(ItemPool.CHUNK_OF_CEMENT);
        ResultProcessor.removeItem(ItemPool.PENGUIN_GRAPPLING_HOOK);
        ResultProcessor.removeItem(ItemPool.CARDBOARD_ELF_EAR);
        ResultProcessor.removeItem(ItemPool.SPIRALING_SHAPE);
      }
      case ItemPool.HAMMER_OF_SMITING,
          ItemPool.CHELONIAN_MORNINGSTAR,
          ItemPool.GREEK_PASTA_OF_PERIL,
          ItemPool.SEVENTEEN_ALARM_SAUCEPAN,
          ItemPool.SHAGADELIC_DISCO_BANJO,
          ItemPool.SQUEEZEBOX_OF_THE_AGES ->
          QuestDatabase.setQuestProgress(Quest.NEMESIS, "step8");
      case ItemPool.FIZZING_SPORE_POD -> {
        if (InventoryManager.getCount(ItemPool.FIZZING_SPORE_POD) + count >= 6
            && QuestDatabase.isQuestLaterThan(Quest.NEMESIS, "step9")) {
          QuestDatabase.setQuestIfBetter(Quest.NEMESIS, "step14");
        }
      }
      case ItemPool.SCALP_OF_GORGOLOK,
          ItemPool.ELDER_TURTLE_SHELL,
          ItemPool.COLANDER_OF_EMERIL,
          ItemPool.ANCIENT_SAUCEHELM,
          ItemPool.DISCO_FRO_PICK,
          ItemPool.EL_SOMBRERO_DE_LOPEZ -> {
        if (adventureResults) {
          QuestDatabase.setQuestProgress(Quest.NEMESIS, "step16");
        }
      }
      case ItemPool.KRAKROXS_LOINCLOTH,
          ItemPool.GALAPAGOSIAN_CUISSES,
          ItemPool.ANGELHAIR_CULOTTES,
          ItemPool.NEWMANS_OWN_TROUSERS,
          ItemPool.VOLARTTAS_BELLBOTTOMS,
          ItemPool.LEDERHOSEN_OF_THE_NIGHT -> {
        if (adventureResults) {
          QuestDatabase.setQuestProgress(Quest.NEMESIS, "step27");
        }
      }
      case ItemPool.HELLSEAL_DISGUISE -> {
        ResultProcessor.processItem(ItemPool.HELLSEAL_HIDE, -6);
        ResultProcessor.processItem(ItemPool.HELLSEAL_BRAIN, -6);
        ResultProcessor.processItem(ItemPool.HELLSEAL_SINEW, -6);
      }
      case ItemPool.DECODED_CULT_DOCUMENTS -> ResultProcessor.processItem(ItemPool.CULT_MEMO, -5);

      // If you acquire this item you've just completed Nemesis quest
      // Contents of Hacienda for Accordion Thief changes
      case ItemPool.BELT_BUCKLE_OF_LOPEZ -> {
        if (adventureResults) {
          HaciendaManager.questCompleted();
          QuestDatabase.setQuestProgress(Quest.NEMESIS, QuestDatabase.FINISHED);
        }
      }
      case ItemPool.INFERNAL_SEAL_CLAW,
          ItemPool.TURTLE_POACHER_GARTER,
          ItemPool.SPAGHETTI_BANDOLIER,
          ItemPool.SAUCEBLOB_BELT,
          ItemPool.NEW_WAVE_BLING -> {
        if (adventureResults) {
          QuestDatabase.setQuestProgress(Quest.NEMESIS, QuestDatabase.FINISHED);
        }
      }
      case ItemPool.PIXEL_CHAIN_WHIP -> {
        if (adventureResults) {
          // If you acquire a pixel chain whip, you lose
          // the pixel whip you were wielding and wield
          // the chain whip in its place.

          AdventureResult whip = ItemPool.get(ItemPool.PIXEL_WHIP, 1);
          EquipmentManager.transformEquipment(whip, result);
          ResultProcessor.processItem(itemId, -1);
        }
      }
      case ItemPool.PIXEL_MORNING_STAR -> {
        if (adventureResults) {
          // If you acquire a pixel morning star, you
          // lose the pixel chain whip you were wielding
          // and wield the morning star in its place.

          AdventureResult chainWhip = ItemPool.get(ItemPool.PIXEL_CHAIN_WHIP, 1);
          EquipmentManager.transformEquipment(chainWhip, result);
          ResultProcessor.processItem(itemId, -1);
        }
      }
      case ItemPool.REFLECTION_OF_MAP -> {
        if (adventureResults) {
          int current = Preferences.getInteger("pendingMapReflections");
          current = Math.max(0, current - 1);
          Preferences.setInteger("pendingMapReflections", current);
        }
      }
      case ItemPool.GONG -> {
        if (adventureResults && KoLCharacter.currentFamiliar.getId() == FamiliarPool.LLAMA) {
          Preferences.increment("_gongDrops", 1);
        }
      }
      case ItemPool.SLIME_STACK -> {
        if (adventureResults && KoLCharacter.currentFamiliar.getId() == FamiliarPool.SLIMELING) {
          int dropped = Preferences.increment("slimelingStacksDropped", 1);
          if (dropped > Preferences.getInteger("slimelingStacksDue")) {
            // in case it's out of sync, nod and smile
            Preferences.setInteger("slimelingStacksDue", dropped);
          }
        }
      }
      case ItemPool.ABSINTHE -> {
        if (adventureResults && KoLCharacter.currentFamiliar.getId() == FamiliarPool.PIXIE) {
          Preferences.increment("_absintheDrops", 1);
        }
      }
      case ItemPool.ASTRAL_MUSHROOM -> {
        if (adventureResults && KoLCharacter.currentFamiliar.getId() == FamiliarPool.BADGER) {
          Preferences.increment("_astralDrops", 1);
        }
      }
      case ItemPool.AGUA_DE_VIDA -> {
        if (adventureResults && KoLCharacter.currentFamiliar.getId() == FamiliarPool.SANDWORM) {
          Preferences.increment("_aguaDrops", 1);
        }
      }
      case ItemPool.DEVILISH_FOLIO -> {
        if (adventureResults && KoLCharacter.currentFamiliar.getId() == FamiliarPool.KLOOP) {
          Preferences.increment("_kloopDrops", 1);
        }
      }
      case ItemPool.FURIOUS_STONE, ItemPool.VANITY_STONE -> {
        String step =
            ((InventoryManager.getCount(ItemPool.FURIOUS_STONE)
                        + InventoryManager.getCount(ItemPool.VANITY_STONE))
                    < 2)
                ? "step2"
                : QuestDatabase.FINISHED;

        QuestDatabase.setQuest(Quest.CLUMSINESS, step);
        Preferences.setString("clumsinessGroveBoss", "");
      }
      case ItemPool.LECHEROUS_STONE, ItemPool.JEALOUSY_STONE -> {
        String step =
            ((InventoryManager.getCount(ItemPool.LECHEROUS_STONE)
                        + InventoryManager.getCount(ItemPool.JEALOUSY_STONE))
                    < 2)
                ? "step2"
                : QuestDatabase.FINISHED;

        QuestDatabase.setQuest(Quest.MAELSTROM, step);
        Preferences.setString("maelstromOfLoversBoss", "");
      }
      case ItemPool.AVARICE_STONE, ItemPool.GLUTTONOUS_STONE -> {
        String step =
            ((InventoryManager.getCount(ItemPool.AVARICE_STONE)
                        + InventoryManager.getCount(ItemPool.GLUTTONOUS_STONE))
                    < 2)
                ? "step2"
                : QuestDatabase.FINISHED;

        QuestDatabase.setQuest(Quest.GLACIER, step);
        Preferences.setString("glacierOfJerksBoss", "");
      }
      case ItemPool.GROOSE_GREASE -> {
        if (adventureResults && KoLCharacter.currentFamiliar.getId() == FamiliarPool.GROOSE) {
          Preferences.increment("_grooseDrops", 1);
        }
      }
      case ItemPool.GG_TOKEN -> {
        if (adventureResults && KoLCharacter.currentFamiliar.getId() == FamiliarPool.TRON) {
          Preferences.increment("_tokenDrops", 1);
        }
        // If this is the first token or ticket we've gotten
        // this ascension, visit the wrong side of the tracks
        // to unlock the arcade.
        if (Preferences.getInteger("lastArcadeAscension") < KoLCharacter.getAscensions()) {
          Preferences.setInteger("lastArcadeAscension", KoLCharacter.getAscensions());
          RequestThread.postRequest(new PlaceRequest("town_wrong"));
        }
      }
      case ItemPool.GG_TICKET -> {
        // If this is the first token or ticket we've gotten
        // this ascension, visit the wrong side of the tracks
        // to unlock the arcade.
        if (Preferences.getInteger("lastArcadeAscension") < KoLCharacter.getAscensions()) {
          Preferences.setInteger("lastArcadeAscension", KoLCharacter.getAscensions());
          RequestThread.postRequest(new PlaceRequest("town_wrong"));
        }
      }
      case ItemPool.TRANSPORTER_TRANSPONDER -> {
        if (adventureResults && KoLCharacter.currentFamiliar.getId() == FamiliarPool.ALIEN) {
          Preferences.increment("_transponderDrops", 1);
        }
      }
      case ItemPool.UNCONSCIOUS_COLLECTIVE_DREAM_JAR -> {
        if (adventureResults
            && KoLCharacter.currentFamiliar.getId() == FamiliarPool.UNCONSCIOUS_COLLECTIVE) {
          Preferences.increment("_dreamJarDrops", 1);
        }
      }
      case ItemPool.HOT_ASHES -> {
        if (adventureResults
            && KoLCharacter.currentFamiliar.getId() == FamiliarPool.GALLOPING_GRILL) {
          Preferences.increment("_hotAshesDrops", 1);
        }
      }
      case ItemPool.PSYCHOANALYTIC_JAR -> {
        if (adventureResults
            && KoLCharacter.currentFamiliar.getId() == FamiliarPool.ANGRY_JUNG_MAN) {
          Preferences.increment("_jungDrops", 1);
          Preferences.setInteger("jungCharge", 0);
          KoLCharacter.usableFamiliar(FamiliarPool.ANGRY_JUNG_MAN).setCharges(0);
        }
      }
      case ItemPool.TALES_OF_SPELUNKING -> {
        if (adventureResults
            && KoLCharacter.currentFamiliar.getId() == FamiliarPool.ADVENTUROUS_SPELUNKER) {
          Preferences.increment("_spelunkingTalesDrops", 1);
        }
      }
      case ItemPool.POWDERED_GOLD -> {
        if (adventureResults
            && KoLCharacter.currentFamiliar.getId() == FamiliarPool.GOLDEN_MONKEY) {
          Preferences.increment("_powderedGoldDrops", 1);
        }
      }
      case ItemPool.MINI_MARTINI -> {
        if (adventureResults
            && KoLCharacter.currentFamiliar.getId() == FamiliarPool.SWORD_AND_MARTINI_GUY) {
          Preferences.increment("_miniMartiniDrops", 1);
        }
      }
      case ItemPool.POWER_PILL -> {
        if (adventureResults
            && (KoLCharacter.currentFamiliar.getId() == FamiliarPool.PUCK_MAN
                || KoLCharacter.currentFamiliar.getId() == FamiliarPool.MS_PUCK_MAN)) {
          Preferences.setInteger("powerPillProgress", 0);
          Preferences.increment("_powerPillDrops", 1);
        }
      }
      case ItemPool.MACHINE_SNOWGLOBE -> {
        if (adventureResults && KoLCharacter.currentFamiliar.getId() == FamiliarPool.MACHINE_ELF) {
          Preferences.increment("_snowglobeDrops", 1);
        }
      }
      case ItemPool.LIVER_PIE,
          ItemPool.BADASS_PIE,
          ItemPool.FISH_PIE,
          ItemPool.PIPING_PIE,
          ItemPool.IGLOO_PIE,
          ItemPool.TURNOVER,
          ItemPool.DEAD_PIE,
          ItemPool.THROBBING_PIE -> {
        if (adventureResults && KoLCharacter.currentFamiliar.getId() == FamiliarPool.GRINDER) {
          Preferences.increment("_pieDrops", 1);
          Preferences.setInteger("_piePartsCount", -1);
          Preferences.setString("pieStuffing", "");
        }
      }
      case ItemPool.GOOEY_PASTE,
          ItemPool.BEASTLY_PASTE,
          ItemPool.OILY_PASTE,
          ItemPool.ECTOPLASMIC,
          ItemPool.GREASY_PASTE,
          ItemPool.BUG_PASTE,
          ItemPool.HIPPY_PASTE,
          ItemPool.ORC_PASTE,
          ItemPool.DEMONIC_PASTE,
          ItemPool.INDESCRIBABLY_HORRIBLE_PASTE,
          ItemPool.FISHY_PASTE,
          ItemPool.GOBLIN_PASTE,
          ItemPool.PIRATE_PASTE,
          ItemPool.CHLOROPHYLL_PASTE,
          ItemPool.STRANGE_PASTE,
          ItemPool.MER_KIN_PASTE,
          ItemPool.SLIMY_PASTE,
          ItemPool.PENGUIN_PASTE,
          ItemPool.ELEMENTAL_PASTE,
          ItemPool.COSMIC_PASTE,
          ItemPool.HOBO_PASTE,
          ItemPool.CRIMBO_PASTE -> {
        if (adventureResults && KoLCharacter.currentFamiliar.getId() == FamiliarPool.BOOTS) {
          Preferences.increment("_pasteDrops", 1);
        }
      }
      case ItemPool.BEER_LENS -> {
        if (adventureResults) {
          Preferences.increment("_beerLensDrops", 1);
        }
      }
      case ItemPool.COTTON_CANDY_CONDE,
          ItemPool.COTTON_CANDY_PINCH,
          ItemPool.COTTON_CANDY_SMIDGEN,
          ItemPool.COTTON_CANDY_SKOSHE,
          ItemPool.COTTON_CANDY_PLUG,
          ItemPool.COTTON_CANDY_PILLOW,
          ItemPool.COTTON_CANDY_BALE -> {
        if (adventureResults && KoLCharacter.currentFamiliar.getId() == FamiliarPool.CARNIE) {
          Preferences.increment("_carnieCandyDrops", 1);
        }
      }
      case ItemPool.LESSER_GRODULATED_VIOLET,
          ItemPool.TIN_MAGNOLIA,
          ItemPool.BEGPWNIA,
          ItemPool.UPSY_DAISY,
          ItemPool.HALF_ORCHID -> {
        if (adventureResults) {
          Preferences.increment("_mayflowerDrops", 1);
        }
      }
      case ItemPool.EVILOMETER -> CryptManager.acquireEvilometer();
      case ItemPool.TEACHINGS_OF_THE_FIST -> {
        // save which location the scroll was found in.
        String setting =
            AdventureDatabase.fistcoreLocationToSetting(KoLAdventure.lastAdventureId());
        if (setting != null) {
          Preferences.setBoolean(setting, true);
        }
      }
      case ItemPool.KEYOTRON -> {
        BugbearManager.resetStatus();
        Preferences.setInteger("lastKeyotronUse", KoLCharacter.getAscensions());
      }
      case ItemPool.JICK_JAR -> {
        if (RequestLogger.getLastURLString().contains("action=jung")) {
          Preferences.setBoolean("_psychoJarFilled", true);
          ResultProcessor.removeItem(ItemPool.PSYCHOANALYTIC_JAR);
        }
      }
      case ItemPool.SUSPICIOUS_JAR,
          ItemPool.GOURD_JAR,
          ItemPool.MYSTIC_JAR,
          ItemPool.OLD_MAN_JAR,
          ItemPool.ARTIST_JAR,
          ItemPool.MEATSMITH_JAR -> {
        if (RequestLogger.getLastURLString().contains("action=jung")) {
          ResultProcessor.removeItem(ItemPool.PSYCHOANALYTIC_JAR);
        }
      }
      case ItemPool.BRICKO_EYE -> {
        if (RequestLogger.getLastURLString().startsWith("campground.php")
            || RequestLogger.getLastURLString().startsWith("skills.php")) {
          Preferences.increment("_brickoEyeSummons");
        }
      }
      case ItemPool.DIVINE_CHAMPAGNE_POPPER, ItemPool.DIVINE_CRACKER, ItemPool.DIVINE_FLUTE -> {
        if (RequestLogger.getLastURLString().startsWith("campground.php")
            || RequestLogger.getLastURLString().startsWith("skills.php")) {
          Preferences.increment("_favorRareSummons");
        }
      }
      case ItemPool.RESOLUTION_KINDER,
          ItemPool.RESOLUTION_ADVENTUROUS,
          ItemPool.RESOLUTION_LUCKIER -> {
        if (RequestLogger.getLastURLString().startsWith("campground.php")
            || RequestLogger.getLastURLString().startsWith("skills.php")) {
          Preferences.increment("_resolutionRareSummons");
        }
      }
      case ItemPool.YELLOW_TAFFY -> {
        if (RequestLogger.getLastURLString().startsWith("campground.php")
            || RequestLogger.getLastURLString().startsWith("skills.php")) {
          Preferences.increment("_taffyRareSummons");
          Preferences.increment("_taffyYellowSummons");
        }
      }
      case ItemPool.GREEN_TAFFY, ItemPool.INDIGO_TAFFY -> {
        if (RequestLogger.getLastURLString().startsWith("campground.php")
            || RequestLogger.getLastURLString().startsWith("skills.php")) {
          Preferences.increment("_taffyRareSummons");
        }
      }
      case ItemPool.BOSS_HELM,
          ItemPool.BOSS_CLOAK,
          ItemPool.BOSS_SWORD,
          ItemPool.BOSS_SHIELD,
          ItemPool.BOSS_PANTS,
          ItemPool.BOSS_GAUNTLETS,
          ItemPool.BOSS_BOOTS,
          ItemPool.BOSS_BELT -> {
        if (adventureResults) {
          ResultProcessor.removeItem(ItemPool.GAMEPRO_WALKTHRU);
        }
      }
      case ItemPool.CARROT_NOSE -> {
        if (adventureResults) {
          Preferences.increment("_carrotNoseDrops");
        }
      }
      case ItemPool.COSMIC_SIX_PACK -> Preferences.setBoolean("_cosmicSixPackConjured", true);
      case ItemPool.COBBS_KNOB_MAP ->
          QuestDatabase.setQuestProgress(Quest.GOBLIN, QuestDatabase.STARTED);
      case ItemPool.MERKIN_LOCKKEY -> {
        MonsterData merkinMonster = MonsterStatusTracker.getLastMonster();
        if (merkinMonster == null) {
          break;
        }
        String lockkeyMonster = merkinMonster.getName();
        Preferences.setString("merkinLockkeyMonster", lockkeyMonster);
        switch (lockkeyMonster) {
          case "Mer-kin burglar" -> Preferences.setInteger("choiceAdventure312", 1);
          case "Mer-kin raider" -> Preferences.setInteger("choiceAdventure312", 2);
          case "Mer-kin healer" -> Preferences.setInteger("choiceAdventure312", 3);
        }
      }
      case ItemPool.YEARBOOK_CAMERA -> {
        String desc = DebugDatabase.rawItemDescriptionText(ItemPool.YEARBOOK_CAMERA);
        int upgrades = ItemDatabase.parseYearbookCamera(desc);
        Preferences.setInteger("yearbookCameraAscensions", upgrades);
      }
      case ItemPool.CLANCY_LUTE -> {
        if (adventureResults) {
          QuestDatabase.setQuestProgress(Quest.CLANCY, "step5");
        }
      }
      case ItemPool.BEER_BATTERED_ACCORDION,
          ItemPool.BARITONE_ACCORDION,
          ItemPool.MAMAS_SQUEEZEBOX,
          ItemPool.GUANCERTINA,
          ItemPool.ACCORDION_FILE,
          ItemPool.ACCORD_ION,
          ItemPool.BONE_BANDONEON,
          ItemPool.PENTATONIC_ACCORDION,
          ItemPool.NON_EUCLIDEAN_NON_ACCORDION,
          ItemPool.ACCORDION_OF_JORDION,
          ItemPool.AUTOCALLIOPE,
          ItemPool.ACCORDIONOID_ROCCA,
          ItemPool.PYGMY_CONCERTINETTE,
          ItemPool.GHOST_ACCORDION,
          ItemPool.PEACE_ACCORDION,
          ItemPool.ALARM_ACCORDION,
          ItemPool.BAL_MUSETTE_ACCORDION,
          ItemPool.CAJUN_ACCORDION,
          ItemPool.QUIRKY_ACCORDION -> {
        if (adventureResults) {
          StringBuilder buffer = new StringBuilder(Preferences.getString("_stolenAccordions"));
          if (buffer.length() > 0) {
            buffer.append(",");
          }
          buffer.append(itemId);
          Preferences.setString("_stolenAccordions", buffer.toString());
        }
      }
      case ItemPool.DAMP_OLD_BOOT -> {
        QuestDatabase.setQuestProgress(Quest.SEA_OLD_GUY, "step1");
        Preferences.setBoolean("dampOldBootPurchased", true);
      }
      case ItemPool.GRIMSTONE_MASK -> {
        if (adventureResults) {
          if (KoLCharacter.getFamiliar()
              .equals(KoLCharacter.usableFamiliar(FamiliarPool.GRIMSTONE_GOLEM))) {
            Preferences.increment("_grimstoneMaskDrops");
            Preferences.setInteger("grimstoneCharge", 0);
            KoLCharacter.usableFamiliar(FamiliarPool.GRIMSTONE_GOLEM).setCharges(0);
          } else if (KoLCharacter.currentBjorned.getId() == FamiliarPool.GRIMSTONE_GOLEM
              || KoLCharacter.currentEnthroned.getId() == FamiliarPool.GRIMSTONE_GOLEM) {
            Preferences.increment("_grimstoneMaskDropsCrown");
          }
        }
      }
      case ItemPool.GRIM_FAIRY_TALE -> {
        if (adventureResults) {
          if (KoLCharacter.getFamiliar()
              .equals(KoLCharacter.usableFamiliar(FamiliarPool.GRIM_BROTHER))) {
            Preferences.increment("_grimFairyTaleDrops");
          } else if (KoLCharacter.currentBjorned.getId() == FamiliarPool.GRIM_BROTHER
              || KoLCharacter.currentEnthroned.getId() == FamiliarPool.GRIM_BROTHER) {
            Preferences.increment("_grimFairyTaleDropsCrown");
          }
        }
      }
      case ItemPool.TOASTED_HALF_SANDWICH, ItemPool.MULLED_HOBO_WINE -> {
        if (adventureResults) {
          if (KoLCharacter.getFamiliar()
              .equals(KoLCharacter.usableFamiliar(FamiliarPool.GARBAGE_FIRE))) {
            // This will be updated to 0 in FightRequest later
            Preferences.setInteger("garbageFireProgress", -1);
            Preferences.increment("_garbageFireDrops", 1);
          }
        }
      }
      case ItemPool.BURNING_NEWSPAPER -> {
        if (adventureResults) {
          if (KoLCharacter.getFamiliar()
              .equals(KoLCharacter.usableFamiliar(FamiliarPool.GARBAGE_FIRE))) {
            // This will be updated to 0 in FightRequest later
            Preferences.setInteger("garbageFireProgress", -1);
            Preferences.increment("_garbageFireDrops", 1);
          } else if (KoLCharacter.currentBjorned.getId() == FamiliarPool.GARBAGE_FIRE
              || KoLCharacter.currentEnthroned.getId() == FamiliarPool.GARBAGE_FIRE) {
            Preferences.increment("_garbageFireDropsCrown");
          }
        }
      }
      case ItemPool.HOARDED_CANDY_WAD -> {
        if (adventureResults) {
          Preferences.increment("_hoardedCandyDropsCrown");
        }
      }
      case ItemPool.SPACE_BEAST_FUR -> {
        if (adventureResults) {
          // It could still drop from a space beast while this is true, but that would
          // be harder to check for
          if (KoLCharacter.currentBjorned.getId() == FamiliarPool.TWITCHING_SPACE_CRITTER
              || KoLCharacter.currentEnthroned.getId() == FamiliarPool.TWITCHING_SPACE_CRITTER) {
            Preferences.increment("_spaceFurDropsCrown");
          }
        }
      }
      case ItemPool.CARDBOARD_ORE,
          ItemPool.STYROFOAM_ORE,
          ItemPool.BUBBLEWRAP_ORE,
          ItemPool.VELCRO_ORE,
          ItemPool.TEFLON_ORE,
          ItemPool.VINYL_ORE -> {
        if (adventureResults) {
          // First three could still drop from a ghost miner while this is true, but that would
          // be harder to check for
          if (KoLCharacter.currentBjorned.getId() == FamiliarPool.ADVENTUROUS_SPELUNKER
              || KoLCharacter.currentEnthroned.getId() == FamiliarPool.ADVENTUROUS_SPELUNKER) {
            Preferences.increment("_oreDropsCrown");
          }
        }
      }
      case ItemPool.ABSTRACTION_ACTION,
          ItemPool.ABSTRACTION_THOUGHT,
          ItemPool.ABSTRACTION_SENSATION,
          ItemPool.ABSTRACTION_PURPOSE,
          ItemPool.ABSTRACTION_CATEGORY,
          ItemPool.ABSTRACTION_PERCEPTION -> {
        if (adventureResults) {
          if (KoLCharacter.currentBjorned.getId() == FamiliarPool.MACHINE_ELF
              || KoLCharacter.currentEnthroned.getId() == FamiliarPool.MACHINE_ELF) {
            Preferences.increment("_abstractionDropsCrown");
          }
        }
      }
      case ItemPool.PROFESSOR_WHAT_GARMENT -> QuestDatabase.setQuestProgress(Quest.SHIRT, "step1");
      case ItemPool.THINKNERD_PACKAGE -> {
        if (adventureResults) {
          Preferences.increment("_thinknerdPackageDrops");
        }
      }
      case ItemPool.STEAM_FIST_1,
          ItemPool.STEAM_FIST_2,
          ItemPool.STEAM_FIST_3,
          ItemPool.STEAM_TRIP_1,
          ItemPool.STEAM_TRIP_2,
          ItemPool.STEAM_TRIP_3,
          ItemPool.STEAM_METEOID_1,
          ItemPool.STEAM_METEOID_2,
          ItemPool.STEAM_METEOID_3,
          ItemPool.STEAM_DEMON_1,
          ItemPool.STEAM_DEMON_2,
          ItemPool.STEAM_DEMON_3,
          ItemPool.STEAM_PLUMBER_1,
          ItemPool.STEAM_PLUMBER_2,
          ItemPool.STEAM_PLUMBER_3 -> {
        if (adventureResults) {
          Preferences.increment("_steamCardDrops");
        }
      }
      case ItemPool.PENCIL_THIN_MUSHROOM -> {
        if (InventoryManager.getCount(ItemPool.PENCIL_THIN_MUSHROOM) >= 9) {
          QuestDatabase.setQuestProgress(Quest.JIMMY_MUSHROOM, "step1");
        }
      }
      case ItemPool.SAILOR_SALT -> {
        if (InventoryManager.getCount(ItemPool.SAILOR_SALT) >= 49) {
          QuestDatabase.setQuestProgress(Quest.JIMMY_SALT, "step1");
        }
      }
      case ItemPool.TACO_DAN_RECEIPT -> {
        if (InventoryManager.getCount(ItemPool.TACO_DAN_RECEIPT) >= 9) {
          QuestDatabase.setQuestProgress(Quest.TACO_DAN_AUDIT, "step1");
        }
      }
      case ItemPool.BROUPON -> {
        if (InventoryManager.getCount(ItemPool.BROUPON) >= 14) {
          QuestDatabase.setQuestProgress(Quest.BRODEN_DEBT, "step1");
        }
      }
      case ItemPool.ELIZABETH_DOLLIE -> {
        if (adventureResults) {
          Preferences.setString("nextSpookyravenElizabethRoom", "none");
        }
      }
      case ItemPool.STEPHEN_LAB_COAT -> {
        if (adventureResults) {
          Preferences.setString("nextSpookyravenStephenRoom", "none");
        }
      }
      case ItemPool.WINE_BOMB -> EquipmentManager.discardEquipment(ItemPool.UNSTABLE_FULMINATE);
      case ItemPool.LIGHTNING_MILK -> {
        // These are starting skills if no turns have been played this ascension
        if (KoLCharacter.getCurrentRun() == 0) {
          Preferences.setInteger("heavyRainsStartingLightning", count);
        }
      }
      case ItemPool.AQUA_BRAIN -> {
        // These are starting skills if no turns have been played this ascension
        if (KoLCharacter.getCurrentRun() == 0) {
          Preferences.setInteger("heavyRainsStartingRain", count);
        }
      }
      case ItemPool.THUNDER_THIGH -> {
        // These are starting skills if no turns have been played this ascension
        if (KoLCharacter.getCurrentRun() == 0) {
          Preferences.setInteger("heavyRainsStartingThunder", count);
        }
      }
      case ItemPool.EXPERIMENTAL_SERUM_P00 -> {
        if (InventoryManager.getCount(ItemPool.EXPERIMENTAL_SERUM_P00) >= 4
            && QuestDatabase.isQuestStep(Quest.SERUM, QuestDatabase.STARTED)) {
          QuestDatabase.setQuestProgress(Quest.SERUM, "step1");
        }
      }
      case ItemPool.MINI_CASSETTE_RECORDER -> {
        QuestDatabase.setQuestProgress(Quest.JUNGLE_PUN, QuestDatabase.STARTED);
        Preferences.setInteger("junglePuns", 0);
      }
      case ItemPool.GORE_BUCKET -> {
        QuestDatabase.setQuestProgress(Quest.GORE, QuestDatabase.STARTED);
        Preferences.setInteger("goreCollected", 0);
      }
      case ItemPool.FINGERNAIL_CLIPPERS -> {
        QuestDatabase.setQuestProgress(Quest.CLIPPER, QuestDatabase.STARTED);
        Preferences.setInteger("fingernailsClipped", 0);
      }
      case ItemPool.ESP_COLLAR -> QuestDatabase.setQuestProgress(Quest.FAKE_MEDIUM, "step1");
      case ItemPool.GPS_WATCH ->
          QuestDatabase.setQuestProgress(Quest.OUT_OF_ORDER, QuestDatabase.STARTED);
      case ItemPool.PROJECT_TLB -> QuestDatabase.setQuestProgress(Quest.OUT_OF_ORDER, "step2");
      case ItemPool.PACK_OF_SMOKES -> {
        if (InventoryManager.getCount(ItemPool.PACK_OF_SMOKES) >= 10) {
          QuestDatabase.setQuestProgress(Quest.SMOKES, "step1");
        }
      }
      case ItemPool.SUBJECT_37_FILE -> QuestDatabase.setQuestProgress(Quest.ESCAPE, "step1");
      case ItemPool.GOTO -> {
        if (QuestDatabase.isQuestStep(Quest.ESCAPE, "step2")) {
          QuestDatabase.setQuestProgress(Quest.ESCAPE, "step3");
        }
      }
      case ItemPool.WEREMOOSE_SPIT -> {
        if (QuestDatabase.isQuestStep(Quest.ESCAPE, "step4")) {
          QuestDatabase.setQuestProgress(Quest.ESCAPE, "step5");
        }
      }
      case ItemPool.ABOMINABLE_BLUBBER -> {
        if (QuestDatabase.isQuestStep(Quest.ESCAPE, "step6")) {
          QuestDatabase.setQuestProgress(Quest.ESCAPE, "step7");
        }
      }
      case ItemPool.FRIENDLY_TURKEY, ItemPool.AGITATED_TURKEY, ItemPool.AMBITIOUS_TURKEY -> {
        if (adventureResults && KoLCharacter.currentFamiliar.getId() == FamiliarPool.FIST_TURKEY) {
          Preferences.increment("_turkeyBooze");
        }
      }
      case ItemPool.XIBLAXIAN_ALLOY, ItemPool.XIBLAXIAN_CIRCUITRY, ItemPool.XIBLAXIAN_POLYMER -> {
        if (adventureResults) {
          Preferences.increment("_holoWristDrops");
          // This will be incremented to 0 during later processing
          Preferences.setInteger("_holoWristProgress", -1);
        }
      }
      case ItemPool.ED_EYE -> {
        EquipmentManager.removeEquipment(ItemPool.ED_STAFF);
        ResultProcessor.removeItem(ItemPool.ED_STAFF);
      }
      case ItemPool.XIBLAXIAN_CRYSTAL -> {
        if (RequestLogger.getLastURLString().contains("mining.php")) {
          Preferences.setBoolean("_holoWristCrystal", true);
        }
      }
      case ItemPool.SCARAB_BEETLE_STATUETTE ->
          QuestDatabase.setQuestProgress(Quest.FINAL, QuestDatabase.FINISHED);
      case ItemPool.MEATSMITH_CHECK -> QuestDatabase.setQuestProgress(Quest.MEATSMITH, "step1");
      case ItemPool.TOXIC_GLOBULE -> {
        if (InventoryManager.getCount(ItemPool.TOXIC_GLOBULE) + count >= 20
            && QuestDatabase.isQuestStep(Quest.GIVE_ME_FUEL, QuestDatabase.STARTED)) {
          QuestDatabase.setQuestProgress(Quest.GIVE_ME_FUEL, "step1");
        }
      }
      case ItemPool.LUBE_SHOES ->
          QuestDatabase.setQuestProgress(Quest.SUPER_LUBER, QuestDatabase.STARTED);
      case ItemPool.TRASH_NET -> {
        QuestDatabase.setQuestProgress(Quest.FISH_TRASH, QuestDatabase.STARTED);
        Preferences.setInteger("dinseyFilthLevel", 100);
      }
      case ItemPool.MASCOT_MASK ->
          QuestDatabase.setQuestProgress(Quest.ZIPPITY_DOO_DAH, QuestDatabase.STARTED);
      case ItemPool.YELLOW_PIXEL -> {
        if (adventureResults) {
          if (KoLCharacter.currentBjorned.getId() == FamiliarPool.PUCK_MAN
              || KoLCharacter.currentEnthroned.getId() == FamiliarPool.PUCK_MAN
              || KoLCharacter.currentBjorned.getId() == FamiliarPool.MS_PUCK_MAN
              || KoLCharacter.currentEnthroned.getId() == FamiliarPool.MS_PUCK_MAN) {
            Preferences.increment("_yellowPixelDropsCrown");
          }
        }
      }
      case ItemPool.FRAUDWORT -> {
        if (InventoryManager.getCount(ItemPool.FRAUDWORT) + count >= 3
            && InventoryManager.getCount(ItemPool.SHYSTERWEED) >= 3
            && InventoryManager.getCount(ItemPool.SWINDLEBLOSSOM) >= 3) {
          QuestDatabase.setQuestIfBetter(Quest.DOC, "step1");
        }
      }
      case ItemPool.SHYSTERWEED -> {
        if (InventoryManager.getCount(ItemPool.FRAUDWORT) >= 3
            && InventoryManager.getCount(ItemPool.SHYSTERWEED) + count >= 3
            && InventoryManager.getCount(ItemPool.SWINDLEBLOSSOM) >= 3) {
          QuestDatabase.setQuestIfBetter(Quest.DOC, "step1");
        }
      }
      case ItemPool.SWINDLEBLOSSOM -> {
        if (InventoryManager.getCount(ItemPool.FRAUDWORT) >= 3
            && InventoryManager.getCount(ItemPool.SHYSTERWEED) >= 3
            && InventoryManager.getCount(ItemPool.SWINDLEBLOSSOM) + count >= 3) {
          QuestDatabase.setQuestIfBetter(Quest.DOC, "step1");
        }
      }
      case ItemPool.MIRACLE_WHIP -> {
        Preferences.setBoolean("itemBoughtPerAscension8266", true);
        Preferences.setBoolean("_mayoDeviceRented", true);
      }
      case ItemPool.SPHYGMAYOMANOMETER, ItemPool.REFLEX_HAMMER, ItemPool.MAYO_LANCE ->
          Preferences.setBoolean("_mayoDeviceRented", true);
      case ItemPool.NO_HANDED_PIE -> QuestDatabase.setQuestProgress(Quest.ARMORER, "step4");
      case ItemPool.POPULAR_PART -> {
        QuestDatabase.setQuestProgress(Quest.ARMORER, QuestDatabase.FINISHED);
        ResultProcessor.removeItem(ItemPool.NO_HANDED_PIE);
      }
      case ItemPool.SUPERHEATED_METAL -> {
        if (adventureResults) {
          ResultProcessor.removeItem(ItemPool.HEAT_RESISTANT_SHEET_METAL);
        }
      }
      case ItemPool.SUPERDUPERHEATED_METAL -> {
        if (adventureResults) {
          Preferences.setBoolean("_volcanoSuperduperheatedMetal", true);
          ResultProcessor.removeItem(ItemPool.HEAT_RESISTANT_SHEET_METAL);
        }
      }
      case ItemPool.BARREL_LID -> Preferences.setBoolean("prayedForProtection", true);
      case ItemPool.BARREL_HOOP_EARRING -> Preferences.setBoolean("prayedForGlamour", true);
      case ItemPool.BANKRUPTCY_BARREL -> Preferences.setBoolean("prayedForVigor", true);

      // Correct Snojo progress based on drops - note that it increments after the fight!
      case ItemPool.ANCIENT_MEDICINAL_HERBS -> {
        if (adventureResults) {
          int progress = Preferences.getInteger("snojoMuscleWins");
          // Always should be a multiple of 7 for this drop, after the counter increments later!
          if (progress % 7 != 6) {
            Preferences.setInteger("snojoMuscleWins", (int) Math.floor(progress / 7) * 7 + 6);
          }
        }
      }
      case ItemPool.ICE_RICE -> {
        if (adventureResults) {
          int progress = Preferences.getInteger("snojoMysticalityWins");
          // Always should be a multiple of 7 for this drop, after the counter increments later!
          if (progress % 7 != 6) {
            Preferences.setInteger("snojoMysticalityWins", (int) Math.floor(progress / 7) * 7 + 6);
          }
        }
      }
      case ItemPool.ICED_PLUM_WINE -> {
        if (adventureResults) {
          int progress = Preferences.getInteger("snojoMoxieWins");
          // Always should be a multiple of 7 for this drop, after the counter increments later!
          if (progress % 7 != 6) {
            Preferences.setInteger("snojoMoxieWins", (int) Math.floor(progress / 7) * 7 + 6);
          }
        }
      }
      case ItemPool.TRAINING_BELT -> {
        if (adventureResults) {
          Preferences.setInteger("snojoMuscleWins", 10);
        }
      }
      case ItemPool.TRAINING_LEGWARMERS -> {
        if (adventureResults) {
          Preferences.setInteger("snojoMysticalityWins", 10);
        }
      }
      case ItemPool.TRAINING_HELMET -> {
        if (adventureResults) {
          Preferences.setInteger("snojoMoxieWins", 10);
        }
      }
      case ItemPool.SCROLL_SHATTERING_PUNCH -> {
        if (adventureResults) {
          Preferences.setInteger("snojoMuscleWins", 49);
        }
      }
      case ItemPool.SCROLL_SNOKEBOMB -> {
        if (adventureResults) {
          Preferences.setInteger("snojoMysticalityWins", 49);
        }
      }
      case ItemPool.SCROLL_SHIVERING_MONKEY -> {
        if (adventureResults) {
          Preferences.setInteger("snojoMoxieWins", 49);
        }
      }
      case ItemPool.EXPERIMENTAL_GENE_THERAPY,
          ItemPool.SELF_DEFENSE_TRAINING,
          ItemPool.CONFIDENCE_BUILDING_HUG ->
          BatManager.gainItem(result);
      case ItemPool.COWBOY_BOOTS -> EquipmentRequest.checkCowboyBoots();
      case ItemPool.ROBIN_EGG -> {
        if (adventureResults) {
          if (KoLCharacter.currentFamiliar.getId() == FamiliarPool.ROCKIN_ROBIN) {
            // This will be updated to 0 in FightRequest later
            Preferences.setInteger("rockinRobinProgress", -1);
            Preferences.increment("_robinEggDrops", 1);
          }
        }
      }
      case ItemPool.WAX_GLOB -> {
        if (adventureResults) {
          if (KoLCharacter.currentFamiliar.getId() == FamiliarPool.CANDLE) {
            // This will be updated to 0 in FightRequest later
            Preferences.setInteger("optimisticCandleProgress", -1);
            Preferences.increment("_waxGlobDrops", 1);
          } else if (KoLCharacter.currentBjorned.getId() == FamiliarPool.CANDLE
              || KoLCharacter.currentEnthroned.getId() == FamiliarPool.CANDLE) {
            Preferences.increment("_optimisticCandleDropsCrown");
          }
        }
      }
      case ItemPool.X -> {
        if (adventureResults) {
          if (KoLCharacter.currentFamiliar.getId() == FamiliarPool.XO_SKELETON) {
            // This will be updated to 0 in FightRequest later
            Preferences.setInteger("xoSkeleltonXProgress", -1);
            Preferences.setInteger("xoSkeleltonOProgress", 3);
          }
        }
      }
      case ItemPool.O -> {
        if (adventureResults) {
          if (KoLCharacter.currentFamiliar.getId() == FamiliarPool.XO_SKELETON) {
            // This will be updated to 0 in FightRequest later
            Preferences.setInteger("xoSkeleltonOProgress", -1);
            Preferences.setInteger("xoSkeleltonXProgress", 4);
          }
        }
      }
      case ItemPool.SHIELDING_POTION,
          ItemPool.PUNCHING_POTION,
          ItemPool.SPECIAL_SEASONING,
          ItemPool.NIGHTMARE_FUEL,
          ItemPool.MEAT_CLIP -> {
        if (adventureResults) {
          // This will be updated to 0 in FightRequest later
          Preferences.setInteger("_boomBoxFights", -1);
        }
      }
      case ItemPool.NO_SPOON -> QuestDatabase.setQuestProgress(Quest.ORACLE, "step1");
      case ItemPool.TALES_OF_DREAD -> Preferences.setBoolean("itemBoughtPerCharacter6423", true);
      case ItemPool.BRASS_DREAD_FLASK -> Preferences.setBoolean("itemBoughtPerCharacter6428", true);
      case ItemPool.SILVER_DREAD_FLASK ->
          Preferences.setBoolean("itemBoughtPerCharacter6429", true);
      case ItemPool.NO_HAT -> {
        String rawText =
            DebugDatabase.rawItemDescriptionText(ItemDatabase.getDescriptionId(itemId), true);
        String mod =
            DebugDatabase.parseItemEnchantments(rawText, new ArrayList<>(), ConsumptionType.HAT);
        ModifierDatabase.overrideModifier(ModifierType.ITEM, itemId, mod);
        Preferences.setString("_noHatModifier", mod);
      }
      case ItemPool.HOT_JELLY,
          ItemPool.COLD_JELLY,
          ItemPool.SPOOKY_JELLY,
          ItemPool.SLEAZE_JELLY,
          ItemPool.STENCH_JELLY -> {
        if (adventureResults
            && KoLCharacter.currentFamiliar.getId() == FamiliarPool.SPACE_JELLYFISH) {
          Preferences.increment("_spaceJellyfishDrops");
        }
      }
      case ItemPool.LICENSE_TO_CHILL -> {
        if (adventureResults) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.LICENSE_TO_KILL, -11));
        }
      }
      case ItemPool.POKE_GROW_FERTILIZER -> {
        if (adventureResults) {
          Preferences.increment("_pokeGrowFertilizerDrops");
        }
      }
      case ItemPool.BOOMBOX -> KoLCharacter.addAvailableSkill(SkillPool.SING_ALONG);
      case ItemPool.GARLAND_OF_GREATNESS -> {
        if (adventureResults) {
          Preferences.increment("garlandUpgrades");
        }
      }
      case ItemPool.HUMAN_MUSK,
          ItemPool.EXTRA_WARM_FUR,
          ItemPool.INDUSTRIAL_LUBRICANT,
          ItemPool.UNFINISHED_PLEASURE,
          ItemPool.HUMANOID_GROWTH_HORMONE,
          ItemPool.BUG_LYMPH,
          ItemPool.ORGANIC_POTPOURRI,
          ItemPool.BOOT_FLASK,
          ItemPool.INFERNAL_SNOWBALL,
          ItemPool.POWDERED_MADNESS,
          ItemPool.FISH_SAUCE,
          ItemPool.GUFFIN,
          ItemPool.SHANTIX,
          ItemPool.GOODBERRY,
          ItemPool.EUCLIDEAN_ANGLE,
          ItemPool.PEPPERMINT_SYRUP,
          ItemPool.MERKIN_EYEDROPS,
          ItemPool.EXTRA_STRENGTH_GOO,
          ItemPool.ENVELOPE_MEAT,
          ItemPool.LIVID_ENERGY,
          ItemPool.MICRONOVA,
          ItemPool.BEGGIN_COLOGNE -> {
        if (adventureResults) {
          // The end of the fight will increment it to 0
          Preferences.setInteger("redSnapperProgress", -1);
        }
      }
      case ItemPool.POWDER_PUFF, ItemPool.FINEST_GOWN, ItemPool.DANCING_SHOES -> {
        if (InventoryManager.hasItem(ItemPool.POWDER_PUFF)
            && InventoryManager.hasItem(ItemPool.FINEST_GOWN)
            && InventoryManager.hasItem(ItemPool.DANCING_SHOES)) {
          QuestDatabase.setQuestProgress(Quest.SPOOKYRAVEN_DANCE, "step2");
        }
      }
      case ItemPool.BLACK_MAP -> QuestDatabase.setQuestProgress(Quest.BLACK, "step1");
      case ItemPool.FEDORA_MOUNTED_FOUNTAIN,
          ItemPool.PORKPIE_MOUNTED_POPPER,
          ItemPool.SOMBRERO_MOUNTED_SPARKLER ->
          Preferences.setBoolean("_fireworksShopHatBought", true);
      case ItemPool.CATHERINE_WHEEL, ItemPool.ROCKET_BOOTS, ItemPool.OVERSIZED_SPARKLER ->
          Preferences.setBoolean("_fireworksShopEquipmentBought", true);
      case ItemPool.VAMPIRE_VINTNER_WINE -> {
        Preferences.setInteger("vintnerCharge", 13);
        ItemDatabase.parseVampireVintnerWine();
      }
      case ItemPool.COSMIC_BOWLING_BALL -> {
        if (adventureResults) {
          BanishManager.resetCosmicBowlingBall();
          Preferences.setInteger("cosmicBowlingBallReturnCombats", -1);
        }
      }
      case ItemPool.MAYDAY_SUPPLY_PACKAGE -> {
        if (adventureResults) {
          Preferences.setBoolean("_maydayDropped", true);
        }
      }
      case ItemPool.ROBY_BORIS_BEER,
          ItemPool.ROBY_HONEY_BUN_OF_BORIS,
          ItemPool.ROBY_RATATOUILLE_DE_JARLSBERG,
          ItemPool.ROBY_JARLSBERGS_VEGETABLE_SOUP,
          ItemPool.ROBY_PETES_WILY_WHEY_BAR,
          ItemPool.ROBY_PETES_SNEAKY_SMOOTHIE,
          ItemPool.ROBY_BORIS_BREAD,
          ItemPool.ROBY_ROASTED_VEGETABLE_OF_J,
          ItemPool.ROBY_PETES_RICH_RICOTTA,
          ItemPool.ROBY_ROASTED_VEGETABLE_FOCACCIA,
          ItemPool.ROBY_PLAIN_CALZONE,
          ItemPool.ROBY_BAKED_VEGGIE_RICOTTA,
          ItemPool.ROBY_DEEP_DISH_OF_LEGEND,
          ItemPool.ROBY_CALZONE_OF_LEGEND,
          ItemPool.ROBY_PIZZA_OF_LEGEND -> {
        if (adventureResults) {
          Preferences.setBoolean("_cookbookbatRecipeDrops", true);
        }
      }
      case ItemPool.GRUBBY_WOOL -> {
        if (adventureResults
            && KoLCharacter.currentFamiliar.getId() == FamiliarPool.HOBO_IN_SHEEPS_CLOTHING) {
          Preferences.increment("_grubbyWoolDrops", 1);
        }
      }
      case ItemPool.SHADOW_LIGHTER,
          ItemPool.SHADOW_HEPTAHEDRON,
          ItemPool.SHADOW_SNOWFLAKE,
          ItemPool.SHADOW_HEART,
          ItemPool.SHADOW_BUCKET,
          ItemPool.SHADOW_WAVE ->
          QuestDatabase.setQuestProgress(Quest.RUFUS, "step1");
      case ItemPool.LED_CANDLE -> {
        if (adventureResults
            && KoLCharacter.currentFamiliar.getId() == FamiliarPool.JILL_OF_ALL_TRADES) {
          Preferences.setBoolean("ledCandleDropped", true);
        }
      }
      case ItemPool.MAP_TO_A_CANDY_RICH_BLOCK -> {
        if (adventureResults
            && KoLCharacter.currentFamiliar.getId() == FamiliarPool.JILL_OF_ALL_TRADES) {
          Preferences.increment("_mapToACandyRichBlockDrops", 1);
        }
      }
      case ItemPool.MINI_KIWI -> {
        if (adventureResults && KoLCharacter.currentFamiliar.getId() == FamiliarPool.MINI_KIWI) {
          Preferences.increment("_miniKiwiDrops", 1);
        }
      }
      case ItemPool.KNUCKLEBONE -> {
        if (adventureResults
            && KoLCharacter.currentFamiliar.getId() == FamiliarPool.SKELETON_OF_CRIMBO_PAST) {
          Preferences.increment("_knuckleboneDrops", 1, 100);
        }
      }
    }

    // Gaining items can achieve goals.
    GoalManager.updateProgress(result);
  }

  public static void autoCreate(final int itemId) {
    if (ResultProcessor.autoCrafting || !Preferences.getBoolean("autoCraft")) {
      return;
    }

    // If we are still in a choice, defer until after results have
    // been processed
    if (ChoiceManager.handlingChoice) {
      return;
    }

    ConcoctionDatabase.refreshConcoctionsNow();
    CreateItemRequest creator = CreateItemRequest.getInstance(itemId);

    // getQuantityPossible() should take meat paste or
    // Knoll Sign into account

    int possible = creator.getQuantityPossible();

    if (possible > 0) {
      ResultProcessor.autoCrafting = true;
      creator.setQuantityNeeded(1);
      RequestThread.postRequest(creator);
      ResultProcessor.autoCrafting = false;
    }
  }

  private static final Pattern HIPPY_PATTERN = Pattern.compile("we donated (\\d+) meat");
  public static boolean onlyAutosellDonationsCount = true;

  public static void handleDonations(final String urlString, final String responseText) {
    // Apparently, only autoselling items counts towards the trophy..
    if (ResultProcessor.onlyAutosellDonationsCount) {
      return;
    }

    // ITEMS

    // Dolphin King's map:
    //
    // The treasure includes some Meat, but you give it away to
    // some moist orphans. They need it to buy dry clothes.

    if (responseText.contains("give it away to moist orphans")) {
      KoLCharacter.makeCharitableDonation(150);
      return;
    }

    // chest of the Bonerdagon:
    //
    // The Cola Wars Veterans Administration is really gonna
    // appreciate the huge donation you're about to make!

    if (responseText.contains("Cola Wars Veterans Administration")) {
      KoLCharacter.makeCharitableDonation(3000);
      return;
    }

    // ancient vinyl coin purse
    //
    // You head into town and give the Meat to a guy wearing thick
    // glasses and a tie. Maybe now he'll be able to afford eye
    // surgery and a new wardrobe.
    //
    // black pension check
    //
    // You head back to the Black Forest and give the proceeds to
    // one of the black widows. Any given widow is more or less the
    // same as any other widow, right?
    //
    // old coin purse
    //
    // You wander around town until you find somebody named
    // Charity, and give her the Meat.
    //
    // old leather wallet
    //
    // You take the Meat to a soup kitchen and hand it to the first
    // person you see. He smelled bad, so he was probably a
    // volunteer.
    //
    // orcish meat locker
    //
    // You unlock the Meat locker with your rusty metal key, and
    // then dump the contents directly into a charity box at a
    // nearby convenience store. Those kids with boneitis are sure
    // to appreciate the gesture.
    //
    // Penultimate Fantasy chest
    //
    // There some Meat in it, but you drop it off the side of the
    // airship. It'll probably land on someone needy.
    //
    // Warm Subject gift certificate
    //
    // Then you walk next door to the hat store, and you give the
    // hat store all of your Meat. We need all kinds of things in
    // this economy.

    // QUESTS

    // Spooky Forest quest:
    //
    // Thanks for the larva, Adventurer. We'll put this to good use.

    if (responseText.contains("Thanks for the larva, Adventurer")
        && !responseText.contains("You gain")) {
      KoLCharacter.makeCharitableDonation(500);
      return;
    }

    // Wizard of Ego: from the "Other Class in the Guild" -> place=ocg
    // Nemesis: from the "Same Class in the Guild" -> place=scg
    //
    // You take the Meat into town and drop it in the donation slot
    // at the orphanage. You know, the one next to the library.

    if (responseText.contains("the one next to the library")) {
      int donation =
          urlString.contains("place=ocg") ? 500 : urlString.contains("place=scg") ? 1000 : 0;
      KoLCharacter.makeCharitableDonation(donation);
      return;
    }

    // Tr4pz0r quest:
    //
    // The furs you divide up between yourself and the Tr4pz0r, the
    // Meat you divide up between the Tr4pz0r and the needy.

    if (responseText.contains("you divide up between the Tr4pz0r and the needy")) {
      KoLCharacter.makeCharitableDonation(5000);
      return;
    }

    // Cap'n Caronch:
    //
    // (3000 meat with pirate fledges)

    // Post-filthworm orchard:
    //
    // Oh, hey, boss! Welcome back! Hey man, we don't want to
    // impose on your vow of poverty, so we donated 4248 meat from
    // our profits to the human fund in your honor. Thanks for
    // getting rid of those worms, man!

    Matcher matcher = ResultProcessor.HIPPY_PATTERN.matcher(responseText);
    if (matcher.find()) {
      int donation = StringUtilities.parseInt(matcher.group(1));
      KoLCharacter.makeCharitableDonation(donation);
      return;
    }
  }
}
