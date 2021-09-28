package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;

public class SetHolidayCommand extends AbstractCommand {
  {
    this.usage = " <HolidayName> - enable special processing for unpredicted holidays.";
  }

  @Override
  public void run(final String command, final String parameters) {
    KoLCharacter.setHoliday(parameters);
    KoLCharacter.updateStatus();
  }
}
