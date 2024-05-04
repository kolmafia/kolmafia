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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

  @Test
  void mustUseValidCommand() {
    var cleanups = new Cleanups(withCalendar(), withProperty("_mayamSymbolsUsed", ""));

    try (cleanups) {
      String output = execute("wish yam yam yam yam");
      assertErrorState();
      assertThat(output, containsString("Mayam command not recognised"));
    }
  }

  @Nested
  class Rings {
    @BeforeEach()
    void beforeEach() {
      HttpClientWrapper.setupFakeClient();
    }

    @Test
    void requiresCalendar() {
      String output = execute("rings yam yam yam yam");
      assertErrorState();
      assertThat(output, containsString("You need a Mayam Calendar"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"yam yam yam", "yam yam yam yam yam"})
    void mustProvideExactlyFourSymbols(final String symbols) {
      var cleanups = new Cleanups(withCalendar(), withProperty("_mayamSymbolsUsed", ""));

      try (cleanups) {
        String output = execute("rings " + symbols);
        assertErrorState();
        assertThat(output, containsString("You must supply exactly four symbols."));
      }
    }

    @Test
    void cannotReuseSymbol() {
      var cleanups =
          new Cleanups(
              withCalendar(), withProperty("_mayamSymbolsUsed", "sword,lightning,eyepatch,clock"));

      try (cleanups) {
        String output = execute("rings sword yam yam yam");
        assertErrorState();
        assertThat(output, containsString("You've already used the sword symbol."));
      }
    }

    @Test
    void cannotReuseYamSymbol() {
      var cleanups =
          new Cleanups(
              withCalendar(), withProperty("_mayamSymbolsUsed", "yam1,lightning,eyepatch,clock"));

      try (cleanups) {
        String output = execute("rings yam yam yam yam");
        assertErrorState();
        assertThat(output, containsString("You've already used the yam symbol in position 1."));
      }
    }

    @Test
    void symbolMustExist() {
      var cleanups = new Cleanups(withCalendar(), withProperty("_mayamSymbolsUsed", ""));

      try (cleanups) {
        String output = execute("rings zippy yam yam yam");
        assertErrorState();
        assertThat(output, containsString("Cannot match symbol zippy on ring 1."));
      }
    }

    @Test
    void symbolMustExistOnRing() {
      var cleanups = new Cleanups(withCalendar(), withProperty("_mayamSymbolsUsed", ""));

      try (cleanups) {
        String output = execute("rings clock yam yam yam");
        assertErrorState();
        assertThat(output, containsString("Cannot match symbol clock on ring 1."));
      }
    }

    @Test
    void considersTheCalendar() {
      var cleanups = withCalendar();

      try (cleanups) {
        String output = execute("rings eye yam eyepatch yam");
        var requests = getRequests();

        assertContinueState();
        assertThat(output, equalTo("Calendar considered.\n"));

        assertThat(requests, hasSize(6));

        assertPostRequest(
            requests.get(0),
            "/inv_use.php",
            containsString("whichitem=" + ItemPool.MAYAM_CALENDAR));
        assertPostRequest(requests.get(1), "/choice.php", containsString("r=3&p=5"));
        assertPostRequest(requests.get(2), "/choice.php", containsString("r=2&p=0"));
        assertPostRequest(requests.get(3), "/choice.php", containsString("r=1&p=1"));
        assertPostRequest(requests.get(4), "/choice.php", containsString("r=0&p=0"));
        assertPostRequest(requests.get(5), "/choice.php", containsString("option=1"));
      }
    }
  }

  @Nested
  class Resonances {
    @Test
    void failsWithMultipleSubstringMatch() {
      var cleanups = new Cleanups(withCalendar());

      try (cleanups) {
        String output = execute("resonance am cannon");
        assertErrorState();
        assertThat(output, containsString("Too many resonance matches for am cannon."));
      }
    }

    @Test
    void passesWithExactMatch() {
      var cleanups = new Cleanups(withCalendar());

      try (cleanups) {
        String output = execute("resonance yam cannon");

        assertContinueState();
        assertThat(output, equalTo("Calendar considered.\n"));
      }
    }

    @Test
    void passesWithUniqueSubstringMatch() {
      var cleanups = new Cleanups(withCalendar());

      try (cleanups) {
        String output = execute("resonance battery");

        assertContinueState();
        assertThat(output, equalTo("Calendar considered.\n"));
      }
    }
  }
}
