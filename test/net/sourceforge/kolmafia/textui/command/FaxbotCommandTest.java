package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Player.withDataFile;
import static internal.helpers.Player.withProperty;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.persistence.FaxBotDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ChoiceManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class FaxbotCommandTest extends AbstractCommandTestBase {
  public FaxbotCommandTest() {
    this.command = "faxbot";
  }

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("testUser");
    Preferences.reset("testUser");

    // Set to true so we make http requests
    ChatManager.setChatLiteracy(true);

    // This needs to happen before database configuration.
    // ignoring cleanup

    var ignoredCleanup =
        new Cleanups(
            withDataFile("cheesefax.xml", "cheesefax.xml"),
            withDataFile("easyfax.xml", "easyfax.xml"),
            withDataFile("onlyfax.xml", "onlyfax.xml"));

    // Configure
    FaxBotDatabase.configure();
  }

  @AfterAll
  public static void afterAll() {
    var cleanup =
        new Cleanups(
            withDataFile("cheesefax.xml", "cheesefax.xml"),
            withDataFile("easyfax.xml", "easyfax.xml"),
            withDataFile("onlyfax.xml", "onlyfax.xml"));
    try (cleanup) {
      // This is primarily here to trigger the cleanup
      ChatManager.setChatLiteracy(false);
    }
  }

  @BeforeEach
  public void initEach() {
    HttpClientWrapper.setupFakeClient();
    ChoiceManager.handlingChoice = false;
  }

  @Test
  void doesntErrorUnknownFaxbot() {
    var cleanups = new Cleanups(withProperty("lastSuccessfulFaxbot", "$FaxBot$"));

    try (cleanups) {
      // Start the process of faxing in a Knob Goblin Embezzler
      execute("embezzler");

      var requests = getRequests();

      // Assert that the first faxbot we try is a known faxbot
      assertGetRequest(
          requests.get(0),
          "/submitnewchat.php",
          "pwd=&playerid=0&graf=/whois+" + FaxBotDatabase.getFaxbot(0).getName());
    }
  }

  static Stream<String> provideFaxbotNames() {
    return FaxBotDatabase.faxbots.stream().map(FaxBotDatabase.FaxBot::getName);
  }

  @ParameterizedTest
  @MethodSource("provideFaxbotNames")
  void usesLastSuccessfulFaxbot(String lastFaxbot) {
    var cleanups = new Cleanups(withProperty("lastSuccessfulFaxbot", lastFaxbot));

    try (cleanups) {
      // Start the process of faxing in a Knob Goblin Embezzler
      execute("embezzler");

      var requests = getRequests();

      // Assert that the first faxbot we try, is the faxbot that was last successful
      assertGetRequest(
          requests.get(0), "/submitnewchat.php", "pwd=&playerid=0&graf=/whois+" + lastFaxbot);
    }
  }
}
