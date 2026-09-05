package net.sourceforge.kolmafia.maximizer;

public record SearchMetrics(
    int combinationsChecked,
    int catalogCandidates,
    int shortlistedCandidates,
    int scoreCalculations,
    long searchNodes,
    long boundPrunes,
    boolean searchComplete,
    long candidateCompilationNanos) {
  public static final SearchMetrics EMPTY = new SearchMetrics(0, 0, 0, 0, 0, 0, true, 0);
}
