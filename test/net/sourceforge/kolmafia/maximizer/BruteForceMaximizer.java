package net.sourceforge.kolmafia.maximizer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

final class BruteForceMaximizer {
  private BruteForceMaximizer() {}

  record Result<T>(List<T> assignment, SolutionQuality quality, int completeCandidates) {}

  static <T> Result<T> maximize(
      List<List<T>> choices,
      Predicate<List<T>> feasible,
      Function<List<T>, SolutionQuality> quality) {
    return maximize(choices, feasible, quality, new ArrayList<>(choices.size()), 0);
  }

  private static <T> Result<T> maximize(
      List<List<T>> choices,
      Predicate<List<T>> feasible,
      Function<List<T>, SolutionQuality> quality,
      List<T> assignment,
      int depth) {
    if (depth == choices.size()) {
      if (!feasible.test(assignment)) return new Result<>(null, null, 1);
      var complete = List.copyOf(assignment);
      return new Result<>(complete, quality.apply(complete), 1);
    }

    List<T> bestAssignment = null;
    SolutionQuality bestQuality = null;
    int completeCandidates = 0;
    for (T choice : choices.get(depth)) {
      assignment.add(choice);
      var result = maximize(choices, feasible, quality, assignment, depth + 1);
      assignment.removeLast();
      completeCandidates += result.completeCandidates();
      if (result.quality() != null
          && (bestQuality == null || result.quality().compareTo(bestQuality) > 0)) {
        bestAssignment = result.assignment();
        bestQuality = result.quality();
      }
    }
    return new Result<>(bestAssignment, bestQuality, completeCandidates);
  }
}
