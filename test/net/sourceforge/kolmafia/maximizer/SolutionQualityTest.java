package net.sourceforge.kolmafia.maximizer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class SolutionQualityTest {
  private static final SolutionQuality.AttachmentQuality NO_ACQUISITION =
      new SolutionQuality.AttachmentQuality(false, ResourceUsage.EMPTY, false, false);

  private static SolutionQuality quality(boolean feasible, double score, int simplicity) {
    return new SolutionQuality(
        feasible, score, ResourceUsage.EMPTY, 0, 0, 0, 0, 0, simplicity, NO_ACQUISITION);
  }

  @Test
  void feasibilityOutranksScoreAndScoreOutranksStability() {
    assertThat(quality(true, 1, 0), greaterThan(quality(false, 100, 100)));
    assertThat(quality(true, 2, 0), greaterThan(quality(true, 1, 100)));
    assertThat(quality(true, 1, 0), lessThan(quality(true, 1, 1)));
  }

  @Test
  void comparesEverySecondaryQualityInOrder() {
    var baseline = quality(true, 1, 0);

    assertThat(
        new SolutionQuality(true, 1, ResourceUsage.EMPTY, 1, 0, 0, 0, 0, 0, NO_ACQUISITION),
        greaterThan(baseline));
    assertThat(
        new SolutionQuality(true, 1, ResourceUsage.EMPTY, 0, 1, 0, 0, 0, 0, NO_ACQUISITION),
        greaterThan(baseline));
    assertThat(
        new SolutionQuality(true, 1, ResourceUsage.EMPTY, 0, 0, 1, 0, 0, 0, NO_ACQUISITION),
        greaterThan(baseline));
    assertThat(
        new SolutionQuality(true, 1, ResourceUsage.EMPTY, 0, 0, 0, 1, 0, 0, NO_ACQUISITION),
        greaterThan(baseline));
    assertThat(
        new SolutionQuality(true, 1, ResourceUsage.EMPTY, 0, 0, 0, 0, 1, 0, NO_ACQUISITION),
        lessThan(baseline));
    assertThat(
        new SolutionQuality(true, 1, ResourceUsage.of(1), 0, 0, 0, 0, 0, 0, NO_ACQUISITION),
        lessThan(baseline));
  }

  @Test
  void prefersOwnedAttachmentsBeforeResourceCost() {
    var owned = new SolutionQuality.AttachmentQuality(false, ResourceUsage.of(3), true, true);
    var lowerResourceCost =
        new SolutionQuality.AttachmentQuality(false, ResourceUsage.of(0), false, false);

    assertThat(
        new SolutionQuality(true, 1, ResourceUsage.EMPTY, 0, 0, 0, 0, 0, 0, owned),
        greaterThan(
            new SolutionQuality(
                true, 1, ResourceUsage.EMPTY, 0, 0, 0, 0, 0, 0, lowerResourceCost)));
  }

  @Test
  void resourceUsageSupportsIncrementalSearchAccounting() {
    var usage = ResourceUsage.of(1, 2).plus(ResourceUsage.of(3, 4).times(2));

    assertThat(usage.cost(0), is(7));
    assertThat(usage.cost(1), is(10));
    assertThat(ResourceUsage.of(1).times(0).cost(0), is(0));
  }

  @Test
  void resourceUsageRejectsMismatchedResourceSets() {
    var resources = List.of(new SharedResource("test", 1, ignored -> 0, ignored -> 0));

    assertThrows(
        IllegalArgumentException.class, () -> ResourceUsage.of(1).plus(ResourceUsage.of(1, 2)));
    assertThrows(
        IllegalArgumentException.class,
        () -> ResourceUsage.of(1).hasRemainingCapacityFor(ResourceUsage.of(1, 2), resources));
  }
}
