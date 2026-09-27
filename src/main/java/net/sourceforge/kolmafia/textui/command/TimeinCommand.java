package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.request.LoginRequest;

public class TimeinCommand extends AbstractCommand {
  public TimeinCommand() {
    this.usage = " - log out and log in again, preserving character state";
  }

  @Override
  public void run(final String cmd, String parameters) {
    LoginRequest.retimein();
  }
}
