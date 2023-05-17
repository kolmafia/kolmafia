package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Player.withDaycount;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withoutItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.session.ChoiceManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class ChibiBuddyCommandTest extends AbstractCommandTestBase {
  @BeforeEach
  public void initEach() {
    HttpClientWrapper.setupFakeClient();
  }

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("ChibiBuddyCommandTest");
    ChoiceManager.handlingChoice = false;
    FightRequest.currentRound = 0;
  }

  public ChibiBuddyCommandTest() {
    this.command = "chibi";
  }

  @Test
  void noStateIfNoBuddy() {
    var cleanups =
        new Cleanups(withoutItem(ItemPool.CHIBIBUDDY_OFF), withoutItem(ItemPool.CHIBIBUDDY_ON));
    try (cleanups) {
      String output = execute("");

      assertThat(output, containsString("You don't own a ChibiBuddy&trade;"));
      assertErrorState();
    }
  }

  @Test
  void canShowStateIfOff() {
    var cleanups =
        new Cleanups(withItem(ItemPool.CHIBIBUDDY_OFF), withoutItem(ItemPool.CHIBIBUDDY_ON));
    try (cleanups) {
      String output = execute("");

      assertThat(output, containsString("Your ChibiBuddy&trade; is currently powered off"));
      assertContinueState();
    }
  }

  @ParameterizedTest
  @CsvSource({"0, earlier today", "1, yesterday", "2, 2 days ago"})
  void canShowState(final int daysAgo, final String daysAgoString) {
    var daycount = 10;

    var cleanups =
        new Cleanups(
            withItem(ItemPool.CHIBIBUDDY_ON),
            withDaycount(daycount),
            withProperty("chibiName", "Ronald"),
            withProperty("chibiBirthday", 8),
            withProperty("chibiLastVisit", daycount - daysAgo),
            withProperty("chibiAlignment", 3),
            withProperty("chibiFitness", 4),
            withProperty("chibiIntelligence", 5),
            withProperty("chibiSocialization", 6));

    try (cleanups) {
      String output = execute("");

      assertThat(
          output,
          containsString(
              "Your ChibiBuddy&trade; <b>Ronald</b> is 3 days old and you last visited them "
                  + daysAgoString
                  + "."));
      assertThat(
          output,
          containsString(
              "<tr><td>Alignment</td><td>"
                  + ChibiBuddyCommand.DOT_SYMBOL.repeat(3)
                  + "</td><td>[3/10]</td></tr>"));
      assertThat(
          output,
          containsString(
              "<tr><td>Fitness</td><td>"
                  + ChibiBuddyCommand.DOT_SYMBOL.repeat(4)
                  + "</td><td>[4/10]</td></tr>"));
      assertThat(
          output,
          containsString(
              "<tr><td>Intelligence</td><td>"
                  + ChibiBuddyCommand.DOT_SYMBOL.repeat(5)
                  + "</td><td>[5/10]</td></tr>"));
      assertThat(
          output,
          containsString(
              "<tr><td>Socialization</td><td>"
                  + ChibiBuddyCommand.DOT_SYMBOL.repeat(6)
                  + "</td><td>[6/10]</td></tr>"));
      assertContinueState();
    }
  }

  @Test
  void canHaveChibiChatIfNotChanged() {
    var cleanups =
        new Cleanups(withProperty("_chibiChanged", false), withItem(ItemPool.CHIBIBUDDY_ON));
    try (cleanups) {
      var output = execute("chat");
      assertThat(
          output, not(containsString("You've already chatted with your ChibiBuddy&trade; today")));
    }
  }

  @Test
  void cannotHaveChibiChatIfChanged() {
    var cleanups =
        new Cleanups(withProperty("_chibiChanged", true), withItem(ItemPool.CHIBIBUDDY_ON));
    try (cleanups) {
      var output = execute("chat");
      assertThat(
          output, containsString("You've already chatted with your ChibiBuddy&trade; today"));
    }
  }
}
