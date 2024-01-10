package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class WitchessRequestTest {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("WitchessRequestTest");
    Preferences.reset("WitchessRequestTest");
  }

  private static Stream<Arguments> generateTests() {
    return Stream.of(
        Arguments.of(23, true),
        Arguments.of(23, false),
        Arguments.of(56, true),
        Arguments.of(56, false),
        Arguments.of(63, true),
        Arguments.of(63, false),
        Arguments.of(66, true),
        Arguments.of(66, false),
        Arguments.of(67, true),
        Arguments.of(67, false),
        Arguments.of(68, true),
        Arguments.of(68, false),
        Arguments.of(70, true),
        Arguments.of(70, false),
        Arguments.of(71, true),
        Arguments.of(71, false),
        Arguments.of(78, true),
        Arguments.of(78, false),
        Arguments.of(79, true),
        Arguments.of(79, false),
        Arguments.of(82, true),
        Arguments.of(82, false),
        Arguments.of(84, true),
        Arguments.of(84, false),
        Arguments.of(87, true),
        Arguments.of(87, false),
        Arguments.of(89, true),
        Arguments.of(89, false),
        Arguments.of(93, true),
        Arguments.of(93, false));
  }

  @ParameterizedTest
  @MethodSource("generateTests")
  void getSuccessfulWitchessRequest(int puzzleId, boolean isSolved) {
    var solvedPart = isSolved ? "solved" : "unsolved";
    var path = "request/witchess/" + solvedPart + "/puzzle_" + puzzleId + ".html";
    var cleanups = new Cleanups(withNextResponse(200, html(path)));

    try (cleanups) {
      var req = new WitchessRequest(String.valueOf(puzzleId));
      req.run();

      assertThat(req.getThisPuzzle(), is(puzzleId));
      assertThat(req.getIsSolved(), is(isSolved));
    }
  }

  @ParameterizedTest
  @MethodSource("generateTests")
  void alreadyHasBuffRequest(int puzzleId, boolean isSolved) {
    var solvedPart = isSolved ? "solved" : "unsolved";
    var path = "request/witchess/" + solvedPart + "/puzzle_" + puzzleId + ".html";
    var cleanups =
        new Cleanups(withProperty("_witchessBuff", true), withNextResponse(200, html(path)));

    try (cleanups) {
      var req = new WitchessRequest(String.valueOf(puzzleId));
      req.run();

      assertThat(req.getThisPuzzle(), is(-1));
      assertThat(req.getIsSolved(), is(false));
    }
  }
}
