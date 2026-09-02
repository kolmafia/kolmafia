package net.sourceforge.kolmafia.maximizer;

final class MaximizerSession {
  final long combinationLimit;
  MaximizerSpeculation best;
  int combinationsChecked;
  long nextProgressUpdate;

  MaximizerSession(MaximizerSpeculation best, long combinationLimit) {
    this.best = best;
    this.combinationLimit = combinationLimit;
  }

  void resetSearch() {
    this.combinationsChecked = 0;
    this.nextProgressUpdate = System.currentTimeMillis() + 5000;
  }

  SearchMetrics metrics() {
    return new SearchMetrics(this.combinationsChecked);
  }
}
