package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Player.withProperty;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.persistence.FaxBotDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ChoiceManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    // Configure
    FaxBotDatabase.configure();
  }

  @BeforeEach
  public void initEach() {
    HttpClientWrapper.setupFakeClient();
    ChoiceManager.handlingChoice = false;
  }

  @Test
  void doesntErrorUnknownFaxbot() {
    var cleanups = new Cleanups(withProperty("preferredFaxbot", "$FaxBot$"));

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

  @ParameterizedTest
  @ValueSource(strings = {"CheeseFax", "OnlyFax", "Easyfax"})
  void usesPreferedFaxbot(String preferredFaxbot) {
    var cleanups = new Cleanups(withProperty("preferredFaxbot", preferredFaxbot));

    try (cleanups) {
      // Start the process of faxing in a Knob Goblin Embezzler
      execute("embezzler");

      var requests = getRequests();

      // Assert that the first faxbot we try, is the faxbot that we prefer
      assertGetRequest(
          requests.get(0), "/submitnewchat.php", "pwd=&playerid=0&graf=/whois+" + preferredFaxbot);
    }
  }
}
