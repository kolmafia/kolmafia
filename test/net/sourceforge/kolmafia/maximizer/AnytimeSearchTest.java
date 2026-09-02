package net.sourceforge.kolmafia.maximizer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
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
    assertThat(reduced.result(), is(oracle.assignment()));
    assertThat(reduced.optimal(), is(true));
    assertThat(reduced.dominancePrunes(), greaterThan(0L));
    assertThat(reduced.boundPrunes(), greaterThan(0L));
    assertThat(reduced.leaves(), lessThan(unpruned.leaves()));
  }

  @Test
  void retainsTheIncumbentWhenTheBudgetExpires() {
    var incumbentChoice = new Choice("incumbent", 3, 0);
    var incumbent = new AnytimeSearch.Candidate<>(quality(3), List.of(incumbentChoice));
    var choices =
        List.of(
            List.of(new Choice("first", 1, 0), new Choice("better", 5, 0)),
            List.of(new Choice("second", 1, 0)));

    var result =
        AnytimeSearch.maximize(new KnapsackProblem(choices, 0, false, false), incumbent, calls(3));

    assertThat(result.quality(), is(incumbent.quality()));
    assertThat(result.result(), is(incumbent.result()));
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
          public SolutionQuality upperBound() {
            if (this.depth() > 0) throw new IllegalStateException("failed");
            return null;
          }
        };

    assertThrows(
        IllegalStateException.class, () -> AnytimeSearch.maximize(problem, null, () -> true));

    assertThat(problem.depth(), is(0));
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
      implements AnytimeSearch.Problem<Choice, SolutionQuality, List<Choice>> {
    private final List<List<Choice>> choices;
    private final int capacity;
    private final boolean useBounds;
    private final boolean useFrontier;
    private final List<Choice> assignment = new ArrayList<>();
    private final Map<Integer, List<Point>> frontier = new HashMap<>();
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
    public SolutionQuality upperBound() {
      if (!this.useBounds) return null;

      int optimistic = this.score;
      for (int depth = this.assignment.size(); depth < this.choices.size(); depth++) {
        optimistic += this.choices.get(depth).stream().mapToInt(Choice::score).max().orElseThrow();
      }
      return quality(optimistic);
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
    public AnytimeSearch.Candidate<SolutionQuality, List<Choice>> evaluate() {
      return new AnytimeSearch.Candidate<>(quality(this.score), List.copyOf(this.assignment));
    }

    int depth() {
      return this.assignment.size();
    }
  }
}
