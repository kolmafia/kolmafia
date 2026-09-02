package net.sourceforge.kolmafia.maximizer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import org.junit.jupiter.api.Test;

class BruteForceMaximizerTest {
  private static final SolutionQuality.AttachmentQuality NO_ACQUISITION =
      new SolutionQuality.AttachmentQuality(false, ResourceUsage.EMPTY, false, false);

  private record Choice(String name, int score, int resourceCost) {}

  @Test
  void findsTheBestFeasibleAssignmentIndependentlyOfCandidateOrder() {
    var low = new Choice("low", 1, 0);
    var efficient = new Choice("efficient", 3, 1);
    var expensive = new Choice("expensive", 5, 3);
    var choices = List.of(List.of(low, expensive), List.of(low, efficient));

    var result =
        BruteForceMaximizer.maximize(
            choices,
            assignment -> assignment.stream().mapToInt(Choice::resourceCost).sum() <= 2,
            assignment -> quality(assignment.stream().mapToInt(Choice::score).sum()));
    var reordered =
        BruteForceMaximizer.maximize(
            List.of(List.of(expensive, low), List.of(efficient, low)),
            assignment -> assignment.stream().mapToInt(Choice::resourceCost).sum() <= 2,
            assignment -> quality(assignment.stream().mapToInt(Choice::score).sum()));

    assertThat(result.assignment(), is(List.of(low, efficient)));
    assertThat(result.quality(), is(reordered.quality()));
    assertThat(result.completeCandidates(), is(4));
  }

  private static SolutionQuality quality(double score) {
    return new SolutionQuality(true, score, ResourceUsage.EMPTY, 0, 0, 0, 0, 0, 0, NO_ACQUISITION);
  }
}
