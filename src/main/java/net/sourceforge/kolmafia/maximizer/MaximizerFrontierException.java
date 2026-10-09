package net.sourceforge.kolmafia.maximizer;

class MaximizerFrontierException extends RuntimeException {
  private final long estimatedBytes;

  MaximizerFrontierException(long estimatedBytes) {
    this.estimatedBytes = estimatedBytes;
  }

  long estimatedBytes() {
    return this.estimatedBytes;
  }
}
