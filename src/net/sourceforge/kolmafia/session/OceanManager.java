package net.sourceforge.kolmafia.session;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
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
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.FileUtilities;
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

  // All special ocean destinations
  private static final Map<Point, Destination> destinations = new HashMap<>();

  // Map from description to Destination
  private static final Map<String, Destination> descToDest = new HashMap<>();

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
    RAINBOW_SAND("rainbow sand"),
    ALTAR("sinister altar fragment"),
    SPHERE("El Vibrato power sphere"),
    PLINTH("Plinth"),
    MAINLAND("mainland");

    String desc;
    Set<Point> locations = new HashSet<>();

    Destination(String desc) {
      this.desc = desc;
      descToDest.put(desc, this);
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
  }

  static {
    // This forces the enum to be initialized, which will populate any sets and
    // maps used in the constructors.
    Destination[] destinations = Destination.values();
  }

  // Load data file

  private static final String OCEAN_FILE_NAME = "ocean.txt";
  private static final int OCEAN_FILE_VERSION = 1;

  static {
    try (BufferedReader reader =
        FileUtilities.getVersionedReader(OCEAN_FILE_NAME, OCEAN_FILE_VERSION)) {
      String[] data;

      while ((data = FileUtilities.readData(reader)) != null) {
        if (data.length != 3) {
          continue;
        }

        int lon = StringUtilities.parseInt(data[0]);
        int lat = StringUtilities.parseInt(data[1]);
        if (!Point.valid(lon, lat)) {
          System.out.println("Invalid ocean location: " + lon + "," + lat);
          continue;
        }

        String desc = data[2];
        Destination destination = descToDest.get(desc);
        if (destination == null) {
          System.out.println("Unknown destination: " + desc);
          continue;
        }

        destination.add(lon, lat);
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }
  }

  // Special ocean destinations by keyword category.
  // These are Lists so we can easily choose a random index.
  public static final List<Point> muscleDestinations = new ArrayList<>();
  public static final List<Point> mysticalityDestinations = new ArrayList<>();
  public static final List<Point> moxieDestinations = new ArrayList<>();
  public static final List<Point> altarDestinations = new ArrayList<>();
  public static final List<Point> sandDestinations = new ArrayList<>();
  public static final List<Point> sphereDestinations = new ArrayList<>();
  public static final List<Point> plinthDestinations = new ArrayList<>();

  static {
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
  //     sand
  //     altar
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
      case "muscle", "mysticality", "moxie", "sand", "altar", "sphere", "plinth" ->
          getRandomDestination(dest);
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
