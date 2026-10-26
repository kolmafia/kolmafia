package net.sourceforge.kolmafia.session;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import internal.helpers.Networking;
import internal.helpers.Player;
import java.util.List;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class IntriguingPuzzleBoxManagerTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("IntriguingPuzzleBoxManagerTest");
    Preferences.reset("IntriguingPuzzleBoxManagerTest");
  }

  private static Cleanups visitFirstPage() {
    return Player.withChoice(
        (url, req) -> ChoiceControl.visitChoice(req),
        525,
        0,
        null,
        Networking.html("request/test_choice_525_intriguing_puzzle_box.html"));
  }

  @Nested
  class RealPage {
    @Test
    void recordsOptionCodesInOrder() {
      try (var cleanups = visitFirstPage()) {
        assertThat(
            Preferences.getString("intriguingPuzzleBoxHistory"), is("FCW,PB,RCCW,LCW,KCW,PR"));
      }
    }

    @Test
    void narrowsToOneSeed() {
      try (var cleanups = visitFirstPage()) {
        List<Integer> seeds = AscensionSeedManager.possibleAscensionSeeds();
        assertThat(seeds.size(), is(1));
      }
    }
  }

  @ParameterizedTest
  @CsvSource({
    "turn the left face clockwise, LCW",
    "turn the left face counterclockwise, LCCW",
    "turn the right face clockwise, RCW",
    "turn the right face counterclockwise, RCCW",
    "turn the top face clockwise, TCW",
    "turn the top face counterclockwise, TCCW",
    "turn the bottom face clockwise, BCW",
    "turn the bottom face counterclockwise, BCCW",
    "turn the front face clockwise, FCW",
    "turn the front face counterclockwise, FCCW",
    "turn the rear face clockwise, KCW",
    "turn the rear face counterclockwise, KCCW",
    "push the left face, PL",
    "push the right face, PR",
    "push the top face, PT",
    "push the bottom face, PB",
    "push the front face, PF",
    "push the rear face, PK",
  })
  void parsesOptionLines(String option, String code) {
    assertThat(IntriguingPuzzleBoxManager.parseOption(option), is(code));
  }

  @Nested
  class Presses {
    private static final String FIRST = "request/test_choice_525_intriguing_puzzle_box.html";
    private static final String PRESS1 =
        "request/test_choice_525_intriguing_puzzle_box_press1.html";
    private static final String PRESS2 =
        "request/test_choice_525_intriguing_puzzle_box_press2.html";
    private static final String PRESS3 =
        "request/test_choice_525_intriguing_puzzle_box_press3.html";

    private static void visit(final String page, final int decision) {
      try (var cleanups =
          Player.withChoice(
              (url, req) -> ChoiceControl.visitChoice(req),
              525,
              decision,
              null,
              Networking.html(page))) {
        // The manager writes the history while the page is visited.
      }
    }

    private static void visitChain() {
      visit(FIRST, 0);
      visit(PRESS1, 3);
      visit(PRESS2, 1);
      visit(PRESS3, 1);
    }

    @Test
    void pressesAttachToPreviousPages() {
      visitChain();
      assertThat(
          Preferences.getString("intriguingPuzzleBoxHistory"),
          is(
              "FCW,PB,RCCW,LCW,KCW,PR#3|FCW,PB,RCCW,LCW,KCW,PR#1|FCW,TCW,TCCW,PT,FCCW,PR#1|BCW,PB,PF,RCW,BCCW,KCW"));
    }

    @Test
    void firstPageWithoutPressStartsHistory() {
      visit(FIRST, 0);
      assertThat(Preferences.getString("intriguingPuzzleBoxHistory"), is("FCW,PB,RCCW,LCW,KCW,PR"));
    }

    @Test
    void pressWithoutHistoryRecordsNothing() {
      visit(PRESS1, 3);
      assertThat(Preferences.getString("intriguingPuzzleBoxHistory"), is(""));
    }

    @Test
    void refreshOfLatestPageChangesNothing() {
      visitChain();
      visit(PRESS3, 0);
      assertThat(
          Preferences.getString("intriguingPuzzleBoxHistory"),
          is(
              "FCW,PB,RCCW,LCW,KCW,PR#3|FCW,PB,RCCW,LCW,KCW,PR#1|FCW,TCW,TCCW,PT,FCCW,PR#1|BCW,PB,PF,RCW,BCCW,KCW"));
    }

    @Test
    void restartingBoxStartsNewHistory() {
      visitChain();
      visit(FIRST, 0);
      assertThat(Preferences.getString("intriguingPuzzleBoxHistory"), is("FCW,PB,RCCW,LCW,KCW,PR"));
    }
  }
}
