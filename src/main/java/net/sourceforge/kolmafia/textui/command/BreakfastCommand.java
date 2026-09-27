package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.session.BreakfastManager;

public class BreakfastCommand extends AbstractCommand {
  public BreakfastCommand() {
    this.usage = " - perform start-of-day activities.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    BreakfastManager.getBreakfast(true);
  }
}
