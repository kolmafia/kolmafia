package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withTurnsPlayed;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import java.io.File;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
    public void returnsFunctions() {
      var cleanups = withTurnsPlayed(22);
      try (cleanups) {
        var rr =
            this.makeApiRequest(
                """
          { "functions": [{ "name": "myTurncount", "args": [] }] }
          """);

        JSONObject expected = JSON.parseObject("""
          { "functions": [0] }
          """);
        assertThat(rr.statusLine, is("HTTP/1.1 200 OK"));
        assertThat(rr.responseCode, is(200));
        assertThat(JSON.parse(rr.responseText), is(expected));
      }
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

    @Test
    public void handlesIdentity() {
      var cleanups = withItem(ItemPool.SEAL_CLUB);
      try (cleanups) {
        var rr =
            this.makeApiRequest(
                """
      { "functions": [{ "name": "identity", "args": [{
        "objectType": "Class",
        "identifierString": "Seal Clubber"
      }] }] }
      """);

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
          "identifierNumber": -1,
          "id": 0,
          "name": "none",
          "avatar": false,
          "image": "blank.gif",
          "points": 0,
          "familiars": true
        }
      }] }
      """);
        assertThat(rr.statusLine, is("HTTP/1.1 200 OK"));
        assertThat(rr.responseCode, is(200));
        assertThat(JSON.parse(rr.responseText), is(expected));
      }
    }

    @Test
    public void handlesIdentityWithLoops() {
      var rr =
          this.makeApiRequest(
              """
    { "functions": [{ "name": "identity", "args": [{
      "objectType": "Location",
      "identifierString": "The Haunted Kitchen"
    }] }] }
    """);

      JSONObject expected =
          JSON.parseObject(html("request/test_relay_request_identity_with_loops.json"));
      assertThat(rr.statusLine, is("HTTP/1.1 200 OK"));
      assertThat(rr.responseCode, is(200));
      assertThat(JSON.parse(rr.responseText), is(expected));
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
  }
}
