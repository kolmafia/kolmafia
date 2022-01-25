package net.sourceforge.kolmafia.request;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.DebugDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.ConsequenceManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResponseTextParser;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ClanLoungeRequest extends GenericRequest {
  private static final int SEARCH = 0;
  private static final int SEARCH2 = 200;

  public static final int KLAW = 1;
  public static final int HOTTUB = 2;
  public static final int POOL_TABLE = 3;
  public static final int CRIMBO_TREE = 4;
  public static final int LOOKING_GLASS = 5;
  public static final int FAX_MACHINE = 6;
  public static final int APRIL_SHOWER = 7;
  public static final int SWIMMING_POOL = 8;
  public static final int HOT_DOG_STAND = 9;
  public static final int SPEAKEASY = 10;
  public static final int FLOUNDRY = 11;
  public static final int FORTUNE = 12;
  public static final int FIREWORKS = 13;

  // Pool options
  public static final int AGGRESSIVE_STANCE = 1;
  public static final int STRATEGIC_STANCE = 2;
  public static final int STYLISH_STANCE = 3;

  // Fax options
  public static final int SEND_FAX = 1;
  public static final int RECEIVE_FAX = 2;

  // Shower options
  public static final int COLD_SHOWER = 1;
  public static final int COOL_SHOWER = 2;
  public static final int LUKEWARM_SHOWER = 3;
  public static final int WARM_SHOWER = 4;
  public static final int HOT_SHOWER = 5;

  // Swimming Pool options
  public static final int CANNONBALL = 1;
  public static final int LAPS = 2;
  public static final int SPRINTS = 3;

  private final int action;
  private int option;

  private static final Pattern STANCE_PATTERN = Pattern.compile("stance=(\\d*)");
  private static final Pattern WHICHDOG_PATTERN = Pattern.compile("whichdog=(-\\d*)");
  private static final Pattern LUCKY_LINDY_PATTERN =
      Pattern.compile("burp-speak the number <b>(\\d+)</b>.");
  private static final Pattern WHICH_SPEAKEASY_PATTERN = Pattern.compile("drink=(\\d+)");
  private static final Pattern TREE_PATTERN = Pattern.compile("Check back in (\\d+) day");
  private static final Pattern TREE_LEVEL_PATTERN = Pattern.compile("tree(\\d+)(?:nopressie|).gif");
  private static final Pattern FAX_PATTERN = Pattern.compile("preaction=(.+?)fax");
  private static final Pattern TEMPERATURE_PATTERN = Pattern.compile("temperature=(\\d*)");
  private static final Pattern SWIMMING_POOL_PATTERN = Pattern.compile("subaction=([^&]+)");
  private static final Pattern LAPS_PATTERN = Pattern.compile("manage to swim ([\\d,]+) before");
  private static final Pattern SPRINTS_PATTERN = Pattern.compile("you do ([\\d,]+) of them");
  private static final Pattern FISH_STOCK_PATTERN =
      Pattern.compile("<br>(\\d+)?[,]?(\\d+)?[,]?(\\d+) (carp|cod|trout|bass|hatchetfish|tuna)");

  public static final Object[][] POOL_GAMES =
      new Object[][] {
        {"aggressive", "muscle", "billiards belligerence", IntegerPool.get(AGGRESSIVE_STANCE)},
        {"strategic", "mysticality", "mental a-cue-ity", IntegerPool.get(STRATEGIC_STANCE)},
        {"stylish", "moxie", "hustlin'", IntegerPool.get(STYLISH_STANCE)},
      };

  public static final Object[][] FAX_OPTIONS =
      new Object[][] {
        {"send", "put", IntegerPool.get(SEND_FAX)},
        {"receive", "get", IntegerPool.get(RECEIVE_FAX)},
      };

  public static final Object[][] SHOWER_OPTIONS =
      new Object[][] {
        {"cold", "ice", IntegerPool.get(COLD_SHOWER)},
        {"cool", "moxie", IntegerPool.get(COOL_SHOWER)},
        {"lukewarm", "mysticality", IntegerPool.get(LUKEWARM_SHOWER)},
        {"warm", "muscle", IntegerPool.get(WARM_SHOWER)},
        {"hot", "mp", IntegerPool.get(HOT_SHOWER)},
      };

  public static final Object[][] SWIMMING_OPTIONS =
      new Object[][] {
        {"cannonball", "item", IntegerPool.get(CANNONBALL)},
        {"laps", "ml", IntegerPool.get(LAPS)},
        {"sprints", "noncombat", IntegerPool.get(SPRINTS)},
      };

  public static final Object[][] HOTDOG_DATA =
      new Object[][] {
        {"basic hot dog", IntegerPool.get(-92), IntegerPool.get(1)},
        {
          "savage macho dog",
          IntegerPool.get(-93),
          IntegerPool.get(2),
          ItemPool.get(ItemPool.FURRY_FUR, 10),
          ItemPool.get(ItemPool.VICIOUS_SPIKED_COLLAR, 1),
        },
        {
          "one with everything",
          IntegerPool.get(-94),
          IntegerPool.get(2),
          ItemPool.get(ItemPool.CRANBERRIES, 10),
          ItemPool.get(ItemPool.ANCIENT_HOT_DOG_WRAPPER, 1),
        },
        {
          "sly dog",
          IntegerPool.get(-95),
          IntegerPool.get(2),
          ItemPool.get(ItemPool.SKELETON_BONE, 10),
          ItemPool.get(ItemPool.DEBONAIR_DEBONER, 1),
        },
        {
          "devil dog",
          IntegerPool.get(-96),
          IntegerPool.get(3),
          ItemPool.get(ItemPool.HOT_WAD, 25),
          ItemPool.get(ItemPool.CHICLE_DE_SALCHICA, 1),
        },
        {
          "chilly dog",
          IntegerPool.get(-97),
          IntegerPool.get(3),
          ItemPool.get(ItemPool.COLD_WAD, 25),
          ItemPool.get(ItemPool.JAR_OF_FROSTIGKRAUT, 1),
        },
        {
          "ghost dog",
          IntegerPool.get(-98),
          IntegerPool.get(3),
          ItemPool.get(ItemPool.SPOOKY_WAD, 25),
          ItemPool.get(ItemPool.GNAWED_UP_DOG_BONE, 1),
        },
        {
          "junkyard dog",
          IntegerPool.get(-99),
          IntegerPool.get(3),
          ItemPool.get(ItemPool.STENCH_WAD, 25),
          ItemPool.get(ItemPool.GREY_GUANON, 1),
        },
        {
          "wet dog",
          IntegerPool.get(-100),
          IntegerPool.get(3),
          ItemPool.get(ItemPool.SLEAZE_WAD, 25),
          ItemPool.get(ItemPool.ENGORGED_SAUSAGES_AND_YOU, 1),
        },
        {
          "optimal dog",
          IntegerPool.get(-102),
          IntegerPool.get(1),
          ItemPool.get(ItemPool.SCRAP_OF_PAPER, 25),
          ItemPool.get(ItemPool.OPTIMAL_SPREADSHEET, 1),
        },
        {
          "sleeping dog",
          IntegerPool.get(-101),
          IntegerPool.get(2),
          ItemPool.get(ItemPool.GAUZE_HAMMOCK, 10),
          ItemPool.get(ItemPool.DREAM_OF_A_DOG, 1),
        },
        {
          "video games hot dog",
          IntegerPool.get(-103),
          IntegerPool.get(3),
          ItemPool.get(ItemPool.GAMEPRO_MAGAZINE, 3),
          ItemPool.get(ItemPool.DEFECTIVE_TOKEN, 1),
        },
      };

  public static final Object[][] SPEAKEASY_DATA =
      new Object[][] {
        {"glass of &quot;milk&quot;", IntegerPool.get(1), IntegerPool.get(1), IntegerPool.get(250)},
        {"cup of &quot;tea&quot;", IntegerPool.get(2), IntegerPool.get(1), IntegerPool.get(250)},
        {
          "thermos of &quot;whiskey&quot;",
          IntegerPool.get(3),
          IntegerPool.get(1),
          IntegerPool.get(250)
        },
        {"Lucky Lindy", IntegerPool.get(4), IntegerPool.get(1), IntegerPool.get(500)},
        {"Bee's Knees", IntegerPool.get(5), IntegerPool.get(2), IntegerPool.get(500)},
        {"Sockdollager", IntegerPool.get(6), IntegerPool.get(2), IntegerPool.get(500)},
        {
          "Ish Kabibble", IntegerPool.get(7), IntegerPool.get(2), IntegerPool.get(500),
        },
        {"Hot Socks", IntegerPool.get(8), IntegerPool.get(3), IntegerPool.get(5000)},
        {"Phonus Balonus", IntegerPool.get(9), IntegerPool.get(3), IntegerPool.get(10000)},
        {"Flivver", IntegerPool.get(10), IntegerPool.get(2), IntegerPool.get(20000)},
        {
          "Sloppy Jalopy", IntegerPool.get(11), IntegerPool.get(5), IntegerPool.get(100000),
        },
      };

  public static final Object[][] FLOUNDRY_DATA =
      new Object[][] {
        {"carp", ItemPool.get(ItemPool.CARPE, 1)},
        {"cod", ItemPool.get(ItemPool.CODPIECE, 1)},
        {"trout", ItemPool.get(ItemPool.TROUTSERS, 1)},
        {"bass", ItemPool.get(ItemPool.BASS_CLARINET, 1)},
        {"hatchetfish", ItemPool.get(ItemPool.FISH_HATCHET, 1)},
        {"tuna", ItemPool.get(ItemPool.TUNAC, 1)},
      };

  public static void setClanLoungeItem(final int itemId, int count) {
    ClanLoungeRequest.setClanLoungeItem(ItemPool.get(itemId, count));
  }

  private static void setClanLoungeItem(final AdventureResult item) {
    int i = ClanManager.getClanLounge().indexOf(item);
    if (i != -1) {
      AdventureResult old = ClanManager.getClanLounge().get(i);
      if (old.getCount() == item.getCount()) {
        return;
      }
      ClanManager.getClanLounge().remove(i);
    }
    ClanManager.addToLounge(item);
  }

  public static boolean hasClanLoungeItem(AdventureResult item) {
    if (item == null) {
      return false;
    }
    return item.getCount(ClanManager.getClanLounge()) > 0;
  }

  public static final int hotdogIdToIndex(int id) {
    for (int i = 0; i < HOTDOG_DATA.length; ++i) {
      if (id == ((Integer) ClanLoungeRequest.HOTDOG_DATA[i][1]).intValue()) {
        return i;
      }
    }
    return -1;
  }

  public static final String hotdogIdToName(int id) {
    int index = ClanLoungeRequest.hotdogIdToIndex(id);
    return index < 0 ? null : (String) ClanLoungeRequest.HOTDOG_DATA[index][0];
  }

  public static final String hotdogIndexToName(int index) {
    return (index < 0 || index > ClanLoungeRequest.HOTDOG_DATA.length)
        ? null
        : (String) ClanLoungeRequest.HOTDOG_DATA[index][0];
  }

  public static final Integer hotdogIndexToId(int index) {
    return (index < 0 || index > ClanLoungeRequest.HOTDOG_DATA.length)
        ? -1
        : (Integer) ClanLoungeRequest.HOTDOG_DATA[index][1];
  }

  public static final Integer hotdogIndexToFullness(int index) {
    return (index < 0 || index > ClanLoungeRequest.HOTDOG_DATA.length)
        ? -1
        : (Integer) ClanLoungeRequest.HOTDOG_DATA[index][2];
  }

  public static final Integer hotdogNameToFullness(final String name) {
    int index = ClanLoungeRequest.hotdogNameToIndex(name);
    return (index < 0 || index > ClanLoungeRequest.HOTDOG_DATA.length)
        ? -1
        : (Integer) ClanLoungeRequest.HOTDOG_DATA[index][2];
  }

  public static final AdventureResult hotdogIndexToItem(int index) {
    return (index < 0 || index > ClanLoungeRequest.HOTDOG_DATA.length)
        ? null
        : (AdventureResult) ClanLoungeRequest.HOTDOG_DATA[index][3];
  }

  public static final AdventureResult hotdogIndexToUnlocker(int index) {
    return (index < 0 || index > ClanLoungeRequest.HOTDOG_DATA.length)
        ? null
        : (AdventureResult) ClanLoungeRequest.HOTDOG_DATA[index][4];
  }

  public static final ArrayList<String> HOTDOG_NAMES = new ArrayList<String>();
  public static final ArrayList<Concoction> ALL_HOTDOGS = new ArrayList<Concoction>();

  static {
    for (int i = 0; i < HOTDOG_DATA.length; ++i) {
      String itemName = (String) HOTDOG_DATA[i][0];
      Concoction concoction = new Concoction(itemName);
      concoction.hotdog = true;
      ClanLoungeRequest.HOTDOG_NAMES.add(itemName);
      ClanLoungeRequest.ALL_HOTDOGS.add(concoction);
      if (i > 0) {
        concoction.fancydog = true;
      }
    }
  }

  public static final void resetHotdogs() {
    // Remove all hot dogs from the usable list
    ConcoctionDatabase.getUsables().removeAll(ClanLoungeRequest.ALL_HOTDOGS);
    ConcoctionDatabase.refreshConcoctions(false);
  }

  private static int hotdogNameToIndex(final String name) {
    return ClanLoungeRequest.HOTDOG_NAMES.indexOf(name);
  }

  public static final boolean isHotDog(String name) {
    return ClanLoungeRequest.HOTDOG_NAMES.contains(name);
  }

  public static final boolean isFancyHotDog(String name) {
    return ClanLoungeRequest.HOTDOG_NAMES.indexOf(name) > 0;
  }

  public static final int speakeasyIdToIndex(int id) {
    for (int i = 0; i < SPEAKEASY_DATA.length; ++i) {
      if (id == ((Integer) ClanLoungeRequest.SPEAKEASY_DATA[i][1]).intValue()) {
        return i;
      }
    }
    return -1;
  }

  public static final String speakeasyIdToName(int id) {
    int index = ClanLoungeRequest.speakeasyIdToIndex(id);
    return index < 0 ? null : (String) ClanLoungeRequest.SPEAKEASY_DATA[index][0];
  }

  public static final String speakeasyIndexToName(int index) {
    return (index < 0 || index > ClanLoungeRequest.SPEAKEASY_DATA.length)
        ? null
        : (String) ClanLoungeRequest.SPEAKEASY_DATA[index][0];
  }

  public static final Integer speakeasyIndexToId(int index) {
    return (index < 0 || index > ClanLoungeRequest.SPEAKEASY_DATA.length)
        ? -1
        : (Integer) ClanLoungeRequest.SPEAKEASY_DATA[index][1];
  }

  public static final Integer speakeasyIndexToInebriety(int index) {
    return (index < 0 || index > ClanLoungeRequest.SPEAKEASY_DATA.length)
        ? -1
        : (Integer) ClanLoungeRequest.SPEAKEASY_DATA[index][2];
  }

  public static final Integer speakeasyIndexToCost(int index) {
    return (index < 0 || index > ClanLoungeRequest.SPEAKEASY_DATA.length)
        ? -1
        : (Integer) ClanLoungeRequest.SPEAKEASY_DATA[index][3];
  }

  public static final Integer speakeasyNameToInebriety(final String name) {
    int index = ClanLoungeRequest.speakeasyNameToIndex(name);
    return (index < 0 || index > ClanLoungeRequest.SPEAKEASY_DATA.length)
        ? -1
        : (Integer) ClanLoungeRequest.SPEAKEASY_DATA[index][2];
  }

  public static final Integer speakeasyNameToCost(final String name) {
    int index = ClanLoungeRequest.speakeasyNameToIndex(name);
    return (index < 0 || index > ClanLoungeRequest.SPEAKEASY_DATA.length)
        ? -1
        : (Integer) ClanLoungeRequest.SPEAKEASY_DATA[index][3];
  }

  public static final AdventureResult speakeasyIndexToItem(int index) {
    return (index < 0 || index > ClanLoungeRequest.SPEAKEASY_DATA.length)
        ? null
        : (AdventureResult) ClanLoungeRequest.SPEAKEASY_DATA[index][3];
  }

  public static final ArrayList<Concoction> ALL_SPEAKEASY = new ArrayList<Concoction>();
  private static final String[] CANONICAL_SPEAKEASY_ARRAY =
      new String[ClanLoungeRequest.SPEAKEASY_DATA.length];

  static {
    for (int i = 0; i < SPEAKEASY_DATA.length; ++i) {
      String itemName = (String) SPEAKEASY_DATA[i][0];
      int itemId = ItemDatabase.getItemId(itemName, 1, false);
      Concoction concoction = ConcoctionPool.get(itemId, itemName);
      concoction.speakeasy = true;
      concoction.price = ClanLoungeRequest.speakeasyNameToCost(itemName);
      ClanLoungeRequest.ALL_SPEAKEASY.add(concoction);
      ClanLoungeRequest.CANONICAL_SPEAKEASY_ARRAY[i] = StringUtilities.getCanonicalName(itemName);
    }
  }

  public static final void resetSpeakeasy() {
    // Remove all Speakeasy drinks from the usable list
    ConcoctionDatabase.getUsables().removeAll(ClanLoungeRequest.ALL_SPEAKEASY);
    ConcoctionDatabase.refreshConcoctions(false);
  }

  private static int speakeasyNameToIndex(final String name) {
    if (name == null) {
      return -1;
    }
    for (int i = 0; i < SPEAKEASY_DATA.length; ++i) {
      if (name.equalsIgnoreCase((String) ClanLoungeRequest.SPEAKEASY_DATA[i][0])) {
        return i;
      }
    }
    return -1;
  }

  public static final boolean isSpeakeasyDrink(String name) {
    return speakeasyNameToIndex(name) != -1;
  }

  public static final String findSpeakeasyDrink(String searchString) {
    List<String> matchingNames =
        StringUtilities.getMatchingNames(ClanLoungeRequest.CANONICAL_SPEAKEASY_ARRAY, searchString);

    if (matchingNames.size() != 1) {
      return null;
    }

    String name = matchingNames.get(0);
    for (int index = 0; index < ClanLoungeRequest.CANONICAL_SPEAKEASY_ARRAY.length; ++index) {
      if (name.equals(ClanLoungeRequest.CANONICAL_SPEAKEASY_ARRAY[index])) {
        return ClanLoungeRequest.speakeasyIndexToName(index);
      }
    }

    return null;
  }

  public static final ArrayList<Concoction> ALL_FLOUNDRY = new ArrayList<Concoction>();

  static {
    for (int i = 0; i < FLOUNDRY_DATA.length; ++i) {
      AdventureResult item = (AdventureResult) FLOUNDRY_DATA[i][1];
      if (item != null) {
        Concoction concoction = ConcoctionPool.get(item);
        concoction.setMixingMethod(CraftingType.FLOUNDRY);
        ClanLoungeRequest.ALL_FLOUNDRY.add(concoction);
      }
    }
  }

  public static final void resetFloundry() {
    // Remove all Floundry Items from the usable list
    ConcoctionDatabase.getUsables().removeAll(ClanLoungeRequest.ALL_FLOUNDRY);
    ConcoctionDatabase.refreshConcoctions(false);
  }

  private static AdventureResult floundryFishToItem(final String fish) {
    if (fish == null) {
      return null;
    }
    for (int i = 0; i < FLOUNDRY_DATA.length; ++i) {
      if (fish.equalsIgnoreCase((String) ClanLoungeRequest.FLOUNDRY_DATA[i][0])) {
        return (AdventureResult) ClanLoungeRequest.FLOUNDRY_DATA[i][1];
      }
    }
    return null;
  }

  public static final boolean isFloundryItem(AdventureResult item) {
    for (int i = 0; i < FLOUNDRY_DATA.length; ++i) {
      if (item.equals(FLOUNDRY_DATA[i][1])) {
        return true;
      }
    }
    return false;
  }

  public static final int findPoolGame(String tag) {
    if (StringUtilities.isNumeric(tag)) {
      int index = StringUtilities.parseInt(tag);
      if (index >= 1 && index <= POOL_GAMES.length) {
        return index;
      }
    }

    tag = tag.toLowerCase();
    for (int i = 0; i < POOL_GAMES.length; ++i) {
      Object[] game = POOL_GAMES[i];
      Integer index = (Integer) game[3];
      String stance = (String) game[0];
      if (stance.startsWith(tag)) {
        return index.intValue();
      }
      String stat = (String) game[1];
      if (stat.startsWith(tag)) {
        return index.intValue();
      }
      String effect = (String) game[2];
      if (effect.startsWith(tag)) {
        return index.intValue();
      }
    }

    return 0;
  }

  public static final int findFaxOption(String tag) {
    tag = tag.toLowerCase();
    for (int i = 0; i < FAX_OPTIONS.length; ++i) {
      Object[] faxOption = FAX_OPTIONS[i];
      Integer index = (Integer) faxOption[2];
      String faxCommand0 = (String) faxOption[0];
      if (faxCommand0.startsWith(tag)) {
        return index.intValue();
      }
      String faxCommand1 = (String) faxOption[1];
      if (faxCommand1.startsWith(tag)) {
        return index.intValue();
      }
    }

    return 0;
  }

  public static final int findShowerOption(String tag) {
    tag = tag.toLowerCase();
    for (int i = 0; i < SHOWER_OPTIONS.length; ++i) {
      Object[] showerOption = SHOWER_OPTIONS[i];
      Integer index = (Integer) showerOption[2];
      String temp = (String) showerOption[0];
      if (temp.startsWith(tag)) {
        return index.intValue();
      }
      String effect = (String) showerOption[1];
      if (effect.startsWith(tag)) {
        return index.intValue();
      }
    }

    return 0;
  }

  public static final int findSwimmingOption(String tag) {
    tag = tag.toLowerCase();
    for (int i = 0; i < SWIMMING_OPTIONS.length; ++i) {
      Object[] swimmingOption = SWIMMING_OPTIONS[i];
      Integer index = (Integer) swimmingOption[2];
      String action = (String) swimmingOption[0];
      if (action.startsWith(tag)) {
        return index.intValue();
      }
      String effect = (String) swimmingOption[1];
      if (effect.startsWith(tag)) {
        return index.intValue();
      }
    }

    return 0;
  }

  public static final String prettyStanceName(final int stance) {
    switch (stance) {
      case AGGRESSIVE_STANCE:
        return "an aggressive stance";
      case STRATEGIC_STANCE:
        return "a strategic stance";
      case STYLISH_STANCE:
        return "a stylish stance";
    }
    return "an unknown stance";
  }

  public static final String prettyFaxCommand(final int faxCommand) {
    switch (faxCommand) {
      case SEND_FAX:
        return "Sending a fax.";
      case RECEIVE_FAX:
        return "Receiving a fax.";
    }
    return "Unknown fax command.";
  }

  public static final String prettyTemperatureName(final int temp) {
    switch (temp) {
      case COLD_SHOWER:
        return "a cold";
      case COOL_SHOWER:
        return "a cool";
      case LUKEWARM_SHOWER:
        return "a lukewarm";
      case WARM_SHOWER:
        return "a warm";
      case HOT_SHOWER:
        return "a hot";
    }
    return "an unknown";
  }

  public static final String prettySwimmingName(final int action) {
    switch (action) {
      case CANNONBALL:
        return "cannonball";
      case LAPS:
        return "swim laps";
      case SPRINTS:
        return "do submarine sprints";
    }
    return "do something";
  }

  /**
   * Constructs a new <code>ClanLoungeRequest</code>.
   *
   * @param action The identifier for the action you're requesting
   */
  private ClanLoungeRequest() {
    this(SEARCH);
  }

  public ClanLoungeRequest(final int action) {
    super("clan_viplounge.php");
    this.action = action;
  }

  public ClanLoungeRequest(final int action, final int option) {
    super("clan_viplounge.php");
    this.action = action;
    this.option = option;
  }

  public static final ClanLoungeRequest buyHotDogRequest(final String name) {
    int index = ClanLoungeRequest.hotdogNameToIndex(name);
    if (index < 0) {
      return null;
    }

    if (!EatItemRequest.allowFoodConsumption(name, 1)) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Aborted eating " + name + ".");
      return null;
    }

    ClanLoungeRequest request =
        new ClanLoungeRequest(HOT_DOG_STAND, ClanLoungeRequest.hotdogIndexToId(index));
    return request;
  }

  public static final ClanLoungeRequest buySpeakeasyDrinkRequest(final String name) {
    int index = ClanLoungeRequest.speakeasyNameToIndex(name);
    if (index < 0) {
      return null;
    }

    if (!DrinkItemRequest.allowBoozeConsumption(name, 1)) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Aborted drinking " + name + ".");
      return null;
    }

    ClanLoungeRequest request =
        new ClanLoungeRequest(SPEAKEASY, ClanLoungeRequest.speakeasyIndexToId(index));
    return request;
  }

  public static final AdventureResult VIP_KEY = ItemPool.get(ItemPool.VIP_LOUNGE_KEY, 1);
  private static GenericRequest VISIT_REQUEST = new ClanLoungeRequest();

  public static boolean canVisitLounge() {
    // If we have no Clan VIP Lounge key, nothing to do
    return VIP_KEY.getCount(KoLConstants.inventory) > 0;
  }

  public static boolean visitLounge(final int location) {
    // If we have no Clan VIP Lounge key, nothing to do
    if (VIP_KEY.getCount(KoLConstants.inventory) == 0) {
      ClanLoungeRequest.resetHotdogs();
      ClanLoungeRequest.resetSpeakeasy();
      ClanManager.getClanLounge().clear();
      return false;
    }

    VISIT_REQUEST = new ClanLoungeRequest(location);
    RequestThread.postRequest(VISIT_REQUEST);

    // If you are not in a clan, KoL redirects you to
    // clan_signup.php - which we do not follow.
    return VISIT_REQUEST.redirectLocation == null;
  }

  public static boolean visitLounge() {
    return ClanLoungeRequest.visitLounge(SEARCH);
  }

  public static boolean visitLoungeFloor2() {
    return ClanLoungeRequest.visitLounge(SEARCH2);
  }

  public static void updateLounge() {
    // Equipment can be on either the first or second floor
    ClanLoungeRequest.visitLounge();
    ClanLoungeRequest.visitLoungeFloor2();

    // Check hotdog stand, speakeasy, and floundry, if present
    if (ClanManager.getClanLounge().contains(ClanManager.HOT_DOG_STAND)) {
      ClanLoungeRequest.visitLounge(ClanLoungeRequest.HOT_DOG_STAND);
    }
    if (ClanManager.getClanLounge().contains(ClanManager.SPEAKEASY)) {
      ClanLoungeRequest.visitLounge(ClanLoungeRequest.SPEAKEASY);
    }
    if (ClanManager.getClanLounge().contains(ClanManager.FLOUNDRY)) {
      ClanLoungeRequest.visitLounge(ClanLoungeRequest.FLOUNDRY);
    }
  }

  private static String equipmentName(final String urlString) {
    if (urlString.contains("preaction=lovetester")) {
      return "Fortune Teller";
    }
    if (urlString.contains("preaction")) {
      return null;
    }

    if (urlString.contains("klaw")) {
      return "Deluxe Mr. Klaw \"Skill\" Crane Game";
    }
    if (urlString.contains("hottub")) {
      return "Relaxing Hot Tub";
    }
    if (urlString.contains("pooltable")) {
      return "Pool Table";
    }
    if (urlString.contains("crimbotree")) {
      return "Crimbo Tree";
    }
    if (urlString.contains("lookingglass")) {
      return "Looking Glass";
    }
    if (urlString.contains("faxmachine")) {
      return "Fax Machine";
    }
    if (urlString.contains("action=shower")) {
      return "April Shower";
    }
    if (urlString.contains("action=swimmingpool")) {
      return "Swimming Pool";
    }
    if (urlString.contains("action=hotdogstand")) {
      return "Hot Dog Stand";
    }
    if (urlString.contains("action=speakeasy")) {
      return "Speakeasy";
    }
    if (urlString.contains("action=floundry")) {
      return "Floundry";
    }
    if (urlString.contains("action=fwshop")) {
      return "Underground Fireworks Shop";
    }
    return null;
  }

  private static String equipmentVisit(final String urlString) {
    String name = ClanLoungeRequest.equipmentName(urlString);
    if (name != null) {
      return "Visiting " + name + " in clan VIP lounge";
    }
    return null;
  }

  /**
   * Runs the request. Note that this does not report an error if it fails; it merely parses the
   * results to see if any gains were made.
   */
  @Override
  public void run() {
    switch (this.action) {
      case ClanLoungeRequest.SEARCH:
        break;

      case ClanLoungeRequest.SEARCH2:
        this.constructURLString("clan_viplounge.php");
        this.addFormField("whichfloor", "2");
        break;

      case ClanLoungeRequest.KLAW:
        this.constructURLString("clan_viplounge.php");
        this.addFormField("action", "klaw");
        break;

      case ClanLoungeRequest.HOTTUB:
        // If on the Hidden Apartment Quest, and have a Curse, ask if you are sure you want to lose
        // it ?
        boolean cursed =
            KoLConstants.activeEffects.contains(EffectPool.CURSE1_EFFECT)
                || KoLConstants.activeEffects.contains(EffectPool.CURSE2_EFFECT)
                || KoLConstants.activeEffects.contains(EffectPool.CURSE3_EFFECT);
        if (cursed
            && Preferences.getInteger("hiddenApartmentProgress") < 7
            && !InputFieldUtilities.confirm("Are you sure, that will remove your Cursed effect?")) {
          break;
        }
        this.constructURLString("clan_viplounge.php");
        this.addFormField("action", "hottub");
        break;

      case ClanLoungeRequest.POOL_TABLE:
        RequestLogger.printLine(
            "Approaching pool table with " + ClanLoungeRequest.prettyStanceName(this.option) + ".");

        this.constructURLString("clan_viplounge.php");
        if (this.option != 0) {
          this.addFormField("preaction", "poolgame");
          this.addFormField("stance", String.valueOf(this.option));
        } else {
          this.addFormField("action", "pooltable");
        }
        this.addFormField("whichfloor", "2");
        break;

      case ClanLoungeRequest.CRIMBO_TREE:
        this.constructURLString("clan_viplounge.php");
        this.addFormField("action", "crimbotree");
        break;

      case ClanLoungeRequest.LOOKING_GLASS:
        this.constructURLString("clan_viplounge.php");
        this.addFormField("action", "lookingglass");
        this.addFormField("whichfloor", "2");
        break;

      case ClanLoungeRequest.FAX_MACHINE:
        this.constructURLString("clan_viplounge.php");
        switch (this.option) {
          case SEND_FAX:
            KoLmafia.updateDisplay("Sending a fax.");
            this.addFormField("preaction", "sendfax");
            break;
          case RECEIVE_FAX:
            KoLmafia.updateDisplay("Receiving a fax.");
            this.addFormField("preaction", "receivefax");
            break;
          default:
            this.addFormField("action", "faxmachine");
            break;
        }
        this.addFormField("whichfloor", "2");
        break;

      case ClanLoungeRequest.APRIL_SHOWER:
        RequestLogger.printLine(
            "Let's take " + ClanLoungeRequest.prettyTemperatureName(this.option) + " shower.");

        this.constructURLString("clan_viplounge.php");
        if (this.option != 0) {
          this.addFormField("preaction", "takeshower");
          this.addFormField("temperature", String.valueOf(option));
        } else {
          this.addFormField("action", "shower");
        }
        this.addFormField("whichfloor", "2");
        break;

      case ClanLoungeRequest.SWIMMING_POOL:
        RequestLogger.printLine(
            "Let's "
                + ClanLoungeRequest.prettySwimmingName(this.option)
                + " in the swimming pool.");

        this.constructURLString("clan_viplounge.php");
        switch (this.option) {
          case CANNONBALL:
            this.addFormField("preaction", "goswimming");
            this.addFormField("subaction", "screwaround");
            break;
          case LAPS:
            this.addFormField("preaction", "goswimming");
            this.addFormField("subaction", "laps");
            break;
          case SPRINTS:
            this.addFormField("preaction", "goswimming");
            this.addFormField("subaction", "submarine");
            break;
          default:
            this.addFormField("action", "swimmingpool");
            break;
        }
        this.addFormField("whichfloor", "2");
        break;

      case ClanLoungeRequest.HOT_DOG_STAND:
        this.constructURLString("clan_viplounge.php");
        if (this.option != 0) {
          this.addFormField("preaction", "eathotdog");
          this.addFormField("whichdog", String.valueOf(option));
        } else {
          this.addFormField("action", "hotdogstand");
        }
        break;

      case ClanLoungeRequest.SPEAKEASY:
        this.constructURLString("clan_viplounge.php");
        if (this.option != 0) {
          this.addFormField("preaction", "speakeasydrink");
          this.addFormField("drink", String.valueOf(option));
        } else {
          this.addFormField("action", "speakeasy");
        }
        this.addFormField("whichfloor", "2");
        break;

      case ClanLoungeRequest.FLOUNDRY:
        this.constructURLString("clan_viplounge.php");
        if (this.option != 0) {
          this.addFormField("preaction", "buyfloundryitem");
          this.addFormField("whichitem", String.valueOf(option));
        } else {
          this.addFormField("action", "floundry");
        }
        break;

      case ClanLoungeRequest.FORTUNE:
        this.constructURLString("clan_viplounge.php");
        this.addFormField("preaction", "lovetester");
        break;

      case ClanLoungeRequest.FIREWORKS:
        this.constructURLString("clan_viplounge.php");
        this.addFormField("action", "fwshop");
        break;

      default:
        break;
    }

    super.run();

    if (this.redirectLocation != null && this.redirectLocation.equals("clan_signup.php")) {
      RequestLogger.printLine("You don't seem to be in a clan!");
      return;
    }

    if (this.responseText == null) {
      // KoL returned a blank page; complain to Jick, not us.
      return;
    }

    switch (this.action) {
      case ClanLoungeRequest.POOL_TABLE:
        if (responseText.contains("You skillfully defeat")) {
          RequestLogger.printLine("You won the pool game!");
        } else if (responseText.contains("You play a game of pool against yourself")) {
          RequestLogger.printLine("You beat yourself at pool. Is that a win or a loss?");
        } else if (responseText.contains("you are unable to defeat")) {
          RequestLogger.printLine("You lost. Boo hoo.");
        } else if (responseText.contains("kind of pooled out")) {
          RequestLogger.printLine("You decided not to play.");
        }
        // Those things are old news.  You only care about the <i>latest</i> gadgets.
        else if (responseText.contains("Those things are old news")) {
          KoLmafia.updateDisplay("Boring! Nobody plays <i>that</i> any more.");
        } else if (!responseText.contains("pooltable.gif")) {
          KoLmafia.updateDisplay("There is no pool table in this clan.");
        } else {
          RequestLogger.printLine("Huh? Unknown response.");
        }
        break;

      case ClanLoungeRequest.FAX_MACHINE:
        if (responseText.contains("Your photocopy slowly slides into the machine")) {
          String monster = Preferences.getString("photocopyMonster");
          if (!monster.equals("")) {
            KoLmafia.updateDisplay("You load your photocopied " + monster + " in the fax machine.");
          } else {
            KoLmafia.updateDisplay("You load your photocopied monster in the fax machine.");
          }
        } else if (responseText.contains("just be a blank sheet of paper")) {
          KoLmafia.updateDisplay("Your fax machine doesn't have any monster.");
        } else if (responseText.contains("top half of a document prints out")) {
          // the message is printed by parseResponse()
        } else if (responseText.contains("waiting for an important fax")) {
          KoLmafia.updateDisplay("You already had a photocopied monster in your inventory.");
        }
        // Those things are old news.  You only care about the <i>latest</i> gadgets.
        else if (responseText.contains("Those things are old news")) {
          KoLmafia.updateDisplay("The fax machine is <i>so</i> last year.");
        }
        // You approach the fax machine.  Loathing wells up
        // within you and fear clutches at your heart...  What
        // do you want to do?
        else if (responseText.contains("What do you want to do?")) {
          // Simple visit.
        } else if (!responseText.contains("faxmachine.gif")) {
          KoLmafia.updateDisplay("There is no fax machine in this clan.");
        } else {
          KoLmafia.updateDisplay("Huh? Unknown response.");
        }
        break;

      case ClanLoungeRequest.APRIL_SHOWER:
        if (responseText.contains("this is way too hot")) {
          RequestLogger.printLine("You took a hot shower.");
        } else if (responseText.contains("relaxes your muscles")) {
          RequestLogger.printLine("You took a warm shower.");
        } else if (responseText.contains("mind expands")) {
          RequestLogger.printLine("You took a lukewarm shower.");
        } else if (responseText.contains("your goosebumps absorb")) {
          RequestLogger.printLine("You took a cool shower.");
        } else if (responseText.contains("shards of frosty double-ice")) {
          RequestLogger.printLine("You took a cold shower.");
        } else if (responseText.contains("already had a shower today")
            || responseText.contains("<table><tr><td></td></tr></table>")
                && responseText.contains("aprilshower.gif")) {
          RequestLogger.printLine("You already took a shower today.");
        } else if (responseText.contains("Shower!")) {
          // Simple visit.
        } else if (!responseText.contains("aprilshower.gif")) {
          KoLmafia.updateDisplay("There is no April shower in this clan.");
        } else {
          RequestLogger.printLine("Huh? Unknown response.");
        }
        break;

      case ClanLoungeRequest.SWIMMING_POOL:
        if (this.redirectLocation != null && this.redirectLocation.startsWith("choice.php")) {
          RequestLogger.printLine("You start screwing around in the swimming pool.");
        } else if (responseText.contains("manage to swim")) {
          // the message is printed by parseResponse()
        } else if (responseText.contains("doing submarine sprints")) {
          // the message is printed by parseResponse()
        } else if (responseText.contains("already worked out in the pool today")
            || responseText.contains("<table><tr><td></td></tr></table>")
                && responseText.contains("vippool.gif")) {
          RequestLogger.printLine("You already worked out in the pool today.");
        } else if (responseText.contains("change into your swimsuit")) {
          // Simple visit.
        } else if (!responseText.contains("vippool.gif")) {
          KoLmafia.updateDisplay("There is no swimming pool in this clan.");
        } else {
          RequestLogger.printLine("Huh? Unknown response.");
        }
        break;
      case ClanLoungeRequest.FIREWORKS:
        if (this.redirectLocation != null
            && this.redirectLocation.contains("shop.php?whichshop=fwshop")) {
          Preferences.setBoolean("_fireworksShop", true);
        } else {
          RequestLogger.printLine("There is no underground fireworks shop in this clan");
        }
        break;
    }
  }

  @Override
  public void processResults() {
    String urlString = this.getURLString();
    String responseText = this.responseText;

    ClanLoungeRequest.parseResponse(urlString, responseText);

    if (this.action == ClanLoungeRequest.HOT_DOG_STAND
        || this.action == ClanLoungeRequest.SPEAKEASY) {
      ResponseTextParser.learnSkill(urlString, responseText);
    }
  }

  private static final Pattern HOTTUB_PATTERN = Pattern.compile("hottub(\\d).gif");

  private static void parseLounge(
      final String action, final String clan, final String responseText) {
    if (!clan.equals(ClanManager.getClanName(false))) {
      ClanManager.setClanName(clan);
      ClanManager.setClanId(0);
    }

    // *** KoL should move this to the second floor as soon as it leaves Standard
    findImage(responseText, "vipfloundry.gif", ItemPool.CLAN_FLOUNDRY);
    findImage(responseText, "fortuneteller.gif", ItemPool.CLAN_CARNIVAL_GAME);
    if (findImage(responseText, "fireworks.gif", ItemPool.CLAN_UNDERGROUND_FIREWORKS_SHOP)
        && !Preferences.getBoolean("_fireworksShop")) {
      // We haven't visited it yet today so it is not unlocked yet.
      new ClanLoungeRequest(ClanLoungeRequest.FIREWORKS).run();
    }

    Matcher hottubMatcher = HOTTUB_PATTERN.matcher(responseText);
    if (hottubMatcher.find()) {
      Preferences.setInteger("_hotTubSoaks", 5 - Integer.parseInt(hottubMatcher.group(1)));
    }
  }

  private static void parseLoungeFloor2(
      final String action, final String clan, final String responseText) {
    if (!clan.equals(ClanManager.getClanName(false))) {
      ClanManager.setClanName(clan);
      ClanManager.setClanId(0);
    }

    findImage(responseText, "lookingglass.gif", ItemPool.CLAN_LOOKING_GLASS);
    Matcher treeMatcher = TREE_LEVEL_PATTERN.matcher(responseText);
    if (treeMatcher.find()) {
      ClanLoungeRequest.setClanLoungeItem(
          ItemPool.CRIMBOUGH, Integer.parseInt(treeMatcher.group(1)));
    }
    findImage(responseText, "aprilshower.gif", ItemPool.CLAN_SHOWER);
    findImage(responseText, "pooltable.gif", ItemPool.CLAN_POOL_TABLE);
    findImage(responseText, "faxmachine.gif", ItemPool.CLAN_FAX_MACHINE);
    findImage(responseText, "hotdogstand.gif", ItemPool.CLAN_HOT_DOG_STAND);
    findImage(responseText, "vippool.gif", ItemPool.CLAN_SWIMMING_POOL);
    findImage(responseText, "speakeasy.gif", ItemPool.CLAN_SPEAKEASY);

    // *** KoL should move this to the second floor as soon as it leaves Standard
    findImage(responseText, "vipfloundry.gif", ItemPool.CLAN_FLOUNDRY);
    findImage(responseText, "fortuneteller.gif", ItemPool.CLAN_CARNIVAL_GAME);

    // Look at the Crimbo tree and report on whether there is a present waiting.
    if (responseText.contains("tree5.gif")) {
      Preferences.setInteger("crimboTreeDays", 0);
      Preferences.setBoolean("_crimboTree", true);
      // Only log it for a simple visit.
      if (action == null) {
        RequestLogger.printLine(
            "You have a present under the Crimbo tree in your clan's VIP lounge!");
      }
    } else if (responseText.contains("crimbotree")) {
      if (!Preferences.getBoolean("_crimboTree")) {
        ClanLoungeRequest request = new ClanLoungeRequest(ClanLoungeRequest.CRIMBO_TREE);
        Preferences.setBoolean("_crimboTree", true);
        request.run();
      }
    } else {
      Preferences.setBoolean("_crimboTree", false);
    }
  }

  private static boolean findImage(
      final String responseText, final String filename, final int itemId) {
    return ClanLoungeRequest.findImage(responseText, filename, itemId, false);
  }

  private static boolean findImage(
      final String responseText, final String filename, final int itemId, boolean allowMultiple) {
    int count = 0;
    int i = responseText.indexOf(filename);
    while (i != -1) {
      ++count;
      i = responseText.indexOf(filename, i + 1);
    }

    if (count > 0) {
      ClanLoungeRequest.setClanLoungeItem(itemId, allowMultiple ? count : 1);
    }

    return (count > 0);
  }

  private static boolean findImage(
      final String responseText, final String filename, final int itemId, int count) {
    if (!responseText.contains(filename)) {
      return false;
    }

    ClanLoungeRequest.setClanLoungeItem(itemId, count);

    return true;
  }

  // The HTML for the Hot Dog Stand is - surprise! - malformed.
  // The table rows contain a <span></td> rather than <span></span>
  //
  // To eat a hot dog:
  //   clan_viplounge.php?preaction=eathotdog&whichdog=xxx
  //
  // To contribute supplies for a hotdog:
  //   clan_viplounge.php?preaction=hotdogsupply&whichdog=xxx&quantity=yyy
  //
  // whichdog = xxx
  //   -92  basic hot dog
  //   -93  savage macho dog
  //   -94  one with everything
  //   -95  sly dog
  //   -96  devil dog
  //   -97  chilly dog
  //   -98  ghost dog
  //   -99  junkyard dog
  //  -100  wet dog
  //  -102  optimal dog
  //  -101  sleeping dog
  //  -103  video games hot dog

  // <input class=button type=submit value=Eat>
  // <input class=button type=submit value=Eat disabled=disabled style='color: #cccccc'>

  private static void registerHotDog(
      String name, int id, boolean available, String supply, int needed, int stocked) {
    StringBuilder buffer = new StringBuilder();
    if (!available) {
      buffer.append("(unavailable) ");
    }
    buffer.append(name);
    buffer.append(" (");
    buffer.append(id);
    buffer.append(")");
    if (!available && needed == 0) {
      buffer.append(" can be unlocked with ");
      buffer.append(supply == null ? "(unknown)" : supply);
    }
    if (needed > 0) {
      buffer.append(" requires ");
      buffer.append(needed);
      buffer.append(" ");
      buffer.append(supply);
      buffer.append(" (");
      buffer.append(stocked);
      buffer.append(" in stock)");
    }

    RequestLogger.printLine(buffer.toString());
  }

  public static boolean availableHotDog(final String itemName) {
    int index = ClanLoungeRequest.hotdogNameToIndex(itemName);
    Concoction item = ClanLoungeRequest.ALL_HOTDOGS.get(index);
    return ConcoctionDatabase.getUsables().contains(item);
  }

  public static Concoction addHotDog(final String itemName) {
    int index = ClanLoungeRequest.hotdogNameToIndex(itemName);
    Concoction item = ClanLoungeRequest.ALL_HOTDOGS.get(index);
    return ConcoctionDatabase.getUsables().contains(item) ? null : item;
  }

  private static final Pattern HOTDOG_PATTERN =
      Pattern.compile(
          ".*?<input class=button type=submit value=Eat( disabled.*?)?>.*?<span onclick='descitem.\"(.*?)_food\".*?<b>(.*?)</b>(?:.*?<img.*?title=\"(.*?)\"(?:.*?<b>x (.*?)</b>.*?([0123456789,]*) in stock)?)?",
          Pattern.DOTALL);

  private static Concoction parseHotDog(final String hotdog) {
    Matcher matcher = HOTDOG_PATTERN.matcher(hotdog);
    if (!matcher.find()) {
      return null;
    }

    boolean disabled = matcher.group(1) != null;
    // String itemIdString = matcher.group(2);
    // int itemId = StringUtilities.parseInt( itemIdString );
    String name = matcher.group(3);
    // String supply = matcher.group(4);
    // String neededString = matcher.group(5);
    // int needed = neededString == null ? 0 : StringUtilities.parseInt( neededString );
    // String stockedString = matcher.group(6);
    // int stocked = stockedString == null ? 0 : StringUtilities.parseInt( stockedString );

    // ClanLoungeRequest.registerHotDog( name, itemId, !disabled, supply, needed, stocked );

    return disabled ? null : ClanLoungeRequest.addHotDog(name);
  }

  private static final Pattern HOTDOG_STAND_PATTERN =
      Pattern.compile(
          "<table>(<tr><form action=clan_viplounge.php method=post>.*?)</table>", Pattern.DOTALL);
  private static final Pattern HOTDOG_ROW_PATTERN = Pattern.compile("<tr>.*?</tr>", Pattern.DOTALL);

  private static void parseHotDogStand(final String responseText) {
    // Rebuild list of available hot dogs every time we visit
    ClanLoungeRequest.resetHotdogs();

    // Extract the table for the hot dog stand
    Matcher standMatcher = HOTDOG_STAND_PATTERN.matcher(responseText);
    if (!standMatcher.find()) {
      return;
    }

    // Make a list of all currently available hot dogs
    ArrayList<Concoction> available = new ArrayList<Concoction>();

    String stand = standMatcher.group(1);
    Matcher hotdogMatcher = HOTDOG_ROW_PATTERN.matcher(stand);
    while (hotdogMatcher.find()) {
      Concoction hotdog = ClanLoungeRequest.parseHotDog(hotdogMatcher.group(0));
      if (hotdog != null) {
        available.add(hotdog);
        ClanManager.addHotdog(hotdog.getName());
      }
    }

    // Add hot dogs en masse to the usables list
    if (available.size() > 0) {
      ConcoctionDatabase.getUsables().addAll(available);
    }

    // Refresh available concoctions with currently available hot dogs
    ConcoctionDatabase.refreshConcoctions();
  }

  private static void registerSpeakeasyDrink(String name, int id, boolean available) {
    StringBuilder buffer = new StringBuilder();
    if (!available) {
      buffer.append("(unavailable) ");
    }
    buffer.append(name);
    buffer.append(" (");
    buffer.append(id);
    buffer.append(")");

    RequestLogger.printLine(buffer.toString());
  }

  public static boolean availableSpeakeasyDrink(final String itemName) {
    int index = ClanLoungeRequest.speakeasyNameToIndex(itemName);
    if (index < 0) {
      return false;
    }
    Concoction item = ClanLoungeRequest.ALL_SPEAKEASY.get(index);
    return ConcoctionDatabase.getUsables().contains(item);
  }

  public static Concoction addSpeakeasyDrink(final String itemName) {
    int index = ClanLoungeRequest.speakeasyNameToIndex(itemName);
    if (index < 0) {
      return null;
    }
    Concoction item = ClanLoungeRequest.ALL_SPEAKEASY.get(index);
    if (ConcoctionDatabase.getUsables().contains(item)) {
      return null;
    }
    item.resetCalculations();
    return item;
  }

  private static final Pattern SPEAKEASY_ROW_PATTERN =
      Pattern.compile("name=\"drink\" value=\"(\\d+)\"", Pattern.DOTALL);

  public static void parseSpeakeasy(final String responseText, final boolean verbose) {
    // Rebuild list of available speakeasy drinks every time we visit
    ClanLoungeRequest.resetSpeakeasy();

    // Update remaining number of drinks available to you today
    if (responseText.contains("have 3 more drinks")) {
      Preferences.setInteger("_speakeasyDrinksDrunk", 0);
    } else if (responseText.contains("have 2 more drinks")) {
      Preferences.setInteger("_speakeasyDrinksDrunk", 1);
    } else if (responseText.contains("have one more drink")) {
      Preferences.setInteger("_speakeasyDrinksDrunk", 2);
    } else if (responseText.contains("had your limit")) {
      Preferences.setInteger("_speakeasyDrinksDrunk", 3);
    }

    // Make a list of all currently available speakeasy drinks
    ArrayList<Concoction> available = new ArrayList<Concoction>();

    Matcher speakeasyMatcher = SPEAKEASY_ROW_PATTERN.matcher(responseText);
    while (speakeasyMatcher.find()) {
      int speakeasyId = StringUtilities.parseInt(speakeasyMatcher.group(1));
      int drinkIndex = ClanLoungeRequest.speakeasyIdToIndex(speakeasyId);
      if (drinkIndex >= 0 && drinkIndex < 12) {
        String drinkName = ClanLoungeRequest.speakeasyIndexToName(drinkIndex);
        Concoction speakeasyDrink = ClanLoungeRequest.addSpeakeasyDrink(drinkName);
        if (verbose) {
          RequestLogger.printLine(
              "Found speakeasy drink #"
                  + speakeasyId
                  + " ("
                  + drinkName
                  + ") "
                  + (speakeasyDrink == null ? "NO" : "and")
                  + " concoction.");
        }
        if (speakeasyDrink != null) {
          available.add(speakeasyDrink);
          AdventureResult drink = ItemPool.get(drinkName, 1);
          if (drink != null) {
            ClanManager.addToLounge(drink);
          }
        }
      }
    }

    if (verbose) {
      RequestLogger.printLine("Total speakeasy drinks found: " + available.size());
    }

    // Add speakeasy drinks en masse to the usables list
    if (available.size() > 0) {
      ConcoctionDatabase.getUsables().addAll(available);
    }

    // Refresh available concoctions with currently available speakeasy drinks
    ConcoctionDatabase.refreshConcoctions();
  }

  public static void parseFloundry(final String responseText, final boolean verbose) {
    // Rebuild list of available floundry items every time we visit
    ClanLoungeRequest.resetFloundry();

    // Make a list of all currently available floundry items
    ArrayList<Concoction> available = new ArrayList<Concoction>();

    Matcher fishStockMatcher = FISH_STOCK_PATTERN.matcher(responseText);
    while (fishStockMatcher.find()) {
      String fishName = fishStockMatcher.group(4);
      StringBuilder buffer = new StringBuilder();
      if (fishStockMatcher.group(1) != null) buffer.append(fishStockMatcher.group(1));
      if (fishStockMatcher.group(2) != null) buffer.append(fishStockMatcher.group(2));
      if (fishStockMatcher.group(3) != null) buffer.append(fishStockMatcher.group(3));
      int fishStock = StringUtilities.parseInt(buffer.toString());

      if (verbose) {
        RequestLogger.printLine(fishName + " stock: " + fishStock + ".");
      }

      if (fishStock >= 10) {
        AdventureResult item = ClanLoungeRequest.floundryFishToItem(fishName);
        Concoction concoction = ConcoctionPool.get(item);
        concoction.setMixingMethod(CraftingType.FLOUNDRY);
        if (item != null) {
          AdventureResult countedItem =
              ItemPool.get(item.getItemId(), (int) Math.floor(fishStock / 10));
          if (countedItem != null) {
            ClanManager.addToLounge(countedItem);
          }
          available.add(concoction);
        }
      }
    }

    // Add Floundry Items en masse to the usables list
    if (available.size() > 0) {
      ConcoctionDatabase.getUsables().addAll(available);
    }

    // Refresh available concoctions with currently available floundry items
    ConcoctionDatabase.refreshConcoctions();
  }

  public static boolean availableFloundryItem(final String itemName) {
    boolean clanFloundryItem = ClanLoungeRequest.hasClanLoungeItem(ItemPool.get(itemName, 1));
    boolean gotFloundryItem =
        InventoryManager.hasItem(ItemPool.CARPE)
            || InventoryManager.hasItem(ItemPool.CODPIECE)
            || InventoryManager.hasItem(ItemPool.TROUTSERS)
            || InventoryManager.hasItem(ItemPool.BASS_CLARINET)
            || InventoryManager.hasItem(ItemPool.FISH_HATCHET)
            || InventoryManager.hasItem(ItemPool.TUNAC);
    return clanFloundryItem && !gotFloundryItem;
  }

  private static final Pattern LOUNGE_PATTERN =
      Pattern.compile(
          "<table.*?<b>Clan VIP Lounge \\(Ground Floor\\)</b>.*?<center><b>(?:<a.*?>)?(.*?)(?:</a>)?</b>.*?</center>(<table.*?</table>)",
          Pattern.DOTALL);
  private static final Pattern LOUNGE2_PATTERN =
      Pattern.compile(
          "<table.*?<b>Clan VIP Lounge \\(Attic\\)</b>.*?<center><b>(?:<a.*?>)?(.*?)(?:</a>)?</b>.*?</center>(<table.*?</table>)",
          Pattern.DOTALL);

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("clan_viplounge.php") || responseText == null) {
      return;
    }

    String action = GenericRequest.getAction(urlString);
    Matcher loungeMatcher = LOUNGE_PATTERN.matcher(responseText);
    if (loungeMatcher.find()) {
      ClanLoungeRequest.parseLounge(action, loungeMatcher.group(1), loungeMatcher.group(2));
    }

    loungeMatcher = LOUNGE2_PATTERN.matcher(responseText);
    if (loungeMatcher.find()) {
      ClanLoungeRequest.parseLoungeFloor2(action, loungeMatcher.group(1), loungeMatcher.group(2));
    }

    if (action == null) {
      return;
    }

    if (action.equals("hottub")) {
      // No action needed here because this is handled already
      return;
    }

    if (action.equals("hotdogstand")) {
      // Visiting the hot dog stand. See what's on offer
      ClanLoungeRequest.parseHotDogStand(responseText);
      return;
    }

    if (action.equals("klaw")) {
      // You carefully guide the claw over a prize and press
      // the button (which is mahogany inlaid with
      // mother-of-pearl -- very nice!) -- the claw slowly
      // descends...
      if (responseText.contains("claw slowly descends")) {
        Preferences.increment("_deluxeKlawSummons", 1);
      }
      // You probably shouldn't play with this machine any
      // more today -- you wouldn't want to look greedy in
      // front of the other VIPs, would you?
      else if (responseText.contains("you wouldn't want to look greedy")) {
        Preferences.setInteger("_deluxeKlawSummons", 3);
      }

      return;
    }

    if (action.equals("pooltable")) {
      // You've already played quite a bit of pool today, so
      // you just watch with your hands in your pockets.

      if (responseText.contains("hands in your pockets")) {
        Preferences.setInteger("_poolGames", 3);
      }

      return;
    }

    if (action.equals("poolgame")) {
      // You skillfully defeat (player) and take control of
      // the table. Go you!
      //
      // You play a game of pool against yourself.
      // Unsurprisingly, you win! Inevitably, you lose.
      //
      // Try as you might, you are unable to defeat
      // (player). Ah well. You gave it your best.

      if (responseText.contains("take control of the table")
          || responseText.contains("play a game of pool against yourself")
          || responseText.contains("you are unable to defeat")) {
        Preferences.increment("_poolGames", 1, 3, false);
      }

      // You're kind of pooled out for today. Maybe you'll be
      // in the mood to play again tomorrow.
      else if (responseText.contains("pooled out for today")) {
        Preferences.setInteger("_poolGames", 3);
      }

      return;
    }

    if (action.equals("speakeasy")) {
      // Visiting the Speakeasy. See what's on offer
      ClanLoungeRequest.parseSpeakeasy(responseText, Preferences.getBoolean("verboseSpeakeasy"));
      return;
    }

    if (action.equals("crimbotree")) {
      // You look under the Crimbo Tree and find a present
      // with your name on it! You excitedly tear it open.
      if (responseText.contains("You look under the Crimbo Tree and find a present")) {
        Preferences.setInteger("crimboTreeDays", 7);
      } else if (responseText.contains("Check back tomorrow")) {
        Preferences.setInteger("crimboTreeDays", 1);
      } else if (responseText.contains("There's nothing under the Crimbo Tree")) {
        int ctd;
        String groupStr = "";
        Matcher m = TREE_PATTERN.matcher(responseText);
        boolean matchFound = m.find();
        if (matchFound) {
          for (int i = 0; i <= m.groupCount(); i++) {
            groupStr = m.group(i);
            if (!StringUtilities.isNumeric(groupStr)) continue;
            ctd = Integer.parseInt(groupStr);
            Preferences.setInteger("crimboTreeDays", ctd);
            return;
          }
        }
      }

      return;
    }

    if (action.equals("lookingglass")) {
      Preferences.setBoolean("_lookingGlass", true);
      return;
    }

    if (action.equals("faxmachine")) {
      return;
    }

    if (action.equals("sendfax")) {
      if (responseText.contains("Your photocopy slowly slides into the machine")) {
        ResultProcessor.processItem(ItemPool.PHOTOCOPIED_MONSTER, -1);
        Preferences.setString("photocopyMonster", "");
        return;
      }

      return;
    }

    if (action.equals("receivefax")) {
      if (responseText.contains("top half of a document prints out")) {
        String description =
            DebugDatabase.rawItemDescriptionText(
                ItemDatabase.getDescriptionId(ItemPool.PHOTOCOPIED_MONSTER), true);
        ConsequenceManager.parseItemDesc(
            ItemDatabase.getDescriptionId(ItemPool.PHOTOCOPIED_MONSTER), description);

        String monster = Preferences.getString("photocopyMonster");
        if (monster.equals("")) {
          monster = "monster";
        }
        KoLmafia.updateDisplay("You receive a photocopied " + monster + " from the fax machine.");
      }
      return;
    }

    if (action.equals("shower")) {
      if (responseText.contains("already had a shower today")) {
        Preferences.setBoolean("_aprilShower", true);
      }
      return;
    }

    if (action.equals("takeshower")) {
      if (responseText.contains("this is way too hot")
          || responseText.contains("relaxes your muscles")
          || responseText.contains("mind expands")
          || responseText.contains("your goosebumps absorb")
          || responseText.contains("shards of frosty double-ice")
          || responseText.contains("already had a shower today")
          || responseText.contains("<table><tr><td></td></tr></table>")
              && responseText.contains("aprilshower.gif")) {
        Preferences.setBoolean("_aprilShower", true);
        ResponseTextParser.learnRecipe(urlString, responseText);
      }
      return;
    }

    if (action.equals("swimmingpool")) {
      if (responseText.contains("already worked out in the pool today")) {
        Preferences.setBoolean("_olympicSwimmingPool", true);
      }
      return;
    }

    if (action.equals("goswimming")) {
      if (responseText.contains("<table><tr><td></td></tr></table>")
          && responseText.contains("vippool.gif")) {
        Preferences.setBoolean("_olympicSwimmingPool", true);
        return;
      }

      Matcher m = LAPS_PATTERN.matcher(responseText);
      if (m.find()) {
        KoLmafia.updateDisplay("You swam " + m.group(1) + " laps.");
        Preferences.setBoolean("_olympicSwimmingPool", true);
        return;
      }

      m = SPRINTS_PATTERN.matcher(responseText);
      if (m.find()) {
        KoLmafia.updateDisplay("You did " + m.group(1) + " submarine sprints.");
        Preferences.setBoolean("_olympicSwimmingPool", true);
        return;
      }
      return;
    }

    if (action.equals("floundry")) {
      // Visiting the Floundry. See what's on offer
      ClanLoungeRequest.parseFloundry(responseText, Preferences.getBoolean("verboseFloundry"));
      return;
    }

    if (action.equals("buyfloundryitem")) {
      // Buying an item. If it succeeded or failed because
      // you have already bought an item from the Clan
      // Floundry today, remember it.

      // There is a clattering of machinery followed by a
      // series of wet smacking sounds. Your freshly fishily
      // fabricated item plops into the floundery's output
      // hopper.
      //
      // You acquire... something.

      if (responseText.contains("You acquire")) {
        Preferences.setBoolean("_floundryItemCreated", true);
        ConcoctionDatabase.refreshConcoctions();
      }

      // *** What is the message if you've already created an
      // item today? That should also set the property.

      return;
    }

    if (action.equals("eathotdog")) {
      // Do nothing if consumption of a basic hot dog failed
      // Do nothing if overeating on basic hot dogs causes you to lose stats
      // Don't be fooled!
      //   You lose some of an effect: Got Milk
      //   You lose an effect: Got Milk
      //   The Mayodiol kicks in and converts some of what you just ate into pure ethanol. You lose
      // 1 Fullness.
      if (responseText.contains("You don't feel up to eating that")
          || (!responseText.contains("You lose some of an effect")
              && !responseText.contains("You lose an effect")
              && !responseText.contains("Mayodiol kicks in")
              && responseText.contains("You lose"))) {
        return;
      }
      // Recognize when you fail to eat the hot dog because you are too full
      if (responseText.contains("You're too full")) {
        return;
      }
      // If consumption of a fancy hot dog failed, remember
      if (responseText.contains("You aren't in the mood for any more fancy dogs today")) {
        Preferences.setBoolean("_fancyHotDogEaten", true);
        return;
      }
      Matcher m = WHICHDOG_PATTERN.matcher(urlString);
      if (!m.find()) {
        return;
      }
      int index = ClanLoungeRequest.hotdogIdToIndex(StringUtilities.parseInt(m.group(1)));
      if (index < 0) {
        return;
      }
      String name = ClanLoungeRequest.hotdogIndexToName(index);
      // Handle food helpers and adjust fullness, if necessary
      EatItemRequest.handleFoodHelper(name, 1, responseText);
      if (index > 0) {
        Preferences.setBoolean("_fancyHotDogEaten", true);
      }

      return;
    }
    if (action.equals("hotdogsupply")) {
      // You have put some hot dog making supplies into the
      // hot dog cart man's hot dog cart supply crate.
      if (!responseText.contains("You have put some hot dog making supplies")) {
        return;
      }

      //   clan_viplounge.php?preaction=hotdogsupply&whichdog=-101&quantity=10
      Matcher m = WHICHDOG_PATTERN.matcher(urlString);
      if (!m.find()) {
        return;
      }
      int index = ClanLoungeRequest.hotdogIdToIndex(StringUtilities.parseInt(m.group(1)));
      if (index < 0) {
        return;
      }
      AdventureResult item = ClanLoungeRequest.hotdogIndexToItem(index);
      if (item == null) {
        return;
      }
      m = GenericRequest.QUANTITY_PATTERN.matcher(urlString);
      int quantity = m.find() ? StringUtilities.parseInt(m.group(1)) : 1;
      if (urlString.contains("hagnks=1")) {
        AdventureResult.removeResultFromList(
            KoLConstants.storage, ItemPool.get(item.getItemId(), -quantity));
      } else {
        ResultProcessor.processItem(item.getItemId(), -1 * quantity);
      }
      return;
    }
    if (action.equals("unlockhotdog")) {
      // <unlock message>
      // <b>You have unlocked a new hot dog!</b>
      // Your clan members should be really grateful!

      if (!responseText.contains("Your clan members should be really grateful")) {
        return;
      }

      //   clan_viplounge.php?preaction=unlockhotdog&whichdog=-101
      Matcher m = WHICHDOG_PATTERN.matcher(urlString);
      if (!m.find()) {
        return;
      }
      int index = ClanLoungeRequest.hotdogIdToIndex(StringUtilities.parseInt(m.group(1)));
      if (index < 0) {
        return;
      }
      AdventureResult item = ClanLoungeRequest.hotdogIndexToUnlocker(index);
      if (item == null) {
        return;
      }
      ResultProcessor.processItem(item.getItemId(), -1);
      return;
    }
    if (action.equals("speakeasydrink")) {
      // Do nothing if consumption of a speakeasy drink failed
      // Find failure messages and handle
      if (responseText.contains("We don't serve minors here, kid")
          || responseText.contains("You can't afford that")) {
        return;
      }
      if (responseText.contains("You pour your drink into your mime army shotglass")) {
        Preferences.setBoolean("_mimeArmyShotglassUsed", true);
      }
      Matcher m = WHICH_SPEAKEASY_PATTERN.matcher(urlString);
      if (!m.find()) {
        return;
      }
      int index = ClanLoungeRequest.speakeasyIdToIndex(StringUtilities.parseInt(m.group(1)));
      if (index >= 0) {
        Preferences.increment("_speakeasyDrinksDrunk", 1);
      }

      return;
    }
  }

  public static void getBreakfast() {
    // No Clan Lounge in Bad Moon
    if (KoLCharacter.inBadMoon() && !KoLCharacter.kingLiberated()) {
      return;
    }

    // Visit the lounge to see what furniture is available in ground floor
    if (!visitLounge()) {
      return;
    }

    // The Klaw can be accessed regardless of whether or not
    // you are in hardcore, so handle it first.
    //
    // Unlike the regular Klaw, there is no message to tell you
    // that you are done for the day except when you try one too
    // many times: "You probably shouldn't play with this machine
    // any more today -- you wouldn't want to look greedy in front
    // of the other VIPs, would you?"

    ClanLoungeRequest request = new ClanLoungeRequest(KLAW);
    while (Preferences.getInteger("_deluxeKlawSummons") < 3) {
      request.run();
    }

    if (VISIT_REQUEST.responseText.contains("fireworks.gif")
        && !Preferences.getBoolean("_fireworksShop")) {
      request = new ClanLoungeRequest(FIREWORKS);
      request.run();
    }

    // Visit the lounge to see what furniture is available in ground floor
    if (!visitLoungeFloor2()) {
      return;
    }

    // Not every clan has a looking glass
    if (VISIT_REQUEST.responseText.contains("lookingglass.gif")
        && !Preferences.getBoolean("_lookingGlass")) {
      request = new ClanLoungeRequest(LOOKING_GLASS);
      request.run();
    }

    // Not every clan has a crimbo tree
    if (VISIT_REQUEST.responseText.contains("crimbotree")) {
      Preferences.setBoolean("_crimboTree", true);
    }

    if (VISIT_REQUEST.responseText.contains("tree5.gif")) {
      // Get the crimbo gift now whenever breakfast is run, since there is
      // no reason not to anymore.
      request = new ClanLoungeRequest(CRIMBO_TREE);
      request.run();
    }

    // Not every clan has a swimming pool
    if (VISIT_REQUEST.responseText.contains("vippool.gif")
        && !Preferences.getBoolean("_olympicSwimmingPoolItemFound")) {
      try {
        RequestThread.postRequest(new ClanLoungeRequest(SWIMMING_POOL, CANNONBALL));
        RequestThread.postRequest(
            new ClanLoungeSwimmingPoolRequest(ClanLoungeSwimmingPoolRequest.HANDSTAND));
        RequestThread.postRequest(
            new ClanLoungeSwimmingPoolRequest(ClanLoungeSwimmingPoolRequest.TREASURE));
      } finally {
        RequestThread.postRequest(
            new ClanLoungeSwimmingPoolRequest(ClanLoungeSwimmingPoolRequest.GET_OUT));
      }
    }
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("clan_viplounge.php")) {
      return false;
    }

    String message = ClanLoungeRequest.equipmentVisit(urlString);

    if (message == null) {
      String action = GenericRequest.getAction(urlString);
      if (action == null) {
        return true;
      }
      if (action.equals("poolgame")) {
        Matcher m = STANCE_PATTERN.matcher(urlString);
        if (!m.find()) {
          return false;
        }
        int stance = StringUtilities.parseInt(m.group(1));
        if (stance < 1 || stance > POOL_GAMES.length) {
          return false;
        }
        message = "pool " + POOL_GAMES[stance - 1][0];
      } else if (action.equals("sendfax") || action.equals("receivefax")) {
        Matcher m = FAX_PATTERN.matcher(urlString);
        if (!m.find()) {
          return false;
        }
        String faxCommand = m.group(1);
        if (!faxCommand.equals("send") && !faxCommand.equals("receive")) {
          return false;
        }
        message = "fax " + faxCommand;
      } else if (action.equals("takeshower")) {
        Matcher m = TEMPERATURE_PATTERN.matcher(urlString);
        if (!m.find()) {
          return false;
        }
        int temp = StringUtilities.parseInt(m.group(1));
        if (temp < 1 || temp > SHOWER_OPTIONS.length) {
          return false;
        }
        message = "shower " + SHOWER_OPTIONS[temp - 1][0];
      } else if (action.equals("goswimming")) {
        Matcher m = SWIMMING_POOL_PATTERN.matcher(urlString);
        if (!m.find()) {
          return false;
        }
        String poolCommand = m.group(1);
        if (!poolCommand.equals("screwaround")
            && !poolCommand.equals("laps")
            && !poolCommand.equals("submarine")) {
          return false;
        }
        message = "swimming pool " + poolCommand;
      } else if (action.equals("eathotdog")) {
        //   clan_viplounge.php?preaction=eathotdog&whichdog=xxx
        Matcher m = WHICHDOG_PATTERN.matcher(urlString);
        if (!m.find()) {
          return false;
        }
        String hotdog = ClanLoungeRequest.hotdogIdToName(StringUtilities.parseInt(m.group(1)));
        if (hotdog == null) {
          return false;
        }
        message = "eat 1 " + hotdog;
      } else if (action.equals("hotdogsupply")) {
        //   clan_viplounge.php?preaction=hotdogsupply&whichdog =-101&quantity=10
        Matcher m = WHICHDOG_PATTERN.matcher(urlString);
        if (!m.find()) {
          return false;
        }
        int index = ClanLoungeRequest.hotdogIdToIndex(StringUtilities.parseInt(m.group(1)));
        AdventureResult item = ClanLoungeRequest.hotdogIndexToItem(index);
        if (item == null) {
          return false;
        }
        m = GenericRequest.QUANTITY_PATTERN.matcher(urlString);
        int quantity = m.find() ? StringUtilities.parseInt(m.group(1)) : 1;
        message = "stock Hot Dog Stand with " + quantity + " " + item.getPluralName(quantity);
      } else if (action.equals("unlockhotdog")) {
        //   clan_viplounge.php?preaction=unlockhotdog&whichdog=xxx
        Matcher m = WHICHDOG_PATTERN.matcher(urlString);
        if (!m.find()) {
          return false;
        }
        String hotdog = ClanLoungeRequest.hotdogIdToName(StringUtilities.parseInt(m.group(1)));
        if (hotdog == null) {
          return false;
        }
        message = "unlock " + hotdog;
      } else if (action.equals("speakeasydrink")) {
        //   clan_viplounge.php?preaction=speakeasydrink&drink=xxx
        Matcher m = WHICH_SPEAKEASY_PATTERN.matcher(urlString);
        if (!m.find()) {
          return false;
        }
        String speakeasyDrink =
            ClanLoungeRequest.speakeasyIdToName(StringUtilities.parseInt(m.group(0)));
        if (speakeasyDrink == null) {
          return false;
        }
        message = "drink 1 " + speakeasyDrink;
      } else {
        return false;
      }
    } else {
      RequestLogger.printLine(message);
    }

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog(message);

    return true;
  }
}
