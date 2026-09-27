package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.session.EventManager;

public class EventsCommand extends AbstractCommand {
  public EventsCommand() {
    this.usage = " [clear] - clear or show recent events.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    if (parameters.equals("clear")) {
      EventManager.clearEventHistory();
    } else {
      RequestLogger.printList(EventManager.getEventTexts());
    }
  }
}
