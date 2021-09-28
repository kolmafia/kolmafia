package net.sourceforge.kolmafia.utilities;

import javax.swing.SwingUtilities;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.preferences.Preferences;

public class PauseObject implements Runnable {
  private long milliseconds = 0;

  public void run() {
    this.pause(this.milliseconds);
  }

  public void pause() {
    try {
      synchronized (this) {
        this.wait();
      }
    } catch (InterruptedException e) {
      // We expect this to happen only when we are
      // interrupted.  Fall through.
    }
  }

  public void pause(long milliseconds) {
    if (milliseconds <= 0) {
      return;
    }

    if (SwingUtilities.isEventDispatchThread()) {
      if (Preferences.getBoolean("debugFoxtrotRemoval")) {
        StaticEntity.printStackTrace("Pause object in event dispatch thread");
      }

      return;
    }

    this.milliseconds = milliseconds;

    try {
      synchronized (this) {
        this.wait(milliseconds);
      }
    } catch (InterruptedException e) {
      // We expect this to happen only when we are
      // interrupted.  Fall through.
    }
  }

  public void unpause() {
    synchronized (this) {
      this.notifyAll();
    }
  }
}
