package net.sourceforge.kolmafia.request;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.Limitmode;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ClanRumpusRequest extends GenericRequest {
  private static final Pattern SPOT_PATTERN = Pattern.compile("spot=(\\d*)");
  private static final Pattern FURNI_PATTERN = Pattern.compile("furni=(\\d*)");
  private static final Pattern ROOM_PATTERN =
      Pattern.compile("action=click&spot=(\\d)&furni=(\\d)");
  private static final Pattern BALLPIT_PATTERN = Pattern.compile("with ([\\d,]+) ball");

  public enum RequestType {
    SEARCH,
    MUSCLE,
    MYST,
    MOXIE,
    SOFA,
    CHIPS,
    BALLS,
    JUKEBOX,
    VISIT,
    ;

    public static RequestType fromString(String text) {
      if (text != null) {
        for (RequestType req : RequestType.values()) {
          if (text.equals(req.toString())) {
            return req;
          }
        }
      }
      return RequestType.SEARCH;
    }
  }

  public static final int RADIUM = 1;
  public static final int WINTERGREEN = 2;
  public static final int ENNUI = 3;

  public static final Object[][] CHIP_FLAVORS =
      new Object[][] {
        {"radium", IntegerPool.get(RADIUM)},
        {"wintergreen", IntegerPool.get(WINTERGREEN)},
        {"ennui", IntegerPool.get(ENNUI)},
      };

  public static final Object[][] SONGS =
      new Object[][] {
        {"meat", "Material Witness", IntegerPool.get(1)},
        {"stats", "No Worries", IntegerPool.get(2)},
        {"item", "Techno Bliss", IntegerPool.get(3)},
        {"initiative", "Metal Speed", IntegerPool.get(4)},
      };

  public static final int findChips(final String name) {
    for (int i = 0; i < CHIP_FLAVORS.length; ++i) {
      String flavor = (String) CHIP_FLAVORS[i][0];
      if (name.equals(flavor)) {
        Integer index = (Integer) CHIP_FLAVORS[i][1];
        return index.intValue();
      }
    }

    return 0;
  }

  public static final int findSong(final String name) {
    if (StringUtilities.isNumeric(name)) {
      int n = StringUtilities.parseInt(name);
      return n > 0 && n <= SONGS.length ? n : 0;
    }

    for (int i = 0; i < SONGS.length; ++i) {
      String modifier = (String) SONGS[i][0];
      String effect = (String) SONGS[i][1];
      if (name.equals(modifier) || name.equals(effect)) {
        return i + 1;
      }
    }

    return 0;
  }

  private static final Pattern TURN_PATTERN = Pattern.compile("numturns=(\\d+)");

  public enum Equipment {
    NONE("", 0, 0, 0),

    GIRL_CALENDAR("Girls of Loathing Calendar", 1, 1, 0),
    BOY_CALENDAR("Boys of Loathing Calendar", 1, 2, 0),
    PAINTING("Infuriating Painting", 1, 3, 0),
    MEAT_ORCHID("Exotic Hanging Meat Orchid", 1, 4, 1),

    ARCANE_TOMES("Collection of Arcane Tomes and Whatnot", 2, 1, 0),
    SPORTS_MEMORABILIA("Collection of Sports Memorabilia", 2, 2, 0),
    SELF_HELP_BOOKS("Collection of Self-Help Books", 2, 3, 0),

    SODA_MACHINE("Soda Machine", 3, 1, 3),
    JUKEBOX("Jukebox", 3, 2, 0),
    KLAW_GAME("Mr. Klaw \"Skill\" Crane Game", 3, 3, 3),

    RADIO("Old-Timey Radio", 4, 1, 1),
    POTTED_MEAT_BUSH("Potted Meat Bush", 4, 2, 1),
    DESK_CALENDAR("Inspirational Desk Calendar", 4, 3, 0),

    WRESTLING_MAT("Wrestling Mat", 5, 1, 0),
    TANNING_BED("Tan-U-Lots Tanning Bed", 5, 2, 0),
    COMFY_SOFA("Comfy Sofa", 5, 3, 0),

    BALLPIT("Awesome Ball Pit", 7, 1, 0),

    HOBO_WORKOUT("Hobo-Flex Workout System", 9, 1, 0),
    SNACK_MACHINE("Snack Machine", 9, 2, 0),
    POTTED_MEAT_TREE("Potted Meat Tree", 9, 3, 1),
    ;

    public String name;
    public int slot;
    public int furni;
    public int maxUses;

    Equipment(String name, int slot, int furni, int maxUses) {
      this.name = name;
      this.slot = slot;
      this.furni = furni;
      this.maxUses = maxUses;
    }

    public static String equipmentName(int slot, int furni) {
      if (furni == 0) {
        return "";
      }
      for (Equipment equipment : Equipment.values()) {
        if (slot == equipment.slot && furni == equipment.furni) {
          return equipment.name;
        }
      }
      return "";
    }

    @Override
    public String toString() {
      return this.name;
    }

    public static Equipment toEquip(String name) {
      if (name == null || name.equals("")) {
        return NONE;
      }
      for (Equipment equipment : Equipment.values()) {
        if (name.equals(equipment.name)) {
          return equipment;
        }
      }
      return NONE;
    }
  }

  private final RequestType action;
  private int option;
  private int turnCount;

  private ClanRumpusRequest() {
    super("clan_rumpus.php");
    this.action = RequestType.VISIT;
  }

  /**
   * Constructs a new <code>ClanRumpusRequest</code>.
   *
   * @param action The identifier for the action you're requesting
   */
  public ClanRumpusRequest(final RequestType action) {
    super("clan_rumpus.php");
    this.action = action;
  }

  public ClanRumpusRequest(final RequestType action, final int option) {
    super("clan_rumpus.php");
    this.action = action;
    this.option = option;
  }

  /**
   * Runs the request. Note that this does not report an error if it fails; it merely parses the
   * results to see if any gains were made.
   */
  public ClanRumpusRequest setTurnCount(final int turnCount) {
    this.turnCount = turnCount;
    return this;
  }

  private void visitEquipment(final int spot, final int furniture) {
    this.clearDataFields();
    this.addFormField("action", "click");
    this.addFormField("spot", String.valueOf(spot));
    this.addFormField("furni", String.valueOf(furniture));
  }

  @Override
  public int getAdventuresUsed() {
    return this.turnCount;
  }

  @Override
  public void run() {
    // Sometimes can't access in Limitmode
    if (Limitmode.limitClan()) {
      return;
    }

    switch (this.action) {
      case SEARCH:
        this.addFormField("action", "click");
        this.addFormField("spot", "7");
        break;

      case MUSCLE:
        // If we can do inside Degrassi Knoll use the gym.
        if (KoLCharacter.knollAvailable() && !KoLCharacter.inZombiecore()) {
          // First load the choice adventure page
          RequestThread.postRequest(new PlaceRequest("knoll_friendly", "dk_gym"));
          this.constructURLString("choice.php");
          this.addFormField("whichchoice", "792");
          this.addFormField("option", "1");
        }
        // Otherwise, use the one in our clan - if we're in one.
        else {
          if (!ClanManager.getClanRumpus().contains("Hobo-Flex Workout System")) {
            KoLmafia.updateDisplay(MafiaState.ERROR, "You don't have access to a clan muscle gym.");
            return;
          }
          this.constructURLString("clan_rumpus.php");
          this.addFormField("preaction", "gym");
          this.addFormField("whichgym", "3");
        }
        break;

      case MYST:
        // If we can go to Little Canadia, use the gym.
        if (KoLCharacter.canadiaAvailable()) {
          // First load the choice adventure page
          RequestThread.postRequest(new PlaceRequest("canadia", "lc_institute"));
          this.constructURLString("choice.php");
          this.addFormField("whichchoice", "770");
          this.addFormField("option", "1");
        }
        // Otherwise, use the one in our clan - if we're in one.
        else {
          if (!ClanManager.getClanRumpus().contains("Collection of Arcane Tomes and Whatnot")) {
            KoLmafia.updateDisplay(
                MafiaState.ERROR, "You don't have access to a clan mysticality gym.");
            return;
          }
          this.constructURLString("clan_rumpus.php");
          this.addFormField("preaction", "gym");
          this.addFormField("whichgym", "1");
        }
        break;

      case MOXIE:
        // If we can go to the Gnomish Gnomads Camp, use the gym
        if (KoLCharacter.gnomadsAvailable()) {
          this.constructURLString("gnomes.php");
          this.addFormField("action", "train");
        }
        // Otherwise, use the one in our clan - if we're in one.
        else {
          if (!ClanManager.getClanRumpus().contains("Tan-U-Lots Tanning Bed")) {
            KoLmafia.updateDisplay(MafiaState.ERROR, "You don't have access to a clan moxie gym.");
            return;
          }
          this.constructURLString("clan_rumpus.php");
          this.addFormField("preaction", "gym");
          this.addFormField("whichgym", "2");
        }
        break;

      case SOFA:
        if (!ClanManager.getClanRumpus().contains("Comfy Sofa")) {
          KoLmafia.updateDisplay(MafiaState.ERROR, "Your clan doesn't have a sofa.");
          return;
        }
        this.constructURLString("clan_rumpus.php");
        this.addFormField("preaction", "nap");
        break;

      case CHIPS:
        if (!ClanManager.getClanRumpus().contains("Snack Machine")) {
          KoLmafia.updateDisplay(MafiaState.ERROR, "Your clan doesn't have a Snack Machine.");
          return;
        }
        this.constructURLString("clan_rumpus.php");
        this.addFormField("preaction", "buychips");
        this.addFormField("whichbag", String.valueOf(this.option));
        break;

      case BALLS:
        this.constructURLString("clan_rumpus.php");
        this.addFormField("preaction", "ballpit");
        break;

      case JUKEBOX:
        if (!ClanManager.getClanRumpus().contains("Jukebox")) {
          KoLmafia.updateDisplay(MafiaState.ERROR, "Your clan doesn't have a Jukebox.");
          return;
        }
        this.constructURLString("clan_rumpus.php");
        this.addFormField("preaction", "jukebox");
        this.addFormField("whichsong", String.valueOf(this.option));
        break;

      default:
        break;
    }

    if (this.turnCount > 0) {
      this.addFormField("numturns", String.valueOf(this.turnCount));

      if (KoLCharacter.getAdventuresLeft() < this.turnCount) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Insufficient adventures.");
        return;
      }
    } else if (this.action == RequestType.SOFA) {
      return;
    }

    if (this.action != RequestType.SEARCH) {
      KoLmafia.updateDisplay("Executing request...");
    }

    super.run();
  }

  @Override
  public void processResults() {
    ClanRumpusRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (urlString.startsWith("choice.php")) {
      if (urlString.contains("whichchoice=770") || urlString.contains("whichchoice=792")) {
        if (responseText.contains("feel the burn")
            || responseText.contains("learn from the sages")) {
          RequestLogger.printLine("Workout completed.");
        } else {
          KoLmafia.updateDisplay(MafiaState.ABORT, "You can't access that gym");
        }
      }
      return;
    }

    if (urlString.startsWith("gnomes.php")) {
      if (urlString.contains("place=train")) {
        if (responseText.contains("learn to be sneakier")) {
          RequestLogger.printLine("Workout completed.");
        } else {
          KoLmafia.updateDisplay(MafiaState.ABORT, "You can't access that gym");
        }
      }
      return;
    }

    if (!urlString.startsWith("clan_rumpus.php")) {
      return;
    }

    // Start by saving the number of balls in the pit, in case this response
    // doesn't have that information.  If the user switches clans, then the
    // first check is guaranteed to have that information.

    String ballpit = null;
    if (!ClanManager.getClanRumpus().isEmpty()) {
      String last = ClanManager.getClanRumpus().get(ClanManager.getClanRumpus().size() - 1);
      if (last.startsWith("Awesome Ball Pit")) {
        ballpit = last;
      }
    }

    ClanManager.getClanRumpus().clear();
    Matcher matcher = ClanRumpusRequest.ROOM_PATTERN.matcher(responseText);
    while (matcher.find()) {
      int spot = StringUtilities.parseInt(matcher.group(1));
      int furni = StringUtilities.parseInt(matcher.group(2));
      String equipmentName = ClanRumpusRequest.Equipment.equipmentName(spot, furni);
      if (!equipmentName.equals("")) {
        ClanManager.addToRumpus(equipmentName);
      }
    }

    if (responseText.contains("action=click&spot=7")) {
      // We have an Awesome Ball Pit
      matcher = ClanRumpusRequest.BALLPIT_PATTERN.matcher(responseText);
      if (matcher.find()) {
        String balls = matcher.group(1);
        ballpit = "Awesome Ball Pit (" + balls + ")";
      } else if (responseText.contains("single ball")) {
        ballpit = "Awesome Ball Pit (1)";
      }
      if (ballpit != null) {
        ClanManager.addToRumpus(ballpit);
      }
    }
    KoLCharacter.recalculateAdjustments();
    KoLCharacter.updateStatus();

    matcher = GenericRequest.ACTION_PATTERN.matcher(urlString);
    String action = matcher.find() ? matcher.group(1) : null;

    if (action == null) {
      return;
    }

    if (action.equals("gym")) {
      if (responseText.contains("You work it on out.")
          || responseText.contains("You study the secrets of the cosmos.")
          || responseText.contains("You bake under the artificial sunlight.")) {
        KoLmafia.updateDisplay("Workout completed.");
      } else {
        KoLmafia.updateDisplay(MafiaState.ABORT, "You can't access that gym");
      }
      return;
    }

    if (action.equals("nap")) {
      // You take a nap on the comfy sofa.
      if (responseText.contains("You take a nap")) {
        KoLmafia.updateDisplay("Resting completed.");
      }
      // Either you aren't in a clan or your clan doesn't have a sofa
      else {
        KoLmafia.updateDisplay(MafiaState.ABORT, "Resting failed - no Clan Sofa available.");
      }
      return;
    }

    if (action.equals("ballpit")) {
      // You play in the ball pit. Wheeeeeee!
      // (You've already played in the ball pit today.)
      if (responseText.contains("play in the ball pit")
          || responseText.contains("already played in the ball pit")) {
        Preferences.setBoolean("_ballpit", true);
      }
      return;
    }

    if (action.equals("buychips")) {
      // a bag of chips drops into the tray at the bottom
      if (responseText.contains("a bag of chips drops")) {
        Preferences.increment("_chipBags", 1);
      }
      // You press the button and the big metal coil rotates,
      // but not far enough to actually drop the
      // chips. Dangit!
      else if (responseText.contains("but not far enough")) {
        Preferences.setInteger("_chipBags", 3);
      }

      return;
    }

    if (action.equals("jukebox")) {
      // Whether we get a song or not, we are done for the
      // day with the Jukebox, unless we ascend, which will
      // reset the preference.
      Preferences.setBoolean("_jukebox", true);
      return;
    }

    if (urlString.contains("spot=3") && urlString.contains("furni=3")) {
      // You carefully guide the claw over the prize that
      // looks the easiest to grab. You press the button and
      // the claw slowly descends.
      if (responseText.contains("slowly descends")) {
        Preferences.increment("_klawSummons", 1);
      }
      // The machine makes a horrible clanking noise, and a
      // wisp of smoke pours out of the prize chute.
      //
      // The crane machine seems to be broken down. Oh
      // well. Maybe they'll fix it by tomorrow.
      else if (responseText.contains("seems to be broken down")) {
        Preferences.setInteger("_klawSummons", 3);
      }

      return;
    }
  }

  public static void getBreakfast() {
    // Sometimes can't access in Limitmode
    if (Limitmode.limitClan()) {
      return;
    }

    ClanRumpusRequest request = new ClanRumpusRequest();

    // The Klaw can be accessed regardless of whether or not
    // you are in hardcore, so handle it first.

    if (ClanManager.getClanRumpus().contains("Mr. Klaw \"Skill\" Crane Game")) {
      request.visitEquipment(3, 3);

      int klawCount = Preferences.getInteger("_klawSummons");
      while (klawCount < 3) {
        request.run();

        int count = Preferences.getInteger("_klawSummons");
        if (klawCount == count) {
          KoLmafia.updateDisplay(
              MafiaState.ERROR, "Something went wrong while using the Klaw Machine.");
          break;
        }

        klawCount = count;
      }
    }

    if (!KoLCharacter.canInteract()) {
      return;
    }

    List<String> rumpus = new ArrayList<String>(ClanManager.getClanRumpus());
    for (String equip : rumpus) {
      Equipment equipment = Equipment.toEquip(equip);
      // Skip the Mr. Klaw game, since we ran it above
      if (equipment == Equipment.KLAW_GAME) {
        continue;
      }

      // Things that can be used only once a day should have
      // daily preferences to track that and we should check
      // that rather than making a server hit

      request.visitEquipment(equipment.slot, equipment.furni);
      for (int i = 0; i < equipment.maxUses; i++) {
        request.run();
      }
    }
  }

  public static boolean registerRequest(final String urlString) {
    String action = null;

    if (urlString.startsWith("clan_rumpus.php")) {
      if (urlString.contains("whichgym=3")) {
        action = "Pump Up Muscle";
      } else if (urlString.contains("whichgym=1")) {
        action = "Pump Up Mysticality";
      } else if (urlString.contains("whichgym=2")) {
        action = "Pump Up Moxie";
      } else if (urlString.contains("preaction=nap")) {
        action = "Rest in Clan Sofa";
      }
    } else if (urlString.startsWith("place.php")) {
      if (urlString.contains("action=dk_gym")) {
        action = "Pump Up Muscle";
      } else if (urlString.contains("action=lc_institute")) {
        action = "Pump Up Mysticality";
      }
    } else if (urlString.startsWith("gnomes.php")) {
      if (urlString.contains("action=train")) {
        action = "Pump Up Moxie";
      }
    }

    if (action != null) {
      Matcher matcher = ClanRumpusRequest.TURN_PATTERN.matcher(urlString);
      if (!matcher.find()) {
        String message = "[" + KoLAdventure.getAdventureCount() + "] " + action;

        RequestLogger.printLine();
        RequestLogger.updateSessionLog();

        RequestLogger.printLine(message);
        RequestLogger.updateSessionLog(message);
        return true;
      }

      // If not enough turns available, nothing will happen.
      int turns = StringUtilities.parseInt(matcher.group(1));
      int available = KoLCharacter.getAdventuresLeft();
      if (turns > available) {
        return true;
      }

      String message =
          "[" + KoLAdventure.getAdventureCount() + "] " + action + " (" + turns + " turns)";

      RequestLogger.printLine();
      RequestLogger.updateSessionLog();

      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
      return true;
    }

    if (!urlString.startsWith("clan_rumpus.php")) {
      return false;
    }

    if (urlString.contains("action=buychips")) {
      String message = "Buying chips from the Snack Machine in the clan rumpus room";
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
      return true;
    }

    if (urlString.contains("preaction=ballpit")) {
      String message = "Jumping into the Awesome Ball Pit in the clan rumpus room";
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
      return true;
    }

    if (urlString.contains("preaction=jukebox")) {
      String message = "Playing a song on the Jukebox in the clan rumpus room";
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
      return true;
    }

    // The only other actions we handle here are clicking on clan
    // furniture

    if (!urlString.contains("action=click")) {
      return false;
    }

    Matcher matcher = SPOT_PATTERN.matcher(urlString);
    if (!matcher.find()) {
      return true;
    }

    int spot = StringUtilities.parseInt(matcher.group(1));

    matcher = FURNI_PATTERN.matcher(urlString);
    if (!matcher.find()) {
      return true;
    }

    int furniture = StringUtilities.parseInt(matcher.group(1));

    String equipment = ClanRumpusRequest.Equipment.equipmentName(spot, furniture);

    if (equipment == null) {
      return false;
    }

    String message = "Visiting " + equipment + " in clan rumpus room";
    RequestLogger.printLine(message);
    RequestLogger.updateSessionLog(message);

    return true;
  }
}
