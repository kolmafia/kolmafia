package net.sourceforge.kolmafia.objectpool;

// Used for handling queued concoctions.
public enum ConcoctionType {
  NONE("(none)"),
  FOOD("(food)"),
  BOOZE("(booze)"),
  SPLEEN("(spleen)"),
  POTION("(potions)");

  private String signal;

  ConcoctionType(String signal) {
    this.signal = signal;
  }

  public String getSignal() {
    return this.signal;
  }
}
