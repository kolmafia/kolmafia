package net.sourceforge.kolmafia.listener;

public class CharacterListener implements Listener {
  private final Runnable updater;

  public CharacterListener() {
    this(null);
  }

  public CharacterListener(final Runnable updater) {
    this.updater = updater;
  }

  public void update() {
    if (this.updater != null) {
      this.updater.run();
    }
  }
}
