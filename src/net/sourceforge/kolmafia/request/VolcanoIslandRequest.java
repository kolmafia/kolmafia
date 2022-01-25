package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;

public class VolcanoIslandRequest extends GenericRequest {
  public static final Pattern ACTION_PATTERN = Pattern.compile("(action|subaction)=([^&]*)");

  // Actions
  private static final String NPC = "npc";

  // Subactions
  private static final String SLIME = "getslime";

  /**
   * Constructs a new <code>VolcanoIslandRequest</code>.
   *
   * @param action The identifier for the action you're requesting
   */
  private VolcanoIslandRequest() {
    this(NPC);
  }

  public VolcanoIslandRequest(final String action) {
    super("volcanoisland.php");
    this.addFormField("action", action);
  }

  public VolcanoIslandRequest(final String action, final String subaction) {
    this(action);
    this.addFormField("subaction", subaction);
  }

  public static void getSlime() {
    VolcanoIslandRequest request = new VolcanoIslandRequest(NPC, SLIME);
    RequestThread.postRequest(request);
  }

  public static String npcName() {
    switch (KoLCharacter.getAscensionClass()) {
      case SEAL_CLUBBER:
        return "a Palm Tree Shelter";
      case TURTLE_TAMER:
        return "a Guy in the Bushes";
      case DISCO_BANDIT:
        return "a Girl in a Black Dress";
      case ACCORDION_THIEF:
        return "the Fishing Village";
      case PASTAMANCER:
        return "a Protestor";
      case SAUCEROR:
        return "a Boat";
      default:
        return null;
    }
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
    if (!urlString.startsWith("volcanoisland.php") || urlString.indexOf("action=tniat") == -1) {
      return;
    }

    // A Pastamancer wearing the spaghetti cult robes loses them
    // when first visiting the Temple
    //
    // "A fierce wind whips through the chamber, first blowing back
    // your hood and then ripping the robe from your shoulders."

    if (KoLCharacter.isPastamancer()
        && responseText.indexOf("ripping the robe from your shoulders") != -1) {
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
