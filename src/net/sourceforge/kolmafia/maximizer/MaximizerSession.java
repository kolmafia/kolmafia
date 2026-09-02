package net.sourceforge.kolmafia.maximizer;

final class MaximizerSession {
  final long combinationLimit;
  MaximizerSpeculation best;
  int combinationsChecked;
  int catalogCandidates;
  int shortlistedCandidates;
  int scoreCalculations;
  boolean searchingEquipment;
  long nextProgressUpdate;
  CharacterSnapshot character;
  boolean active = true;

  MaximizerSession(MaximizerSpeculation best, long combinationLimit) {
    this.best = best;
    this.combinationLimit = combinationLimit;
  }

  void resetSearch() {
    this.combinationsChecked = 0;
    this.catalogCandidates = 0;
    this.shortlistedCandidates = 0;
    this.scoreCalculations = 0;
    this.searchingEquipment = true;
    this.nextProgressUpdate = System.currentTimeMillis() + 5000;
  }

  SearchMetrics metrics() {
    return new SearchMetrics(
        this.combinationsChecked,
        this.catalogCandidates,
        this.shortlistedCandidates,
        this.scoreCalculations);
  }

  void refreshCharacterSnapshot(Evaluator evaluator) {
    this.character = CharacterSnapshot.capture(evaluator);
  }

  void finish() {
    this.active = false;
  }
}
