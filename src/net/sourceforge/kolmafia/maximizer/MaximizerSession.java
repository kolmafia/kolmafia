package net.sourceforge.kolmafia.maximizer;

final class MaximizerSession {
  final long combinationLimit;
  MaximizerSpeculation best;
  int combinationsChecked;
  int catalogCandidates;
  int shortlistedCandidates;
  int scoreCalculations;
  long searchNodes;
  long dominancePrunes;
  long boundPrunes;
  boolean searchComplete = true;
  long candidateCompilationNanos;
  long candidateCompilationStartedNanos;
  long searchDeadlineNanos = Long.MAX_VALUE;
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
    this.searchNodes = 0;
    this.dominancePrunes = 0;
    this.boundPrunes = 0;
    this.searchComplete = true;
    this.candidateCompilationNanos = 0;
    this.candidateCompilationStartedNanos = System.nanoTime();
    this.searchingEquipment = true;
    this.nextProgressUpdate = System.currentTimeMillis() + 5000;
  }

  SearchMetrics metrics() {
    return new SearchMetrics(
        this.combinationsChecked,
        this.catalogCandidates,
        this.shortlistedCandidates,
        this.scoreCalculations,
        this.searchNodes,
        this.dominancePrunes,
        this.boundPrunes,
        this.searchComplete,
        this.candidateCompilationNanos);
  }

  void startSearch(int timeLimitMillis) {
    this.finishCandidateCompilation();
    long now = System.nanoTime();
    this.searchDeadlineNanos =
        timeLimitMillis <= 0 ? Long.MAX_VALUE : now + timeLimitMillis * 1_000_000L;
  }

  void finishCandidateCompilation() {
    if (this.candidateCompilationStartedNanos == 0) return;
    this.candidateCompilationNanos = System.nanoTime() - this.candidateCompilationStartedNanos;
    this.candidateCompilationStartedNanos = 0;
  }

  boolean keepSearching() {
    if (System.nanoTime() < this.searchDeadlineNanos) return true;
    this.searchComplete = false;
    return false;
  }

  void refreshCharacterSnapshot(Evaluator evaluator) {
    this.character = CharacterSnapshot.capture(evaluator);
  }

  void finish() {
    this.active = false;
  }
}
