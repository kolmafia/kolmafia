package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.request.LoginRequest;
import net.sourceforge.kolmafia.request.LogoutRequest;

public class TimeinCommand extends AbstractCommand {
  public TimeinCommand() {
    this.usage = " - log out and log in again, preserving character state";
  }

  @Override
  public void run(final String cmd, String parameters) {
    if (LoginRequest.completedLogin()) {
      RequestThread.postRequest(new LogoutRequest());
    }
    LoginRequest.retimein();
  }
}
