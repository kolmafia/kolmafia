package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.request.LoginRequest;
import net.sourceforge.kolmafia.request.LogoutRequest;

public class ReloginCommand extends AbstractCommand {
  public ReloginCommand() {
    this.usage = " - log out and log in again, preserving character state";
  }

  @Override
  public void run(final String cmd, String parameters) {
    RequestThread.postRequest(new LogoutRequest());
    LoginRequest.retimein();
  }
}
