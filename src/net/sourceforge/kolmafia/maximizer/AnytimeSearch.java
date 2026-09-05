package net.sourceforge.kolmafia.maximizer;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Domain-independent deterministic depth-first anytime branch-and-bound.
 *
 * <p>The problem supplies reversible choices, complete candidates, and optional safe dominance and
 * upper-bound checks. The engine knows nothing about equipment or KoL mechanics. If the deadline
 * stops traversal, the best legal incumbent found so far is still returned and {@link
 * Result#optimal()} is false.
 */
final class AnytimeSearch {
  private AnytimeSearch() {}

  interface Problem<C, Q extends Comparable<Q>, E extends Exception> {
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

    /** Returns a competitive candidate at the current state, or null when none is available. */
    Candidate<Q> candidate(Q incumbent) throws E;

    default void finished(Result<Q> result) {}
  }

  record Candidate<Q>(Q quality) {}

  record Result<Q>(Q quality, long nodes, long boundPrunes, boolean optimal) {}

  static <C, Q extends Comparable<Q>, E extends Exception> Result<Q> maximize(
      Problem<C, Q, E> problem, Candidate<Q> incumbent, BooleanSupplier keepSearching) throws E {
    var search = new Search<>(problem, incumbent, keepSearching);
    try {
      search.visit();
      search.completed = true;
      return search.result();
    } finally {
      problem.finished(search.result());
    }
  }

  private static final class Search<C, Q extends Comparable<Q>, E extends Exception> {
    private final Problem<C, Q, E> problem;
    private final BooleanSupplier keepSearching;
    private Candidate<Q> best;
    private long nodes;
    private long boundPrunes;
    private boolean stopped;
    private boolean completed;

    private Search(
        Problem<C, Q, E> problem, Candidate<Q> incumbent, BooleanSupplier keepSearching) {
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
      if (this.best != null && !this.problem.canBeat(this.best.quality())) {
        this.boundPrunes++;
        return;
      }

      Candidate<Q> candidate =
          this.problem.candidate(this.best == null ? null : this.best.quality());
      if (candidate != null) {
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

    private Result<Q> result() {
      return new Result<>(
          this.best == null ? null : this.best.quality(),
          this.nodes,
          this.boundPrunes,
          this.completed && !this.stopped);
    }
  }
}
