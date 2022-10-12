package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Networking.json;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withSkill;
import static internal.helpers.Player.withStats;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mockStatic;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.TurnCounter;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class QuantumTerrariumRequestTest {

  @BeforeAll
  public static void beforeAll() {
    // Simulate logging out and back in again.
    KoLCharacter.reset("");
    KoLCharacter.reset("quantum terrarium user");
  }

  private static Cleanups mockApiRequest(ApiRequest request) {
    var mocked = mockStatic(ApiRequest.class, Mockito.CALLS_REAL_METHODS);
    mocked
        .when(() -> ApiRequest.updateStatus(anyBoolean()))
        .thenAnswer(
            invocation -> {
              request.processResults();
              return null;
            });
    return new Cleanups(mocked::close);
  }

  @Test
  void canDetectCurrentAndNextFamiliar() {
    String text = html("request/test_quantum_terrarium_api.json");
    JSONObject JSON = json(text);

    // Quantum Terrarium will call api.php to set up familiar in middle of processing.
    ApiRequest apiRequest = new ApiRequest("status");
    apiRequest.responseText = text;

    // Stats affect Familiar Weight in Quantum Familiar
    int basemuscle = JSON.getInt("basemuscle");
    int basemysticality = JSON.getInt("basemysticality");
    int basemoxie = JSON.getInt("basemoxie");

    var cleanups =
        new Cleanups(
            withPath(Path.QUANTUM),
            withClass(AscensionClass.ACCORDION_THIEF),
            withStats(basemuscle, basemysticality, basemoxie),
            withSkill("Amphibian Sympathy"),
            mockApiRequest(apiRequest));

    try (cleanups) {
      String urlString = "qterrarium.php";
      String responseText = html("request/test_quantum_terrarium_visit.html");
      QuantumTerrariumRequest.parseResponse(urlString, responseText);

      // Current Familiar is Weenabego, KarmaHunter's El Vibrato Megadrone
      FamiliarData current = KoLCharacter.getFamiliar();
      assertEquals("El Vibrato Megadrone", current.getRace());
      assertEquals(81, current.getTotalExperience());
      assertEquals("Weenabego", current.getName());
      assertEquals("KarmaHunter", current.getOwner());
      assertEquals(1270203, current.getOwnerId());

      // Next Familiar in 11 adventures is Grabert, JoeRo1's Synthetic Rock
      assertEquals("Synthetic Rock", Preferences.getString("nextQuantumFamiliar"));
      assertEquals("Grabert", Preferences.getString("nextQuantumFamiliarName"));
      assertEquals("JoeRo1", Preferences.getString("nextQuantumFamiliarOwner"));
      assertEquals(335281, Preferences.getInteger("nextQuantumFamiliarOwnerId"));

      // There should be a counter
      assertEquals(11, TurnCounter.turnsRemaining("Quantum Familiar"));
    }
  }
}
