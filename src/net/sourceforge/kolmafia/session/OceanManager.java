package net.sourceforge.kolmafia.session;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class OceanManager {

  // You can sail the ocean on the Pirate Ship by adventuring in The Poop Deck.
  //
  // Every thirty turns or so, the navigator mistakes you for Cap'm Caronch and
  // offers to go the destination of your choice if you give him 977 Meat to
  // outfit the ship.
  //
  // Doing so sends you to Set Sail for Virgin Booty, where you select the
  // Longitude & Latitude of your destination.
  //
  // There are a variety of special destinations.
  //
  // "muscle" - Gilligan's Island, Monkey Island, Oyster Island - 5 locations
  // each - give 250-350 muscle substats
  //
  // "mysticality" - Dinosaur Comics, Land of the Lost, Myst Island - 5
  // locations each - give 250-350 mysticality substats
  //
  // "moxie" - Cast Away Lord of the Flies, LOST - 5 locations each - give
  // 250-350 moxie substats
  //
  // "sand" - 9 locations originally contained a rainbow pearl.  After each
  // pearl was retrieved, the location yields only rainbow sand.
  //
  // "altar" - 43 "small tropical islands" originally contained a strange tiki
  // idol. After each idol was retrieved, the location yields only a sinister
  // altar fragment.
  //
  // Riff published a puzzle regarding these; the map of the islands can be
  // read as "Ak'gyxoth", a demon name. Seventh was the first to figure this
  // out on August 9, 2008 and summon the demon. He was granted the Emblem of
  // Ak'gyxoth and the strange tiki idol was renamed to be Idol of Ak'gyxoth.
  //
  // KoLmafia connection: Seventh is Jason Harper, the KoLmafia dev who gave us
  // the Modifier Maximizer among many other things.
  //
  // "sphere" - 3 locations originally contained a small triangle, a medium
  // triangle, and a large triangle. Subsequent visits dropped a strange stone
  // sphere, which was renamed to be the El Vibrato power sphere. These are
  // used to charge up your portal and are still obtainable.
  //
  // "plinth" - 1 location contains the Plinth. This is El Vibrato island,
  // named after the character who assembled the strange stone pyramid from the
  // 3 strange stone triangles and sailed there on February 7, 2008.
  //
  // Once per ascension, if you have an El Vibrato power sphere in inventory,
  // the plinth will absorb it and give you an El Vibrato trapezoid.  You can
  // set this up in your Campground and gain access to a Strange Portal which
  // lets you adventure in the underworld of El Vibrato island.
  //
  // Sailing here again after obtaining the trapezoid with a power sphere in
  // inventory absorbs the sphere and gives you nothing in return.

  // Avoid useless warning
  private OceanManager() {}

  // All special ocean destinations
  private static final Map<Point, Destination> destinations = new HashMap<>();

  // Special ocean destinations by keyword category
  public static final List<Point> muscleDestinations = new ArrayList<>();
  public static final List<Point> mysticalityDestinations = new ArrayList<>();
  public static final List<Point> moxieDestinations = new ArrayList<>();
  public static final List<Point> altarDestinations = new ArrayList<>();
  public static final List<Point> sandDestinations = new ArrayList<>();
  public static final List<Point> sphereDestinations = new ArrayList<>();
  public static final List<Point> plinthDestinations = new ArrayList<>();

  public static final Pattern POINT_PATTERN = Pattern.compile("(\\d+),(\\d+)");

  public static class Point {
    public static final int xMin = 1;
    public static final int xMax = 242;
    public static final int yMin = 1;
    public static final int yMax = 100;

    public final int x, y;

    public Point(int x, int y) {
      this.x = x;
      this.y = y;
    }

    public static boolean valid(int x, int y) {
      return x >= xMin && x <= xMax && y >= yMin && y <= yMax;
    }

    public static Point parse(String input) {
      Matcher matcher = POINT_PATTERN.matcher(input);
      if (matcher.find()) {
        int x = StringUtilities.parseInt(matcher.group(1));
        int y = StringUtilities.parseInt(matcher.group(2));
        if (valid(x, y)) {
          return new Point(x, y);
        }
      }
      return null;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof Point that) {
        return this.x == that.x && this.y == that.y;
      }
      return false;
    }

    @Override
    public int hashCode() {
      return (x - 1) * yMax + (y - 1);
    }

    @Override
    public String toString() {
      return this.x + "," + this.y;
    }
  }

  public enum Destination {
    GILLIGAN("Gilligan's Island"),
    MONKEY("Monkey Island"),
    OYSTER("Oyster Island"),
    DINOSAUR("Dinosaur Comics"),
    LAND_OF_LOST("Land of the Lost"),
    MYST("Myst Island"),
    CAST_AWAY("Cast Away"),
    LORD_OF_FLIES("Lord of the Flies"),
    LOST("LOST"),
    RAINBOW_SAND("Rainbow Sand"),
    ALTAR("Small Tropical Island"),
    SPHERE("Power Sphere"),
    PLINTH("Plinth"),
    MAINLAND("Mainland");

    String desc;
    Set<Point> locations = new HashSet<>();

    Destination(String desc) {
      this.desc = desc;
    }

    public void add(int lon, int lat) {
      Point location = new Point(lon, lat);
      this.locations.add(location);
      destinations.put(location, this);
    }

    public Set<Point> getLocations() {
      return this.locations;
    }

    @Override
    public String toString() {
      return this.desc;
    }
  };

  static {
    Destination.GILLIGAN.add(12, 84);
    Destination.GILLIGAN.add(63, 10);
    Destination.GILLIGAN.add(81, 40);
    Destination.GILLIGAN.add(111, 59);
    Destination.GILLIGAN.add(185, 86);

    Destination.MONKEY.add(56, 14);
    Destination.MONKEY.add(90, 36);
    Destination.MONKEY.add(105, 13);
    Destination.MONKEY.add(147, 5);
    Destination.MONKEY.add(148, 72);

    Destination.OYSTER.add(19, 59);
    Destination.OYSTER.add(49, 42);
    Destination.OYSTER.add(64, 64);
    Destination.OYSTER.add(173, 51);
    Destination.OYSTER.add(186, 95);

    Destination.DINOSAUR.add(23, 66);
    Destination.DINOSAUR.add(55, 8);
    Destination.DINOSAUR.add(60, 14);
    Destination.DINOSAUR.add(110, 15);
    Destination.DINOSAUR.add(196, 42);

    Destination.LAND_OF_LOST.add(89, 44);
    Destination.LAND_OF_LOST.add(138, 43);
    Destination.LAND_OF_LOST.add(135, 14);
    Destination.LAND_OF_LOST.add(151, 76);
    Destination.LAND_OF_LOST.add(187, 88);

    Destination.MYST.add(3, 35);
    Destination.MYST.add(13, 86);
    Destination.MYST.add(44, 45);
    Destination.MYST.add(52, 50);
    Destination.MYST.add(81, 5);

    Destination.CAST_AWAY.add(22, 62);
    Destination.CAST_AWAY.add(30, 35);
    Destination.CAST_AWAY.add(60, 5);
    Destination.CAST_AWAY.add(83, 45);
    Destination.CAST_AWAY.add(185, 98);

    Destination.LORD_OF_FLIES.add(13, 91);
    Destination.LORD_OF_FLIES.add(44, 51);
    Destination.LORD_OF_FLIES.add(85, 35);
    Destination.LORD_OF_FLIES.add(94, 65);
    Destination.LORD_OF_FLIES.add(115, 14);

    Destination.LOST.add(5, 39);
    Destination.LOST.add(52, 45);
    Destination.LOST.add(133, 60);
    Destination.LOST.add(143, 11);
    Destination.LOST.add(187, 92);

    Destination.RAINBOW_SAND.add(124, 31);
    Destination.RAINBOW_SAND.add(134, 30);
    Destination.RAINBOW_SAND.add(144, 29);
    Destination.RAINBOW_SAND.add(154, 28);
    Destination.RAINBOW_SAND.add(164, 27);
    Destination.RAINBOW_SAND.add(172, 19);
    Destination.RAINBOW_SAND.add(174, 26);
    Destination.RAINBOW_SAND.add(176, 33);
    Destination.RAINBOW_SAND.add(178, 22);
    Destination.RAINBOW_SAND.add(180, 29);
    Destination.RAINBOW_SAND.add(184, 25);

    Destination.ALTAR.add(30, 85);
    Destination.ALTAR.add(34, 79);
    Destination.ALTAR.add(38, 70);
    Destination.ALTAR.add(40, 81);
    Destination.ALTAR.add(41, 90);
    Destination.ALTAR.add(47, 74);
    Destination.ALTAR.add(47, 83);
    Destination.ALTAR.add(47, 91);
    Destination.ALTAR.add(51, 79);
    Destination.ALTAR.add(54, 93);
    Destination.ALTAR.add(58, 77);
    Destination.ALTAR.add(59, 75);
    Destination.ALTAR.add(62, 89);
    Destination.ALTAR.add(63, 81);
    Destination.ALTAR.add(68, 88);
    Destination.ALTAR.add(69, 77);
    Destination.ALTAR.add(69, 94);
    Destination.ALTAR.add(70, 86);
    Destination.ALTAR.add(73, 81);
    Destination.ALTAR.add(73, 88);
    Destination.ALTAR.add(77, 74);
    Destination.ALTAR.add(79, 93);
    Destination.ALTAR.add(82, 83);
    Destination.ALTAR.add(86, 72);
    Destination.ALTAR.add(89, 92);
    Destination.ALTAR.add(90, 79);
    Destination.ALTAR.add(94, 86);
    Destination.ALTAR.add(97, 81);
    Destination.ALTAR.add(98, 94);
    Destination.ALTAR.add(100, 81);
    Destination.ALTAR.add(104, 76);
    Destination.ALTAR.add(104, 85);
    Destination.ALTAR.add(107, 79);
    Destination.ALTAR.add(110, 73);
    Destination.ALTAR.add(113, 94);
    Destination.ALTAR.add(116, 74);
    Destination.ALTAR.add(119, 95);
    Destination.ALTAR.add(120, 88);
    Destination.ALTAR.add(121, 82);
    Destination.ALTAR.add(123, 76);
    Destination.ALTAR.add(125, 97);
    Destination.ALTAR.add(127, 90);
    Destination.ALTAR.add(129, 83);

    Destination.SPHERE.add(48, 47);
    Destination.SPHERE.add(59, 10);
    Destination.SPHERE.add(86, 40);

    Destination.PLINTH.add(63, 29);

    // Mainland coordinates from:
    //
    // https://kol.coldfront.net/thekolwiki/index.php/File:BootyMap.gif

    // Destination.MAINLAND.add(11, 11);
    Destination.MAINLAND.add(11, 12);
    Destination.MAINLAND.add(11, 13);
    Destination.MAINLAND.add(11, 14);
    Destination.MAINLAND.add(11, 15);
    // Destination.MAINLAND.add(11, 16);
    Destination.MAINLAND.add(11, 17);
    Destination.MAINLAND.add(11, 18);
    Destination.MAINLAND.add(11, 19);
    Destination.MAINLAND.add(11, 20);
    // Destination.MAINLAND.add(11, 21);

    Destination.MAINLAND.add(12, 11);
    Destination.MAINLAND.add(12, 12);
    Destination.MAINLAND.add(12, 13);
    Destination.MAINLAND.add(12, 14);
    Destination.MAINLAND.add(12, 15);
    Destination.MAINLAND.add(12, 16);
    Destination.MAINLAND.add(12, 17);
    Destination.MAINLAND.add(12, 18);
    Destination.MAINLAND.add(12, 19);
    Destination.MAINLAND.add(12, 20);
    Destination.MAINLAND.add(12, 21);

    Destination.MAINLAND.add(13, 11);
    Destination.MAINLAND.add(13, 12);
    Destination.MAINLAND.add(13, 13);
    Destination.MAINLAND.add(13, 14);
    Destination.MAINLAND.add(13, 15);
    Destination.MAINLAND.add(13, 16);
    Destination.MAINLAND.add(13, 17);
    Destination.MAINLAND.add(13, 18);
    Destination.MAINLAND.add(13, 19);
    Destination.MAINLAND.add(13, 20);
    Destination.MAINLAND.add(13, 21);

    Destination.MAINLAND.add(14, 11);
    Destination.MAINLAND.add(14, 12);
    Destination.MAINLAND.add(14, 13);
    Destination.MAINLAND.add(14, 14);
    Destination.MAINLAND.add(14, 15);
    Destination.MAINLAND.add(14, 16);
    Destination.MAINLAND.add(14, 17);
    Destination.MAINLAND.add(14, 18);
    Destination.MAINLAND.add(14, 19);
    Destination.MAINLAND.add(14, 20);
    Destination.MAINLAND.add(14, 21);

    // Destination.MAINLAND.add(15, 11);
    Destination.MAINLAND.add(15, 12);
    Destination.MAINLAND.add(15, 13);
    Destination.MAINLAND.add(15, 14);
    Destination.MAINLAND.add(15, 15);
    Destination.MAINLAND.add(15, 16);
    Destination.MAINLAND.add(15, 17);
    Destination.MAINLAND.add(15, 18);
    Destination.MAINLAND.add(15, 19);
    Destination.MAINLAND.add(15, 20);
    Destination.MAINLAND.add(15, 21);

    // Destination.MAINLAND.add(16, 11);
    Destination.MAINLAND.add(16, 12);
    Destination.MAINLAND.add(16, 13);
    Destination.MAINLAND.add(16, 14);
    Destination.MAINLAND.add(16, 15);
    Destination.MAINLAND.add(16, 16);
    Destination.MAINLAND.add(16, 17);
    Destination.MAINLAND.add(16, 18);
    Destination.MAINLAND.add(16, 19);
    Destination.MAINLAND.add(16, 20);
    Destination.MAINLAND.add(16, 21);

    Destination.MAINLAND.add(17, 11);
    Destination.MAINLAND.add(17, 12);
    Destination.MAINLAND.add(17, 13);
    Destination.MAINLAND.add(17, 14);
    Destination.MAINLAND.add(17, 15);
    Destination.MAINLAND.add(17, 16);
    Destination.MAINLAND.add(17, 17);
    Destination.MAINLAND.add(17, 18);
    Destination.MAINLAND.add(17, 19);
    Destination.MAINLAND.add(17, 20);
    // Destination.MAINLAND.add(17, 21);

    Destination.MAINLAND.add(18, 11);
    Destination.MAINLAND.add(18, 12);
    Destination.MAINLAND.add(18, 13);
    Destination.MAINLAND.add(18, 14);
    Destination.MAINLAND.add(18, 15);
    Destination.MAINLAND.add(18, 16);
    Destination.MAINLAND.add(18, 17);
    // Destination.MAINLAND.add(18, 18);
    // Destination.MAINLAND.add(18, 19);
    // Destination.MAINLAND.add(18, 20);
    // Destination.MAINLAND.add(18, 21);

    Destination.MAINLAND.add(19, 11);
    Destination.MAINLAND.add(19, 12);
    Destination.MAINLAND.add(19, 13);
    Destination.MAINLAND.add(19, 14);
    Destination.MAINLAND.add(19, 15);
    Destination.MAINLAND.add(19, 16);
    // Destination.MAINLAND.add(19, 17);
    // Destination.MAINLAND.add(19, 18);
    Destination.MAINLAND.add(19, 19);
    Destination.MAINLAND.add(19, 20);
    // Destination.MAINLAND.add(19, 21);

    Destination.MAINLAND.add(20, 11);
    Destination.MAINLAND.add(20, 12);
    Destination.MAINLAND.add(20, 13);
    Destination.MAINLAND.add(20, 14);
    Destination.MAINLAND.add(20, 15);
    Destination.MAINLAND.add(20, 16);
    // Destination.MAINLAND.add(20, 17);
    Destination.MAINLAND.add(20, 18);
    Destination.MAINLAND.add(20, 19);
    Destination.MAINLAND.add(20, 20);
    Destination.MAINLAND.add(20, 21);

    // Destination.MAINLAND.add(21, 11);
    Destination.MAINLAND.add(21, 12);
    Destination.MAINLAND.add(21, 13);
    Destination.MAINLAND.add(21, 14);
    Destination.MAINLAND.add(21, 15);
    // Destination.MAINLAND.add(21, 16);
    // Destination.MAINLAND.add(21, 17);
    // Destination.MAINLAND.add(21, 18);
    Destination.MAINLAND.add(21, 19);
    Destination.MAINLAND.add(21, 20);
    // Destination.MAINLAND.add(21, 21);

    muscleDestinations.addAll(Destination.GILLIGAN.getLocations());
    muscleDestinations.addAll(Destination.MONKEY.getLocations());
    muscleDestinations.addAll(Destination.OYSTER.getLocations());

    mysticalityDestinations.addAll(Destination.DINOSAUR.getLocations());
    mysticalityDestinations.addAll(Destination.LAND_OF_LOST.getLocations());
    mysticalityDestinations.addAll(Destination.MYST.getLocations());

    moxieDestinations.addAll(Destination.CAST_AWAY.getLocations());
    moxieDestinations.addAll(Destination.LORD_OF_FLIES.getLocations());
    moxieDestinations.addAll(Destination.LOST.getLocations());

    sandDestinations.addAll(Destination.RAINBOW_SAND.getLocations());
    altarDestinations.addAll(Destination.ALTAR.getLocations());
    sphereDestinations.addAll(Destination.SPHERE.getLocations());
    plinthDestinations.addAll(Destination.PLINTH.getLocations());
  }

  public static List<Point> getDestinations(final String keyword) {
    return switch (keyword) {
      case "muscle" -> muscleDestinations;
      case "mysticality" -> mysticalityDestinations;
      case "moxie" -> moxieDestinations;
      case "sand" -> sandDestinations;
      case "altar" -> altarDestinations;
      case "sphere" -> sphereDestinations;
      case "plinth" -> plinthDestinations;
      default -> null;
    };
  }

  private static Point getRandomDestination(final String keyword) {
    List<Point> destinations = getDestinations(keyword);
    if (destinations != null) {
      return destinations.get(KoLConstants.RNG.nextInt(destinations.size()));
    }
    return null;
  }

  private static Point getRandomDestination() {
    while (true) {
      int lon = KoLConstants.RNG.nextInt(Point.xMax) + 1;
      int lat = KoLConstants.RNG.nextInt(Point.yMax) + 1;
      Point point = new Point(lon, lat);

      // You cannot go to the mainland
      if (Destination.MAINLAND.getLocations().contains(point)) {
        continue;
      }

      // Going to the Plinth will use up a power sphere for no benefit
      if (Destination.PLINTH.getLocations().contains(point)) {
        continue;
      }

      // We found a safe location
      return point;
    }
  }

  // Automation when GenericRequest gets a redirect to ocean.php
  //
  // - oceanDestination:
  //     muscle
  //     mysticality
  //     moxie
  //     sphere
  //     plinth
  //     LON,LAT
  //
  // - oceanAction
  //     continue
  //     show
  //     stop
  //     save and continue
  //     save and show
  //     save and stop

  public static Point getDestination() {
    String dest = Preferences.getString("oceanDestination");

    return switch (dest) {
      case "manual" -> null;
      case "muscle",
          "mysticality",
          "moxie",
          "sand",
          "altar",
          "sphere",
          "plinth" -> getRandomDestination(dest);
      case "random" -> getRandomDestination();
      default -> (dest.contains(",")) ? Point.parse(dest) : null;
    };
  }

  public static void processOceanAdventure() {
    // We are called when choice.php?whichchoice=189&option=1 redirects to ocean.php.
    GenericRequest request = new GenericRequest("ocean.php");
    Point destination = OceanManager.getDestination();

    if (destination == null) {
      KoLmafia.updateDisplay(MafiaState.ABORT, "Pick a valid course.");
      request.showInBrowser(true);
      return;
    }

    // The navigator says "Sorry, Cap'm, but we can't sail to those
    // coordinates, because that's where the mainland is, and we've pretty much
    // plundered the mainland dry. Perhaps a more exotic locale is in order?"
    if (Destination.MAINLAND.getLocations().contains(destination)) {
      destination = getRandomDestination();
      KoLmafia.updateDisplay(MafiaState.ERROR, "You cannot sail to the mainland.");
      KoLmafia.updateDisplay(MafiaState.ERROR, "Random destination chosen: " + destination);
    }

    String action = Preferences.getString("oceanAction");
    boolean stop = action.equals("stop") || action.equals("savestop");
    boolean show = action.equals("show") || action.equals("saveshow");
    boolean save =
        action.equals("savecontinue") || action.equals("saveshow") || action.equals("savestop");

    // ocean.php?lon=10&lat=10
    request.addFormField("lon", String.valueOf(destination.x));
    request.addFormField("lat", String.valueOf(destination.y));

    request.run();

    if (save) {
      // Save the response Text

      // Trim to contain only HTML body
      int start = request.responseText.indexOf("<body>");
      int end = request.responseText.indexOf("</body>");

      if (start != -1 && end != -1) {
        File output = new File(KoLConstants.DATA_LOCATION, "ocean.html");
        PrintStream writer = LogStream.openStream(output, false);

        String text = request.responseText.substring(start + 6, end);
        writer.println(text);
        writer.close();
      }
    }

    if (stop) {
      // Show result in browser and stop automation
      KoLmafia.updateDisplay(MafiaState.ABORT, "Stop");
      request.showInBrowser(true);
      return;
    }

    if (show) {
      // Show the response in the browser
      request.showInBrowser(true);
    }
  }

  // Relay Browser decoration: preload the Longitude and Latitude text fields with what the
  // "oceanDestination" property specifies

  private static final Pattern LON_PATTERN =
      Pattern.compile("<input type=text class=text size=5 name=lon");
  private static final Pattern LAT_PATTERN =
      Pattern.compile("<input type=text class=text size=5 name=lat");

  public static void decorate(final StringBuffer buffer) {
    Point destination = OceanManager.getDestination();

    if (destination == null) {
      return;
    }

    int lon = destination.x;
    int lat = destination.y;

    // getDestination should always return a valid point.
    if (!Point.valid(lon, lat)) {
      return;
    }

    Matcher lonMatcher = LON_PATTERN.matcher(buffer);
    if (lonMatcher.find()) {
      buffer.insert(lonMatcher.end(), " value=\"" + lon + "\"");
    }

    Matcher latMatcher = LAT_PATTERN.matcher(buffer);
    if (latMatcher.find()) {
      buffer.insert(latMatcher.end(), " value=\"" + lat + "\"");
    }
  }

  private static final Pattern URL_LON_PATTERN = Pattern.compile("lon=(\\d+)");
  private static final Pattern URL_LAT_PATTERN = Pattern.compile("lat=(\\d+)");

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("ocean.php")) {
      return false;
    }

    if (urlString.contains("intro=1")) {
      String message = "Encounter: Set an Open Course for the Virgin Booty";
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
      return true;
    }

    // ocean.php?lon=63&lat=29

    Matcher lonMatcher = URL_LON_PATTERN.matcher(urlString);
    if (!lonMatcher.find()) {
      return true;
    }

    Matcher latMatcher = URL_LAT_PATTERN.matcher(urlString);
    if (!latMatcher.find()) {
      return true;
    }

    int lon = StringUtilities.parseInt(lonMatcher.group(1));
    int lat = StringUtilities.parseInt(latMatcher.group(1));
    Point location = new Point(lon, lat);

    StringBuilder buffer = new StringBuilder();
    buffer.append("Setting sail for (");
    buffer.append(location.toString());
    buffer.append(") = ");
    Destination dest = destinations.get(location);
    buffer.append(dest == null ? "open ocean" : dest.toString());

    String message = buffer.toString();
    RequestLogger.printLine(message);
    RequestLogger.updateSessionLog(message);

    return true;
  }
}
