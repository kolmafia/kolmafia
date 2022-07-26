package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.setProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ChoiceManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class BarrelPrayerCommandTest extends AbstractCommandTestBase {

  public BarrelPrayerCommandTest() {
    this.command = "barrelprayer";
  }

  @BeforeAll
  public static void setup() {
    KoLCharacter.reset("barrel");
    Preferences.reset("barrel");
  }

  @BeforeEach
  public void initializeState() {
    HttpClientWrapper.setupFakeClient();
    StaticEntity.setContinuationState(MafiaState.CONTINUE);
  }

  @Test
  void mustMakeCommand() {
    String output = execute("");

    assertContinueState();
    assertThat(output, containsString("Usage: barrelprayer"));
  }

  @Test
  void mustMakeValidCommand() {
    String output = execute("foobar");

    assertErrorState();
    assertThat(output, containsString("I don't understand what 'foobar' barrel prayer is."));
  }

  @Test
  void mustHaveShrine() {
    var cleanups = setProperty("barrelShrineUnlocked", false);

    try (cleanups) {
      String output = execute("buff");

      assertErrorState();
      assertThat(output, containsString("Barrel Shrine not installed"));
    }
  }

  @Test
  void mustNotHavePrayed() {
    var cleanups =
        new Cleanups(setProperty("barrelShrineUnlocked", true), setProperty("_barrelPrayer", true));

    try (cleanups) {
      String output = execute("buff");

      assertErrorState();
      assertThat(output, containsString("You have already prayed to the Barrel God today"));
    }
  }

  public static Stream<Arguments> barrelItems() {
    return Stream.of(
        Arguments.of("prayedForProtection", "protection"),
        Arguments.of("prayedForGlamour", "glamour"),
        Arguments.of("prayedForVigor", "vigor"));
  }

  @ParameterizedTest
  @MethodSource("barrelItems")
  void mustNotHavePrayedForItemThisAscension(String preference, String prayer) {
    var cleanups =
        new Cleanups(
            setProperty("barrelShrineUnlocked", true),
            setProperty("_barrelPrayer", false),
            setProperty(preference, true));

    try (cleanups) {
      String output = execute(prayer);

      assertErrorState();
      assertThat(output, containsString("You have already prayed for that item this ascension"));
    }
  }

  @Test
  void canPray() {
    var cleanups =
        new Cleanups(
            setProperty("barrelShrineUnlocked", true), setProperty("_barrelPrayer", false));

    try (cleanups) {
      execute("buff");

      assertContinueState();
      var requests = getRequests();
      assertThat(requests, not(empty()));
      assertPostRequest(requests.get(0), "/da.php", "barrelshrine=1");
      assertPostRequest(requests.get(1), "/choice.php", "whichchoice=1100&option=4");
    }

    ChoiceManager.reset();
  }
}
