package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.RequestThread;

public class GrayGUICommand extends AbstractCommand {
  public GrayGUICommand() {
    this.usage =
        " - print out a stack trace to help figure out why the UI might be gray/stuck (requires use of the JDK instead of the JRE).";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    String[] split = parameters.split(" ");
    String command = split[0];
    boolean force = command.equals("force");

    RequestThread.checkOpenRequestSequences(force);
  }
}
