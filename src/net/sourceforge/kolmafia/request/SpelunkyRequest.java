package net.sourceforge.kolmafia.request;

import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.Stat;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import org.json.JSONException;
import org.json.JSONObject;

public class SpelunkyRequest extends GenericRequest {
  private static final Pattern MUSCLE_PATTERN =
      Pattern.compile("Mus:</td><td>(?:<font color=blue>|)<b>(\\d+)(?:</font> \\((\\d+)\\)|)</b>");
  private static final Pattern MOXIE_PATTERN =
      Pattern.compile("Mox:</td><td>(?:<font color=blue>|)<b>(\\d+)(?:</font> \\((\\d+)\\)|)</b>");
  private static final Pattern HP_PATTERN = Pattern.compile("HP:</td><td><b>(\\d+) / (\\d+)");
  private static final Pattern TURNS_PATTERN = Pattern.compile("Ghost'>(?:</a>)?<br><b>(\\d+)</b>");
  private static final Pattern GOLD_PATTERN =
      Pattern.compile("Gold: <b>\\$((\\d{1,3},\\d{3})|(\\d+))</b>");
  private static final Pattern BOMB_PATTERN =
      Pattern.compile("Bombs' width=30 height=30></td><td valign=center align=left><b>(\\d+)</b>");
  private static final Pattern ROPE_PATTERN =
      Pattern.compile("Ropes' width=30 height=30></td><td valign=center align=left><b>(\\d+)</b>");
  private static final Pattern KEY_PATTERN =
      Pattern.compile("Keys' width=30 height=30></td><td valign=center align=left><b>(\\d+)</b>");
  private static final Pattern BUDDY_PATTERN = Pattern.compile("Buddy:</b(?:.*)alt='(.*?)' ");
  private static final Pattern GEAR_SECTION_PATTERN = Pattern.compile("Gear:</b(.*?)</table>");
  private static final Pattern EQUIPMENT_PATTERN = Pattern.compile("descitem\\((\\d+)\\)");
  private static final Pattern SHOP_PATTERN = Pattern.compile("Buddy:</b(?:.*)alt='(.*?)' ");

  private static final Pattern TURNS_STATUS_PATTERN = Pattern.compile("Turns: (\\d+)");
  private static final Pattern GOLD_STATUS_PATTERN = Pattern.compile("Gold: (\\d+)");
  private static final Pattern BOMB_STATUS_PATTERN = Pattern.compile("Bombs: (\\d+)");
  private static final Pattern ROPE_STATUS_PATTERN = Pattern.compile("Ropes: (\\d+)");
  private static final Pattern KEY_STATUS_PATTERN = Pattern.compile("Keys: (\\d+)");
  private static final Pattern BUDDY_STATUS_PATTERN = Pattern.compile("Buddy: (.*?),");
  private static final Pattern UNLOCK_STATUS_PATTERN = Pattern.compile("Unlocks: (.*)");

  private static final String[][] BUDDY = {
    {"Skeleton", "spelbuddy1.gif", "Punches opponent"},
    {"Helpful Guy", "spelbuddy2.gif", "Delevels opponent"},
    {"Damselfly", "spelbuddy3.gif", "Heals after combat"},
    {"Resourceful Kid", "spelbuddy4.gif", "Finds extra gold"},
    {"Golden Monkey", "spelbuddy5.gif", "Finds extra gold"},
  };

  private static final AdventureResult WHIP = ItemPool.get(ItemPool.SPELUNKY_WHIP, 1);
  private static final AdventureResult MACHETE = ItemPool.get(ItemPool.SPELUNKY_MACHETE, 1);
  private static final AdventureResult SHOTGUN = ItemPool.get(ItemPool.SPELUNKY_SHOTGUN, 1);
  private static final AdventureResult YELLOW_CAPE = ItemPool.get(ItemPool.SPELUNKY_CAPE, 1);
  private static final AdventureResult JETPACK = ItemPool.get(ItemPool.SPELUNKY_JETPACK, 1);
  private static final AdventureResult XRAY_GOGGLES = ItemPool.get(ItemPool.SPELUNKY_GOGGLES, 1);
  private static final AdventureResult CLOWN_CROWN = ItemPool.get(ItemPool.SPELUNKY_CROWN, 1);
  private static final AdventureResult SPRING_BOOTS =
      ItemPool.get(ItemPool.SPELUNKY_SPRING_BOOTS, 1);
  private static final AdventureResult HEAVY_PICKAXE = ItemPool.get(ItemPool.SPELUNKY_PICKAXE, 1);
  private static final AdventureResult SKULL = ItemPool.get(ItemPool.SPELUNKY_SKULL, 1);
  private static final AdventureResult ROCK = ItemPool.get(ItemPool.SPELUNKY_ROCK, 1);
  private static final AdventureResult POT = ItemPool.get(ItemPool.SPELUNKY_POT, 1);
  private static final AdventureResult TORCH = ItemPool.get(ItemPool.SPELUNKY_TORCH, 1);

  private static final AdventureResult[] ITEMS = {
    // Weapons
    SpelunkyRequest.WHIP,
    SpelunkyRequest.MACHETE,
    SpelunkyRequest.SHOTGUN,
    ItemPool.get(ItemPool.SPELUNKY_BOOMERANG, 1),
    ItemPool.get(ItemPool.SPELUNKY_RIFLE, 1),
    ItemPool.get(ItemPool.SPELUNKY_STAFF, 1),

    // Off-hand items
    SpelunkyRequest.SKULL,
    SpelunkyRequest.ROCK,
    SpelunkyRequest.POT,
    SpelunkyRequest.HEAVY_PICKAXE,
    SpelunkyRequest.TORCH,
    ItemPool.get(ItemPool.SPELUNKY_COFFEE_CUP, 1),
    ItemPool.get(ItemPool.SPELUNKY_JOKE_BOOK, 1),

    // Hats
    ItemPool.get(ItemPool.SPELUNKY_FEDORA, 1),
    ItemPool.get(ItemPool.SPELUNKY_HELMET, 1),
    SpelunkyRequest.XRAY_GOGGLES,
    SpelunkyRequest.CLOWN_CROWN,

    // Back items
    SpelunkyRequest.YELLOW_CAPE,
    SpelunkyRequest.JETPACK,

    // Accessories
    SpelunkyRequest.SPRING_BOOTS,
    ItemPool.get(ItemPool.SPELUNKY_SPIKED_BOOTS, 1),
  };

  private static final HashMap<String, String> adventureImages = new HashMap<String, String>();

  static {
    SpelunkyRequest.adventureImages.put("The Mines", "mines.gif");
    SpelunkyRequest.adventureImages.put("The Jungle", "jungle.gif");
    SpelunkyRequest.adventureImages.put("The Ice Caves", "icecaves.gif");
    SpelunkyRequest.adventureImages.put("The Temple Ruins", "templeruins.gif");
    SpelunkyRequest.adventureImages.put("Hell", "heckofirezzz.gif");
    SpelunkyRequest.adventureImages.put("The Snake Pit", "snakepit.gif");
    SpelunkyRequest.adventureImages.put("The Spider Hole", "spiderhole.gif");
    SpelunkyRequest.adventureImages.put("The Ancient Burial Ground", "burialground.gif");
    SpelunkyRequest.adventureImages.put("The Beehive", "beehive.gif");
    SpelunkyRequest.adventureImages.put("The Crashed U.F.O.", "ufo.gif");
    SpelunkyRequest.adventureImages.put("The City of Goooold", "citygold.gif");
    SpelunkyRequest.adventureImages.put("LOLmec's Lair", "lolmec.gif");
    SpelunkyRequest.adventureImages.put("Yomama's Throne", "yomama.gif");
  }

  public static String adventureImage(KoLAdventure adventure) {
    return SpelunkyRequest.adventureImage(adventure.getAdventureName());
  }

  public static String adventureImage(String adventureName) {
    String image = SpelunkyRequest.adventureImages.get(adventureName);
    return image == null ? null : "otherimages/spelunky/" + image;
  }

  public SpelunkyRequest() {
    super("place.php");
  }

  public static void reset() {
    Preferences.resetToDefault("spelunkyNextNoncombat");
    Preferences.resetToDefault("spelunkySacrifices");
    Preferences.resetToDefault("spelunkyStatus");
    Preferences.resetToDefault("spelunkyWinCount");
    SpelunkyRequest.resetItems();
  }

  public static void resetItems() {
    EquipmentManager.removeAllEquipment();
    for (AdventureResult item : SpelunkyRequest.ITEMS) {
      int count = item.getCount(KoLConstants.inventory);
      if (count > 0) {
        AdventureResult result = item.getInstance(-count);
        AdventureResult.addResultToList(KoLConstants.inventory, result);
        AdventureResult.addResultToList(KoLConstants.tally, result);
      }
    }
  }

  public static void parseCharpane(final String responseText) {
    if (!responseText.contains(">Last Spelunk</a>")) {
      return;
    }

    CharPaneRequest.parseAvatar(responseText);

    boolean ghostWaving = false;

    String spelunkyStatus = Preferences.getString("spelunkyStatus");

    int buffedMus = 0;
    int buffedMox = 0;
    long baseMusPoints = 0;
    long baseMoxPoints = 0;
    Matcher matcher = SpelunkyRequest.MUSCLE_PATTERN.matcher(responseText);
    if (matcher.find()) {
      buffedMus = StringUtilities.parseInt(matcher.group(1));
      int baseMus = buffedMus;
      if (matcher.group(2) != null) {
        baseMus = StringUtilities.parseInt(matcher.group(2));
      }
      baseMusPoints = KoLCharacter.calculatePointSubpoints(baseMus);
    }
    matcher = SpelunkyRequest.MOXIE_PATTERN.matcher(responseText);
    if (matcher.find()) {
      buffedMox = StringUtilities.parseInt(matcher.group(1));
      int baseMox = buffedMox;
      if (matcher.group(2) != null) {
        baseMox = StringUtilities.parseInt(matcher.group(2));
      }
      baseMoxPoints = KoLCharacter.calculatePointSubpoints(baseMox);
    }
    KoLCharacter.setStatPoints(buffedMus, baseMusPoints, 0, 0, buffedMox, baseMoxPoints);
    matcher = SpelunkyRequest.HP_PATTERN.matcher(responseText);
    if (matcher.find()) {
      int currentHP = StringUtilities.parseInt(matcher.group(1));
      int maximumHP = StringUtilities.parseInt(matcher.group(2));
      KoLCharacter.setHP(currentHP, maximumHP, maximumHP);
    }
    matcher = SpelunkyRequest.TURNS_PATTERN.matcher(responseText);
    int turnsLeft = matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : 0;
    matcher = SpelunkyRequest.GOLD_PATTERN.matcher(responseText);
    int gold = matcher.find() ? StringUtilities.parseInt(matcher.group(1).replaceAll(",", "")) : 0;
    matcher = SpelunkyRequest.BOMB_PATTERN.matcher(responseText);
    int bombs = matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : 0;
    matcher = SpelunkyRequest.ROPE_PATTERN.matcher(responseText);
    int ropes = matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : 0;
    matcher = SpelunkyRequest.KEY_PATTERN.matcher(responseText);
    int keys = matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : 0;
    matcher = SpelunkyRequest.BUDDY_PATTERN.matcher(responseText);
    String buddy = matcher.find() ? matcher.group(1) : "";
    matcher = SpelunkyRequest.UNLOCK_STATUS_PATTERN.matcher(spelunkyStatus);
    String unlocks = matcher.find() ? matcher.group(1) : "";
    matcher = SpelunkyRequest.GEAR_SECTION_PATTERN.matcher(responseText);
    String gear = matcher.find() ? matcher.group(1) : "";
    matcher = SpelunkyRequest.EQUIPMENT_PATTERN.matcher(gear);
    while (matcher.find()) {
      String descId = matcher.group(1);
      int itemId = ItemDatabase.getItemIdFromDescription(descId);
      AdventureResult item = ItemPool.get(itemId, 1);
      switch (ItemDatabase.getConsumptionType(itemId)) {
        case KoLConstants.EQUIP_HAT:
          EquipmentManager.setEquipment(EquipmentManager.HAT, item);
          break;
        case KoLConstants.EQUIP_WEAPON:
          EquipmentManager.setEquipment(EquipmentManager.WEAPON, item);
          break;
        case KoLConstants.EQUIP_OFFHAND:
          EquipmentManager.setEquipment(EquipmentManager.OFFHAND, item);
          switch (itemId) {
            case ItemPool.SPELUNKY_SKULL:
              KoLCharacter.addAvailableSkill("Throw Skull");
              KoLCharacter.removeAvailableSkill("Throw Rock");
              KoLCharacter.removeAvailableSkill("Throw Pot");
              KoLCharacter.removeAvailableSkill("Throw Torch");
              break;
            case ItemPool.SPELUNKY_ROCK:
              KoLCharacter.addAvailableSkill("Throw Rock");
              KoLCharacter.removeAvailableSkill("Throw Skull");
              KoLCharacter.removeAvailableSkill("Throw Pot");
              KoLCharacter.removeAvailableSkill("Throw Torch");
              break;
            case ItemPool.SPELUNKY_POT:
              KoLCharacter.addAvailableSkill("Throw Pot");
              KoLCharacter.removeAvailableSkill("Throw Rock");
              KoLCharacter.removeAvailableSkill("Throw Skull");
              KoLCharacter.removeAvailableSkill("Throw Torch");
              break;
            case ItemPool.SPELUNKY_TORCH:
              KoLCharacter.addAvailableSkill("Throw Torch");
              KoLCharacter.removeAvailableSkill("Throw Rock");
              KoLCharacter.removeAvailableSkill("Throw Skull");
              KoLCharacter.removeAvailableSkill("Throw Pot");
              break;
            case ItemPool.SPELUNKY_COFFEE_CUP:
            case ItemPool.SPELUNKY_PICKAXE:
              KoLCharacter.removeAvailableSkill("Throw Rock");
              KoLCharacter.removeAvailableSkill("Throw Skull");
              KoLCharacter.removeAvailableSkill("Throw Pot");
              KoLCharacter.removeAvailableSkill("Throw Torch");
              break;
          }
          break;
        case KoLConstants.EQUIP_CONTAINER:
          EquipmentManager.setEquipment(EquipmentManager.CONTAINER, item);
          break;
        case KoLConstants.EQUIP_ACCESSORY:
          EquipmentManager.setEquipment(EquipmentManager.ACCESSORY1, item);
          break;
      }
    }
    if (gear.contains(">hat<")) {
      EquipmentManager.setEquipment(EquipmentManager.HAT, EquipmentRequest.UNEQUIP);
    }
    if (gear.contains(">off<")) {
      EquipmentManager.setEquipment(EquipmentManager.OFFHAND, EquipmentRequest.UNEQUIP);
      KoLCharacter.removeAvailableSkill("Throw Rock");
      KoLCharacter.removeAvailableSkill("Throw Skull");
      KoLCharacter.removeAvailableSkill("Throw Pot");
      KoLCharacter.removeAvailableSkill("Throw Torch");
    }
    if (gear.contains(">back<")) {
      EquipmentManager.setEquipment(EquipmentManager.CONTAINER, EquipmentRequest.UNEQUIP);
    }
    if (gear.contains(">hand<")) {
      EquipmentManager.setEquipment(EquipmentManager.WEAPON, EquipmentRequest.UNEQUIP);
    }
    if (gear.contains(">shoes<")) {
      EquipmentManager.setEquipment(EquipmentManager.ACCESSORY1, EquipmentRequest.UNEQUIP);
      EquipmentManager.setEquipment(EquipmentManager.ACCESSORY2, EquipmentRequest.UNEQUIP);
      EquipmentManager.setEquipment(EquipmentManager.ACCESSORY3, EquipmentRequest.UNEQUIP);
    }

    if (responseText.contains("spelghostarms.gif")) {
      ghostWaving = true;
    }

    StringBuffer newUnlocks = new StringBuffer(unlocks);
    if (responseText.contains("'Sticky Bombs'") && !unlocks.contains("Sticky Bombs")) {
      newUnlocks.append(", Sticky Bombs");
    }

    // Make right skills available based on resources
    if (bombs > 0) {
      KoLCharacter.addAvailableSkill("Throw Bomb");
    } else {
      KoLCharacter.removeAvailableSkill("Throw Bomb");
    }
    if (bombs >= 10) {
      KoLCharacter.addAvailableSkill("Throw Ten Bombs");
    } else {
      KoLCharacter.removeAvailableSkill("Throw Ten Bombs");
    }
    if (ropes > 0) {
      KoLCharacter.addAvailableSkill("Use Rope");
    } else {
      KoLCharacter.removeAvailableSkill("Use Rope");
    }

    // Have we gained a buddy? Log it
    if (turnsLeft != 40 && !buddy.equals("") && !spelunkyStatus.contains(buddy)) {
      String message = "You have found a new Buddy, " + buddy;
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
    }

    // Write status string
    StringBuilder statusString = new StringBuilder();
    statusString.append("Turns: ");
    statusString.append(turnsLeft);
    if (ghostWaving) {
      statusString.append(", Non-combat Due");
    }
    statusString.append(", Gold: ");
    statusString.append(gold);
    statusString.append(", Bombs: ");
    statusString.append(bombs);
    statusString.append(", Ropes: ");
    statusString.append(ropes);
    statusString.append(", Keys: ");
    statusString.append(keys);
    statusString.append(", Buddy: ");
    statusString.append(buddy);
    statusString.append(", Unlocks: ");
    statusString.append(newUnlocks);
    Preferences.setString("spelunkyStatus", statusString.toString());

    String upgradeString = Preferences.getString("spelunkyUpgrades");
    StringBuilder newUpgradeString = new StringBuilder(upgradeString);

    // If we have all upgrades, no point looking at upgrades
    // If first turn, update upgrades unlocked
    if (!upgradeString.equals("YYYYYYYYY") && turnsLeft == 40) {
      if (gold == 100) {
        newUpgradeString.replace(3, 6, "YYY");
      } else if (bombs == 3) {
        newUpgradeString.replace(3, 4, "Y");
      }
      if (keys == 1) {
        newUpgradeString.replace(6, 9, "YYY");
      } else if (responseText.contains("hobofedora.gif")) {
        newUpgradeString.replace(6, 8, "YY");
      } else if (ropes == 3) {
        newUpgradeString.replace(6, 7, "Y");
      }
      Preferences.setString("spelunkyUpgrades", newUpgradeString.toString());
    }
    KoLCharacter.recalculateAdjustments();
    KoLCharacter.updateStatus();
  }

  public static final void parseStatus(final JSONObject JSON) throws JSONException {
    // api.php is not a complete replacement for charpane.php in
    // Spelunky, but parse equipment, at least.

    JSONObject equip = JSON.getJSONObject("equipment");
    Iterator keys = equip.keys();
    while (keys.hasNext()) {
      String slotName = (String) keys.next();
      if (slotName.equals("fakehands")) {
        continue;
      }

      int slot = EquipmentRequest.phpSlotNumber(slotName);
      if (slot == -1) {
        continue;
      }

      int itemId = equip.getInt(slotName);
      AdventureResult item = EquipmentManager.equippedItem(itemId);

      switch (slot) {
        case EquipmentManager.HAT:
          EquipmentManager.setEquipment(EquipmentManager.HAT, item);
          break;
        case EquipmentManager.WEAPON:
          EquipmentManager.setEquipment(EquipmentManager.WEAPON, item);
          break;
        case EquipmentManager.OFFHAND:
          EquipmentManager.setEquipment(EquipmentManager.OFFHAND, item);
          switch (itemId) {
            case ItemPool.SPELUNKY_SKULL:
              KoLCharacter.addAvailableSkill("Throw Skull");
              KoLCharacter.removeAvailableSkill("Throw Rock");
              KoLCharacter.removeAvailableSkill("Throw Pot");
              KoLCharacter.removeAvailableSkill("Throw Torch");
              break;
            case ItemPool.SPELUNKY_ROCK:
              KoLCharacter.addAvailableSkill("Throw Rock");
              KoLCharacter.removeAvailableSkill("Throw Skull");
              KoLCharacter.removeAvailableSkill("Throw Pot");
              KoLCharacter.removeAvailableSkill("Throw Torch");
              break;
            case ItemPool.SPELUNKY_POT:
              KoLCharacter.addAvailableSkill("Throw Pot");
              KoLCharacter.removeAvailableSkill("Throw Rock");
              KoLCharacter.removeAvailableSkill("Throw Skull");
              KoLCharacter.removeAvailableSkill("Throw Torch");
              break;
            case ItemPool.SPELUNKY_TORCH:
              KoLCharacter.addAvailableSkill("Throw Torch");
              KoLCharacter.removeAvailableSkill("Throw Rock");
              KoLCharacter.removeAvailableSkill("Throw Skull");
              KoLCharacter.removeAvailableSkill("Throw Pot");
              break;
            default:
              KoLCharacter.removeAvailableSkill("Throw Rock");
              KoLCharacter.removeAvailableSkill("Throw Skull");
              KoLCharacter.removeAvailableSkill("Throw Pot");
              KoLCharacter.removeAvailableSkill("Throw Torch");
              break;
          }
          break;
        case EquipmentManager.CONTAINER:
          EquipmentManager.setEquipment(EquipmentManager.CONTAINER, item);
          break;
        case EquipmentManager.ACCESSORY1:
          EquipmentManager.setEquipment(EquipmentManager.ACCESSORY1, item);
          break;
      }
    }

    KoLCharacter.recalculateAdjustments();
    KoLCharacter.updateStatus();
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.contains("whichplace=spelunky")) {
      return;
    }

    String spelunkyStatus = Preferences.getString("spelunkyStatus");

    Matcher matcher = SpelunkyRequest.TURNS_STATUS_PATTERN.matcher(spelunkyStatus);
    int turnsLeft = matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : 0;
    matcher = SpelunkyRequest.GOLD_STATUS_PATTERN.matcher(spelunkyStatus);
    int gold = matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : 0;
    matcher = SpelunkyRequest.BOMB_STATUS_PATTERN.matcher(spelunkyStatus);
    int bombs = matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : 0;
    matcher = SpelunkyRequest.ROPE_STATUS_PATTERN.matcher(spelunkyStatus);
    int ropes = matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : 0;
    matcher = SpelunkyRequest.KEY_STATUS_PATTERN.matcher(spelunkyStatus);
    int keys = matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : 0;
    matcher = SpelunkyRequest.BUDDY_STATUS_PATTERN.matcher(spelunkyStatus);
    String buddy = matcher.find() ? matcher.group(1) : "";
    matcher = SpelunkyRequest.UNLOCK_STATUS_PATTERN.matcher(spelunkyStatus);
    String unlocks = matcher.find() ? matcher.group(1) : "";
    boolean ghostWaving = spelunkyStatus.contains("Non-combat Due");

    boolean jungleUnlocked =
        unlocks.contains("Jungle") || responseText.contains("spelunky/jungle.gif");
    boolean iceCavesUnlocked =
        unlocks.contains("Ice Caves") || responseText.contains("spelunky/icecaves.gif");
    boolean templeRuinsUnlocked =
        unlocks.contains("Temple Ruins") || responseText.contains("spelunky/templeruins.gif");
    boolean snakePitUnlocked =
        unlocks.contains("Snake Pit") || responseText.contains("spelunky/snakepit.gif");
    boolean spiderHoleUnlocked =
        unlocks.contains("Spider Hole") || responseText.contains("spelunky/spiderhole.gif");
    boolean burialGroundUnlocked =
        unlocks.contains("Burial Ground") || responseText.contains("spelunky/burialground.gif");
    boolean beehiveUnlocked =
        unlocks.contains("Beehive") || responseText.contains("spelunky/beehive.gif");
    boolean crashedUFOUnlocked =
        unlocks.contains("Crashed UFO") || responseText.contains("spelunky/ufo.gif");
    boolean altarUnlocked =
        unlocks.contains("Altar") || responseText.contains("spelunky/altar.gif");
    boolean cityOfGooooldUnlocked =
        unlocks.contains("City of Goooold") || responseText.contains("spelunky/citygold.gif");
    boolean LOLmecLairUnlocked =
        unlocks.contains("LOLmec's Lair") || responseText.contains("spelunky/lolmec.gif");
    boolean HellUnlocked =
        unlocks.contains("Hell") || responseText.contains("spelunky/heckofirezzz.gif");
    boolean YomamaThroneUnlocked =
        unlocks.contains("Yomama's Throne") || responseText.contains("spelunky/yomama.gif");

    StringBuffer newUnlocks = new StringBuffer(unlocks);
    if (jungleUnlocked && !unlocks.contains("Jungle")) {
      if (!unlocks.equals("")) {
        newUnlocks.append(", ");
      }
      newUnlocks.append("Jungle");
    }
    if (iceCavesUnlocked && !unlocks.contains("Ice Caves")) {
      if (!unlocks.equals("") || newUnlocks.length() > 0) {
        newUnlocks.append(", ");
      }
      newUnlocks.append("Ice Caves");
    }
    if (templeRuinsUnlocked && !unlocks.contains("Temple Ruins")) {
      if (!unlocks.equals("") || newUnlocks.length() > 0) {
        newUnlocks.append(", ");
      }
      newUnlocks.append("Temple Ruins");
    }
    if (snakePitUnlocked && !unlocks.contains("SnakePit")) {
      if (!unlocks.equals("") || newUnlocks.length() > 0) {
        newUnlocks.append(", ");
      }
      newUnlocks.append("Snake Pit");
    }
    if (spiderHoleUnlocked && !unlocks.contains("Spider Hole")) {
      if (!unlocks.equals("") || newUnlocks.length() > 0) {
        newUnlocks.append(", ");
      }
      newUnlocks.append("Spider Hole");
    }
    if (beehiveUnlocked && !unlocks.contains("Beehive")) {
      if (!unlocks.equals("") || newUnlocks.length() > 0) {
        newUnlocks.append(", ");
      }
      newUnlocks.append("Beehive");
    }
    if (burialGroundUnlocked && !unlocks.contains("Burial Ground")) {
      if (!unlocks.equals("") || newUnlocks.length() > 0) {
        newUnlocks.append(", ");
      }
      newUnlocks.append("Burial Ground");
    }
    if (crashedUFOUnlocked && !unlocks.contains("Crashed UFO")) {
      if (!unlocks.equals("") || newUnlocks.length() > 0) {
        newUnlocks.append(", ");
      }
      newUnlocks.append("Crashed UFO");
    }
    if (altarUnlocked && !unlocks.contains("Altar")) {
      if (!unlocks.equals("") || newUnlocks.length() > 0) {
        newUnlocks.append(", ");
      }
      newUnlocks.append("Altar");
    }
    if (cityOfGooooldUnlocked && !unlocks.contains("City of Goooold")) {
      if (!unlocks.equals("") || newUnlocks.length() > 0) {
        newUnlocks.append(", ");
      }
      newUnlocks.append("City of Goooold");
    }
    if (LOLmecLairUnlocked && !unlocks.contains("LOLmec's Lair")) {
      if (!unlocks.equals("") || newUnlocks.length() > 0) {
        newUnlocks.append(", ");
      }
      newUnlocks.append("LOLmec's Lair");
    }
    if (HellUnlocked && !unlocks.contains("Hell")) {
      if (!unlocks.equals("") || newUnlocks.length() > 0) {
        newUnlocks.append(", ");
      }
      newUnlocks.append("Hell");
    }
    if (YomamaThroneUnlocked && !unlocks.contains("Yomama's Throne")) {
      if (!unlocks.equals("") || newUnlocks.length() > 0) {
        newUnlocks.append(", ");
      }
      newUnlocks.append("Yomama's Throne");
    }

    // Write status string
    StringBuilder statusString = new StringBuilder();
    statusString.append("Turns: ");
    statusString.append(turnsLeft);
    if (ghostWaving) {
      statusString.append(", Non-combat Due");
    }
    statusString.append(", Gold: ");
    statusString.append(gold);
    statusString.append(", Bombs: ");
    statusString.append(bombs);
    statusString.append(", Ropes: ");
    statusString.append(ropes);
    statusString.append(", Keys: ");
    statusString.append(keys);
    statusString.append(", Buddy: ");
    statusString.append(buddy);
    statusString.append(", Unlocks: ");
    statusString.append(newUnlocks);
    Preferences.setString("spelunkyStatus", statusString.toString());

    String upgradeString = Preferences.getString("spelunkyUpgrades");
    StringBuilder newUpgradeString = new StringBuilder(upgradeString);

    // If we have all upgrades, no point looking at upgrades
    // If first turn, update unlocks
    if (!upgradeString.equals("YYYYYYYYY") && turnsLeft == 40) {
      if (iceCavesUnlocked) {
        newUpgradeString.replace(0, 2, "YY");
      } else if (jungleUnlocked) {
        newUpgradeString.replace(0, 1, "Y");
      }
      Preferences.setString("spelunkyUpgrades", newUpgradeString.toString());
    }
  }

  public static void wonFight(final String monsterName, final String responseText) {
    // Check for unlocks
    if (responseText.contains("New Area Unlocked")) {
      if (responseText.contains("The Jungle")) {
        SpelunkyRequest.unlock("The Jungle", "Jungle");
      }
      if (responseText.contains("The Ice Caves")) {
        SpelunkyRequest.unlock("The Ice Caves", "Ice Caves");
      }
      if (responseText.contains("The Temple Ruins")) {
        SpelunkyRequest.unlock("The Temple Ruins", "Temple Ruins");
      }
      if (responseText.contains("LOLmec's Lair")) {
        SpelunkyRequest.unlock("LOLmec's Lair", "LOLmec's Lair");
      }
    }
    if (monsterName.equals("spider queen")) {
      SpelunkyRequest.spiderQueenDefeated();
    }
    if (!monsterName.equals("shopkeeper") && !monsterName.equals("ghost (Spelunky)")) {
      SpelunkyRequest.incrementWinCount();
    }
  }

  public static void spiderQueenDefeated() {
    String spelunkyStatus = Preferences.getString("spelunkyStatus");
    if (!spelunkyStatus.contains("Sticky Bombs")) {
      Preferences.setString("spelunkyStatus", (spelunkyStatus + ", Sticky Bombs"));
      String message = "You have unlocked Sticky Bombs";
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
    }
  }

  public static void incrementNonCombatPhase() {
    if (Preferences.increment("spelunkyNextNoncombat", 1) > 3) {
      Preferences.setInteger("spelunkyNextNoncombat", 1);
    }
  }

  public static void incrementWinCount() {
    // Once the win count reaches 3, a noncombat is available.  If
    // you adventure in Hell, which does not have a non-combat, the
    // noncombat continues to be available. If you do not take it
    // before winning 3 more fights, a noncombat remains available
    // - but at the next phase step.
    if (Preferences.increment("spelunkyWinCount") == 6) {
      SpelunkyRequest.incrementNonCombatPhase();
      Preferences.setInteger("spelunkyWinCount", 3);
    }
  }

  public static void parseChoice(final int choice, final String responseText, final int decision) {
    // Sacrifice doesn't increment win count or counter
    if (choice != 1040 && choice != 1041) {
      // If you win more than three fights before taking a
      // noncombat, the extra wins count towards the next
      // noncombat
      Preferences.decrement("spelunkyWinCount", 3);

      // Shopkeeper doesn't increment the counter til you leave or fight
      if (choice != 1028 || decision >= 5) {
        SpelunkyRequest.incrementNonCombatPhase();
      }
    }

    switch (choice) {
      case 1030:
        // It's a Trap!  A Dart Trap
        if (responseText.contains("The Spider Hole")) {
          SpelunkyRequest.unlock("The Spider Hole", "Spider Hole");
        } else if (responseText.contains("The Snake Pit")) {
          SpelunkyRequest.unlock("The Snake Pit", "Snake Pit");
        }
        break;
      case 1032:
        // It's a Trap!  A Tiki Trap.
        if (responseText.contains("The Ancient Burial Ground")) {
          SpelunkyRequest.unlock("The Ancient Burial Ground", "Burial Ground");
        } else if (responseText.contains("The Beehive")) {
          SpelunkyRequest.unlock("The Beehive", "Beehive");
        }
        break;
      case 1034:
        // A Landmine
        if (responseText.contains("An Ancient Altar")) {
          SpelunkyRequest.unlock("An Ancient Altar", "Altar");
        } else if (responseText.contains("The Crashed U. F. O.")) {
          SpelunkyRequest.unlock("The Crashed U. F. O.", "Crashed UFO");
        }
        break;

      case 1037:
        // It's a Trap!  A Smashy Trap.
        if (responseText.contains("The City of Goooold")) {
          SpelunkyRequest.unlock("The City of Goooold", "City of Goooold");
        }
        break;

      case 1041:
        // Spelunkrifice
        if (decision == 1) {
          String spelunkyStatus = Preferences.getString("spelunkyStatus");
          Matcher matcher = SpelunkyRequest.BUDDY_STATUS_PATTERN.matcher(spelunkyStatus);
          String buddy = matcher.find() ? matcher.group(1) : "";

          String message = "You have sacrificed your Buddy, " + buddy;
          RequestLogger.printLine(message);
          RequestLogger.updateSessionLog(message);
          Preferences.increment("spelunkySacrifices", 1);
        }
        break;
    }
    SpelunkyRequest.gainGold(responseText);
  }

  public static void unlock(final String logLocation, final String prefLocation) {
    String spelunkyStatus = Preferences.getString("spelunkyStatus");
    if (!spelunkyStatus.contains(prefLocation)) {
      Preferences.setString("spelunkyStatus", (spelunkyStatus + ", " + prefLocation));
      String message = "You have unlocked " + logLocation;
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
    }
  }

  private static final Pattern GOLD_GAIN_PATTERN =
      Pattern.compile("(?:goldnug.gif|coinpurse.gif|lolmecidol.gif) (?:.*?)+<b>(\\d+) Gold!</b>");

  private static void gainGold(final String responseText) {
    Matcher goldMatcher = SpelunkyRequest.GOLD_GAIN_PATTERN.matcher(responseText);
    while (goldMatcher.find()) {
      String gain = goldMatcher.group(1);
      String updateMessage = "You gain " + gain + " gold";
      RequestLogger.updateSessionLog(updateMessage);
      KoLmafia.updateDisplay(updateMessage);
    }
  }

  public static void upgrade(final int choice) {
    String upgradeString = Preferences.getString("spelunkyUpgrades");
    StringBuilder newUpgradeString = new StringBuilder(upgradeString);

    if (choice >= 1 && choice <= 9 && !upgradeString.equals("YYYYYYYYY")) {
      newUpgradeString.replace(choice - 1, choice, "Y");

      // Log upgrade
      StringBuilder upgradeMessage = new StringBuilder();
      upgradeMessage.append("Spelunky Finished. Upgrade chosen is ");
      switch (choice) {
        case 1:
          upgradeMessage.append("Unlock Jungle.");
          break;
        case 2:
          upgradeMessage.append("Unlock Ice Caves.");
          break;
        case 3:
          upgradeMessage.append("Unlock Temple Ruins.");
          break;
        case 4:
          upgradeMessage.append("Start with +2 bombs.");
          break;
        case 5:
          upgradeMessage.append("More Shopkeeper items for sale.");
          break;
        case 6:
          upgradeMessage.append("Begin with 100 gold.");
          break;
        case 7:
          upgradeMessage.append("Start with +2 Ropes.");
          break;
        case 8:
          upgradeMessage.append("Start with Fedora.");
          break;
        case 9:
          upgradeMessage.append("Start with key.");
          break;
      }
      String message = upgradeMessage.toString();
      RequestLogger.printLine();
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog();
      RequestLogger.updateSessionLog(message);
    }
    Preferences.setString("spelunkyUpgrades", newUpgradeString.toString());
  }

  public static void logShop(final String responseText, final int decision) {
    // We are choosing to buy from shop
    Matcher matcher = ChoiceManager.DECISION_BUTTON_PATTERN.matcher(responseText);
    while (matcher.find()) {
      int choice = StringUtilities.parseInt(matcher.group(1));
      String choiceText = matcher.group(2);

      if (choice == decision && choice != 6) {
        String message = "Buying" + choiceText.substring(3);
        RequestLogger.printLine(message);
        RequestLogger.updateSessionLog(message);
      }
      String upgradeString = Preferences.getString("spelunkyUpgrades");
      if (choice == 4 && !upgradeString.equals("YYYYYYYYY")) {
        StringBuilder newUpgradeString = new StringBuilder(upgradeString);
        newUpgradeString.replace(4, 5, "Y");
        Preferences.setString("spelunkyUpgrades", newUpgradeString.toString());
      }
    }
  }

  public static String getBuddyName() {
    String spelunkyStatus = Preferences.getString("spelunkyStatus");
    Matcher matcher = SpelunkyRequest.BUDDY_STATUS_PATTERN.matcher(spelunkyStatus);
    String buddy = matcher.find() ? matcher.group(1) : null;
    return buddy;
  }

  public static String getBuddyImageName() {
    String buddy = SpelunkyRequest.getBuddyName();
    if (buddy == null) {
      return null;
    }
    for (String[] s : BUDDY) {
      if (buddy.contains(s[0])) {
        return s[1];
      }
    }
    return null;
  }

  public static String getBuddyRole() {
    String buddy = SpelunkyRequest.getBuddyName();
    if (buddy == null) {
      return null;
    }
    for (String[] s : BUDDY) {
      if (buddy.contains(s[0])) {
        return s[2];
      }
    }
    return null;
  }

  public static int getGold() {
    String spelunkyStatus = Preferences.getString("spelunkyStatus");
    Matcher matcher = SpelunkyRequest.GOLD_STATUS_PATTERN.matcher(spelunkyStatus);
    return matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : 0;
  }

  public static int getBomb() {
    String spelunkyStatus = Preferences.getString("spelunkyStatus");
    Matcher matcher = SpelunkyRequest.BOMB_STATUS_PATTERN.matcher(spelunkyStatus);
    return matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : 0;
  }

  public static int getRope() {
    String spelunkyStatus = Preferences.getString("spelunkyStatus");
    Matcher matcher = SpelunkyRequest.ROPE_STATUS_PATTERN.matcher(spelunkyStatus);
    return matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : 0;
  }

  public static int getKey() {
    String spelunkyStatus = Preferences.getString("spelunkyStatus");
    Matcher matcher = SpelunkyRequest.KEY_STATUS_PATTERN.matcher(spelunkyStatus);
    return matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : 0;
  }

  public static int getTurnsLeft() {
    String spelunkyStatus = Preferences.getString("spelunkyStatus");
    Matcher matcher = SpelunkyRequest.TURNS_STATUS_PATTERN.matcher(spelunkyStatus);
    return matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : 0;
  }

  public static boolean registerRequest(final String urlString) {
    // place.php?whichplace=spelunky

    String action = GenericRequest.getAction(urlString);
    if (action == null) {
      return true;
    }

    String location = null;

    if (action.equals("spelunky_camp")) {
      location = urlString.contains("ghostyghostghost=clown") ? "Base Camp" : "Rest at Base Camp";
    } else if (action.equals("spelunky_side6")) {
      location = "The Altar";
    } else if (action.equals("spelunky_quit")) {
      location = "Exit";
    } else if (action.equals("spelunky_board")) {
      return true;
    } else {
      return false;
    }

    String message = "{" + SpelunkyRequest.getTurnsLeft() + "} " + location;

    RequestLogger.printLine();
    RequestLogger.printLine(message);

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog(message);

    return true;
  }

  public static final void decorateSpelunkyExit(final StringBuffer buffer) {
    int index = buffer.indexOf("<center><A href=main.php>Back to the Main Map</a></center>");
    if (index == -1) {
      return;
    }
    index += 8;

    String section =
        "<a href=\""
            + "inv_use.php?pwd="
            + GenericRequest.passwordHash
            + "&which=3&whichitem=8063"
            + "\">"
            + "Read another copy of Tales of Spelunking"
            + "</a></center><center><p>";
    buffer.insert(index, section);
  }

  public static final void decorateSpelunkyMonster(final StringBuffer buffer) {
    // Simplified, since Skills, Elemental Damage, and Bonus
    // Critical Hits are not applicable

    // There are special combat rules in effect during a Spelunking adventure:
    //
    // * You never miss or fumble.
    // * The monster never misses or fumbles.

    // Player stats
    int muscle = KoLCharacter.getAdjustedMuscle();
    int moxie = KoLCharacter.getAdjustedMoxie();

    // Monster stats
    MonsterData monster = MonsterStatusTracker.getLastMonster();
    int monsterAttack = MonsterStatusTracker.getMonsterAttack();
    int monsterDefense = MonsterStatusTracker.getMonsterDefense();
    int physicalResistance = monster == null ? 0 : monster.getPhysicalResistance();

    // Calculate your expected combat damage
    AdventureResult weapon = EquipmentManager.getEquipment(EquipmentManager.WEAPON);
    AdventureResult offhand = EquipmentManager.getEquipment(EquipmentManager.OFFHAND);
    int weaponItemId = weapon.getItemId();
    int offhandItemId = offhand.getItemId();

    // Hit stat is MUSCLE, MOXIE, or NONE, if no weapon equipped.
    Stat stat = EquipmentDatabase.getWeaponStat(weaponItemId);

    // Damage from your hit stat is the amount that it exceeds the
    // monster's defense, or 3/4 of that for ranged weapons
    int statDamage =
        stat == Stat.MUSCLE
            ? Math.max(muscle - monsterDefense, 0)
            : stat == Stat.MOXIE
                ? Math.max((moxie - monsterDefense) * 3 / 4, 0)
                :
                // Can you fight bare-handed in Spelunky?
                Math.max((muscle / 4) - monsterDefense, 0);

    // Weapon power determines damage range: 10% - 20%
    int power = EquipmentDatabase.getPower(weaponItemId);

    // (Minimum) Damage from your weapon is one tenth the weapon's power
    int weaponDamageMin = Math.max(Math.round(power / 10.0f), 1);
    int weaponDamageMax = weaponDamageMin * 2;

    // Spelunky weapons can have bonus damage

    int bonusWeaponDamage =
        (int) Modifiers.getNumericModifier("Item", weaponItemId, "Weapon Damage");
    int bonusOffhandDamage =
        (int) Modifiers.getNumericModifier("Item", offhandItemId, "Weapon Damage");
    int bonusRangedDamage =
        (int) Modifiers.getNumericModifier("Item", weaponItemId, "Ranged Damage");
    int bonusDamage =
        bonusWeaponDamage + (stat == Stat.MOXIE ? bonusRangedDamage : 0) + bonusOffhandDamage;

    buffer.append("<br />Your damage: ");
    buffer.append(
        SpelunkyRequest.adjustedDamageString(
            statDamage + weaponDamageMin + bonusDamage, physicalResistance));
    buffer.append("-");
    buffer.append(
        SpelunkyRequest.adjustedDamageString(
            statDamage + weaponDamageMax + bonusDamage, physicalResistance));

    // You have a 9% chance of scoring a critical hit, which
    // doubles the weapon damage component of combat damage

    // * You never miss or fumble.

    buffer.append(" (9% critical) = ");
    buffer.append(
        SpelunkyRequest.adjustedDamageString(
            statDamage + (int) Math.floor(weaponDamageMin * 1.09) + bonusDamage,
            physicalResistance));
    buffer.append("-");
    buffer.append(
        SpelunkyRequest.adjustedDamageString(
            statDamage + (int) Math.floor(weaponDamageMax * 1.09) + bonusDamage,
            physicalResistance));

    // Append monster's expected combat damage

    // Raw monster damage
    int monsterStatDamage = Math.max(monsterAttack - moxie, 0);

    // Some Spelunky items provide Damage Reduction
    int dr = (int) KoLCharacter.currentNumericModifier(Modifiers.DAMAGE_REDUCTION);

    int monsterDamageMin = Math.max(monsterStatDamage + monsterAttack / 5 - dr, 1);
    int monsterDamageMax = Math.max(monsterStatDamage + monsterAttack / 4 - dr, 1);

    buffer.append("<br />His damage: ");
    buffer.append(monsterDamageMin);
    buffer.append("-");
    buffer.append(monsterDamageMax);

    // * The monster never misses or fumbles.

    /*

    // Monster Hit chance
    //
    // http://kolspading.com/forums/viewtopic.php?f=3&t=36
    //
    // Monster Hit Chance formula is: hit_successful = ( (Monster Attack - Player Moxie) + rand(0,9) - rand(0,9) >= 0
    //
    // When your Moxie is 9 or more under the Monster's Attack, you will always be hit (unless it fumbles).
    // When your Moxie is 10 or more above the Monster's Attack, you will never be hit (unless it crits).
    //
    // flat 6% chance for a critical.
    // flat 6% chance for a fumble.

    float monsterHitChance = CombatUtilities.hitChance( monsterAttack, moxie, 0.06f, 0.06f );

    buffer.append( " (" );
    buffer.append( String.valueOf( (int)Math.round( 100.0f * monsterHitChance ) ) );
    buffer.append( "% hit 6% fumble 6% critical) = " );
    buffer.append( String.valueOf( (int)Math.round( monsterHitChance * monsterDamageMin ) ) );
    buffer.append( "-" );
    buffer.append( String.valueOf( (int)Math.round( monsterHitChance * monsterDamageMax ) ) );

    */
  }

  private static String adjustedDamageString(int damage, int physicalResistance) {
    if (physicalResistance > 0) {
      damage = (damage * (100 - physicalResistance)) / 100;
    }

    if (damage == 0) {
      damage = 1;
    }

    return String.valueOf(damage);
  }

  public static final String spelunkyWarning(final KoLAdventure adventure, final String confirm) {
    // If we have won fewer than 3 combats since the last
    // noncombat, no noncombat is due.

    if (Preferences.getInteger("spelunkyWinCount") < 3) {
      // *** here is where combat warnings would be created
      return null;
    }

    String location = adventure.getAdventureName();

    // A noncombat is due. Tell the user about all the available
    // noncombat options and give him a chance to confirm that he
    // really intends to adventure in the location he has selected

    int phase = Preferences.getInteger("spelunkyNextNoncombat");
    String message =
        "The ghost is waving and a phase "
            + phase
            + " noncombat is available. Click on the icon above to adventure in "
            + location
            + " or equip yourself appropriately and click on one of the locations below to go there instead.";

    String status = Preferences.getString("spelunkyStatus");

    Matcher matcher = SpelunkyRequest.BOMB_STATUS_PATTERN.matcher(status);
    int bombs = matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : 0;

    matcher = SpelunkyRequest.ROPE_STATUS_PATTERN.matcher(status);
    int ropes = matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : 0;

    matcher = SpelunkyRequest.KEY_STATUS_PATTERN.matcher(status);
    int keys = matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : 0;

    matcher = SpelunkyRequest.BUDDY_STATUS_PATTERN.matcher(status);
    String buddy = matcher.find() ? matcher.group(1) : "";

    matcher = SpelunkyRequest.UNLOCK_STATUS_PATTERN.matcher(status);
    String unlocks = matcher.find() ? matcher.group(1) : "";

    boolean mines = true;
    boolean jungle = unlocks.contains("Jungle");
    boolean iceCaves = unlocks.contains("Ice Caves");
    boolean templeRuins = unlocks.contains("Temple Ruins");
    boolean snakePit = unlocks.contains("Snake Pit");
    boolean spiderHole = unlocks.contains("Spider Hole");
    boolean burialGround = unlocks.contains("Burial Ground");
    boolean beehive = unlocks.contains("Beehive");
    boolean crashedUFO = unlocks.contains("Crashed UFO");
    boolean altar = unlocks.contains("Altar");
    boolean cityOfGoooold = unlocks.contains("City of Goooold");

    StringBuilder buffer = new StringBuilder(message);
    buffer.append("<p><table>");

    if (mines) {
      buffer.append("<tr>");
      buffer.append("<td>");
      buffer.append(spelunkyLocationLink("The Mines", AdventurePool.THE_MINES_ID, confirm));
      buffer.append("</td><td>");
      if (phase == 1) {
        buffer.append("gain 20 gold");
        // Assume you can only have 1 pot
        if (!SpelunkyRequest.haveItem(SpelunkyRequest.POT)) {
          buffer.append("<br>");
          buffer.append("pot");
        }
      } else if (phase == 2) {
        buffer.append("A Shop");
      } else if (phase == 3) {
        String divider = "";
        if (SpelunkyRequest.haveItem(SpelunkyRequest.WHIP)) {
          buffer.append(divider);
          buffer.append("use equipped whip and take 5 damage");
          divider = "<br>";
        }
        if (!snakePit && !spiderHole) {
          if (bombs > 0) {
            buffer.append(divider);
            buffer.append("use a bomb to open the Snake Pit");
            divider = "<br>";
          }
          if (ropes > 0) {
            buffer.append(divider);
            buffer.append("use a rope to open the Spider Pit");
            divider = "<br>";
          }
        }
        if (SpelunkyRequest.haveItem(SpelunkyRequest.SKULL)
            || SpelunkyRequest.haveItem(SpelunkyRequest.ROCK)
            || SpelunkyRequest.haveItem(SpelunkyRequest.POT)
            || SpelunkyRequest.haveItem(SpelunkyRequest.TORCH)) {
          buffer.append(divider);
          buffer.append("discard equipped off-hand item to take no damage");
          divider = "<br>";
        }
        buffer.append(divider);
        buffer.append("use no resources and take 10 damage");
        divider = "<br>";
      }
      buffer.append("</td>");
      buffer.append("</tr>");
    }

    if (jungle) {
      buffer.append("<tr>");
      buffer.append("<td>");
      buffer.append(spelunkyLocationLink("The Jungle", AdventurePool.THE_JUNGLE_ID, confirm));
      buffer.append("</td><td>");
      if (phase == 1) {
        buffer.append("A Shop");
      } else if (phase == 2) {
        buffer.append("gain 20-25 gold or a buddy");
        String divider = "<br>";
        // Assume you can only have 1 shotgun
        if (SpelunkyRequest.haveItem(SpelunkyRequest.HEAVY_PICKAXE)
            && !SpelunkyRequest.haveItem(SpelunkyRequest.SHOTGUN)) {
          buffer.append(divider);
          buffer.append("wield pick-axe to get a shotgun");
        }
        // Assume you can only have 1 The Clown Crown
        if (SpelunkyRequest.haveItem(SpelunkyRequest.XRAY_GOGGLES)
            && !SpelunkyRequest.haveItem(SpelunkyRequest.CLOWN_CROWN)) {
          buffer.append(divider);
          buffer.append("wear x-ray goggles to find The Clown Crown");
        }
      } else if (phase == 3) {
        String divider = "";
        if (SpelunkyRequest.haveItem(SpelunkyRequest.SPRING_BOOTS)) {
          buffer.append(divider);
          buffer.append("avoid trap using equipped spring boots");
          divider = "<br>";
        }
        if (!beehive && !burialGround) {
          if (bombs > 0) {
            buffer.append(divider);
            buffer.append("use a bomb to open The Beehive");
            if (!status.contains("Sticky Bombs")) {
              buffer.append(" and take 15 damage");
            }
            divider = "<br>";
          }
          if (ropes > 0) {
            buffer.append(divider);
            buffer.append("use a rope to open the The Ancient Burial Ground");
            buffer.append(" and take 15 damage");
            if (SpelunkyRequest.haveItem(SpelunkyRequest.YELLOW_CAPE)
                || SpelunkyRequest.haveItem(SpelunkyRequest.JETPACK)) {
              buffer.append(" (unless you equip a back item)");
            }
            divider = "<br>";
          }
          buffer.append(divider);
          buffer.append("take 30 damage");
        }
      }
      buffer.append("</td>");
      buffer.append("</tr>");
    }

    if (iceCaves) {
      buffer.append("<tr>");
      buffer.append("<td>");
      buffer.append(spelunkyLocationLink("The Ice Caves", AdventurePool.THE_ICE_CAVES_ID, confirm));
      buffer.append("</td><td>");
      if (phase == 1) {
        buffer.append("A Shop");
      } else if (phase == 2) {
        buffer.append("gain 50-60 gold");
        if (SpelunkyRequest.haveItem(SpelunkyRequest.TORCH)) {
          buffer.append("<br>");
          buffer.append(buddy.equals("") ? "get a buddy" : "gain 60-70 gold");
        }
      } else if (phase == 3) {
        String divider = "";
        if (!altar && !crashedUFO) {
          buffer.append(divider);
          buffer.append("take 10 damage and open The Altar");
          divider = "<br>";
          if (ropes >= 3) {
            buffer.append(divider);
            buffer.append("use 3 ropes and open The Crashed U.F.O.");
            divider = "<br>";
          }
        }
        buffer.append(divider);
        buffer.append("take 30 damage");
      }
      buffer.append("</td>");
      buffer.append("</tr>");
    }

    if (templeRuins) {
      buffer.append("<tr>");
      buffer.append("<td>");
      buffer.append(
          spelunkyLocationLink("The Temple Ruins", AdventurePool.THE_TEMPLE_RUINS_ID, confirm));
      buffer.append("</td><td>");
      if (phase == 1) {
        buffer.append("A Crate");
      } else if (phase == 2) {
        String divider = "";
        if (SpelunkyRequest.haveItem(SpelunkyRequest.JETPACK)) {
          buffer.append(divider);
          buffer.append("gain 250 gold using jetpack");
          divider = "<br>";
        }
        if (SpelunkyRequest.haveItem(SpelunkyRequest.YELLOW_CAPE)
            && SpelunkyRequest.haveItem(SpelunkyRequest.SPRING_BOOTS)) {
          buffer.append(divider);
          buffer.append("gain 250 gold using yellow cape and spring boots");
          divider = "<br>";
        }
        if (buddy.equals("Resourceful Kid")) {
          buffer.append(divider);
          buffer.append("gain 250 gold using Resourceful Kid");
          divider = "<br>";
        }
        buffer.append(divider);
        buffer.append("gain 250 gold and take 50 damage");
        divider = "<br>";
      } else if (phase == 3) {
        String divider = "";
        if (keys > 0 && !cityOfGoooold) {
          buffer.append(divider);
          buffer.append("use key to open The City of Goooold");
          divider = "<br>";
        }
        buffer.append(divider);
        buffer.append("take 40 damage");
        divider = "<br>";
      }
      buffer.append("</td>");
      buffer.append("</tr>");
    }

    if (snakePit) {
      buffer.append("<tr>");
      buffer.append("<td>");
      buffer.append(spelunkyLocationLink("The Snake Pit", AdventurePool.THE_SNAKE_PIT_ID, confirm));
      buffer.append("</td><td>");
      buffer.append("A Crate");
      buffer.append("</td>");
      buffer.append("</tr>");
    }

    if (spiderHole) {
      buffer.append("<tr>");
      buffer.append("<td>");
      buffer.append(
          spelunkyLocationLink("The Spider Hole", AdventurePool.THE_SPIDER_HOLE_ID, confirm));
      buffer.append("</td><td>");
      buffer.append("gain 15-20 gold");
      String divider = "<br>";
      if (SpelunkyRequest.haveItem(SpelunkyRequest.MACHETE)) {
        buffer.append(divider);
        buffer.append("wield sturdy machete to ");
        buffer.append(buddy.equals("") ? "get a buddy" : "gain 30-40 gold");
      }
      if (SpelunkyRequest.haveItem(SpelunkyRequest.TORCH)) {
        buffer.append(divider);
        buffer.append("wield torch to gain 30-50 gold");
      }
      buffer.append("</td>");
      buffer.append("</tr>");
    }

    if (burialGround) {
      buffer.append("<tr>");
      buffer.append("<td>");
      buffer.append(
          spelunkyLocationLink(
              "The Ancient Burial Ground", AdventurePool.THE_ANCIENT_BURIAL_GROUND_ID, confirm));
      buffer.append("</td><td>");
      buffer.append("gain 20-25 gold or a buddy");
      String divider = "<br>";
      // Assume you can only have 1 shotgun
      if (SpelunkyRequest.haveItem(SpelunkyRequest.HEAVY_PICKAXE)
          && !SpelunkyRequest.haveItem(SpelunkyRequest.SHOTGUN)) {
        buffer.append(divider);
        buffer.append("wield pick-axe to get a shotgun");
      }
      // Assume you can only have 1 The Clown Crown
      if (SpelunkyRequest.haveItem(SpelunkyRequest.XRAY_GOGGLES)
          && !SpelunkyRequest.haveItem(SpelunkyRequest.CLOWN_CROWN)) {
        buffer.append(divider);
        buffer.append("wear x-ray goggles to find The Clown Crown");
      }
      buffer.append("</td>");
      buffer.append("</tr>");
    }

    if (beehive) {
      buffer.append("<tr>");
      buffer.append("<td>");
      buffer.append(spelunkyLocationLink("The Beehive", AdventurePool.THE_BEEHIVE_ID, confirm));
      buffer.append("</td><td>");
      buffer.append("A Crate");
      buffer.append("</td>");
      buffer.append("</tr>");
    }

    if (crashedUFO) {
      buffer.append("<tr>");
      buffer.append("<td>");
      buffer.append(
          spelunkyLocationLink("The Crashed U.F.O.", AdventurePool.THE_CRASHED_UFO_ID, confirm));
      buffer.append("</td><td>");
      buffer.append("A Crate");
      buffer.append("</td>");
      buffer.append("</tr>");
    }

    if (cityOfGoooold) {
      buffer.append("<tr>");
      buffer.append("<td>");
      buffer.append(
          spelunkyLocationLink(
              "The City of Goooold", AdventurePool.THE_CITY_OF_GOOOOLD_ID, confirm));
      buffer.append("</td><td>");
      String divider = "";
      if (keys > 0) {
        buffer.append(divider);
        buffer.append("use key to gain 150 gold");
        divider = "<br>";
      }
      if (bombs > 0) {
        buffer.append(divider);
        buffer.append("use bomb to gain 80-100 gold");
        divider = "<br>";
      }
      buffer.append(divider);
      buffer.append("gain 50-60 gold and take 20 damage");
      buffer.append("</td>");
      buffer.append("</tr>");
    }

    buffer.append("</table>");

    return buffer.toString();
  }

  private static boolean haveItem(final AdventureResult item) {
    return item.getCount(KoLConstants.inventory) > 0 || InventoryManager.getEquippedCount(item) > 0;
  }

  private static String spelunkyLocationLink(String name, final String id, final String confirm) {
    String link =
        "<a href=\"adventure.php?snarfblat="
            + id
            + "&"
            + confirm
            + "=on"
            + "\"><img src=\"/images/"
            + SpelunkyRequest.adventureImage(name)
            + "\" height=105 border=0 alt=\""
            + name
            + "\" title=\""
            + name
            + "\"></a>";
    return link;
  }
}
