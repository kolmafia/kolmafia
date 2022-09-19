package net.sourceforge.kolmafia.session;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ElVibratoManager {

  // Avoid useless warning
  private ElVibratoManager() {}

  public record Punchcard(int id, String name, String alias) {}

  public static Punchcard[] PUNCHCARDS = {
    // Verbs
    new Punchcard(
        ItemPool.PUNCHCARD_ATTACK,
        "El Vibrato punchcard (115 holes)",
        "El Vibrato punchcard (ATTACK)"),
    new Punchcard(
        ItemPool.PUNCHCARD_REPAIR,
        "El Vibrato punchcard (97 holes)",
        "El Vibrato punchcard (REPAIR)"),
    new Punchcard(
        ItemPool.PUNCHCARD_BUFF, "El Vibrato punchcard (129 holes)", "El Vibrato punchcard (BUFF)"),
    new Punchcard(
        ItemPool.PUNCHCARD_MODIFY,
        "El Vibrato punchcard (213 holes)",
        "El Vibrato punchcard (MODIFY)"),
    new Punchcard(
        ItemPool.PUNCHCARD_BUILD,
        "El Vibrato punchcard (165 holes)",
        "El Vibrato punchcard (BUILD)"),

    // Objects
    new Punchcard(
        ItemPool.PUNCHCARD_TARGET,
        "El Vibrato punchcard (142 holes)",
        "El Vibrato punchcard (TARGET)"),
    new Punchcard(
        ItemPool.PUNCHCARD_SELF, "El Vibrato punchcard (216 holes)", "El Vibrato punchcard (SELF)"),
    new Punchcard(
        ItemPool.PUNCHCARD_FLOOR,
        "El Vibrato punchcard (88 holes)",
        "El Vibrato punchcard (FLOOR)"),
    new Punchcard(
        ItemPool.PUNCHCARD_DRONE,
        "El Vibrato punchcard (182 holes)",
        "El Vibrato punchcard (DRONE)"),
    new Punchcard(
        ItemPool.PUNCHCARD_WALL, "El Vibrato punchcard (176 holes)", "El Vibrato punchcard (WALL)"),
    new Punchcard(
        ItemPool.PUNCHCARD_SPHERE,
        "El Vibrato punchcard (104 holes)",
        "El Vibrato punchcard (SPHERE)")
  };

  private static Set<Integer> allPunchcards =
      Arrays.stream(PUNCHCARDS).map(Punchcard::id).collect(Collectors.toSet());

  private static Map<Integer, Integer> CARD_EXCHANGES =
      Map.ofEntries(
          Map.entry(ItemPool.PUNCHCARD_ATTACK, ItemPool.PUNCHCARD_TARGET),
          Map.entry(ItemPool.PUNCHCARD_TARGET, ItemPool.PUNCHCARD_ATTACK),
          Map.entry(ItemPool.PUNCHCARD_REPAIR, ItemPool.PUNCHCARD_SELF),
          Map.entry(ItemPool.PUNCHCARD_SELF, ItemPool.PUNCHCARD_REPAIR),
          Map.entry(ItemPool.PUNCHCARD_FLOOR, ItemPool.PUNCHCARD_BUFF),
          Map.entry(ItemPool.PUNCHCARD_BUFF, ItemPool.PUNCHCARD_FLOOR),
          Map.entry(ItemPool.PUNCHCARD_DRONE, ItemPool.PUNCHCARD_MODIFY),
          Map.entry(ItemPool.PUNCHCARD_MODIFY, ItemPool.PUNCHCARD_DRONE),
          Map.entry(ItemPool.PUNCHCARD_BUILD, ItemPool.PUNCHCARD_WALL),
          Map.entry(ItemPool.PUNCHCARD_WALL, ItemPool.PUNCHCARD_BUILD));

  public static void decorate(final StringBuffer buffer) {
    // If we are not on El Vibrato Island, nothing to do.
    KoLAdventure location = KoLAdventure.lastVisitedLocation;
    if (location == null || location.getAdventureNumber() != AdventurePool.EL_VIBRATO_ISLAND) {
      return;
    }

    // If the fight is over, punt
    if (FightRequest.getCurrentRound() == 0) {
      return;
    }

    // *** Here goes something like the Disco Combat Helper for feeding cards to the monster
  }

  private static final Pattern WHICHCARD_PATTERN = Pattern.compile("whichcard=(\\d+)");

  private static AdventureResult extractCard(String urlString) {
    Matcher matcher = WHICHCARD_PATTERN.matcher(urlString);
    if (!matcher.find()) {
      return null;
    }
    int cardId = StringUtilities.parseInt(matcher.group(1));
    if (!allPunchcards.contains(cardId)) {
      return null;
    }
    return ItemPool.get(cardId);
  }

  public static void parseResponse(String urlString, String responseText) {
    var card = extractCard(urlString);
    if (card == null) {
      return;
    }
    ResultProcessor.removeItem(card.getItemId());
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("elvmachine.php")) {
      return false;
    }

    // elvmachine.php
    // elvmachine.php?action=slot&whichcard=3151
    // elvmachine.php?action=button

    String action = GenericRequest.getAction(urlString);
    if (action == null) {
      return true;
    }

    String message = null;

    switch (action) {
      case "slot":
        var card = extractCard(urlString);
        if (card == null) {
          return true;
        }
        message = "Inserting a " + card.getName() + " into the slot.";
        break;
      case "button":
        message = "Pushing the button.";
        break;
      default:
        return true;
    }

    if (message != null) {
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
    }

    return true;
  }
}
