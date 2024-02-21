package net.sourceforge.kolmafia.session;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.VolcanoMazeRequest;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class VolcanoMazeManager {
  private static boolean loaded = false;

  // Java's Random takes a 48 bit seed.
  private static long RANDOM_SEED = 0xe1d2c3b4a596L;

  // We'll use our own RNG generator to make testing deterministic.
  private static final Random RNG = new Random(RANDOM_SEED);

  public static void resetRNG() {
    RNG.setSeed(RANDOM_SEED);
  }

  // The number of maps in the cycle
  public static final int MAPS = 5;

  // ***  Data File: index -> map sequence

  private static final Map<Integer, VolcanoMap[]> keyToMapSequence = new TreeMap<>();
  private static final Map<String, Integer> coordsToKey = new HashMap<>();

  private static final String VOLCANO_FILE_NAME = "volcanomaze.txt";
  private static final int VOLCANO_FILE_VERSION = 1;

  // Load data file

  private static void readMapSequences() {
    keyToMapSequence.clear();
    coordsToKey.clear();

    try (BufferedReader reader =
        FileUtilities.getVersionedReader(VOLCANO_FILE_NAME, VOLCANO_FILE_VERSION)) {
      String[] data;

      while ((data = FileUtilities.readData(reader)) != null) {
        if (data.length != 3) {
          continue;
        }

        int key = StringUtilities.parseInt(data[0]);
        int level = StringUtilities.parseInt(data[1]);
        String platforms = data[2];

        if (level < 1 || level > 5) {
          RequestLogger.printLine("Map Sequence #" + key + ": invalid level: " + level);
          continue;
        }

        VolcanoMap[] mapSequence = keyToMapSequence.get(key);
        if (mapSequence == null) {
          mapSequence = new VolcanoMap[MAPS];
        }

        if (mapSequence[level - 1] != null) {
          RequestLogger.printLine("Map Sequence #" + key + ": duplicate level: " + level);
          continue;
        }

        Integer existing = coordsToKey.get(platforms);

        if (existing != null) {
          RequestLogger.printLine(
              "Map Sequence #"
                  + key
                  + " level "
                  + level
                  + ": platforms already in Map Sequence #"
                  + existing);
          continue;
        }

        VolcanoMap map = new VolcanoMap(platforms);
        mapSequence[level - 1] = map;
        keyToMapSequence.put(key, mapSequence);
        coordsToKey.put(platforms, key);
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }

    // Validation
    for (Entry<Integer, VolcanoMap[]> entry : keyToMapSequence.entrySet()) {
      int key = entry.getKey();
      VolcanoMap[] mapSequence = entry.getValue();
      // All 5 maps must be present
      for (int i = 0; i < mapSequence.length; ++i) {
        if (mapSequence[i] == null) {
          RequestLogger.printLine("Map Sequence #" + key + " missing level " + (i + 1) + " map");
        }
      }
    }
  }

  static {
    readMapSequences();
  }

  // For testing
  public static VolcanoMap[] getMapSequence(int key) {
    return keyToMapSequence.get(key);
  }

  private static final VolcanoMap[] maps = new VolcanoMap[MAPS];

  // Which map we are currently on
  private static int currentMap = 0;

  // Where you are on that map
  private static int currentLocation = -1;

  // Constants dictated by size of puzzle
  public static final int NROWS = 13;
  public static final int NCOLS = 13;
  private static final int CELLS = NCOLS * NROWS;
  private static final int MIN_SQUARE = 0;
  private static final int MAX_SQUARE = CELLS - 1;

  // An array, indexed by position, of map # on which this position is
  // above the lava.
  private static final int[] squares = new int[CELLS];
  private static final Neighbors[] neighbors = new Neighbors[CELLS];

  // The number of known platforms. After MAPS maps are known, this had
  // better be all of them.
  private static int found = 1; // Goal known

  // Position of the start: (6,12)
  public static final int start = 162;

  // Position of the goal: (6,6)
  public static final int goal = 84;

  // Neighbors of the goal
  private static final Set<Integer> goalNeighbors = Set.of(70, 71, 72, 83, 85, 96, 97, 98);

  // true if we can step onto the goal from current location
  public static boolean atGoal() {
    return goalNeighbors.contains(currentLocation);
  }

  private static final String[] IMAGES =
      new String[] {
        "platformupyou.gif",
        "platformgoal.gif",
        "platform3.gif",
        "lava1.gif",
        "lava2.gif",
        "lava3.gif",
        "lava4.gif",
        "lava5.gif",
        "lava6.gif",
        "lava7.gif",
        "lava8.gif",
        "lava9.gif",
        "lava10.gif",
        "lava11.gif",
        "lava12.gif",
        "platformdown1.gif",
        "platformdown2.gif",
        "platformdown3.gif",
        "platformdown4.gif",
        "platformup1.gif",
        "platformup2.gif",
        "platformup3.gif",
        "platformup4.gif",
      };

  public static void downloadImages() {
    String base = KoLmafia.imageServerPath() + "itemimages/";
    for (var img : IMAGES) {
      FileUtilities.downloadImage(base + img);
    }
  }

  public static final void reset() {
    loaded = false;
    currentMap = 0;
    currentLocation = -1;
    Arrays.fill(maps, null);
    Arrays.fill(squares, 0);
    Arrays.fill(neighbors, null);
    found = 1;
  }

  public static final void clear() {
    reset();
    for (int map = 0; map < maps.length; ++map) {
      clearCurrentMap(map);
    }
  }

  private static void loadCurrentMaps() {
    if (!loaded) {
      NemesisManager.ensureUpdatedNemesisStatus();
      for (int map = 0; map < maps.length; ++map) {
        loadCurrentMap(map);
      }
      RequestLogger.printLine(found + " total platforms seen.");
      currentMap = 0;
      currentLocation = -1;
      loaded = true;
    }
  }

  // For testing
  public static void loadCurrentMaps(int current, int level) {
    for (int map = 0; map < maps.length; ++map) {
      loadCurrentMap(map);
    }
    currentMap = level;
    currentLocation = current;
    loaded = true;
  }

  private static void loadCurrentMap(final int map) {
    String setting = "volcanoMaze" + (map + 1);
    String coords = Preferences.getString(setting);
    if (!validMap(coords)) {
      Preferences.setString(setting, "");
      maps[map] = null;
    } else {
      maps[map] = new VolcanoMap(coords);
      addSquares(map);
    }
  }

  private static void clearCurrentMap(final int map) {
    String setting = "volcanoMaze" + (map + 1);
    Preferences.setString(setting, "");
  }

  private static boolean validMap(final String coordinates) {
    if (coordinates == null || coordinates.equals("")) {
      return false;
    }

    String[] platforms = coordinates.split("\\s*,\\s*");
    for (int i = 0; i < platforms.length; ++i) {
      String coord = platforms[i];
      if (!StringUtilities.isNumeric(coord)) {
        return false;
      }
      int val = StringUtilities.parseInt(coord);
      if (val < MIN_SQUARE || val > MAX_SQUARE) {
        return false;
      }
    }
    return true;
  }

  // KoL's implementation of volcanomaze.html works like this:
  //
  // The map contains 13 rows with 13 columns.
  // rows and columns are numbered from 0-12.
  //
  // Each square is wrapped in a "div"
  //
  // <div id="sq84" class="sq no  goal lv2" rel="6,6">
  //    <a href="?move=6,6" title="(6,6 - Goal)">&nbsp;</a>
  // <div id="sq162" class="sq no you lv2" rel="6,12">
  //    <a href="?move=6,12" title="(6,12 - You)">&nbsp;</a>
  // <div id="sq25" class="sq no  lv8" rel="12,1">
  //    <a href="?move=12,1" title="(12,1 - Lava)">&nbsp;</a>
  // <div id="sq27" class="sq yes  lv3" rel="1,2">
  //    <a href="?move=1,2" title="(1,2 - Platform)">&nbsp;</a>
  //
  // "id" is "sqXXX", where XXX is (ROW * 13 + COL)
  // "rel" is "COL,ROW"
  //
  // "class" controls how the square is rendered
  //
  // "you" - where you currently stand. You start at (6,12)
  // "goal" - the center square (6,6)
  // "yes" - a platform
  // "no" - lava
  // "lv1" - "lv12" - which (animated) lava image to use
  //
  // Since platforms rise and sink, all squares have a "lv" class
  //
  // Squares are rendered via a "style" directive
  //
  // "lv1" - "lv12" -> lava1.gif - lava12.gif
  // "yes" -> platform3.gif
  // "goal" -> platformgoal.gif
  // "you" -> platformupyou.gif
  //
  // Mouse clicks on any of these divs is handled by Javascript
  // The script has a $(document).ready function which handles mouse clicks
  // - clicking on a "no" square (lava) confirms 'Swim back to the start?'
  //   If so, it submits "?jump=1"
  // - clicking on a "sq" submits the appropriate URL ("?move=12,1") with "ajax=1"
  //   KoL responds with JSON specifying the new set of platforms which are visible
  //
  // This is how where all the fancy animation happens:
  //   The Javascript function iterates through all squares, removing "yes"
  //   It adds "yes" to exactly the squares which are visible
  //   It changes the titles, as appropriate - "Lava", "Platform", "You"
  //   It changes the images, as appropriate
  //     Lava->Platform = platformup{1,2,3,4}
  //     Platform->Lava = platformdown{1,2,3,4}
  // When it is finished, the browser re-renders all the squares using their new classes

  // KoLmafia augments the above as follows:
  //
  // 1) Decorate the map to show you where the next step is, should you
  //    choose to do it manually
  //
  // When we see the initial HTML page, we calculate the solution.
  // - we add a "next" class to the correct square to step to.
  // - we give that square a "Next Platform" title
  // - we change the style to render "next" with "platform3x.gif"
  //
  // When we get the JSON from a move, we calculate the solution.
  // - we add "next":"COL,ROW"
  // - the Javascript function adds the "next" class to that square
  //
  // 2) Provide a "Step" button to step to the correct platform.
  //
  // The JS function handles that by submitting "volcanomaze.php?autostep"
  // KoLmafia calculates the solution and submits the appropriate "move" with "ajax"
  // We return the JSON and the JS handles it as normal
  //
  // 3) Provide a "Solve" button to step through the whole solution.
  //
  // That submits "/KoLmafia/polledredirectedCommand?cmd=volcano+solve&pwd"
  // "volcano solve" calculates the solution and steps through it, returning
  // the final JSON with you standing next to the goal.

  public static final void decorate(final String location, final StringBuffer buffer) {
    if (!location.contains("volcanomaze.php")) {
      return;
    }

    // Calculate "next" location - where we will step to
    int nextLocation = -1;

    // Stop before stepping onto the goal
    if (!atGoal()) {
      // Calculate the path from here to the goal
      Path solution = solve(currentLocation, currentMap);
      nextLocation = (solution == null) ? -1 : solution.get(0);
    }

    if (buffer.charAt(0) == '{') {
      decorateJSON(buffer, nextLocation);
      return;
    }

    decorateHTML(buffer, nextLocation);
  }

  private static final void decorateHTML(final StringBuffer buffer, int nextLocation) {
    // Replace the inline Javascript for handling the maze with our local copy.
    replaceVolcanoMazeJavaScript(buffer);

    // <div id="sq163" class="sq yes  lv3" rel="7,12"><a href="?move=7,12" title="(7,12 -
    // Platform)">&nbsp;</a></div>
    if (nextLocation != -1) {
      Pattern pattern =
          Pattern.compile("<div id=\"sq" + nextLocation + "\".*?</div>", Pattern.DOTALL);
      Matcher matcher = pattern.matcher(buffer);
      if (matcher.find()) {
        String div = matcher.group(0);
        div = StringUtilities.singleStringReplace(div, "yes", "yes next");
        div = StringUtilities.singleStringReplace(div, "Platform", "Next Platform");
        buffer.replace(matcher.start(), matcher.end(), div);
      }
      String yesStyle =
          ".yes a { background: url('https://d2uyhvukfffg5a.cloudfront.net/itemimages/platform3.gif'); }";
      int index = buffer.indexOf(yesStyle);
      if (index != -1) {
        String nextStyle =
            ".next a { background: url('https://d2uyhvukfffg5a.cloudfront.net/itemimages/platform3x.gif'); }";
        buffer.insert(index + yesStyle.length(), nextStyle);
      }
    }

    // Add a "Solve!" button to the Volcanic Cave which invokes the
    // "volcano solve" command.

    String search = "</form>";
    int index = buffer.lastIndexOf(search);
    if (index == -1) {
      return;
    }
    index += 7;

    // Build "Step" and "Solve!" buttons

    boolean disabled = atGoal();

    StringBuffer span = new StringBuffer();
    span.append("<center><table cols=2><tr>");

    StringBuffer stepButton = new StringBuffer();
    String url = "?autostep";
    stepButton.append("<td>").append("<div id=\"step\">");
    stepButton.append("<form name=stepform action='").append(url).append("' method=get>");
    stepButton.append("<input class=button type=submit value=\"Step\">");
    stepButton.append("</form>");
    stepButton.append("</div>").append("</td>");
    span.append(stepButton);

    StringBuffer solveButton = new StringBuffer();
    url = "/KoLmafia/polledredirectedCommand?cmd=volcano+solve&pwd=" + GenericRequest.passwordHash;
    solveButton.append("<td>");
    solveButton.append("<form name=solveform action='").append(url).append("' method=post>");
    solveButton.append("<input class=button type=submit value=\"Solve!\"");
    if (disabled) {
      solveButton.append(" disabled");
    }
    solveButton.append(">").append("</form>");
    solveButton.append("</td>");
    span.append(solveButton);

    span.append("</tr></table></center>");

    // Insert it into the page
    buffer.insert(index, span);
  }

  private static final void decorateJSON(final StringBuffer buffer, int nextLocation) {
    // {"won":false,"pos":"5,12","show":[3,6,10,14,18,23,26,30,32,41,43,50,52,57,59,60,64,82,84,94,97,102,106,109,111,114,115,117,119,129,136,145,148,153,154,157,164]}
    if (nextLocation == -1) {
      return;
    }
    int index = buffer.indexOf(",\"show\"");
    if (index != -1) {
      String nextCoords = ",\"next\":\"" + coordinateString(nextLocation) + "\"";
      buffer.insert(index, nextCoords);
    }
  }

  public static final void replaceVolcanoMazeJavaScript(StringBuffer buffer) {
    int start = buffer.indexOf("<script type=\"text/javascript\">\n\t\t\tvar uhohs = 0;");
    if (start == -1) {
      return;
    }
    int end = buffer.indexOf("</script>", start);
    if (end == -1) {
      return;
    }
    buffer.replace(
        start,
        end,
        "<script language=\"Javascript\" src=\"/" + KoLConstants.VOLCANOMAZE_JS + "\">");
  }

  public static final void parseResult(final String responseText) {
    // Load current maps, if necessary
    loadCurrentMaps();

    // Parse what the server gave us
    String coords = parseCoords(responseText);

    // Make sure we got a good map
    if (!validMap(coords)) {
      return;
    }

    // Find currently known map
    if (found == CELLS) {
      for (int i = 0; i < maps.length; ++i) {
        if (coords.equals(maps[i].coordinates)) {
          currentMap = i;
          return;
        }
      }
    }

    // If we don't have all the maps and are using cached map sets, see if we
    // can derive which cycle we are in
    if (Preferences.getBoolean("useCachedVolcanoMaps")) {
      int key = coordsToKey.getOrDefault(coords, 0);
      if (key != 0) {
        VolcanoMap[] mapSequence = keyToMapSequence.get(key);
        for (int i = 0; i < mapSequence.length; i++) {
          VolcanoMap map = mapSequence[i];
          // Save the squares
          maps[i] = map;
          addSquares(i);
          if (coords.equals(map.coordinates)) {
            currentMap = i;
          }
          int sequence = i + 1;
          String setting = "volcanoMaze" + sequence;
          Preferences.setString(setting, map.coordinates);
        }
        found = CELLS;
        return;
      }
      // Oh! Oh! A new Map Sequence!
    }

    // Find an empty slot for this map
    int index = currentMap;
    do {
      VolcanoMap current = maps[index];
      // Empty slot found
      if (current == null) {
        currentMap = index;
        maps[index] = new VolcanoMap(coords);
        break;
      }

      if (coords.equals(current.coordinates)) {
        currentMap = index;
        return;
      }

      // Skip to next slot
      index = (index + 1) % maps.length;
    } while (index != currentMap);

    // It's a new map. Save the coordinates in user settings
    int sequence = index + 1;
    String setting = "volcanoMaze" + sequence;
    Preferences.setString(setting, coords);

    // Save the squares
    addSquares(currentMap);
    RequestLogger.printLine(found + " total platforms seen.");

    // If we have found all the maps and we are using cached maps, we have just
    // discovered a new sequence. Log it so we can add it to volcanomaze.txt
    if (found == CELLS) {
      String printMe = "--------------------";
      RequestLogger.printLine(printMe);
      RequestLogger.updateSessionLog(printMe);
      String newSequence = String.valueOf(keyToMapSequence.size() + 1);
      for (int i = 0; i < maps.length; ++i) {
        VolcanoMap map = maps[i];
        printMe = newSequence + "\t" + (i + 1) + "\t" + map.coordinates;
        RequestLogger.printLine(printMe);
        RequestLogger.updateSessionLog(printMe);
      }
      printMe = "--------------------";
      RequestLogger.printLine(printMe);
      RequestLogger.updateSessionLog(printMe);
    }
  }

  private static void addSquares(final int index) {
    VolcanoMap map = maps[index];
    int seq = index + 1;
    Integer[] platforms = map.getPlatforms();
    int ofound = found;
    int pcount = platforms.length;
    RequestLogger.printLine("Map #" + seq + " has " + pcount + " platforms");
    for (int i = 0; i < pcount; ++i) {
      int square = platforms[i];
      int old = squares[square];
      if (old == 0) {
        squares[square] = seq;
        found++;
      } else if (old != seq) {
        // Something is wrong: we already found this
        // square elsewhere in the sequence
        RequestLogger.printLine("Platform " + square + " already seen on map #" + old);
      }
    }
  }

  private static String parseCoords(final String responseText) {
    // move=x,y returns simply "false" if can't move there
    if (responseText.equals("false")) {
      return null;
    } else if (responseText.startsWith("<html>")) {
      return parseHTMLCoords(responseText);
    } else {
      return parseJSONCoords(responseText);
    }
  }

  private static final Pattern SQUARE_PATTERN =
      Pattern.compile(
          "<div id=\"sq(\\d+)\" class=\"sq (no|yes)\\s+(you|goal|)\\s*lv(\\d+)\" rel=\"(\\d+),(\\d+)\">");

  private static String parseHTMLCoords(final String responseText) {
    Matcher matcher = SQUARE_PATTERN.matcher(responseText);
    StringBuffer buffer = new StringBuffer();
    boolean first = true;
    while (matcher.find()) {
      String square = matcher.group(1);
      String special = matcher.group(3);
      if (!"".equals(special)) {
        int squint = Integer.parseInt(square);
        if (special.equals("you")) {
          currentLocation = squint;
        }

        // Sanity check
        else if (special.equals("goal") && goal != squint) {
          RequestLogger.printLine("Map says goal is on square " + squint + ", not " + goal);
        }
      }

      String type = matcher.group(2);
      if (!type.equals("yes")) {
        continue;
      }

      // int column = Integer.parseInt(matcher.group(5));
      // int row = Integer.parseInt(matcher.group(6));

      if (first) {
        first = false;
      } else {
        buffer.append(",");
      }
      buffer.append(square);
    }

    return buffer.toString();
  }

  // {"won":"","pos":"6,3","show":["2","6","9","15","20","24","32","34","38","40","47","49","52","56","57","59","64","84","86","92","97","100","105","106","109","111","114","117","127","129","136","144","145","150","153","160","167"]}

  private static final Pattern POS_PATTERN = Pattern.compile("(\\d+),(\\d+)");

  private static String parseJSONCoords(final String responseText) {
    StringBuffer buffer = new StringBuffer();
    JSONObject JSON;

    // Parse the string into a JSON object
    try {
      JSON = new JSONObject(responseText);
    } catch (JSONException e) {
      return "";
    }

    // "pos" is the player's position
    try {
      String pos = JSON.getString("pos");
      Matcher matcher = POS_PATTERN.matcher(pos);
      if (matcher.find()) {
        int col = Integer.parseInt(matcher.group(1));
        int row = Integer.parseInt(matcher.group(2));
        int square = row * NCOLS + col;
        currentLocation = square;
      }
    } catch (JSONException e) {
      currentLocation = -1;
    }

    // "show" is an array of platforms
    JSONArray show;
    try {
      show = JSON.getJSONArray("show");
    } catch (JSONException e) {
      return "";
    }

    // Iterate over the squares
    boolean first = true;
    int count = show.length();
    for (int index = 0; index < count; ++index) {
      String square = show.optString(index, null);

      // Omit the goal square; that is a platform on all maps
      if (square == null || square.equals("84")) {
        continue;
      }

      if (first) {
        first = false;
      } else {
        buffer.append(",");
      }
      buffer.append(square);
    }

    return buffer.toString();
  }

  private static int row(final int pos) {
    return (pos / NCOLS);
  }

  private static int col(final int pos) {
    return (pos % NCOLS);
  }

  private static int pos(final int row, final int col) {
    return (row * NCOLS + col);
  }

  public static final String coordinateString(final int pos) {
    if (pos == -1) {
      return "(unknown)";
    }

    int row = row(pos);
    int col = col(pos);

    // Yes, KoL really does display ( column, row )
    return col + "," + row;
  }

  public static final String coordinateString(final int pos, final int map) {
    String cstr = coordinateString(pos);
    String mstr = (map >= 0) ? ("map " + (map + 1)) : "(unknown map )";
    return cstr + " on " + mstr;
  }

  public static final String currentCoordinates() {
    return coordinateString(currentLocation);
  }

  public static final void printCurrentCoordinates() {
    String msg =
        (currentLocation == -1)
            ? "I don't know where you are"
            : "Current position: " + coordinateString(currentLocation, currentMap);
    RequestLogger.printLine(msg);
  }

  private static boolean discoverMaps() {
    loadCurrentMaps();
    if (found == CELLS) {
      return true;
    }

    // Visit the cave to find out where we are
    if (currentLocation < 0) {
      internalVisit();
    }

    // Give up now if we couldn't do that.
    if (currentLocation < 0) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You couldn't find the lava cave");
      return false;
    }

    printCurrentCoordinates();

    while (found < CELLS) {
      VolcanoMap map = maps[currentMap];
      int me = currentLocation;
      int next = map.pickNeighbor(me);

      if (next < 0) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "You seem to be stuck");
        return false;
      }

      RequestLogger.printLine("Move to: " + coordinateString(next, currentMap));

      int ofound = found;
      VolcanoMazeRequest req = new VolcanoMazeRequest(next);
      req.run();

      if (ofound >= found) {
        // This shouldn't happen
        KoLmafia.updateDisplay(MafiaState.ERROR, "Moving did not discover new platforms");
        return false;
      }
    }

    return true;
  }

  // CLI command support
  public static final void visit() {
    internalVisit();
    printCurrentCoordinates();
  }

  private static void internalVisit() {
    // Must make a new VolcanoMazeRequest every time since that
    // class follows redirects.
    VolcanoMazeRequest VISITOR = new VolcanoMazeRequest();
    VISITOR.run();
  }

  public static final void jump() {
    // Must make a new VolcanoMazeRequest every time since that
    // class follows redirects.
    VolcanoMazeRequest JUMP = new VolcanoMazeRequest(true);
    JUMP.run();
    printCurrentCoordinates();
  }

  public static final void move(final int x, final int y, final boolean print) {
    VolcanoMazeRequest req = new VolcanoMazeRequest(x, y);
    req.run();
    if (print) {
      displayMap();
    }
    printCurrentCoordinates();
  }

  public static final void displayMap() {
    loadCurrentMaps();
    VolcanoMap map = maps[currentMap];
    if (map == null) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "We haven't seen the volcanic cave yet");
      return;
    }

    map.displayHTMLMap(currentLocation);
  }

  public static final void displayMap(final int num) {
    if (num < 1 || num > MAPS) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Choose map # from 1 - " + MAPS);
      return;
    }

    loadCurrentMaps();
    VolcanoMap map = maps[num - 1];
    if (map == null) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "We haven't seen map #" + num);
      return;
    }

    map.displayHTMLMap(-1);
  }

  public static final void platforms() {
    if (!discoverMaps()) {
      return;
    }

    // Make an HTML table to display platform map
    StringBuffer buffer = new StringBuffer();

    buffer.append("<table border cols=14>");
    buffer.append("<tr><td></td>");
    for (int col = 0; col < NCOLS; ++col) {
      buffer.append("<td align=center><b>");
      buffer.append(col);
      buffer.append("</b></td>");
    }
    buffer.append("</tr>");
    for (int row = 0; row < NROWS; ++row) {
      buffer.append("<tr>");
      buffer.append("<td valign=center><b>");
      buffer.append(row);
      buffer.append("</b></td>");
      for (int col = 0; col < NCOLS; ++col) {
        buffer.append("<td>");
        int map = squares[pos(row, col)];
        buffer.append(map);
        buffer.append("</td>");
      }
      buffer.append("</tr>");
    }
    buffer.append("</table>");
    RequestLogger.printHtml(buffer.toString());
    RequestLogger.printLine();
  }

  public static final void step() {
    loadCurrentMaps();
    autoStep(new RelayRequest(false));
  }

  public static final void autoStep(RelayRequest request) {
    // This is invoked by clicking the "step" button in the relay browser.
    //
    // KoL's ajax JavaScript submits volcanomaze.php?autostep
    // RelayAgent catches that and calls this function.

    if (atGoal()) {
      request.responseText = "false";
      return;
    }

    request.constructURLString(nextStep(), false);
    request.run();
  }

  private static final String nextStep() {
    // Return the URL to submit

    // If we don't know where we are, visit the cave and find out.
    if (currentLocation < 0) {
      return "/volcanomaze.php?start=1";
    }

    // If we have not seen all the maps, take a step and learn one
    if (found < CELLS) {
      VolcanoMap map = maps[currentMap];
      int me = currentLocation;
      int next = map.pickNeighbor(me);
      // If you are stuck, no option but to jump
      if (next < 0) {
        return "/volcanomaze.php?jump=1";
      }
      return "/volcanomaze.php?move=" + coordinateString(next) + "&ajax=1";
    }

    // If current location is adjacent to the goal, don't move.
    // Stop before stepping onto the goal
    if (atGoal()) {
      return "/volcanomaze.php?start=1";
    }

    // Calculate the path from here to the goal
    Path solution = solve(currentLocation, currentMap);
    printStatistics(solution);

    // You can't get there from here.
    if (solution == null) {
      return "/volcanomaze.php?jump=1";
    }

    // Choose the first step on the path
    int next = solution.get(0);
    return "/volcanomaze.php?move=" + coordinateString(next) + "&ajax=1";
  }

  private static int pathsMade = 0;
  private static int pathsExamined = 0;

  public static final void solve() {
    // Save URL to give back to the user's browser
    RelayRequest.redirectedCommandURL = "/volcanomaze.php?start=1";

    if (!discoverMaps()) {
      return;
    }

    // Sanity check
    if (found < CELLS) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "We couldn't discover all the maps");
      return;
    }

    Path solution = solve(currentLocation, currentMap);
    printStatistics(solution);

    if (solution == null) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "You can't get there from here. Swim to shore and try again.");
      return;
    }

    // Move up next to the goal.
    int length = solution.size();
    int i = 0;
    for (int sq : solution) {
      // Quit when we are about to move to the goal
      if (sq == goal) {
        break;
      }

      RelayRequest.specialCommandStatus = "Move " + ++i + " of " + length;
      VolcanoMazeRequest req = new VolcanoMazeRequest(sq);
      req.run();
    }
  }

  public static final void test(final int map, final int x, final int y) {
    loadCurrentMaps();

    // Sanity check
    if (found < CELLS) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You don't know all the maps");
      return;
    }

    int location = pos(y, x);
    Path solution = solve(location, map - 1);
    printStatistics(solution);

    if (solution == null) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "You can't get there from here. Swim to shore and try again.");
      return;
    }

    // Print the solution
    for (Integer next : solution) {
      RequestLogger.printLine("Hop to " + coordinateString(next));
    }
  }

  private static void printStatistics(final Path solution) {
    StringBuffer buffer = new StringBuffer();
    buffer.append("Paths examined/made ");
    buffer.append(KoLConstants.COMMA_FORMAT.format(pathsExamined));
    buffer.append("/");
    buffer.append(KoLConstants.COMMA_FORMAT.format(pathsMade));
    buffer.append(" ->");
    if (solution != null) {
      buffer.append(" solution with ");
      buffer.append(solution.size());
      buffer.append(" hops.");
    } else {
      buffer.append(" no solution found.");
    }
    RequestLogger.printLine(buffer.toString());
  }

  // solve( currentLocation, currentMap ): solve the volcano cave puzzle
  //
  // Inputs:
  //
  // location - starting square
  // map - starting map
  //
  // Global constants:
  //
  // CELLS - number of cells in map: rows & columns
  // MAPS - number of maps in cycle.
  // goal - cell # of goal platform
  //
  // Global input data:
  //
  // VolcanoMap maps[ MAPS ]
  //    Indexed from 0 to MAP
  // int squares[ CELLS ]
  //    Indexed by platform #: map # containing platform
  //
  // Global input/output data:
  //
  // Neighbors neighbors[ CELLS ]
  //    Indexed by platform #: neighbors of platform in same map
  //
  // Global output data:
  //
  // pathsMade - paths generated
  // pathsExamined - paths examined

  public static Path solve(final int location, final int map) {
    // Can't solve unless we know all the maps
    if (found < CELLS) {
      return null;
    }

    // Generate neighbors for every cell
    generateNeighbors();

    // The work queue of Paths
    LinkedList<Path> queue = new LinkedList<>();

    // Statistics
    pathsMade = 0;
    pathsExamined = 0;

    // Find the neighbors for the current location in the current
    // map. These are the first hop for all possible paths.
    VolcanoMap current = maps[map];
    Neighbors roots = current.neighbors(location);

    // We only need to visit any given cell once.
    boolean[] visited = new boolean[CELLS];

    // We have visited the start square
    visited[location] = true;

    // Make a path for each root and add it to the queue.
    Integer[] starts = roots.getPlatforms();
    for (int i = 0; i < starts.length; ++i) {
      ++pathsMade;
      Integer square = starts[i];
      queue.addLast(new Path(square));
      // We (will) have visited each root
      visited[square] = true;
    }

    // Perform a breadth-first search of the maze
    while (!queue.isEmpty()) {
      Path path = queue.removeFirst();
      ++pathsExamined;

      Integer last = path.getLast();
      Integer[] platforms = neighbors[last].getPlatforms();

      // Examine each neighbor
      for (int i = 0; i < platforms.length; ++i) {
        Integer platform = platforms[i];
        // If this is a goal, we have the solution
        if (platform == goal) {
          ++pathsMade;
          return new Path(path, platform);
        }

        // If neighbor not yet seen, add and search it
        int square = platform;
        if (!visited[square]) {
          ++pathsMade;
          queue.addLast(new Path(path, platform));
          // We (will) have visited this platform
          visited[square] = true;
        }
      }
    }

    // No solution found
    return null;
  }

  private static void generateNeighbors() {
    for (int square = 0; square < CELLS; ++square) {
      // Calculate and store neighbors once only
      if (neighbors[square] != null) {
        continue;
      }

      // The goal appears in every map
      if (square == goal) {
        neighbors[square] = new Neighbors(square, null);
        continue;
      }

      // Otherwise, get the neighbors relative to the map
      // the square is in.
      int index = squares[square];
      VolcanoMap pmap = maps[index % MAPS];
      neighbors[square] = pmap.neighbors(square);
    }
  }

  public static class VolcanoMap implements Comparable<VolcanoMap> {
    public final String coordinates;
    public final Integer[] platforms;
    public final boolean[] board = new boolean[CELLS];

    public VolcanoMap(final String coordinates) {
      this.coordinates = coordinates;

      // Make an array of all the platforms
      String[] squares = coordinates.split("\\s*,\\s*");
      List<Integer> list = new ArrayList<>();
      for (int i = 0; i < squares.length; ++i) {
        String coord = squares[i];
        if (!StringUtilities.isNumeric(coord)) {
          continue;
        }
        Integer ival = Integer.valueOf(coord);
        list.add(ival);
        this.board[ival] = true;
      }
      this.platforms = list.toArray(new Integer[list.size()]);

      // Every board has the goal platform
      this.board[goal] = true;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof VolcanoMap that) {
        return this.coordinates.equals(that.coordinates);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return this.coordinates.hashCode();
    }

    @Override
    public int compareTo(final VolcanoMap o) {
      return this.coordinates.compareTo(o.coordinates);
    }

    public String getCoordinates() {
      return this.coordinates;
    }

    public Integer[] getPlatforms() {
      return this.platforms;
    }

    public boolean[] getBoard() {
      return this.board;
    }

    public boolean inMap(final int row, final int col) {
      return this.inMap(pos(row, col));
    }

    public boolean inMap(final int square) {
      return this.board[square];
    }

    public Neighbors neighbors(final int square) {
      return new Neighbors(square, this);
    }

    public int pickNeighbor(final int square) {
      Neighbors neighbors = this.neighbors(square);
      Integer[] platforms = neighbors.getPlatforms();

      // We might be stuck
      if (platforms.length == 0) {
        return -1;
      }

      // If there is only one neighbor, that's it
      if (platforms.length == 1) {
        int next = platforms[0];
        // Don't pick the goal!
        return (next != goal) ? next : -1;
      }

      // Otherwise, pick one at random.
      int next = goal;
      while (next == goal) {
        int rnd = RNG.nextInt(platforms.length);
        next = platforms[rnd];
      }
      return next;
    }

    public void print(final int player) {
      int prow = row(player);
      int pcol = col(player);
      StringBuffer buffer = new StringBuffer();
      for (int row = 0; row < NROWS; ++row) {
        if (row < 9) {
          buffer.append(" ");
        }
        buffer.append((row + 1));
        for (int col = 0; col < NCOLS; ++col) {
          buffer.append(" ");
          if (row == prow && col == pcol) {
            buffer.append("@");
          } else if (row == 6 && col == 6) {
            buffer.append("*");
          } else if (board[pos(row, col)]) {
            buffer.append("O");
          } else {
            buffer.append(".");
          }
        }
        buffer.append(KoLConstants.LINE_BREAK);
      }

      System.out.println(buffer.toString());
    }

    public void displayHTMLMap(final int player) {
      int prow = row(player);
      int pcol = col(player);
      StringBuffer buffer = new StringBuffer();

      buffer.append("<table cellpadding=0 cellspacing=0 cols=14>");
      buffer.append("<tr><td></td>");
      for (int col = 0; col < NCOLS; ++col) {
        buffer.append("<td align=center><b>");
        buffer.append(col);
        buffer.append("</b></td>");
      }
      buffer.append("</tr>");
      for (int row = 0; row < NROWS; ++row) {
        buffer.append("<tr>");
        buffer.append("<td valign=center><b>");
        buffer.append(row);
        buffer.append("</b></td>");
        for (int col = 0; col < NCOLS; ++col) {
          buffer.append("<td>");
          buffer.append("<img src=\"");
          buffer.append(KoLmafia.imageServerPath());
          buffer.append("itemimages/");
          if (row == prow && col == pcol) {
            buffer.append("platformupyou");
          } else if (row == 6 && col == 6) {
            buffer.append("platformgoal");
          } else if (board[pos(row, col)]) {
            buffer.append("platform3");
          } else {
            int rnd = RNG.nextInt(12);
            buffer.append("lava");
            buffer.append((rnd + 1));
          }
          buffer.append(".gif\" width=30 height=30>");
          buffer.append("</td>");
        }
        buffer.append("</tr>");
      }
      buffer.append("</table>");
      RequestLogger.printHtml(buffer.toString());
      RequestLogger.printLine();
    }
  }

  private static class Neighbors {
    public final Integer[] platforms;

    public Neighbors(final int square, final VolcanoMap map) {
      int row = row(square);
      int col = col(square);

      ArrayList<Integer> list = new ArrayList<>();
      Neighbors.addSquare(list, map, row - 1, col - 1);
      Neighbors.addSquare(list, map, row - 1, col);
      Neighbors.addSquare(list, map, row - 1, col + 1);
      Neighbors.addSquare(list, map, row, col - 1);
      Neighbors.addSquare(list, map, row, col + 1);
      Neighbors.addSquare(list, map, row + 1, col - 1);
      Neighbors.addSquare(list, map, row + 1, col);
      Neighbors.addSquare(list, map, row + 1, col + 1);

      this.platforms = new Integer[list.size()];
      list.toArray(this.platforms);
    }

    public Integer[] getPlatforms() {
      return this.platforms;
    }

    private static void addSquare(List<Integer> list, final VolcanoMap map, int row, int col) {
      if (row >= 0 && row < NROWS && col >= 0 && col < NCOLS) {
        int square = pos(row, col);
        if (map == null || map.inMap(square)) {
          list.add(Integer.valueOf(square));
        }
      }
    }
  }

  public static class Path implements Iterable<Integer> {
    private final List<Integer> list;

    public Path(final Integer square) {
      this.list = new ArrayList<>();
      this.list.add(square);
    }

    @Override
    public Iterator<Integer> iterator() {
      return this.list.iterator();
    }

    public Path(final Path prefix, final Integer square) {
      this.list = new ArrayList<>(prefix.list);
      list.add(square);
    }

    public boolean contains(final Integer elem) {
      return list.contains(elem);
    }

    public Integer get(final int index) {
      return list.get(index);
    }

    public Integer getLast() {
      return list.get(list.size() - 1);
    }

    public int size() {
      return list.size();
    }

    @Override
    public String toString() {
      StringBuffer buffer = new StringBuffer();
      int count = list.size();
      boolean first = true;
      buffer.append("[");
      for (int i = 0; i < count; ++i) {
        if (first) {
          first = false;
        } else {
          buffer.append(",");
        }
        buffer.append(list.get(i));
      }
      buffer.append("]");
      return buffer.toString();
    }
  }
}
