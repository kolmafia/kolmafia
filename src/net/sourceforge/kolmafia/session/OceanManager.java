package net.sourceforge.kolmafia.session;

import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
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

  // Avoid useless warning
  private OceanManager() {}

  // All special ocean destinations
  private static Map<Point, Destination> destinations = new HashMap<>();

  private static class Point {
    public final int x, y;

    public Point(int lon, int lat) {
      this.x = lon;
      this.y = lat;
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
      return (x - 1) * 100 + (y - 1);
    }

    @Override
    public String toString() {
      return "(" + this.x + "," + this.y + ")";
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

  private static final Pattern OCEAN_PATTERN = Pattern.compile("(\\d+),(\\d+)");

  public static final Point getDestination() {
    String dest = Preferences.getString("oceanDestination");
    if (dest.equals("manual")) {
      return null;
    }

    int lon = 0;
    int lat = 0;

    if (dest.equals("muscle")) {
      lon = 12;
      lat = 84;
    } else if (dest.equals("mysticality")) {
      lon = 3;
      lat = 35;
    } else if (dest.equals("moxie")) {
      lon = 13;
      lat = 91;
    } else if (dest.equals("sphere")) {
      lon = 59;
      lat = 10;
    } else if (dest.equals("plinth")) {
      lon = 63;
      lat = 29;
    } else if (dest.indexOf(",") != -1) {
      Matcher matcher = OCEAN_PATTERN.matcher(dest);
      if (matcher.find()) {
        lon = StringUtilities.parseInt(matcher.group(1));
        lat = StringUtilities.parseInt(matcher.group(2));
      }
    }

    return new Point(lon, lat);
  }

  private static final GenericRequest OCEAN_HANDLER = new GenericRequest("ocean.php");

  public static final void processOceanAdventure() {
    OceanManager.processOceanAdventure(OceanManager.OCEAN_HANDLER);
  }

  public static final void processOceanAdventure(final GenericRequest request) {
    Point destination = OceanManager.getDestination();

    if (destination == null) {
      KoLmafia.updateDisplay(MafiaState.ABORT, "Pick a course.");
      request.showInBrowser(true);
      return;
    }

    int lon = destination.x;
    int lat = destination.y;

    String action = Preferences.getString("oceanAction");
    boolean stop = action.equals("stop") || action.equals("savestop");
    boolean show = action.equals("show") || action.equals("saveshow");
    boolean save =
        action.equals("savecontinue") || action.equals("saveshow") || action.equals("savestop");

    while (true) {
      if (lon < 1 || lon > 242 || lat < 1 || lat > 100) {
        // Pick a random destination
        lon = KoLConstants.RNG.nextInt(242) + 1;
        lat = KoLConstants.RNG.nextInt(100) + 1;
      }

      String coords = "Coordinates: " + lon + ", " + lat;
      RequestLogger.printLine(coords);
      RequestLogger.updateSessionLog(coords);

      // ocean.php?lon=10&lat=10
      request.constructURLString("ocean.php");
      request.clearDataFields();
      request.addFormField("lon", String.valueOf(lon));
      request.addFormField("lat", String.valueOf(lat));

      request.run();

      if (save) {
        // Save the response Text
        File output = new File(KoLConstants.DATA_LOCATION, "ocean.html");
        PrintStream writer = LogStream.openStream(output, false);

        // Trim to contain only HTML body
        int start = request.responseText.indexOf("<body>");
        int end = request.responseText.indexOf("</body>");
        String text = request.responseText.substring(start + 6, end);
        writer.println(text);
        writer.close();
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

      // And continue

      // The navigator says "Sorry, Cap'm, but we can't sail
      // to those coordinates, because that's where the
      // mainland is, and we've pretty much plundered the
      // mainland dry. Perhaps a more exotic locale is in
      // order?"

      if (request.responseText.indexOf("that's where the mainland is") == -1) {
        return;
      }

      // Pick a different random destination
      lon = lat = 0;
    }
  }

  // Relay Browser decoration: preload the Longitude and Latitude text fields with what the
  // "oceanDestination" property specifies

  private static final Pattern LON_PATTERN =
      Pattern.compile("<input type=text class=text size=5 name=lon");
  private static final Pattern LAT_PATTERN =
      Pattern.compile("<input type=text class=text size=5 name=lat");

  public static final void decorate(final StringBuffer buffer) {
    Point destination = OceanManager.getDestination();

    if (destination == null) {
      return;
    }

    int lon = destination.x;
    int lat = destination.y;

    if (lon < 1 || lon > 242 || lat < 1 || lat > 100) {
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

  public static final boolean registerRequest(final String urlString) {
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
    buffer.append("Setting sail for ");
    buffer.append(location.toString());
    buffer.append(" = ");
    Destination dest = destinations.get(location);
    buffer.append(dest == null ? "open ocean" : dest.toString());

    String message = buffer.toString();
    RequestLogger.printLine(message);
    RequestLogger.updateSessionLog(message);

    return true;
  }
}
