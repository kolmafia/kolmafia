package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;

public class SetHolidayCommand extends AbstractCommand {
  {
    this.usage = " <HolidayName> - enable special processing for unpredicted holidays.";
  }

  @Override
  public void run(final String command, final String parameters) {
    HolidayDatabase.setHoliday(parameters);
    KoLCharacter.updateStatus();
  }
}
