package net.sourceforge.kolmafia.textui.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.session.ChoiceManager;

public class ColdMedicineCabinetCommand extends AbstractCommand {
  public ColdMedicineCabinetCommand() {
    this.usage =
        " - interact with the Cold Medicine Cabinet\n"
            + "Available sub-commands:\n"
            + "<blank> | booze | equipment | food | pill | potion | plan\n"
            + " <blank>: show the status of the Cold Medicine Cabinet including available rewards from the choice encounter\n"
            + " booze: collect the booze reward from the Cold Medicine Cabinet choice encounter\n"
            + " equipment: collect the equipment reward from the Cold Medicine Cabinet choice encounter\n"
            + " food: collect the food reward from the Cold Medicine Cabinet choice encounter\n"
            + " pill: collect the pill reward from the Cold Medicine Cabinet choice encounter\n"
            + " plan: guess combats required for each type of pill reward\n"
            + " potion: collect the potion reward from the Cold Medicine Cabinet choice encounter";
  }

  public static final List<String> ITEM_TYPES =
      List.of("equipment", "food", "booze", "potion", "pill");

  private static final Map<Character, AdventureResult> PILLS =
      Map.ofEntries(
          Map.entry('i', ItemPool.get(ItemPool.EXTROVERMECTIN)),
          Map.entry('o', ItemPool.get(ItemPool.HOMEBODYL)),
          Map.entry('u', ItemPool.get(ItemPool.BREATHITIN)),
          Map.entry('x', ItemPool.get(ItemPool.FLESHAZOLE)));

  private static final Map<Character, String> LOCATION_STRINGS =
      Map.ofEntries(
          Map.entry('i', " combats in an indoor location"),
          Map.entry('o', " combats in an outdoor location"),
          Map.entry('u', " combats in an underground location"),
          Map.entry(
              'x',
              " combats underwater or until no environment has overall majority")); // underwater/unknown

  // need extra stuff

  private static Stream<Character> getCharacters() {
    return Preferences.getString("lastCombatEnvironments").chars().mapToObj(i -> (char) i);
  }

  private static AdventureResult guessNextEquipment() {
    return switch (Preferences.getInteger("_coldMedicineEquipmentTaken")) {
      case 0 -> ItemPool.get("ice crown", 1);
      case 1 -> ItemPool.get("frozen jeans", 1);
      default -> ItemPool.get("ice wrap", 1);
    };
  }

  /**
   * Count all the last combat environments
   *
   * @return The lastCombatEnvironments pref transformed into a map of environment characters to
   *     counts
   */
  private static Map<Character, Integer> getCounts() {
    return getCharacters()
        .collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(i -> 1)));
  }

  public static AdventureResult guessNextPill() {
    return guessNextPill(getCounts());
  }

  public static AdventureResult guessNextPill(Map<Character, Integer> counts) {
    int unknown = counts.getOrDefault('?', 0);

    if (unknown > 10) return null;

    for (var e : counts.entrySet()) {
      var environment = e.getKey();
      if (environment == '?') continue;
      var count = e.getValue();
      // If we have an overall majority return it.
      if (count > 10) return PILLS.get(environment);
      // If there is a potential majority when considering unknowns, return none.
      if ((count + unknown) > 10) return null;
    }

    // Otherwise return the pill you get from having a majority not inside, outside or underground.
    return PILLS.get('x');
  }

  private static final Map<KoLConstants.Stat, AdventureResult> STAT_WINES =
      Map.ofEntries(
          Map.entry(KoLConstants.Stat.MUSCLE, ItemPool.get("Doc's Fortifying Wine", 1)),
          Map.entry(KoLConstants.Stat.MYSTICALITY, ItemPool.get("Doc's Smartifying Wine", 1)),
          Map.entry(KoLConstants.Stat.MOXIE, ItemPool.get("Doc's Limbering Wine", 1)));

  private static final AdventureResult SPECIAL_RESERVE_WINE =
      ItemPool.get("Doc's Special Reserve Wine", 1);
  private static final AdventureResult MEDICAL_GRADE_WINE =
      ItemPool.get("Doc's Medical-Grade Wine", 1);

  private static AdventureResult guessNextWine() {
    var statBuffs =
        new java.util.ArrayList<>(
            List.of(
                Map.entry(
                    KoLConstants.Stat.MUSCLE,
                    KoLCharacter.calculateBasePoints(KoLCharacter.getAdjustedMuscle())
                        - KoLCharacter.getBaseMuscle()),
                Map.entry(
                    KoLConstants.Stat.MYSTICALITY,
                    KoLCharacter.calculateBasePoints(KoLCharacter.getAdjustedMysticality())
                        - KoLCharacter.getBaseMysticality()),
                Map.entry(
                    KoLConstants.Stat.MOXIE,
                    KoLCharacter.calculateBasePoints(KoLCharacter.getAdjustedMoxie())
                        - KoLCharacter.getBaseMoxie())));

    statBuffs.sort(Map.Entry.comparingByValue());

    var top = statBuffs.get(2);

    if (top.getValue().equals(statBuffs.get(1).getValue())) {
      // We have a draw. Give a different wine depending on size of buff
      return (top.getValue() > 5) ? SPECIAL_RESERVE_WINE : MEDICAL_GRADE_WINE;
    }

    return STAT_WINES.get(top.getKey());
  }

  private static final Pattern ITEM_PATTERN = Pattern.compile("descitem\\((\\d+)\\)");

  private static SortedMap<String, AdventureResult> visitCabinet() {
    var request = new CampgroundRequest("workshed");
    RequestThread.postRequest(request);
    String response = request.responseText;

    var matches =
        ITEM_PATTERN
            .matcher(response)
            .results()
            .map(r -> ItemPool.get(ItemDatabase.getItemIdFromDescription(r.group(1))))
            .toList();

    if (matches.size() < 5) {
      return null;
    }

    var map = new TreeMap<String, AdventureResult>();
    map.put("equipment", matches.get(0));
    map.put("food", matches.get(1));
    map.put("booze", matches.get(2));
    map.put("potion", matches.get(3));
    map.put("pill", matches.get(4));
    return map;
  }

  private static SortedMap<String, AdventureResult> guessCabinet() {
    var map = new TreeMap<String, AdventureResult>();
    map.put("equipment", guessNextEquipment());
    map.put("food", null);
    map.put("booze", guessNextWine());
    map.put("potion", null);
    map.put("pill", guessNextPill());
    return map;
  }

  private static int getTurnsToNextConsult() {
    var nextConsult = Preferences.getInteger("_nextColdMedicineConsult");
    return nextConsult - KoLCharacter.getTurnsPlayed();
  }

  private static int getConsultsUsed() {
    return Preferences.getInteger("_coldMedicineConsults");
  }

  private static StringBuilder formatItemList(
      final Map<String, AdventureResult> items, final boolean guessing) {
    var output = new StringBuilder();

    for (var e : items.entrySet()) {
      var type = e.getKey();
      var item = e.getValue();
      output.append("Your next ").append(type);
      if (item == null) {
        output.append(" is ").append(type.equals("pill") ? "unknown" : "not guessed yet");
      } else {
        output.append(guessing ? " should be " : " is ").append(item.getName());
      }

      output.append("\n");
    }

    return output;
  }

  public static Map.Entry<Boolean, StringBuilder> shouldGuess() {
    var consults = getConsultsUsed();
    var turnsToNextConsult = getTurnsToNextConsult();

    boolean guessing = true;
    var output = new StringBuilder();

    if (consults < 5) {
      if (turnsToNextConsult > 0) {
        output.append(turnsToNextConsult).append(" turns until next consult is ready.\n");
      } else {
        output.append("Consult is ready now");
        guessing = KoLCharacter.inFightOrChoice();
        if (guessing) {
          output.append(" but can't visit right now so guessing your options...");
        } else {
          output.append("!");
        }
      }
    }

    return Map.entry(guessing, output);
  }

  public static SortedMap<String, AdventureResult> getCabinet() {
    var guessing = shouldGuess();
    return getCabinet(guessing.getKey());
  }

  public static SortedMap<String, AdventureResult> getCabinet(final boolean guessing) {
    return guessing ? guessCabinet() : visitCabinet();
  }

  public static void status() {
    var output = new StringBuilder();

    var consults = getConsultsUsed();

    output.append(consults).append("/5 consults used today.\n");

    output.append("\n");

    var g = shouldGuess();
    output.append(g.getValue()).append("\n");
    var guessing = g.getKey();
    var cabinet = getCabinet(guessing);

    if (cabinet == null) {
      KoLmafia.updateDisplay(
          KoLConstants.MafiaState.ERROR, "Cold Medicine Cabinet choice could not be parsed.");
    } else {
      output.append(formatItemList(cabinet, guessing));
    }
    RequestLogger.printLine(output.toString());
  }

  private static void collect(final int decision) {
    if (getConsultsUsed() >= 5) {
      KoLmafia.updateDisplay(
          KoLConstants.MafiaState.ERROR, "You do not have any consults left for the day.");
      return;
    }

    int turns = getTurnsToNextConsult();
    if (turns > 0) {
      KoLmafia.updateDisplay(
          KoLConstants.MafiaState.ERROR, "You are not due a consult (" + turns + " turns to go).");
      return;
    }

    RequestThread.postRequest(new CampgroundRequest("workshed"));
    ChoiceManager.processChoiceAdventure(decision, "", true);
  }

  private static boolean isInstalled() {
    var workshedItem = CampgroundRequest.getCurrentWorkshedItem();
    return workshedItem != null && workshedItem.getItemId() == ItemPool.COLD_MEDICINE_CABINET;
  }

  /**
   * Track 20-turn location environment buffer for Cold Medicine Cabinet
   *
   * <p>While this tracking was written to be generic, it turns out that the counter with a 20-turn
   * buffer that this is tracking stops once you hit five consultations with your cold medicine
   * cabinet. We could keep tracking in theory, but there's no guarantee that a future environment
   * tracker could use the same rules and have the same buffer size
   *
   * @param location Location to be added to the tracker
   */
  public static void trackEnvironment(final KoLAdventure location) {
    String symbol;

    if (!isInstalled() || getConsultsUsed() >= 5) {
      return;
    }

    if (location == null) {
      symbol = "?";
    } else {
      if (!location.getFormSource().equals("adventure.php")) return;

      symbol =
          switch (location.getEnvironment()) {
            case OUTDOOR -> "o";
            case INDOOR -> "i";
            case UNDERGROUND -> "u";
            case UNDERWATER -> "x";
            default -> "?";
          };
    }

    // Make sure the value is padded to handle malformed preferences
    var environments = "x".repeat(20) + Preferences.getString("lastCombatEnvironments") + symbol;

    Preferences.setString(
        "lastCombatEnvironments", environments.substring(environments.length() - 20));
  }

  @Override
  public void run(final String cmd, String parameter) {
    if (!isInstalled()) {
      KoLmafia.updateDisplay(
          KoLConstants.MafiaState.ERROR, "You do not have a Cold Medicine Cabinet installed.");
      return;
    }

    switch (parameter) {
      case "" -> status();
      case "equipment", "equip" -> collect(1);
      case "food" -> collect(2);
      case "booze", "wine" -> collect(3);
      case "potion" -> collect(4);
      case "pill" -> collect(5);
      case "plan" -> guessCombatsRequiredForPills();
      default -> {
        KoLmafia.updateDisplay(KoLConstants.MafiaState.ERROR, "Parameter not recognised");
      }
    }
  }

  /**
   * Uses the naive guess from populateNaiveTurnsRequiredForPillMap to calculate a more accurate
   * guess for the number of combats required in an environment for the associated pill. Iterates
   * exactly once over the lastCombatEnvironments property and increases the combats required if the
   * new combat environment is the same as the oldest (19th index) combat environment, which would
   * be removed to append the new combat environment.
   *
   * <p>Outputs the number of combats required for each environment that exists in the
   * lastCombatEnvironments property string to acquire those environments' associated pills. It is
   * assumed the player requires 11 combats in any environment that does exist in the property
   * string to acquire the associated pill for that environment.
   */
  private static void guessCombatsRequiredForPills() {
    final var output = new StringBuilder();
    // create a base map using naive guesses based on the unchanging state of the property string
    final var actualTurnsForMajority = populateNaiveTurnsRequiredForPillMap();
    for (char c : PILLS.keySet()) { // iterate over ALL environment types
      if (actualTurnsForMajority.get(c)
          == null) { // if an environment is not in the property, assume 11 turns outright
        output
            .append("For ")
            .append(PILLS.get(c))
            .append(", spend a minimum of 11")
            .append(LOCATION_STRINGS.get(c))
            .append("\n");
      }
    }
    final var lastEnvironments = getCharacters().toArray(Character[]::new);
    final var keys = new ArrayList<>(actualTurnsForMajority.keySet());
    for (int i = 0; i < lastEnvironments.length; i++) {
      final var last = lastEnvironments[i];
      for (int j = keys.size() - 1; j > -1; j--) { // iterate backwards so we can hot remove keys
        final var key = keys.get(j);
        // if the new combat environment is equal to the old combat environment, we will require an
        // extra turn
        if (actualTurnsForMajority.get(key) > 0 && key == last) {
          actualTurnsForMajority.put(last, actualTurnsForMajority.get(last) + 1);
          continue;
        }
        // if the iterator matches the turns we require (which is kept updated), then we're done and
        // can output
        if (actualTurnsForMajority.get(key) == i) {
          output
              .append("For ")
              .append(PILLS.get(key))
              .append(", spend ")
              .append(actualTurnsForMajority.get(key))
              .append(LOCATION_STRINGS.get(key));
          keys.remove(j); // remove the environments we're done with
          if (keys.size() < 1) {
            RequestLogger.printLine(output.toString());
            return;
          } else {
            output.append("\n"); // only append a newline if it's not the ultimate output
          }
        }
      }
    }
    RequestLogger.printLine(output.toString()); // shouldn't get here in theory, so just in case
  }

  /**
   * Produce a naive guess at the number of turns required to acquire each type of pill. Used to
   * calculate a more accurate guess in guessCombatsRequiredForPills with a single parse of the
   * lastCombatEnvironments property.
   *
   * @return A hashmap where keys are the Character representation for unique environments from the
   *     lastCombatEnvironments property, and values are Integers representing a naive guess of the
   *     number of turns required to get a type of pill: (11 - number of times the environment
   *     appears in the property)
   */
  private static Map<Character, Integer> populateNaiveTurnsRequiredForPillMap() {
    var counts = getCounts();
    final var naiveTurnsForMajority = new HashMap<Character, Integer>();
    for (char c : counts.keySet()) {
      if (c == '?') continue; // ignore unknown locations
      var turnsForMajority = 11 - counts.get(c);
      if (turnsForMajority < 1) turnsForMajority = 0;
      naiveTurnsForMajority.put(c, turnsForMajority);
    }
    return naiveTurnsForMajority;
  }
}
