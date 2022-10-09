package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Networking.printRequests;
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
import static org.junit.jupiter.api.Assertions.assertFalse;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.RelayRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class VolcanoMazeManagerTest {

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("volcano maze");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("volcano maze");
  }

  @Nested
  class Automation {
    @AfterEach
    public void afterEach() {
      VolcanoMazeManager.reset();
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

    public void addVolcanoMazeResponses(FakeHttpClientBuilder builder) {
      var client = builder.client;
    }

    public int validateVolcanoMazeRequests(FakeHttpClientBuilder builder, int i) {
      var client = builder.client;
      var requests = client.getRequests();
      return i;
    }

    @Test
    public void canAutomateVolcanoMazeFromRelayBrowser() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withVolcanoMaze(builder),
              withCurrentRun(800),
              withGender(KoLCharacter.FEMALE),
              withClass(AscensionClass.ACCORDION_THIEF),
              withEquipped(EquipmentManager.WEAPON, ItemPool.SQUEEZEBOX_OF_THE_AGES),
              withHP(500, 500, 500));
      try (cleanups) {
        client.addResponse(200, html("request/test_volcano_intro.html"));
        client.addResponse(200, html("request/test_volcano_start.html"));
        addVolcanoMazeResponses(builder);

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
        assertFalse(Preferences.getString("volcanoMaze1").equals(""));

        /*
               // Simulate user typing "solve" button
               url = "/KoLmafia/redirectedCommand?cmd=volcano+solve&pwd=volcano";
               request = new RelayRequest(false);
               request.constructURLString(url);
               request.run();

               // Wait until the submitted command is done
               request.waitForCommandCompletion();
               assertThat(RelayRequest.specialCommandResponse.length(), greaterThan(0));

        // Verify maze is solved, etc, etc.

        */

        // Verify that expected requests were submitted
        var requests = client.getRequests();
        printRequests(requests);
        assertThat(requests, hasSize(2));

        int i = 0;
        assertPostRequest(requests.get(i++), "/volcanoisland.php", "action=tniat&pwd=volcano");
        assertGetRequest(requests.get(i++), "/volcanomaze.php", "start=1");
        validateVolcanoMazeRequests(builder, i);
      }
    }
  }
}
