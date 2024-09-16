package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;

public class VolcanoIslandRequest extends GenericRequest {
  public static final Pattern ACTION_PATTERN = Pattern.compile("(action|subaction)=([^&]*)");

  // Actions
  private static final String NPC = "npc";

  // Subactions
  private static final String SLIME = "getslime";

  private VolcanoIslandRequest() {
    this(NPC);
  }

  /**
   * Constructs a new <code>VolcanoIslandRequest</code>.
   *
   * @param action The identifier for the action you're requesting
   */
  public VolcanoIslandRequest(final String action) {
    super("volcanoisland.php");
    this.addFormField("action", action);
  }

  public VolcanoIslandRequest(final String action, final String subaction) {
    this(action);
    this.addFormField("subaction", subaction);
  }

  @Override
  public int getAdventuresUsed() {
    return getAdventuresUsed(this.getURLString());
  }

  public static int getAdventuresUsed(String urlString) {
    String action = GenericRequest.getAction(urlString);
    if (action == null) {
      return 0;
    }
    return switch (action) {
      case "tniat" -> 0;
      case "tuba" -> 1;
      default -> 0;
    };
  }

  public static void getSlime() {
    VolcanoIslandRequest request = new VolcanoIslandRequest(NPC, SLIME);
    RequestThread.postRequest(request);
  }

  public static String npcName() {
    return switch (KoLCharacter.getAscensionClass()) {
      case SEAL_CLUBBER -> "a Palm Tree Shelter";
      case TURTLE_TAMER -> "a Guy in the Bushes";
      case DISCO_BANDIT -> "a Girl in a Black Dress";
      case ACCORDION_THIEF -> "the Fishing Village";
      case PASTAMANCER -> "a Protestor";
      case SAUCEROR -> "a Boat";
      default -> null;
    };
  }

  private static String visitNPC(final String urlString) {
    Matcher matcher = VolcanoIslandRequest.ACTION_PATTERN.matcher(urlString);
    String action = null;
    String subaction = null;
    while (matcher.find()) {
      String tag = matcher.group(1);
      String value = matcher.group(2);
      if (tag.equals("action")) {
        action = value;
      } else {
        subaction = value;
      }
    }

    if (action == null || !action.equals(NPC)) {
      return null;
    }

    if (subaction == null) {
      String name = VolcanoIslandRequest.npcName();
      return "Visiting " + name + " on the Secret Tropical Island Volcano Lair";
    }

    if (subaction.equals(SLIME) && KoLCharacter.isSauceror()) {
      return "[" + KoLAdventure.getAdventureCount() + "] Volcano Island (Drums of Slime)";
    }

    return null;
  }

  public static void getBreakfast() {
    // If you have defeated your Nemesis as an Accordion Thief, you
    // have The Trickster's Trikitixa in inventory and can visit
    // the Fishing Village once a day for a free fisherman's sack.
    if (InventoryManager.hasItem(ItemPool.TRICKSTER_TRIKITIXA)) {
      VolcanoIslandRequest request = new VolcanoIslandRequest();
      request.run();
    }
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!(urlString.startsWith("volcanoisland.php")
        && (urlString.contains("action=tniat") || urlString.contains("action=npc")))) return;

    // Increment daily harvest count if successful
    // "You ladle some slime out of one of the drums. Fortunately, you had an empty vial on hand for
    // just such an opportunity.""
    if (KoLCharacter.isSauceror()
        && responseText.contains("ladle some slime out of one of the drums")) {
      Preferences.increment("_slimeVialsHarvested", 1, 10, false);
    }

    // A Pastamancer wearing the spaghetti cult robes loses them
    // when first visiting the Temple
    //
    // "A fierce wind whips through the chamber, first blowing back
    // your hood and then ripping the robe from your shoulders."

    if (KoLCharacter.isPastamancer()
        && responseText.contains("ripping the robe from your shoulders")) {
      EquipmentManager.discardEquipment(ItemPool.SPAGHETTI_CULT_ROBE);
    }
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("volcanoisland.php")) {
      return false;
    }

    if (urlString.indexOf("subaction=make") != -1) {
      return PhineasRequest.registerRequest(urlString);
    }

    String message = VolcanoIslandRequest.visitNPC(urlString);
    if (message == null) {
      return false;
    }

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog(message);

    return true;
  }
}
