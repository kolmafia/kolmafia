package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CampAwayRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.session.ChoiceManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class CampAwayCommandTest extends AbstractCommandTestBase {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("CampAwayCommandTest");
    Preferences.reset("CampAwayCommandTest");
    ChoiceManager.handlingChoice = false;
    FightRequest.currentRound = 0;
  }

  public CampAwayCommandTest() {
    this.command = "campaway";
  }

  private Cleanups withCampsite(int cloudBuffs, int smileBuffs) {
    return new Cleanups(
        withProperty("getawayCampsiteUnlocked", true),
        withProperty("_campAwayCloudBuffs", cloudBuffs),
        withProperty("_campAwaySmileBuffs", smileBuffs));
  }

  @Test
  void mustUseValidCommand() {
    var cleanups = withCampsite(0, 0);

    try (cleanups) {
      String output = execute("test");
      assertErrorState();
      assertThat(output.trim(), is("Campaway command not recognized"));
    }
  }

  @Test
  void requiresCampsite() {
    String output = execute("cloud");
    assertErrorState();
    assertThat(output, containsString("You need a Getaway Campsite"));
  }

  @ParameterizedTest
  @CsvSource({
    "1,0,cloud,Already got a cloud buff today",
    "0,3,smile,Already used all smile buffs today"
  })
  void failsIfUsed(int cloudBuffs, int smileBuffs, String command, String error) {
    var cleanups = withCampsite(cloudBuffs, smileBuffs);

    try (cleanups) {
      String output = execute(command);
      assertErrorState();
      assertThat(output, containsString(error));
    }
  }

  @Test
  void makesRequests() {
    var builder = new FakeHttpClientBuilder();
    var cleanups = new Cleanups(withHttpClientBuilder(builder), withCampsite(0, 0));

    try (cleanups) {
      String output = execute("cloud");
      var requests = builder.client.getRequests();

      assertContinueState();
      assertThat(output.trim(), is("Gazing at the Stars"));

      assertThat(requests, hasSize(1));

      assertPostRequest(
          requests.getFirst(),
          "/place.php",
          both(containsString("whichplace=campaway"))
              .and(containsString("action=" + CampAwayRequest.SKY)));
    }
  }
}
