package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.moods.MoodManager;

public class SaveAsMoodCommand extends AbstractCommand {
  public SaveAsMoodCommand() {
    this.usage = " - add your current effects to the mood.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    MoodManager.minimalSet();
    MoodManager.saveSettings();
  }
}
