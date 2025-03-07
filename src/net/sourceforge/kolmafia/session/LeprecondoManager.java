package net.sourceforge.kolmafia.session;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class LeprecondoManager {
  private enum Need {
    MENTAL_STIMULATION("mental stimulation"),
    EXERCISE("exercise"),
    DUMB_ENTERTAINMENT("dumb entertainment"),
    FOOD("food"),
    BOOZE("booze"),
    SLEEP("sleep");

    private final String name;

    Need(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

  protected enum Furniture {
    CONCRETE(
        "buckets of concrete",
        1,
        Map.ofEntries(
            Map.entry(
                Need.EXERCISE, "spends a few minutes swinging around his buckets of concrete")),
        ""),
    PAINTING(
        "thrift store oil painting",
        2,
        Map.ofEntries(
            Map.entry(
                Need.MENTAL_STIMULATION,
                "wanders over and spends some time studying his painting")),
        ""),
    COMICS(
        "boxes of old comic books",
        3,
        Map.ofEntries(Map.entry(Need.DUMB_ENTERTAINMENT, "plops down and reads some old comics")),
        ""),
    HOT_PLATE(
        "second-hand hot plate",
        4,
        Map.ofEntries(Map.entry(Need.FOOD, "fires up his second-hand hot plate")),
        ""),
    BEER_COOLER(
        "beer cooler",
        5,
        Map.ofEntries(Map.entry(Need.BOOZE, "grabs a beer from the beer cooler")),
        ""),
    MATTRESS(
        "free mattress",
        6,
        Map.ofEntries(Map.entry(Need.SLEEP, "heads to his filthy mattress for a nap")),
        ""),
    CHESS_SET(
        "gigantic chess set",
        7,
        Map.ofEntries(
            Map.entry(
                Need.EXERCISE,
                "spends a few minutes laboriously resetting his big heavy chessboard"),
            Map.entry(
                Need.MENTAL_STIMULATION,
                "plays a game of chess against himself on his giant board")),
        "An Octopus's Garden"),
    KARAOKE(
        "UltraDance karaoke machine",
        8,
        Map.ofEntries(
            Map.entry(
                Need.DUMB_ENTERTAINMENT,
                "wanders over to the karaoke machines and sings a few popular songs"),
            Map.entry(
                Need.EXERCISE,
                "sets his karaoke machine to dance mode and careens around the room")),
        "Infernal Rackets Backstage"),
    TREADMILL(
        "cupcake treadmill",
        9,
        Map.ofEntries(
            Map.entry(Need.EXERCISE, "spends a while running on his cupcake treadmill"),
            Map.entry(Need.FOOD, "grabs a cupcake from his treadmill, and tosses you one as well")),
        "Madness Bakery"),
    BEER_PONG(
        "beer pong table",
        10,
        Map.ofEntries(
            Map.entry(
                Need.BOOZE,
                "plays beer pong for a while, but stops drinking before he finishes the whole table's worth"),
            Map.entry(Need.EXERCISE, "plays beer pong until he gets tired")),
        "Frat House"),
    WEIGHT_BENCH(
        "padded weight bench",
        11,
        Map.ofEntries(
            Map.entry(Need.EXERCISE, "does some benchpresses on his nice weight bench"),
            Map.entry(Need.SLEEP, "takes a nap on his padded weight bench")),
        "The Degrassi Knoll Garage"),
    LAPTOP(
        "internet-connected laptop",
        12,
        Map.ofEntries(
            Map.entry(
                Need.DUMB_ENTERTAINMENT,
                "spends some time arguing with people about politics on social media"),
            Map.entry(
                Need.MENTAL_STIMULATION,
                "watches some home improvement videos and takes some notes")),
        "The Hidden Office Building"),
    SOUS_VIDE(
        "sous vide laboratory",
        13,
        Map.ofEntries(
            Map.entry(
                Need.FOOD, "whips up a giant meal in the sous vide machine and shares it with you"),
            Map.entry(Need.MENTAL_STIMULATION, "reads the manual for his sous vide machine")),
        "The Haunted Kitchen"),
    BLENDER(
        "programmable blender",
        14,
        Map.ofEntries(
            Map.entry(
                Need.MENTAL_STIMULATION,
                "spends a few minutes adjusting the settings on his fancy blender"),
            Map.entry(Need.BOOZE, "makes a huge blended cocktail for himself, and one for you")),
        "Cobb's Knob Kitchens"),
    DEPRIVATION_TANK(
        "sensory deprivation tank",
        15,
        Map.ofEntries(
            Map.entry(Need.MENTAL_STIMULATION, "meditates in his tank for a while"),
            Map.entry(Need.SLEEP, "sleeps in his sensory deprivation tank for a long time")),
        "The Marinara Trench"),
    ROBOT(
        "fruit-smashing robot",
        16,
        Map.ofEntries(
            Map.entry(
                Need.DUMB_ENTERTAINMENT, "watches his fruit-smashing robot for a while, giggling"),
            Map.entry(
                Need.FOOD,
                "grabs some of the fruit from his fruit-smashing robot's storage cache")),
        "Wartime Hippy Camp (Frat Disguise)"),
    SPORTS_BAR(
        "ManCave™ sports bar set",
        17,
        Map.ofEntries(
            Map.entry(Need.DUMB_ENTERTAINMENT, "plays darts for a while in his sports bar"),
            Map.entry(Need.BOOZE, "has a few beers at the sports bar")),
        "A Barroom Brawl"),
    COUCH_AND_FLATSCREEN(
        "couch and flatscreen",
        18,
        Map.ofEntries(
            Map.entry(
                Need.DUMB_ENTERTAINMENT,
                "sits on his couch and watches some reruns of old reality shows"),
            Map.entry(Need.SLEEP, "falls asleep on his comfy couch")),
        "Frat House"),
    KEGERATOR(
        "kegerator",
        19,
        Map.ofEntries(
            Map.entry(Need.FOOD, "grabs some leftovers from the fridge"),
            Map.entry(Need.BOOZE, "grabs a homebrew from his kegerator")),
        "The Orcish Frat House (Bombed Back to the Stone Age)"),
    DINING_SET(
        "fine upholstered dining table set",
        20,
        Map.ofEntries(
            Map.entry(
                Need.FOOD,
                "heads to his dinette set for a nice meal, and tosses you a little something for yourself"),
            Map.entry(
                Need.SLEEP,
                "stretches out on his upholstered dining table and takes a little snooze")),
        "The Hidden Apartment Building"),
    WHISKEYBED(
        "whiskeybed",
        21,
        Map.ofEntries(
            Map.entry(Need.BOOZE, "extracts some of the whiskey from his bed"),
            Map.entry(Need.SLEEP, "takes a long nap in his whiskeybed")),
        "The Castle in the Clouds in the Sky (Ground Floor)"),
    WORKOUT_SYSTEM(
        "high-end home workout system",
        22,
        Map.ofEntries(Map.entry(Need.EXERCISE, "avails himself of his fancy home gym")),
        "The Degrassi Knoll Gym"),
    CLASSICS_LIBRARY(
        "complete classics library",
        23,
        Map.ofEntries(
            Map.entry(Need.MENTAL_STIMULATION, "reads some books about military history")),
        "The Haunted Library"),
    RETRO_CONSOLE(
        "ultimate retro game console",
        24,
        Map.ofEntries(Map.entry(Need.DUMB_ENTERTAINMENT, "play old video games for a few hours")),
        "Megalo-City"),
    OMNIPOT(
        "Omnipot",
        25,
        Map.ofEntries(Map.entry(Need.FOOD, "makes a small but delicious meal in his Omnipot")),
        "Cobb's Knob Laboratory"),
    WET_BAR(
        "fully-stocked wet bar",
        26,
        Map.ofEntries(Map.entry(Need.BOOZE, "whips up a cocktail in his nice bar")),
        "The Purple Light District"),
    POSTER_BED(
        "four-poster bed",
        27,
        Map.ofEntries(Map.entry(Need.SLEEP, "takes a long nap in his nice bed")),
        "Dreadsylvanian Castle");

    private static final Pattern FURNITURE_DISCOVERY_PATTERN =
        Pattern.compile("spots (?:an?|some) (.*?) and runs out of his condo\\.");

    public static Furniture byDiscovery(final String text) {
      var discovery = FURNITURE_DISCOVERY_PATTERN.matcher(text);
      if (!discovery.find()) return null;
      return Arrays.stream(Furniture.values())
          .filter(f -> discovery.group(1).startsWith(f.name))
          .findAny()
          .orElse(null);
    }

    public static Furniture byName(final String name) {
      return Arrays.stream(Furniture.values())
          .filter(f -> f.name.equals(name))
          .findAny()
          .orElse(null);
    }

    public static List<Furniture> byLocation(final String location) {
      return Arrays.stream(Furniture.values()).filter(f -> f.location.equals(location)).toList();
    }

    private final String name;
    private final int id;
    private final Map<Need, String> needs;
    private final String location;

    Furniture(String name, int id, Map<Need, String> needByFulfilment, String location) {
      this.name = name;
      this.id = id;
      this.needs = needByFulfilment;
      this.location = location;
    }

    public String getName() {
      return name;
    }

    public int getId() {
      return id;
    }

    public String getLocation() {
      return location;
    }

    public Map<Need, String> getNeeds() {
      return needs;
    }
  }

  static final Pattern UNMET_NEED_PATTERN =
      Pattern.compile("is upset that his (.*?) need wasn't met");

  public static boolean handlePostCombatMessage(final String text, final String image) {
    if (!image.equals("familiar2.gif")) return false;

    // Check for new furniture discovery
    var discovered = Furniture.byDiscovery(text);
    if (discovered != null) {
      Preferences.increment("_leprecondoFurniture");
      Preferences.setString(
          "leprecondoDiscovered",
          Stream.concat(
                  Arrays.stream(Preferences.getString("leprecondoDiscovered").split(","))
                      .filter(Predicate.not(String::isBlank))
                      .map(StringUtilities::parseInt),
                  Stream.of(discovered.getId()))
              .sorted()
              .distinct()
              .map(String::valueOf)
              .collect(Collectors.joining(",")));
      GoalManager.updateProgress(GoalManager.GOAL_LEPRECONDO);
      return true;
    }

    // Check for unmet need
    var unmetNeed = UNMET_NEED_PATTERN.matcher(text);
    if (unmetNeed.find()) {
      processNeedChange(unmetNeed.group(1));
      return true;
    }

    // Check for met need
    var metNeed =
        Arrays.stream(Furniture.values())
            .map(Furniture::getNeeds)
            .map(Map::entrySet)
            .flatMap(Collection::stream)
            .filter(e -> text.contains(e.getValue()))
            .map(Map.Entry::getKey)
            .map(Need::toString)
            .findAny()
            .orElse(null);
    if (metNeed != null) {
      processNeedChange(metNeed);
      return true;
    }

    return false;
  }

  protected static void processNeedChange(final String need) {
    if (Preferences.getString("leprecondoCurrentNeed").equals(need)) return;
    Preferences.setString("leprecondoCurrentNeed", need);
    Preferences.setInteger("leprecondoLastNeedChange", KoLCharacter.getCurrentRun());
    var history = Preferences.getString("leprecondoNeedOrder");

    // Already seen this need
    if (history.contains(need)) {
      // If we do not have a complete set, the data is broken. Start counting again
      if (history.split(",").length != 6) {
        Preferences.setString("leprecondoNeedOrder", need);
      }

      return;
    }

    Preferences.setString(
        "leprecondoNeedOrder", String.join(",", history) + ((history.isEmpty()) ? "" : ",") + need);
  }

  public static void visit(final String text) {
    var installed =
        Pattern.compile("<img id=\"i(\\d)\" alt=\"(.*?) in (?:top|bottom) (?:left|right)\"")
            .matcher(text)
            .results()
            .map(
                r -> {
                  var f = Furniture.byName(r.group(2));
                  return Map.entry(
                      StringUtilities.parseInt(r.group(1)),
                      String.valueOf(f == null ? 0 : f.getId()));
                })
            .sorted(Map.Entry.comparingByKey())
            .map(Map.Entry::getValue)
            .collect(Collectors.joining(","));

    Preferences.setString("leprecondoInstalled", installed);

    var rearrangementsMatcher =
        Pattern.compile("You can rearrange the furnishings (\\d) more").matcher(text);

    int rearrangements = 0;
    if (rearrangementsMatcher.find()) {
      rearrangements = StringUtilities.parseInt(rearrangementsMatcher.group(1));
      Preferences.setInteger("_leprecondoRearrangements", 3 - rearrangements);
    }

    // We can only learn/validate our discoveries if we have rearrangements left
    if (rearrangements > 0) {
      var discoveryOptions =
          Pattern.compile("<select id=\"r1\" name=\"r1\">(.*?)</select>").matcher(text);
      if (discoveryOptions.find()) {
        var discoveries =
            Pattern.compile("<option (?:selected)? value='(\\d+)'")
                .matcher(discoveryOptions.group(1))
                .results()
                .map(r -> r.group(1))
                .distinct()
                .collect(Collectors.joining(","));

        Preferences.setString("leprecondoDiscovered", discoveries);
      }
    }
  }

  public static String getUndiscoveredFurnitureForLocation(final String zone) {
    if (zone.isBlank()) return "";
    var furniture = Furniture.byLocation(zone);
    if (furniture.isEmpty()) return "";
    var discovered =
        Arrays.stream(Preferences.getString("leprecondoDiscovered").split(","))
            .map(StringUtilities::parseInt)
            .toList();
    return furniture.stream()
        .filter(f -> !discovered.contains(f.getId()))
        .map(Furniture::getName)
        .collect(Collectors.joining(", "));
  }
}
