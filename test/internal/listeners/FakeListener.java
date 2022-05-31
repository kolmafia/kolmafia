package internal.listeners;

import net.sourceforge.kolmafia.listener.Listener;

public class FakeListener implements Listener {

  private int updateCount = 0;

  public int getUpdateCount() {
    return updateCount;
  }

  @Override
  public void update() {
    updateCount++;
  }
}
