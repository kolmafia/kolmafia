package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.moods.MoodManager;
import net.sourceforge.kolmafia.moods.MoodTrigger;

public class EditMoodCommand extends AbstractCommand {
  public EditMoodCommand() {
    this.usage = " list | clear | autofill | [<type>,] <effect> [, <action>] - edit current mood";
    this.flags = KoLmafiaCLI.FULL_LINE_CMD;
  }

  @Override
  public void run(final String cmd, final String parameters) {
    if (parameters.length() == 0 || parameters.equals("list")) {
      RequestLogger.printList(MoodManager.getTriggers());
      return;
    }

    if (parameters.equals("clear")) {
      MoodManager.removeTriggers(MoodManager.getTriggers());
      MoodManager.saveSettings();
      RequestLogger.printLine("Cleared mood.");
      return;
    }

    if (parameters.equals("autofill")) {
      MoodManager.maximalSet();
      MoodManager.saveSettings();
      RequestLogger.printList(MoodManager.getTriggers());
      return;
    }

    int start = 0;
    int end = parameters.indexOf(',');

    if (end == -1) {
      RequestLogger.printLine("Invalid command: " + cmd + " " + parameters);
      return;
    }

    String type = parameters.substring(start, end);
    type = type.trim();

    if (!type.equals("lose_effect")
        && !type.equals("gain_effect")
        && !type.equals("unconditional")) {
      type = "lose_effect";
      end = -1;
    }

    start = end + 1;
    end = parameters.indexOf(',', start);

    String name = (end != -1) ? parameters.substring(start, end) : parameters.substring(start);
    name = name.trim();

    String action =
        (end != -1) ? parameters.substring(end + 1) : MoodManager.getDefaultAction(type, name);
    action = action.trim();

    MoodTrigger trigger = MoodManager.addTrigger(type, name, action);

    if (trigger == null) {
      RequestLogger.printLine("Invalid command: " + cmd + " " + parameters);
      return;
    }

    MoodManager.saveSettings();

    RequestLogger.printLine("Set mood trigger: " + trigger.toString());
  }
}
