package net.sourceforge.kolmafia.maximizer;

record SolutionQuality(
    boolean feasible,
    double score,
    int beeosity,
    int itemDroppers,
    int meatDroppers,
    double tiebreaker,
    int rolloverEffects,
    int breakables,
    int simplicity,
    AttachmentQuality attachment)
    implements Comparable<SolutionQuality> {

  record AttachmentQuality(boolean buyable, int beeosity, boolean inInventory, boolean initial) {}

  @Override
  public int compareTo(SolutionQuality other) {
    int comparison = Boolean.compare(this.feasible, other.feasible);
    if (comparison != 0) return comparison;

    comparison = Double.compare(this.score, other.score);
    if (comparison != 0) return comparison;

    comparison = Integer.compare(other.beeosity, this.beeosity);
    if (comparison != 0) return comparison;

    comparison = Integer.compare(this.itemDroppers, other.itemDroppers);
    if (comparison != 0) return comparison;

    comparison = Integer.compare(this.meatDroppers, other.meatDroppers);
    if (comparison != 0) return comparison;

    comparison = Double.compare(this.tiebreaker, other.tiebreaker);
    if (comparison != 0) return comparison;

    comparison = Integer.compare(this.rolloverEffects, other.rolloverEffects);
    if (comparison != 0) return comparison;

    comparison = Integer.compare(other.breakables, this.breakables);
    if (comparison != 0) return comparison;

    comparison = Integer.compare(this.simplicity, other.simplicity);
    if (comparison != 0) return comparison;

    if (this.attachment == null || other.attachment == null) return 0;

    comparison = Boolean.compare(other.attachment.buyable, this.attachment.buyable);
    if (comparison != 0) return comparison;

    comparison = Boolean.compare(this.attachment.inInventory, other.attachment.inInventory);
    if (comparison != 0) return comparison;

    comparison = Boolean.compare(this.attachment.initial, other.attachment.initial);
    if (comparison != 0) return comparison;

    return Integer.compare(other.attachment.beeosity, this.attachment.beeosity);
  }
}
