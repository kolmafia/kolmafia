package net.sourceforge.kolmafia.session;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import internal.helpers.Networking;
import internal.helpers.Player;
import java.util.ArrayList;
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
    void recordsOptionsForFirstStep() {
      try (var cleanups = visitFirstPage()) {
        assertThat(
            Preferences.getString("intriguingPuzzleBoxOptions"), is("0:FCW,PB,RCCW,LCW,KCW,PR"));
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
  class Outcomes {
    private static final String FIRST = "request/test_choice_525_intriguing_puzzle_box.html";
    private static final String BUTTON_PRESS1 =
        "request/test_choice_525_intriguing_puzzle_box_press1.html";
    private static final String BUTTON_PRESS2 =
        "request/test_choice_525_intriguing_puzzle_box_press2.html";
    private static final String BUTTON_PRESS3 =
        "request/test_choice_525_intriguing_puzzle_box_press3.html";

    private static final String CHAIN_OPTIONS =
        "0:FCW,PB,RCCW,LCW,KCW,PR|1:FCW,TCW,TCCW,PT,FCCW,PR|2:BCW,PB,PF,RCW,BCCW,KCW";
    private static final String CHAIN_OUTCOMES = "0,1:click|0,3:buzz|1,1:click";

    private static void visit(final String page, final int decision) {
      try (var cleanups =
          Player.withChoice(
              (url, req) -> ChoiceControl.visitChoice(req),
              525,
              decision,
              null,
              Networking.html(page))) {}
    }

    private static void visitChain() {
      visit(FIRST, 0);
      visit(BUTTON_PRESS1, 3);
      visit(BUTTON_PRESS2, 1);
      visit(BUTTON_PRESS3, 1);
    }

    @Test
    void buttonPressesAttachToTheirSteps() {
      visitChain();
      assertThat(Preferences.getString("intriguingPuzzleBoxOptions"), is(CHAIN_OPTIONS));
      assertThat(Preferences.getString("intriguingPuzzleBoxOutcomes"), is(CHAIN_OUTCOMES));
    }

    @Test
    void firstPageWithoutButtonPressStartsStore() {
      visit(FIRST, 0);
      assertThat(
          Preferences.getString("intriguingPuzzleBoxOptions"), is("0:FCW,PB,RCCW,LCW,KCW,PR"));
      assertThat(Preferences.getString("intriguingPuzzleBoxOutcomes"), is(""));
    }

    @Test
    void buttonPressWithoutStoreRecordsNothing() {
      visit(BUTTON_PRESS1, 3);
      assertThat(Preferences.getString("intriguingPuzzleBoxOptions"), is(""));
      assertThat(Preferences.getString("intriguingPuzzleBoxOutcomes"), is(""));
    }

    @Test
    void refreshOfLatestStepChangesNothing() {
      visitChain();
      visit(BUTTON_PRESS3, 0);
      assertThat(Preferences.getString("intriguingPuzzleBoxOptions"), is(CHAIN_OPTIONS));
      assertThat(Preferences.getString("intriguingPuzzleBoxOutcomes"), is(CHAIN_OUTCOMES));
    }

    @Test
    void restartingBoxResetsStepAndKeepsRecords() {
      visitChain();
      visit(FIRST, 0);
      assertThat(Preferences.getString("intriguingPuzzleBoxOptions"), is(CHAIN_OPTIONS));
      assertThat(Preferences.getString("intriguingPuzzleBoxOutcomes"), is(CHAIN_OUTCOMES));
      assertThat(Preferences.getInteger("intriguingPuzzleBoxStep"), is(0));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "request/test_choice_525_intriguing_puzzle_box_press1.html, buzz",
    "request/test_choice_525_intriguing_puzzle_box_press2.html, click",
    "request/test_choice_525_intriguing_puzzle_box_say3.html, say3",
    "request/test_choice_525_intriguing_puzzle_box_say13.html, say13",
    "request/test_choice_525_intriguing_puzzle_box_beep.html, beep",
  })
  void parsesButtonPressResults(String path, String outcome) {
    assertThat(IntriguingPuzzleBoxManager.parseOutcome(Networking.html(path)), is(outcome));
  }

  @Nested
  class ButtonPredictions {
    @Test
    void stepSensitivePredictionsMatchButtonPresses() {
      int[] decisions = {3, 1, 1, 1, 1, 1, 2, 5, 2, 4, 4, 2, 6, 3};
      List<String> observed =
          List.of(
              "buzz", "click", "click", "click", "buzz", "click", "buzz", "click", "buzz", "click",
              "say3", "say13", "click", "beep");
      List<String> predicted = new ArrayList<>();
      int step = 0;
      for (int i = 0; i < decisions.length; i++) {
        predicted.add(IntriguingPuzzleBoxManager.getOutcome(2340482, decisions[i], step));
        step = observed.get(i).equals("buzz") ? 0 : step + 1;
      }
      assertThat(predicted, is(observed));
    }
  }
}
