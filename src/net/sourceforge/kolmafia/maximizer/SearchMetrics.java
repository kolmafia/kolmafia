package net.sourceforge.kolmafia.maximizer;

public record SearchMetrics(
    int combinationsChecked,
    int catalogCandidates,
    int shortlistedCandidates,
    int scoreCalculations,
    long searchNodes,
    long dominancePrunes,
    long boundPrunes,
    boolean searchComplete) {
  public static final SearchMetrics EMPTY = new SearchMetrics(0, 0, 0, 0, 0, 0, 0, true);

  public SearchMetrics(int combinationsChecked) {
    this(combinationsChecked, 0, 0, 0, 0, 0, 0, true);
  }

  public SearchMetrics(
      int combinationsChecked,
      int catalogCandidates,
      int shortlistedCandidates,
      int scoreCalculations) {
    this(
        combinationsChecked,
        catalogCandidates,
        shortlistedCandidates,
        scoreCalculations,
        0,
        0,
        0,
        true);
  }
}
