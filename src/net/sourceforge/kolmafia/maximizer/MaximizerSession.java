package net.sourceforge.kolmafia.maximizer;

final class MaximizerSession {
  final long combinationLimit;
  MaximizerSpeculation best;
  int combinationsChecked;
  long nextProgressUpdate;
  CharacterSnapshot character;
  boolean active = true;

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

  void refreshCharacterSnapshot() {
    this.character = CharacterSnapshot.capture();
  }

  void finish() {
    this.active = false;
  }
}
