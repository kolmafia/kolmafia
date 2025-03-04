package net.sourceforge.kolmafia.session;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class LeprecondoManager {
  private static final List<String> FURNITURE =
      List.of(
          "buckets of concrete",
          "thrift store oil painting",
          "boxes of old comic books",
          "second-hand hot plate",
          "beer cooler",
          "free mattress",
          "gigantic chess set",
          "UltraDance karaoke machine",
          "cupcake treadmill",
          "beer pong table",
          "padded weight bench",
          "internet-connected laptop",
          "sous vide laboratory",
          "programmable blender",
          "sensory deprivation tank",
          "fruit-smashing robot",
          "ManCave™ sports bar set",
          "<unknown furniture>",
          "kegerator",
          "fine upholstered dining table set",
          "whiskeybed",
          "high-end home workout system",
          "complete classics library",
          "ultimate retro game console",
          "Omnipot",
          "fully-stocked wet bar",
          "four-poster bed");

  private static final Map<String, String> FURNITURE_LOCATIONS =
      Map.ofEntries(
          Map.entry("buckets of concrete", ""),
          Map.entry("thrift store oil painting", ""),
          Map.entry("boxes of old comic books", ""),
          Map.entry("second-hand hot plate", ""),
          Map.entry("beer cooler", ""),
          Map.entry("free mattress", ""),
          Map.entry("gigantic chess set", "An Octopus's Garden"),
          Map.entry("UltraDance karaoke machine", "Infernal Rackets Backstage"),
          Map.entry("cupcake treadmill", "Madness Bakery"),
          Map.entry("beer pong table", "Frat House"),
          Map.entry("padded weight bench", "The Degrassi Knoll Garage"),
          Map.entry("internet-connected laptop", "The Hidden Office Building"),
          Map.entry("sous vide laboratory", "The Haunted Kitchen"),
          Map.entry("programmable blender", "Cobb's Knob Kitchens"),
          Map.entry("sensory deprivation tank", "The Marinara Trench"),
          Map.entry("fruit-smashing robot", "Wartime Hippy Camp (Frat Disguise)"),
          Map.entry("ManCave™ sports bar set", "A Barroom Brawl"),
          Map.entry("<unknown furniture>", ""),
          Map.entry("kegerator", "The Orcish Frat House (Bombed Back to the Stone Age)"),
          Map.entry("fine upholstered dining table set", "The Hidden Apartment Building"),
          Map.entry("whiskeybed", "The Castle in the Clouds in the Sky (Ground Floor)"),
          Map.entry("high-end home workout system", "The Degrassi Knoll Gym"),
          Map.entry("complete classics library", "The Haunted Library"),
          Map.entry("ultimate retro game console", "Megalo-City"),
          Map.entry("Omnipot", "Cobb's Knob Laboratory"),
          Map.entry("fully-stocked wet bar", "The Purple Light District"),
          Map.entry("four-poster bed", "Dreadsylvanian Castle"));

  private static final Map<String, String> FULFILMENT_TO_NEED =
      Map.ofEntries(
          Map.entry("spends a few minutes swinging around his buckets of concrete", "exercise"),
          Map.entry(
              "wanders over and spends some time studying his painting", "mental stimulation"),
          Map.entry("plops down and reads some old comics", "dumb entertainment"),
          Map.entry("fires up his second-hand hot plate", "food"),
          Map.entry("heads to his filthy mattress for a nap", "sleep"),
          Map.entry("grabs a beer from the beer cooler", "booze"),
          Map.entry(
              "wanders over to the karaoke machines and sings a few popular songs",
              "dumb entertainment"),
          Map.entry(
              "sets his karaoke machine to dance mode and careens around the room", "exercise"),
          Map.entry("spends a while running on his cupcake treadmill", "exericse"),
          Map.entry("grabs a cupcake from his treadmill, and tosses you one as well", "food"),
          Map.entry(
              "plays beer pong for a while, but stops drinking before he finishes the whole table's worth",
              "booze"),
          Map.entry(
              "spends some time arguing with people about politics on social media",
              "dumb entertainment"),
          Map.entry(
              "watches some home improvement videos and takes some notes", "mental stimulation"),
          Map.entry(
              "whips up a giant meal in the sous vide machine and shares it with you", "food"),
          Map.entry("reads the manual for his sous vide machine", "mental stimulation"),
          Map.entry("extracts some of the whiskey from his bed", "booze"),
          Map.entry("takes a long nap in his whiskeybed", "sleep"),
          Map.entry("reads some books about military history", "mental stimulation"),
          Map.entry("play old video games for a few hours", "dumb entertainment"),
          Map.entry("takes a long nap in his nice bed", "sleep"));

  static final Pattern FURNITURE_DISCOVERY_PATTERN =
      Pattern.compile("spots (?:an?|some) (.*?) and runs out of his condo\\.");

  static final Pattern UNMET_NEED_PATTERN =
      Pattern.compile("is upset that his (.*?) need wasn't met");

  public static boolean handlePostCombatMessage(final String text, final String image) {
    if (!image.equals("familiar2.gif")) return false;

    // Check for new furniture discovery
    var discovery = FURNITURE_DISCOVERY_PATTERN.matcher(text);
    if (discovery.find()) {
      var discovered =
          IntStream.range(1, FURNITURE.size())
              .filter((i) -> discovery.group(1).startsWith(FURNITURE.get(i - 1)))
              .findAny()
              .orElse(0);

      if (discovered == 0) return false;

      Preferences.setString(
          "leprecondoDiscovered",
          Stream.concat(
                  Arrays.stream(Preferences.getString("leprecondoDiscovered").split(","))
                      .filter(Predicate.not(String::isBlank))
                      .map(StringUtilities::parseInt),
                  Stream.of(discovered))
              .sorted()
              .distinct()
              .map(String::valueOf)
              .collect(Collectors.joining(",")));
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
        FULFILMENT_TO_NEED.entrySet().stream()
            .filter(e -> text.contains(e.getKey()))
            .map(Map.Entry::getValue)
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
                r ->
                    Map.entry(
                        StringUtilities.parseInt(r.group(1)), FURNITURE.indexOf(r.group(2)) + 1))
            .sorted(Map.Entry.comparingByKey())
            .map(Map.Entry::getValue)
            .map(String::valueOf)
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
    if (zone.isBlank()) return null;
    var furniture =
        FURNITURE_LOCATIONS.entrySet().stream()
            .filter(e -> e.getValue().equals(zone))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
    if (furniture == null) return null;
    var idx = FURNITURE.indexOf(furniture);
    if (Arrays.asList(Preferences.getString("leprecondoDiscovered").split(","))
        .contains(String.valueOf(idx))) return null;
    return furniture;
  }
}
