package net.sourceforge.kolmafia.maximizer;

public record SearchMetrics(
    int combinationsChecked,
    int catalogCandidates,
    int shortlistedCandidates,
    int scoreCalculations) {
  public static final SearchMetrics EMPTY = new SearchMetrics(0, 0, 0, 0);

  public SearchMetrics(int combinationsChecked) {
    this(combinationsChecked, 0, 0, 0);
  }
}
