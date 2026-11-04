package net.sourceforge.kolmafia.utilities;

import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.ui.UserInterfaceManager;

public class PauseObject implements Runnable {
  private long milliseconds = 0;

  @Override
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

    if (UserInterfaceManager.getUserInterface().isUiThread()) {
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
