package net.sourceforge.kolmafia.maximizer;

public record SearchMetrics(int combinationsChecked) {
  public static final SearchMetrics EMPTY = new SearchMetrics(0);
}
