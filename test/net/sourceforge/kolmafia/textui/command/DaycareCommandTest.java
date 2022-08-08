package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class DaycareCommandTest extends AbstractCommandTestBase {
  @BeforeAll
  public static void init() {
    KoLCharacter.reset("testUser");
    KoLCharacter.reset(true);
    Preferences.reset("testUser");
  }

  @BeforeEach
  public void initializeState() {
    HttpClientWrapper.setupFakeClient();
    StaticEntity.setContinuationState(MafiaState.CONTINUE);
  }

  public DaycareCommandTest() {
    this.command = "daycare";
  }

  @Test
  void mustHaveDaycare() {
    String output = execute("item");

    assertContinueState();
    assertThat(output, containsString("You need a boxing daycare first"));
  }

  @Test
  void mustGiveRealCommand() {
    String output;
    var cleanups =
        new Cleanups(withProperty("_daycareToday", true), withProperty("_daycareNap", true));

    try (cleanups) {
      output = execute("flargle");
    }

    assertContinueState();
    assertThat(output, containsString("Choice not recognised"));
  }

  @Test
  void itemIsOncePerDay() {
    String output;
    var cleanups =
        new Cleanups(withProperty("daycareOpen", true), withProperty("_daycareNap", true));

    try (cleanups) {
      output = execute("item");
    }

    assertContinueState();
    assertThat(output, containsString("You have already had a Boxing Daydream today"));

    assertThat(getRequests(), empty());
  }

  @Test
  void canGetItem() {
    var cleanups =
        new Cleanups(withProperty("daycareOpen", true), withProperty("_daycareNap", false));

    try (cleanups) {
      execute("item");
    }

    assertContinueState();
    var requests = getRequests();

    assertThat(requests, hasSize(2));
    assertPostRequest(
        requests.get(0), "/place.php", "whichplace=town_wrong&action=townwrong_boxingdaycare");
    assertPostRequest(requests.get(1), "/choice.php", "whichchoice=1334&option=1");
  }

  @Test
  void wontFreeScavengeIfNotFree() {
    String output;
    var cleanups =
        new Cleanups(withProperty("daycareOpen", true), withProperty("_daycareGymScavenges", 1));

    try (cleanups) {
      output = execute("scavenge free");
    }

    assertThat(
        output, containsString("You have already used your free scavenge for gym equipment today"));

    assertContinueState();
    assertThat(getRequests(), empty());
  }

  @Test
  void willFreeScavengeIfFree() {
    var cleanups =
        new Cleanups(withProperty("daycareOpen", true), withProperty("_daycareGymScavenges", 0));

    try (cleanups) {
      execute("scavenge free");
    }

    assertContinueState();
    var requests = getRequests();
    assertThat(requests, hasSize(5));
    assertPostRequest(
        requests.get(0), "/place.php", "whichplace=town_wrong&action=townwrong_boxingdaycare");
    assertPostRequest(requests.get(1), "/choice.php", "whichchoice=1334&option=3");
    assertPostRequest(requests.get(2), "/choice.php", "whichchoice=1336&option=2");
    assertPostRequest(requests.get(3), "/choice.php", "whichchoice=1336&option=5");
    assertPostRequest(requests.get(4), "/choice.php", "whichchoice=1334&option=4");
  }

  @Test
  void willScavengeIfNotFree() {
    var cleanups =
        new Cleanups(withProperty("daycareOpen", true), withProperty("_daycareGymScavenges", 3));

    try (cleanups) {
      execute("scavenge");
    }

    assertContinueState();
    var requests = getRequests();
    assertThat(requests, hasSize(5));
    assertPostRequest(
        requests.get(0), "/place.php", "whichplace=town_wrong&action=townwrong_boxingdaycare");
    assertPostRequest(requests.get(1), "/choice.php", "whichchoice=1334&option=3");
    assertPostRequest(requests.get(2), "/choice.php", "whichchoice=1336&option=2");
    assertPostRequest(requests.get(3), "/choice.php", "whichchoice=1336&option=5");
    assertPostRequest(requests.get(4), "/choice.php", "whichchoice=1334&option=4");
  }

  @Test
  void statIsOncePerDay() {
    String output;
    var cleanups =
        new Cleanups(withProperty("daycareOpen", true), withProperty("_daycareSpa", true));

    try (cleanups) {
      output = execute("mus");
    }

    assertThat(output, containsString("You have already visited the Boxing Day Spa today"));

    assertContinueState();
    assertThat(getRequests(), empty());
  }

  @ParameterizedTest
  @CsvSource(textBlock = """
      mus, 1
      mys, 3
      mox, 2
      regen, 4
      """)
  public void getStatBuff(String buff, Integer choice) {
    var cleanups =
        new Cleanups(withProperty("daycareOpen", true), withProperty("_daycareSpa", false));

    try (cleanups) {
      execute(buff);
    }

    assertContinueState();
    var requests = getRequests();
    assertThat(requests, hasSize(3));
    assertPostRequest(
        requests.get(0), "/place.php", "whichplace=town_wrong&action=townwrong_boxingdaycare");
    assertPostRequest(requests.get(1), "/choice.php", "whichchoice=1334&option=2");
    assertPostRequest(requests.get(2), "/choice.php", "whichchoice=1335&option=" + choice);
  }
}
