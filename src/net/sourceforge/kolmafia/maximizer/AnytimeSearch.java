package net.sourceforge.kolmafia.maximizer;

import java.util.List;
import java.util.function.BooleanSupplier;

final class AnytimeSearch {
  private AnytimeSearch() {}

  interface Problem<C, Q extends Comparable<Q>, R, E extends Exception> {
    boolean complete();

    List<C> choices();

    /**
     * Applies a choice and reports whether the resulting state is feasible. Every call is followed
     * by {@link #undo}, including calls that return false.
     */
    boolean choose(C choice);

    void undo(C choice);

    /** Returns false only when no completion of the current state can beat the incumbent. */
    default boolean canBeat(Q incumbent) {
      return true;
    }

    default boolean dominated() {
      return false;
    }

    default void record() {}

    /** Returns a competitive candidate at the current state, or null when none is available. */
    Candidate<Q, R> candidate(Q incumbent) throws E;

    default void finished(Result<Q, R> result) {}
  }

  record Candidate<Q, R>(Q quality, R result) {}

  record Result<Q, R>(
      Q quality,
      R result,
      long nodes,
      long leaves,
      long dominancePrunes,
      long boundPrunes,
      boolean optimal) {}

  static <C, Q extends Comparable<Q>, R, E extends Exception> Result<Q, R> maximize(
      Problem<C, Q, R, E> problem, Candidate<Q, R> incumbent, BooleanSupplier keepSearching)
      throws E {
    var search = new Search<>(problem, incumbent, keepSearching);
    try {
      search.visit();
      search.completed = true;
      return search.result();
    } finally {
      problem.finished(search.result());
    }
  }

  private static final class Search<C, Q extends Comparable<Q>, R, E extends Exception> {
    private final Problem<C, Q, R, E> problem;
    private final BooleanSupplier keepSearching;
    private Candidate<Q, R> best;
    private long nodes;
    private long leaves;
    private long dominancePrunes;
    private long boundPrunes;
    private boolean stopped;
    private boolean completed;

    private Search(
        Problem<C, Q, R, E> problem, Candidate<Q, R> incumbent, BooleanSupplier keepSearching) {
      this.problem = problem;
      this.best = incumbent;
      this.keepSearching = keepSearching;
    }

    private void visit() throws E {
      if (!this.keepSearching.getAsBoolean()) {
        this.stopped = true;
        return;
      }

      this.nodes++;
      if (this.problem.dominated()) {
        this.dominancePrunes++;
        return;
      }
      this.problem.record();

      if (this.best != null && !this.problem.canBeat(this.best.quality())) {
        this.boundPrunes++;
        return;
      }

      Candidate<Q, R> candidate =
          this.problem.candidate(this.best == null ? null : this.best.quality());
      if (candidate != null) {
        this.leaves++;
        if (this.best == null || candidate.quality().compareTo(this.best.quality()) > 0) {
          this.best = candidate;
        }
      }

      if (this.problem.complete()) {
        return;
      }

      for (C choice : this.problem.choices()) {
        try {
          if (this.problem.choose(choice)) {
            this.visit();
          }
        } finally {
          this.problem.undo(choice);
        }
        if (this.stopped) return;
      }
    }

    private Result<Q, R> result() {
      return new Result<>(
          this.best == null ? null : this.best.quality(),
          this.best == null ? null : this.best.result(),
          this.nodes,
          this.leaves,
          this.dominancePrunes,
          this.boundPrunes,
          this.completed && !this.stopped);
    }
  }
}
