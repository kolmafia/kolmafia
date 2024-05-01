package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.session.ChoiceManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MayamCommandTest extends AbstractCommandTestBase {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("MayamCommandTest");
    Preferences.reset("MayamCommandTest");
    ChoiceManager.handlingChoice = false;
    FightRequest.currentRound = 0;
  }

  public MayamCommandTest() {
    this.command = "mayam";
  }

  private Cleanups withCalendar() {
    return withItem(ItemPool.MAYAM_CALENDAR);
  }

  @BeforeEach()
  void beforeEach() {
    HttpClientWrapper.setupFakeClient();
  }

  @Test
  void requiresCalendar() {
    String output = execute("yam yam yam yam");
    assertThat(output, containsString("You need a Mayam Calendar"));
  }

  @Test
  void cannotReuseSymbol() {
    var cleanups =
        new Cleanups(
            withCalendar(), withProperty("_mayamSymbolsUsed", "sword,lightning,eyepatch,clock"));

    try (cleanups) {
      String output = execute("sword yam yam yam");
      assertThat(output, containsString("You've already used the sword symbol."));
    }
  }

  @Test
  void cannotReuseYamSymbol() {
    var cleanups =
        new Cleanups(
            withCalendar(), withProperty("_mayamSymbolsUsed", "yam1,lightning,eyepatch,clock"));

    try (cleanups) {
      String output = execute("yam yam yam yam");
      assertThat(output, containsString("You've already used the yam symbol in position 1."));
    }
  }

  @Test
  void symbolMustExist() {
    var cleanups = new Cleanups(withCalendar(), withProperty("_mayamSymbolsUsed", ""));

    try (cleanups) {
      String output = execute("zippy yam yam yam");
      assertThat(output, containsString("Cannot match symbol zippy on ring 1."));
    }
  }

  @Test
  void symbolMustExistOnRing() {
    var cleanups = new Cleanups(withCalendar(), withProperty("_mayamSymbolsUsed", ""));

    try (cleanups) {
      String output = execute("clock yam yam yam");
      assertThat(output, containsString("Cannot match symbol clock on ring 1."));
    }
  }

  @Test
  void considersTheCalendar() {
    var cleanups = withCalendar();

    try (cleanups) {
      String output = execute("eye yam eyepatch yam");
      var requests = getRequests();

      assertThat(output, equalTo("Calendar considered.\n"));

      assertThat(requests, hasSize(6));

      assertPostRequest(
          requests.get(0), "/inv_use.php", containsString("whichitem=" + ItemPool.MAYAM_CALENDAR));
      assertPostRequest(requests.get(1), "/choice.php", containsString("r=3&p=5"));
      assertPostRequest(requests.get(2), "/choice.php", containsString("r=2&p=0"));
      assertPostRequest(requests.get(3), "/choice.php", containsString("r=1&p=1"));
      assertPostRequest(requests.get(4), "/choice.php", containsString("r=0&p=0"));
      assertPostRequest(requests.get(5), "/choice.php", containsString("option=1"));
      assertContinueState();
    }
  }
}
