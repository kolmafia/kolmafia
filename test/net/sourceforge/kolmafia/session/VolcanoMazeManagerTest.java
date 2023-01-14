package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withCurrentRun;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withGender;
import static internal.helpers.Player.withHP;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withPasswordHash;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLCharacter.Gender;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.session.VolcanoMazeManager.VolcanoMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class VolcanoMazeManagerTest {

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("volcano maze");
    VolcanoMazeManager.reset();
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("volcano maze");
  }

  @Nested
  class Maps {
    @BeforeEach
    public void beforeEach() {
      Preferences.reset("volcano maze");
      VolcanoMazeManager.reset();
    }

    // Validate maps.
    //
    // http://ben.bloomroad.com/kol/nemesis/volcano/index.html
    //
    // There are 6 sets of maps - 3 sets of 2, which are mirror images of each other.
    // RoyalTonberry numbers them {1, 2}, {3, 4}, {5, 6}
    //
    // We will number them the same way.

    private static final int NCOLS = VolcanoMazeManager.NCOLS;
    private static final int START = VolcanoMazeManager.start;

    private String[] loadMapSequence(int key) {
      VolcanoMap[] maps = VolcanoMazeManager.getMapSequence(key);
      assertNotNull(maps);
      int size = maps.length;
      assertEquals(VolcanoMazeManager.MAPS, size);
      String[] coords = new String[size];
      for (int i = 0; i < size; ++i) {
        VolcanoMap map = maps[i];
        String coordinates = map.getCoordinates();
        coords[i] = coordinates;
      }
      return coords;
    }

    private String[] reflectMapSequence(String[] maps) {
      int size = maps.length;
      assertEquals(VolcanoMazeManager.MAPS, size);
      String[] reflection = new String[size];
      for (int i = 0; i < size; ++i) {
        String input = maps[i];
        String reflected =
            Arrays.stream(input.split(","))
                .map(
                    in -> {
                      int platform = Integer.valueOf(in);
                      int row = platform / NCOLS;
                      int col = platform % NCOLS;
                      return (row * NCOLS) + (NCOLS - col - 1);
                    })
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        reflection[i] = reflected;
      }
      return reflection;
    }

    @ParameterizedTest
    @CsvSource({"1, 65", "2, 65", "3, 41", "4, 41", "5, 47", "6, 47"})
    public void validateMapSequence(int key, int pathLength) {
      String[] coords = loadMapSequence(key);
      var cleanups =
          new Cleanups(
              withProperty("volcanoMaze1", coords[0]),
              withProperty("volcanoMaze2", coords[1]),
              withProperty("volcanoMaze3", coords[2]),
              withProperty("volcanoMaze4", coords[3]),
              withProperty("volcanoMaze5", coords[4]));
      try (cleanups) {
        VolcanoMazeManager.loadCurrentMaps(START, 0);
        var solution = VolcanoMazeManager.solve(START, 0);
        // The path stops before hopping onto the goal square
        assertEquals(pathLength - 1, solution.size());
      }
    }

    @ParameterizedTest
    @CsvSource({"1, 2", "3, 4", "5, 6"})
    public void validateReflectedSequences(int key1, int key2) {
      String[] coords1 = loadMapSequence(key1);
      String[] reflection = reflectMapSequence(coords1);
      String[] coords2 = loadMapSequence(key2);
      assertEquals(reflection[0], coords2[0]);
      assertEquals(reflection[1], coords2[1]);
      assertEquals(reflection[2], coords2[2]);
      assertEquals(reflection[3], coords2[3]);
      assertEquals(reflection[4], coords2[4]);
    }
  }

  @Nested
  class Automation {

    @BeforeEach
    public void beforeEach() {
      VolcanoMazeManager.resetRNG();
    }

    @AfterEach
    public void afterEach() {
      VolcanoMazeManager.reset();
      RelayRequest.reset();
    }

    static final List<String> findMapMoves = new ArrayList<>();
    static final List<String> stepMapMoves = new ArrayList<>();
    static final List<String> findMapResponses = new ArrayList<>();
    static final List<String> stepMapResponses = new ArrayList<>();

    public static void loadStepData() {
      Pattern POS_PATTERN = Pattern.compile("\"pos\":\"(.*?)\"");
      String data = html("request/volcano.moves.json");
      // Each line is a JSON response for a move to a location
      // Submitting "volcanomaze.php?move=5,12&ajax=1"
      // yields:
      // {"won":false,"pos":"5,12","show":[3,6,10,14,18,23,26,30,32,41,43,50,52,57,59,60,64,82,84,94,97,102,106,109,111,114,115,117,119,129,136,145,148,153,154,157,164]}
      String[] lines = data.split("\n");
      int count = 1;
      for (String line : lines) {
        Matcher m = POS_PATTERN.matcher(line);
        assertTrue(m.find());
        if (count++ <= 4) {
          findMapResponses.add(line);
          findMapMoves.add(m.group(1));
        } else {
          stepMapResponses.add(line);
          stepMapMoves.add(m.group(1));
        }
      }
      assertEquals(4, findMapMoves.size());
      assertEquals(63, stepMapMoves.size());
    }

    static final Map<String, String> properties = new HashMap<>();

    public static void loadProperties() {
      String data = html("request/volcano.properties.txt");
      // Each line is PROPERTY=VALUE
      String[] lines = data.split("\n");
      for (String line : lines) {
        int equals = line.indexOf("=");
        assertTrue(equals != -1);
        String property = line.substring(0, equals);
        String value = line.substring(equals + 1);
        properties.put(property, value);
      }
      assertEquals(5, properties.size());
    }

    static {
      // Do it exactly once.
      loadStepData();
      loadProperties();
    }

    public Cleanups withVolcanoMaze(FakeHttpClientBuilder builder) {
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withPasswordHash("volcano"),
              withProperty("volcanoMaze1"),
              withProperty("volcanoMaze2"),
              withProperty("volcanoMaze3"),
              withProperty("volcanoMaze4"),
              withProperty("volcanoMaze5"));
      return cleanups;
    }

    public void addVolcanoMazeResponses(FakeHttpClientBuilder builder, List<String> responses) {
      var client = builder.client;
      for (String response : responses) {
        client.addResponse(200, response);
      }
    }

    public int validateVolcanoMazeRequests(
        FakeHttpClientBuilder builder, List<String> moves, int i) {
      var client = builder.client;
      var requests = client.getRequests();
      for (String move : moves) {
        var request = requests.get(i++);
        assertGetRequest(request, "/volcanomaze.php", "move=" + move + "&ajax=1");
      }
      return i;
    }

    @Test
    public void canGetFirstMapWithoutSavedData() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(withVolcanoMaze(builder), withProperty("useCachedVolcanoMaps", false));
      try (cleanups) {
        client.addResponse(200, html("request/test_volcano_start.html"));

        String url = "volcanomaze.php?start=1";
        var request = new RelayRequest(false);
        request.constructURLString(url, false);
        request.run();

        assertEquals(Preferences.getString("volcanoMaze1"), properties.get("volcanoMaze1"));
        assertEquals(Preferences.getString("volcanoMaze2"), "");
        assertEquals(Preferences.getString("volcanoMaze3"), "");
        assertEquals(Preferences.getString("volcanoMaze4"), "");
        assertEquals(Preferences.getString("volcanoMaze5"), "");
      }
    }

    @Test
    public void canGetAllMapsWithSavedData() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(withVolcanoMaze(builder), withProperty("useCachedVolcanoMaps", true));
      try (cleanups) {
        client.addResponse(200, html("request/test_volcano_start.html"));

        String url = "volcanomaze.php?start=1";
        var request = new RelayRequest(false);
        request.constructURLString(url, false);
        request.run();

        assertEquals(Preferences.getString("volcanoMaze1"), properties.get("volcanoMaze1"));
        assertEquals(Preferences.getString("volcanoMaze2"), properties.get("volcanoMaze2"));
        assertEquals(Preferences.getString("volcanoMaze3"), properties.get("volcanoMaze3"));
        assertEquals(Preferences.getString("volcanoMaze4"), properties.get("volcanoMaze4"));
        assertEquals(Preferences.getString("volcanoMaze5"), properties.get("volcanoMaze5"));
      }
    }

    @Test
    public void canAutomateVolcanoMazeFromRelayBrowser() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withVolcanoMaze(builder),
              withProperty("useCachedVolcanoMaps", false),
              // Avoid a "familiar warning" from RelayRequest
              withCurrentRun(800),
              // Avoid a "health warning" from RelayRequest
              withHP(500, 500, 500),
              // Avoid looking at your vinyl boots
              withGender(Gender.FEMALE),
              // Not strictly necessary in simulation, but KoL requires it.
              withClass(AscensionClass.ACCORDION_THIEF),
              withEquipped(EquipmentManager.WEAPON, ItemPool.SQUEEZEBOX_OF_THE_AGES));
      try (cleanups) {
        client.addResponse(200, html("request/test_volcano_intro.html"));
        client.addResponse(200, html("request/test_volcano_start.html"));
        addVolcanoMazeResponses(builder, findMapResponses);
        client.addResponse(200, html("request/test_volcano_fail.html"));
        client.addResponse(200, html("request/test_volcano_jump.html"));
        addVolcanoMazeResponses(builder, stepMapResponses);
        client.addResponse(200, html("request/test_volcano_finish.html"));

        // Visit volcanoisland.php and enter the (defeated) Nemesis's lair
        // You are warned that there will be a fiendishly difficult puzzle
        var url = "volcanoisland.php?action=tniat&pwd=volcano";
        var request = new RelayRequest(false);
        request.constructURLString(url);
        request.run();

        // Continue
        url = "volcanomaze.php?start=1";
        request = new RelayRequest(false);
        request.constructURLString(url, false);
        request.run();

        // You now see the first map of the maze.
        // VolcanoMazeManager parsed it and saved it in volcanoMaze1
        assertEquals(Preferences.getString("volcanoMaze1"), properties.get("volcanoMaze1"));

        // Simulate user typing "solve" button
        url = "/KoLmafia/redirectedCommand?cmd=volcano+solve&pwd=volcano";
        request = new RelayRequest(false);
        request.constructURLString(url);
        request.run();

        // Wait until the submitted command is done
        request.waitForCommandCompletion();
        // RelayRequest tells browser to redirect to see the map
        assertEquals("/volcanomaze.php?start=1", RelayRequest.redirectedCommandURL);

        // The browser requests the map
        url = "volcanomaze.php?start=1";
        request = new RelayRequest(false);
        request.constructURLString(url, false);
        request.run();

        assertEquals(Preferences.getString("volcanoMaze2"), properties.get("volcanoMaze2"));
        assertEquals(Preferences.getString("volcanoMaze3"), properties.get("volcanoMaze3"));
        assertEquals(Preferences.getString("volcanoMaze4"), properties.get("volcanoMaze4"));
        assertEquals(Preferences.getString("volcanoMaze5"), properties.get("volcanoMaze5"));

        // The user decides to jump into the lava and swim to shore
        url = "volcanomaze.php?jump=1";
        request = new RelayRequest(false);
        request.constructURLString(url, false);
        request.run();

        // It's time to try to Solve it again
        url = "/KoLmafia/redirectedCommand?cmd=volcano+solve&pwd=volcano";
        request = new RelayRequest(false);
        request.constructURLString(url);
        request.run();

        // Wait until the submitted command is done
        request.waitForCommandCompletion();
        assertEquals("/volcanomaze.php?start=1", RelayRequest.redirectedCommandURL);

        // The browser requests the map
        url = "volcanomaze.php?start=1";
        request = new RelayRequest(false);
        request.constructURLString(url, false);
        request.run();

        // Verify that we are at the goal
        assertTrue(VolcanoMazeManager.atGoal());

        // Verify that expected requests were submitted
        var requests = client.getRequests();
        assertThat(requests, hasSize(72));

        int i = 0;
        assertPostRequest(requests.get(i++), "/volcanoisland.php", "action=tniat&pwd=volcano");
        assertGetRequest(requests.get(i++), "/volcanomaze.php", "start=1");
        i = validateVolcanoMazeRequests(builder, findMapMoves, i);
        assertGetRequest(requests.get(i++), "/volcanomaze.php", "start=1");
        assertGetRequest(requests.get(i++), "/volcanomaze.php", "jump=1");
        i = validateVolcanoMazeRequests(builder, stepMapMoves, i);
        assertGetRequest(requests.get(i++), "/volcanomaze.php", "start=1");
      }
    }

    @Test
    public void canStepThroughVolcanoMazeFromRelayBrowser() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withVolcanoMaze(builder),
              withProperty("useCachedVolcanoMaps", false),
              // Avoid a "familiar warning" from RelayRequest
              withCurrentRun(800),
              // Avoid a "health warning" from RelayRequest
              withHP(500, 500, 500),
              // Avoid looking at your vinyl boots
              withGender(Gender.FEMALE),
              // Not strictly necessary in simulation, but KoL requires it.
              withClass(AscensionClass.ACCORDION_THIEF),
              withEquipped(EquipmentManager.WEAPON, ItemPool.SQUEEZEBOX_OF_THE_AGES));
      try (cleanups) {
        client.addResponse(200, html("request/test_volcano_intro.html"));
        client.addResponse(200, html("request/test_volcano_start.html"));
        addVolcanoMazeResponses(builder, findMapResponses);
        client.addResponse(200, html("request/test_volcano_jump.html"));
        addVolcanoMazeResponses(builder, stepMapResponses);
        client.addResponse(200, html("request/test_volcano_finish.html"));

        // Visit volcanoisland.php and enter the (defeated) Nemesis's lair
        // You are warned that there will be a fiendishly difficult puzzle
        var url = "volcanoisland.php?action=tniat&pwd=volcano";
        var request = new RelayRequest(false);
        request.constructURLString(url);
        request.run();

        // Continue
        url = "volcanomaze.php?start=1";
        request = new RelayRequest(false);
        request.constructURLString(url, false);
        request.run();

        // You now see the first map of the maze.
        // VolcanoMazeManager parsed it and saved it in volcanoMaze1
        assertEquals(Preferences.getString("volcanoMaze1"), properties.get("volcanoMaze1"));

        // Simulate user repeatedly clicking the "step" button
        int count = 100;
        while (!VolcanoMazeManager.atGoal() && count-- > 0) {
          // This is now hooked into KoL's ajax JavaScript.
          //
          // When you click the "step" button, the browser submits
          // volcanomaze.php?autostep.
          //
          // RelayAgent intercepts that and calls VolcanoMazeManager.autoStep,
          // which submits the appropriate request to KoL, leaving the response
          // in the responseText
          url = "volcanomaze,php?autostep";
          request = new RelayRequest(false);
          request.constructURLString(url);
          VolcanoMazeManager.autoStep(request);
        }

        assertEquals(Preferences.getString("volcanoMaze2"), properties.get("volcanoMaze2"));
        assertEquals(Preferences.getString("volcanoMaze3"), properties.get("volcanoMaze3"));
        assertEquals(Preferences.getString("volcanoMaze4"), properties.get("volcanoMaze4"));
        assertEquals(Preferences.getString("volcanoMaze5"), properties.get("volcanoMaze5"));

        // Verify that expected requests were submitted
        var requests = client.getRequests();
        assertThat(requests, hasSize(70));

        int i = 0;
        assertPostRequest(requests.get(i++), "/volcanoisland.php", "action=tniat&pwd=volcano");
        assertGetRequest(requests.get(i++), "/volcanomaze.php", "start=1");
        i = validateVolcanoMazeRequests(builder, findMapMoves, i);
        assertGetRequest(requests.get(i++), "/volcanomaze.php", "jump=1");
        i = validateVolcanoMazeRequests(builder, stepMapMoves, i);
      }
    }
  }
}
