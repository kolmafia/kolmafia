package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;

public class RegisterAdventureCommand extends AbstractCommand {
  public RegisterAdventureCommand() {
    this.usage = null;
  }

  @Override
  public void run(final String cmd, final String parameters) {
    int spaceIndex = parameters.indexOf(" ");
    if (spaceIndex == -1) {
      return;
    }

    KoLAdventure adventure =
        new KoLAdventure(
            "Override",
            "adventure.php",
            parameters.substring(0, spaceIndex),
            parameters.substring(spaceIndex).trim());

    AdventureDatabase.addAdventure(adventure);
  }
}
