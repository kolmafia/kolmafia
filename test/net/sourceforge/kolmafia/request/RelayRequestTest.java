package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.bytes;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withAdventuresSpent;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withMeat;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withTurnsPlayed;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import internal.helpers.Cleanups;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureQueueDatabase;
import net.sourceforge.kolmafia.persistence.AdventureSpentDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class RelayRequestTest {

  /**
   * The global clean up code will delete root/relay since relay is not under Git control. This is
   * the only test that breaks when that happens. Duplicate the code that loads the relay directory
   * here so that the directory will be present when the test is run.
   */
  public void initializeRelayFileDirectory() {
    for (int i = 0; i < KoLConstants.RELAY_FILES.length; ++i) {
      FileUtilities.loadLibrary(
          KoLConstants.RELAY_LOCATION, KoLConstants.RELAY_DIRECTORY, KoLConstants.RELAY_FILES[i]);
    }
  }

  @Test
  public void findVariousRelayFilesOrNot() {
    initializeRelayFileDirectory();
    File f;
    f = RelayRequest.findRelayFile("thisIsNotAPipe");
    assertNotNull(f, "Allowed to find a new file.");
    assertFalse(f.exists(), "Found file is not supposed to exist.");
    f = RelayRequest.findRelayFile("thisIs..NotAPipe");
    assertNull(f, "Not supposed to find file with dots in name.");
    f = RelayRequest.findRelayFile("barrel_sounds.js");
    assertTrue(f.exists(), "Supposed to find file that exists.");
  }

  @Test
  public void exerciseSomeStaticThingsForCoverage() {
    RelayRequest.loadOverrideImages(false);
    RelayRequest.loadOverrideImages(true);
    Preferences.setBoolean("relayOverridesImages", true);
    RelayRequest.loadOverrideImages(false);
    RelayRequest.loadOverrideImages(true);
    RelayRequest.clearImageCache();
    assertFalse(RelayRequest.builtinRelayFile("notafile"));
    assertTrue(RelayRequest.builtinRelayFile("afterlife.1.ash"));
  }

  @Test
  public void exerciseSomeObjectMethodsForCoverage() {
    RelayRequest rr = new RelayRequest(false);
    assertNull(rr.getHashField());
    assertFalse(rr.retryOnTimeout());
    rr.constructURLString("diary.php?textversion=1");
  }

  @Nested
  class LocalFiles {
    private RelayRequest makeFileRequest(String path) throws IOException {
      Files.copy(Paths.get("request", path), Paths.get("relay", path));
      var rr = new RelayRequest(true);
      rr.constructURLString(path, true);
      rr.run();
      Files.deleteIfExists(Paths.get("relay", path));
      return rr;
    }

    @BeforeAll
    public static void beforeAll() throws IOException {
      try {
        Files.createDirectory(Paths.get("relay"));
      } catch (FileAlreadyExistsException e) {
      }
    }

    @Test
    public void returnsNotFound() {
      var rr = new RelayRequest(true);
      rr.constructURLString("nonexistent.png", true);
      rr.run();

      assertThat(rr.statusLine, is("HTTP/1.1 404 Not Found"));
      assertThat(rr.responseCode, is(404));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "test_relay_request_text.txt",
          "test_relay_request_identity_item_none.json",
          "test_relay_request_html.html",
        })
    public void returnsTextFile(String filename) throws IOException {
      var rr = makeFileRequest(filename);
      assertThat(rr.statusLine, is("HTTP/1.1 200 OK"));
      assertThat(rr.responseCode, is(200));
      assertThat(rr.responseText.trim(), is(html("request/" + filename).trim()));
    }

    @Test
    public void returnsPng() throws IOException {
      var rr = makeFileRequest("test_relay_request_sample.png");
      assertThat(rr.statusLine, is("HTTP/1.1 200 OK"));
      assertThat(rr.responseCode, is(200));
      assertThat(rr.rawByteBuffer, is(bytes("request/test_relay_request_sample.png")));

      // Not testing value here, which would require it to complete the same second.
      assertThat(rr.getHeaderField("Last-Modified"), not(emptyOrNullString()));
      assertThat(rr.getHeaderField("Expires"), not(emptyOrNullString()));
    }
  }

  @Nested
  class JsonApi {
    private RelayRequest makeApiRequest(String bodyString) {
      var rr = new RelayRequest(false);
      rr.constructURLString("KoLmafia/jsonApi", true);
      rr.addFormField("pwd", GenericRequest.passwordHash);
      rr.addFormField("body", bodyString);
      rr.run();
      return rr;
    }

    @BeforeAll
    public static void beforeAll() {
      Preferences.reset("RelayRequestTest.ApiRequest");
      AdventureSpentDatabase.resetTurns();
      AdventureQueueDatabase.resetQueue();
    }

    @Test
    public void returnsProperties() {
      var cleanups = withProperty("kingLiberated", true);
      try (cleanups) {
        var rr =
            this.makeApiRequest("""
          { "properties": ["kingLiberated"] }
          """);

        JSONObject expected =
            JSON.parseObject("""
          { "properties": ["true"] }
          """);
        assertThat(rr.statusLine, is("HTTP/1.1 200 OK"));
        assertThat(rr.responseCode, is(200));
        assertThat(JSON.parse(rr.responseText), is(expected));
      }
    }

    @Test
    public void returnsMultipleProperties() {
      var cleanups =
          new Cleanups(
              withProperty("kingLiberated", true), withProperty("lastKingLiberation", 1000));
      try (cleanups) {
        var rr =
            this.makeApiRequest(
                """
          { "properties": ["kingLiberated", "lastKingLiberation"] }
          """);

        JSONObject expected =
            JSON.parseObject("""
          { "properties": ["true", "1000"] }
          """);
        assertThat(rr.statusLine, is("HTTP/1.1 200 OK"));
        assertThat(rr.responseCode, is(200));
        assertThat(JSON.parse(rr.responseText), is(expected));
      }
    }

    @Test
    public void returnsFunctions() {
      var cleanups = withTurnsPlayed(22);
      try (cleanups) {
        var rr =
            this.makeApiRequest(
                """
          { "functions": [{ "name": "totalTurnsPlayed", "args": [] }] }
          """);

        JSONObject expected = JSON.parseObject("""
          { "functions": [22] }
          """);
        assertThat(rr.statusLine, is("HTTP/1.1 200 OK"));
        assertThat(rr.responseCode, is(200));
        assertThat(JSON.parse(rr.responseText), is(expected));
      }
    }

    @Test
    public void returnsMultipleFunctions() {
      var cleanups = new Cleanups(withTurnsPlayed(22), withMeat(1000));
      try (cleanups) {
        var rr =
            this.makeApiRequest(
                """
      { "functions": [
        { "name": "totalTurnsPlayed", "args": [] },
        { "name": "myMeat", "args": [] }
      ] }
      """);

        JSONObject expected =
            JSON.parseObject("""
          { "functions": [22, 1000] }
          """);
        assertThat(rr.statusLine, is("HTTP/1.1 200 OK"));
        assertThat(rr.responseCode, is(200));
        assertThat(JSON.parse(rr.responseText), is(expected));
      }
    }

    @ParameterizedTest
    @CsvSource({"truncate,3.1,3", "urlDecode,\"%40\",\"@\""})
    public void handlesPrimitiveArgument(String function, String input, String output) {
      var rr =
          this.makeApiRequest(
              """
  { "functions": [
    { "name": "%s", "args": [%s] },
  ] }
  """
                  .formatted(function, input));

      JSONObject expected =
          JSON.parseObject("""
          { "functions": [%s] }
          """.formatted(output));
      assertThat(rr.statusLine, is("HTTP/1.1 200 OK"));
      assertThat(rr.responseCode, is(200));
      assertThat(JSON.parse(rr.responseText), is(expected));
    }

    @Test
    public void handlesEnumeratedTypes() {
      var cleanups = withItem(ItemPool.SEAL_CLUB);
      try (cleanups) {
        var rr =
            this.makeApiRequest(
                """
      { "functions": [{ "name": "availableAmount", "args": [{
        "objectType": "Item",
        "identifierString": "seal-clubbing club"
      }] }] }
      """);

        JSONObject expected = JSON.parseObject("""
      { "functions": [1] }
      """);
        assertThat(rr.statusLine, is("HTTP/1.1 200 OK"));
        assertThat(rr.responseCode, is(200));
        assertThat(JSON.parse(rr.responseText), is(expected));
      }
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          """
      {
        "objectType": "Class",
        "identifierString": "Seal Clubber"
      }
      """,
          """
      {
        "objectType": "Class",
        "identifierNumber": 1
      }
      """,
          """
      {
        "objectType": "Class",
        "identifierString": "Turtle Tamer",
        "identifierNumber": 1
      }
      """
        })
    public void handlesIdentity(String json) {
      var cleanups = withItem(ItemPool.SEAL_CLUB);
      try (cleanups) {
        var rr =
            this.makeApiRequest(
                """
      { "functions": [{ "name": "identity", "args": [%s] }] }
      """
                    .formatted(json));

        JSONObject expected =
            JSON.parseObject(
                """
      { "functions": [{
        "objectType": "Class",
        "identifierString": "Seal Clubber",
        "identifierNumber": 1,
        "id": 1,
        "primestat": {
          "objectType": "Stat",
          "identifierString": "Muscle"
        },
        "path": {
          "objectType": "Path",
          "identifierString": "none",
          "identifierNumber": -1
        }
      }] }
      """);
        assertThat(rr.statusLine, is("HTTP/1.1 200 OK"));
        assertThat(rr.responseCode, is(200));
        assertThat(JSON.parse(rr.responseText), is(expected));
      }
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          """
      {
        "a": "b"
      }
            """,
          "\"a\"",
          "2",
          """
      {
        "identifierString": "Seal Clubber"
      }
      """,
          """
      {
        "objectType": "class",
        "identifierString": "Seal Clubber"
      }
      """,
          """
      {
        "objectType": "cLASS",
        "identifierString": "Seal Clubber"
      }
      """
        })
    public void ignoresPartialIdentityOrOtherObject(String json) {
      var rr =
          this.makeApiRequest(
              """
{ "functions": [{ "name": "identity", "args": [%s] }] }
""".formatted(json));

      JSONObject expected = JSON.parseObject("""
{ "functions": [%s] }
""".formatted(json));
      assertThat(rr.statusLine, is("HTTP/1.1 200 OK"));
      assertThat(rr.responseCode, is(200));
      assertThat(JSON.parse(rr.responseText), is(expected));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          """
    {
      "objectType": "Class",
      "identifierString": "xxxxxxx"
    }
    """,
          """
  {
    "objectType": "Class",
    "identifierString": null
  }
  """,
          """
  {
    "objectType": "Class",
    "identifierNumber": 9999999
  }
  """,
          """
    {
      "objectType": "Class"
    }
    """
        })
    public void errorsOnInvalidIdentifiedObject(String json) {
      var rr =
          this.makeApiRequest(
              """
{ "functions": [{ "name": "identity", "args": [%s] }] }
""".formatted(json));

      JSONObject expected = new JSONObject();
      expected.put(
          "error", "Invalid arguments to identity: [" + JSON.toJSONString(JSON.parse(json)) + "]");
      assertThat(rr.statusLine, is("HTTP/1.1 400 Bad Request"));
      assertThat(rr.responseCode, is(400));
      assertThat(JSON.parse(rr.responseText), is(expected));
    }

    @ParameterizedTest
    @CsvSource({
      "Item,none,test_relay_request_identity_item_none.json",
      "Location,The Haunted Kitchen,test_relay_request_identity_with_loops_location.json",
      "Item,backup camera,test_relay_request_identity_with_loops_item.json"
    })
    public void handlesIdentityWithLoops(String type, String identifier, String expectedFile) {
      var cleanups = withAdventuresSpent(AdventurePool.HAUNTED_KITCHEN, 5);
      try (cleanups) {
        var rr =
            this.makeApiRequest(
                """
              { "functions": [{ "name": "identity", "args": [{
                "objectType": "%s",
                "identifierString": "%s"
              }] }] }
              """
                    .formatted(type, identifier));

        JSONObject expected = JSON.parseObject(html("request/" + expectedFile));
        assertThat(rr.statusLine, is("HTTP/1.1 200 OK"));
        assertThat(rr.responseCode, is(200));
        assertThat(JSON.parse(rr.responseText), is(expected));
      }
    }

    @Test
    public void handlesPOJOArgument() {
      var rr =
          this.makeApiRequest(
              """
    { "functions": [{ "name": "count", "args": [{
      "a": "z",
      "b": 2
    }] }] }
    """);

      JSONObject expected = JSON.parseObject("""
       { "functions": [2] }
        """);
      assertThat(rr.statusLine, is("HTTP/1.1 200 OK"));
      assertThat(rr.responseCode, is(200));
      assertThat(JSON.parse(rr.responseText), is(expected));
    }

    @Test
    public void handlesPOJOResult() {
      var rr =
          this.makeApiRequest(
              """
        { "functions": [{ "name": "itemDrops", "args": [{
          "objectType": "Monster",
          "identifierString": "Astronomer"
        }] }] }
        """);

      JSONObject expected =
          JSON.parseObject("""
       { "functions": [{ "star chart": 100.0 }] }
        """);
      assertThat(rr.statusLine, is("HTTP/1.1 200 OK"));
      assertThat(rr.responseCode, is(200));
      assertThat(JSON.parse(rr.responseText), is(expected));
    }

    @ParameterizedTest
    @CsvSource({
      "Element,spooky",
      "Monster,Lord Spookyraven",
    })
    public void handlesOverloadedFunction(String type, String identifier) {
      var cleanups = withEquipped(ItemPool.CURSED_MONKEY_PAW);
      try (cleanups) {
        var rr =
            this.makeApiRequest(
                """
              { "functions": [{ "name": "elementalResistance", "args": [{
                "objectType": "%s",
                "identifierString": "%s"
              }] }] }
              """
                    .formatted(type, identifier));

        JSONObject expected = JSON.parseObject("""
          { "functions": [20.0] }
          """);
        assertThat(rr.statusLine, is("HTTP/1.1 200 OK"));
        assertThat(rr.responseCode, is(200));
        assertThat(JSON.parse(rr.responseText), is(expected));
      }
    }

    @ParameterizedTest
    @CsvSource({
      "Bounty,none,",
      "Bounty,bean-shaped rock,",
      "Class,none,-1",
      "Class,Seal Clubber,1",
      "Coinmaster,none,",
      "Coinmaster,The Black Market,",
      "Effect,none,-1",
      "Effect,Confused,3",
      "Element,none,",
      "Element,cold,",
      "Familiar,none,-1",
      "Familiar,Mosquito,1",
      "Item,none,-1",
      "Item,seal-clubbing club,1",
      "Location,none,0",
      "Location,The Sleazy Back Alley,112",
      "Modifier,none,",
      "Modifier,Item Drop,",
      "Monster,none,0",
      "Monster,spooky vampire,1",
      "Path,none,-1",
      "Path,Standard,22",
      "Phylum,none,",
      "Phylum,construct,",
      "Servant,none,",
      "Servant,Assassin,",
      "Skill,none,-1",
      "Skill,Liver of Steel,1",
      "Slot,none,",
      "Slot,hat,",
      "Stat,none,",
      "Stat,Muscle,",
      "Thrall,none,",
      "Thrall,Lasagmbie,",
      "Vykea,none,",
      "Vykea,level 1 couch,",
    })
    public void identityReturnsSameIdentifier(
        String type, String identifierString, Integer identifierNumber) {
      var rr =
          this.makeApiRequest(
              """
        { "functions": [{ "name": "identity", "args": [{
          "objectType": "%s",
          "identifierString": "%s"
        }] }] }
        """
                  .formatted(type, identifierString));
      assertThat(rr.statusLine, is("HTTP/1.1 200 OK"));
      assertThat(rr.responseCode, is(200));
      JSONObject allResults = JSON.parseObject(rr.responseText);
      assertThat(allResults.get("error"), nullValue());
      JSONObject result = allResults.getJSONArray("functions").getJSONObject(0);
      assertThat(result.get("objectType"), is(type));
      assertThat(result.get("identifierString"), is(identifierString));

      if (identifierNumber != null) {
        assertThat(result.get("identifierNumber"), is(identifierNumber));

        if (!identifierString.equals("none")) {
          rr =
              this.makeApiRequest(
                  """
                { "functions": [{ "name": "identity", "args": [{
                  "objectType": "%s",
                  "identifierNumber": %d
                }] }] }
                """
                      .formatted(type, identifierNumber));
          assertThat(rr.statusLine, is("HTTP/1.1 200 OK"));
          assertThat(rr.responseCode, is(200));
          allResults = JSON.parseObject(rr.responseText);
          assertThat(allResults.get("error"), nullValue());
          result = allResults.getJSONArray("functions").getJSONObject(0);
          assertThat(result.get("objectType"), is(type));
          assertThat(result.get("identifierString"), is(identifierString));
          assertThat(result.get("identifierNumber"), is(identifierNumber));
        }
      }
    }

    @Test
    public void testInvalidJson() {
      var rr = this.makeApiRequest("{ invalid_json }");

      JSONObject expected =
          JSON.parseObject("""
      { "error": "Invalid JSON object in request." }
      """);
      assertThat(rr.statusLine, is("HTTP/1.1 400 Bad Request"));
      assertThat(rr.responseCode, is(400));
      assertThat(JSON.parse(rr.responseText), is(expected));
    }

    @Test
    public void testInvalidPropertyNames() {
      var rr = this.makeApiRequest("""
      { "properties": [123, true] }
      """);

      JSONObject expected =
          JSON.parseObject("""
      { "error": "Invalid property names [123,true]" }
      """);
      assertThat(rr.statusLine, is("HTTP/1.1 400 Bad Request"));
      assertThat(rr.responseCode, is(400));
      assertThat(JSON.parse(rr.responseText), is(expected));
    }

    @Test
    public void testInvalidFunctionCallNoFunctionName() {
      var rr = this.makeApiRequest("""
      { "functions": [{ "args": [] }] }
      """);

      JSONObject expected =
          JSON.parseObject(
              """
      { "error": "Invalid function calls [{\\"args\\":[]}]"}
      """);
      assertThat(rr.statusLine, is("HTTP/1.1 400 Bad Request"));
      assertThat(rr.responseCode, is(400));
      assertThat(JSON.parse(rr.responseText), is(expected));
    }

    @Test
    public void testInvalidFunctionCallNoFunctionArgs() {
      var rr = this.makeApiRequest("""
      { "functions": [{ "name": "abc" }] }
      """);

      JSONObject expected =
          JSON.parseObject(
              """
      { "error": "Invalid function calls [{\\"name\\":\\"abc\\"}]"}
      """);
      assertThat(rr.statusLine, is("HTTP/1.1 400 Bad Request"));
      assertThat(rr.responseCode, is(400));
      assertThat(JSON.parse(rr.responseText), is(expected));
    }

    @Test
    public void testInvalidFunctionCallInvalidArgs() {
      var rr =
          this.makeApiRequest(
              """
      { "functions": [{ "name": "myTurncount", "args": [null] }] }
      """);

      JSONObject expected =
          JSON.parseObject(
              """
      { "error": "Invalid function calls [{\\"name\\":\\"myTurncount\\",\\"args\\":[null]}]" }
      """);
      assertThat(rr.statusLine, is("HTTP/1.1 400 Bad Request"));
      assertThat(rr.responseCode, is(400));
      assertThat(JSON.parse(rr.responseText), is(expected));
    }

    @Test
    public void testFunctionNotFound() {
      var rr =
          this.makeApiRequest(
              """
      { "functions": [{ "name": "nonExistentFunction", "args": [] }] }
      """);

      JSONObject expected =
          JSON.parseObject(
              """
      { "error": "Unable to find method nonExistentFunction" }
      """);
      assertThat(rr.statusLine, is("HTTP/1.1 400 Bad Request"));
      assertThat(rr.responseCode, is(400));
      assertThat(JSON.parse(rr.responseText), is(expected));
    }

    @Test
    public void testInvalidFunctionCallWrongArgTypes() {
      var rr =
          this.makeApiRequest(
              """
      { "functions": [{ "name": "itemDropsArray", "args": [{"objectType": "monster", "identifierString": "fluffy bunny"}] }] }
      """);

      JSONObject expected =
          JSON.parseObject(
              """
      { "error": "Unable to call method: java.lang.ClassCastException: class java.util.TreeMap cannot be cast to class net.sourceforge.kolmafia.MonsterData (java.util.TreeMap is in module java.base of loader 'bootstrap'; net.sourceforge.kolmafia.MonsterData is in unnamed module of loader 'app')" }
      """);
      assertThat(rr.statusLine, is("HTTP/1.1 400 Bad Request"));
      assertThat(rr.responseCode, is(400));
      assertThat(JSON.parse(rr.responseText), is(expected));
    }
  }
}
