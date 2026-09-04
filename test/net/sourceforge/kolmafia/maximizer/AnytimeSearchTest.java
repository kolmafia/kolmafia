package net.sourceforge.kolmafia.maximizer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;

class AnytimeSearchTest {
  private record Choice(String name, int score, int cost) {}

  private record Point(int score, int cost) {
    boolean dominates(Point other) {
      return this.score >= other.score && this.cost <= other.cost;
    }
  }

  @Test
  void matchesBruteForceWithOptionalExactReductions() {
    var choices =
        List.of(
            List.of(new Choice("cheap", 2, 0), new Choice("strong", 6, 2)),
            List.of(new Choice("weak", 1, 0), new Choice("efficient", 4, 1)),
            List.of(new Choice("free", 0, 0), new Choice("costly", 5, 2)));
    var oracle =
        BruteForceMaximizer.maximize(
            choices,
            assignment -> assignment.stream().mapToInt(Choice::cost).sum() <= 3,
            assignment -> quality(assignment.stream().mapToInt(Choice::score).sum()));

    var unpruned =
        AnytimeSearch.maximize(new KnapsackProblem(choices, 3, false, false), null, () -> true);
    var reduced =
        AnytimeSearch.maximize(new KnapsackProblem(choices, 3, true, true), null, () -> true);

    assertThat(unpruned.quality(), is(oracle.quality()));
    assertThat(reduced.quality(), is(oracle.quality()));
    assertThat(reduced.optimal(), is(true));
    assertThat(reduced.dominancePrunes(), greaterThan(0L));
    assertThat(reduced.boundPrunes(), greaterThan(0L));
    assertThat(reduced.leaves(), lessThan(unpruned.leaves()));
  }

  @Test
  void retainsTheIncumbentWhenTheBudgetExpires() {
    var incumbent = new AnytimeSearch.Candidate<>(quality(3));
    var choices =
        List.of(
            List.of(new Choice("first", 1, 0), new Choice("better", 5, 0)),
            List.of(new Choice("second", 1, 0)));

    var result =
        AnytimeSearch.maximize(new KnapsackProblem(choices, 0, false, false), incumbent, calls(3));

    assertThat(result.quality(), is(incumbent.quality()));
    assertThat(result.optimal(), is(false));
  }

  @Test
  void alwaysUndoesInfeasibleChoices() {
    var choices =
        List.of(
            List.of(new Choice("too expensive", 100, 2), new Choice("legal", 3, 0)),
            List.of(new Choice("finish", 2, 0)));

    var result =
        AnytimeSearch.maximize(new KnapsackProblem(choices, 0, false, false), null, () -> true);

    assertThat(result.quality(), is(quality(5)));
    assertThat(result.optimal(), is(true));
  }

  @Test
  void restoresStateWhenProblemEvaluationThrows() {
    var problem =
        new KnapsackProblem(List.of(List.of(new Choice("choice", 1, 0))), 0, false, false) {
          @Override
          public boolean complete() {
            if (this.depth() > 0) throw new IllegalStateException("failed");
            return super.complete();
          }
        };

    assertThrows(
        IllegalStateException.class, () -> AnytimeSearch.maximize(problem, null, () -> true));

    assertThat(problem.depth(), is(0));
    assertThat(problem.finished().nodes(), is(2L));
    assertThat(problem.finished().optimal(), is(false));
  }

  @Test
  void preservesQualityAcrossEquivalentSearchChanges() {
    var choices =
        List.of(
            List.of(new Choice("first", 1, 0), new Choice("better first", 3, 1)),
            List.of(new Choice("second", 2, 0), new Choice("better second", 4, 1)));
    var expanded =
        List.of(
            List.of(
                new Choice("first", 1, 0),
                new Choice("better first", 3, 1),
                new Choice("best first", 5, 1)),
            choices.get(1));

    var shortRun =
        AnytimeSearch.maximize(new KnapsackProblem(choices, 2, false, false), null, calls(3));
    var longRun =
        AnytimeSearch.maximize(new KnapsackProblem(choices, 2, false, false), null, calls(20));
    var expandedRun =
        AnytimeSearch.maximize(new KnapsackProblem(expanded, 2, false, false), null, () -> true);
    var reorderedRun =
        AnytimeSearch.maximize(
            new KnapsackProblem(choices.stream().map(List::reversed).toList(), 2, false, false),
            null,
            () -> true);
    var repeatedRun =
        AnytimeSearch.maximize(new KnapsackProblem(choices, 2, false, false), null, () -> true);

    assertThat(longRun.quality(), greaterThanOrEqualTo(shortRun.quality()));
    assertThat(expandedRun.quality(), greaterThanOrEqualTo(longRun.quality()));
    assertThat(reorderedRun.quality(), is(longRun.quality()));
    assertThat(repeatedRun, is(longRun));
  }

  private static BooleanSupplier calls(int allowed) {
    return new BooleanSupplier() {
      private int remaining = allowed;

      @Override
      public boolean getAsBoolean() {
        return this.remaining-- > 0;
      }
    };
  }

  private static SolutionQuality quality(double score) {
    return new SolutionQuality(true, score, ResourceUsage.EMPTY, 0, 0, 0, 0, 0, 0, null);
  }

  private static class KnapsackProblem
      implements AnytimeSearch.Problem<Choice, SolutionQuality, RuntimeException> {
    private final List<List<Choice>> choices;
    private final int capacity;
    private final boolean useBounds;
    private final boolean useFrontier;
    private final List<Choice> assignment = new ArrayList<>();
    private final Map<Integer, List<Point>> frontier = new HashMap<>();
    private AnytimeSearch.Result<SolutionQuality> finished;
    private int score;
    private int cost;

    private KnapsackProblem(
        List<List<Choice>> choices, int capacity, boolean useBounds, boolean useFrontier) {
      this.choices = choices;
      this.capacity = capacity;
      this.useBounds = useBounds;
      this.useFrontier = useFrontier;
    }

    @Override
    public boolean complete() {
      return this.assignment.size() == this.choices.size();
    }

    @Override
    public List<Choice> choices() {
      return this.choices.get(this.assignment.size());
    }

    @Override
    public boolean choose(Choice choice) {
      this.assignment.add(choice);
      this.score += choice.score();
      this.cost += choice.cost();
      return this.cost <= this.capacity;
    }

    @Override
    public void undo(Choice choice) {
      this.assignment.removeLast();
      this.score -= choice.score();
      this.cost -= choice.cost();
    }

    @Override
    public boolean canBeat(SolutionQuality incumbent) {
      if (!this.useBounds) return true;

      int optimistic = this.score;
      for (int depth = this.assignment.size(); depth < this.choices.size(); depth++) {
        optimistic += this.choices.get(depth).stream().mapToInt(Choice::score).max().orElseThrow();
      }
      return quality(optimistic).compareTo(incumbent) > 0;
    }

    @Override
    public boolean dominated() {
      if (!this.useFrontier) return false;
      Point current = new Point(this.score, this.cost);
      return this.frontier.getOrDefault(this.assignment.size(), List.of()).stream()
          .anyMatch(point -> point.dominates(current));
    }

    @Override
    public void record() {
      if (!this.useFrontier) return;
      Point current = new Point(this.score, this.cost);
      this.frontier
          .computeIfAbsent(this.assignment.size(), ignored -> new ArrayList<>())
          .removeIf(current::dominates);
      this.frontier.get(this.assignment.size()).add(current);
    }

    @Override
    public AnytimeSearch.Candidate<SolutionQuality> candidate(SolutionQuality incumbent) {
      if (!this.complete()) return null;
      return new AnytimeSearch.Candidate<>(quality(this.score));
    }

    @Override
    public void finished(AnytimeSearch.Result<SolutionQuality> result) {
      this.finished = result;
    }

    int depth() {
      return this.assignment.size();
    }

    AnytimeSearch.Result<SolutionQuality> finished() {
      return this.finished;
    }
  }
}
