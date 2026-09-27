package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withHttpClientBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PantogramRequestTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("pantsUser");
  }

  @BeforeEach
  protected void beforeEach() {
    Preferences.reset("pantsUser");
  }

  @Test
  void initialModifierMatchesParsedModifier() {
    // +Mus, Spooky res, porquoise, bubblin' crude, ten-leaf clover
    String initUrlString =
        "choice.php?whichchoice=1270&pwd&option=1&m=1&e=3&s1=5789%2C1&s2=706%2C1&s3=24%2C1";
    String initResponseText = html("request/test_pantogram_pants_acquisition.html");
    PantogramRequest.parseResponse(initUrlString, initResponseText);
    String initPantoModifier = Preferences.getString("_pantogramModifier");

    Preferences.setString("_pantogramModifier", "");
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups = new Cleanups(withHttpClientBuilder(builder));
    client.addResponse(200, html("request/test_pantogram_pants_item_desc.html"));

    try (cleanups) {
      InventoryManager.checkPantogram();
      String parsedPantoModifier = Preferences.getString("_pantogramModifier");

      // Order of modifiers could be different, so do a weird check
      Set<String> initMods = new HashSet<String>(Arrays.asList(initPantoModifier.split(",")));
      Set<String> parsedMods = new HashSet<String>(Arrays.asList(parsedPantoModifier.split(",")));
      assertEquals(initMods, parsedMods);
    }
  }
}
