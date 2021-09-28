package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class AdventureCommand extends AbstractCommand {
  public AdventureCommand() {
    this.usage = "[?] last | [<count>] <location> - spend your turns.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    int adventureCount;
    KoLAdventure adventure =
        AdventureDatabase.getAdventure(
            parameters.equalsIgnoreCase("last")
                ? Preferences.getString("lastAdventure")
                : parameters);

    if (adventure != null) {
      adventureCount = 1;
    } else {
      String adventureCountString = parameters.split(" ")[0];
      adventureCount =
          adventureCountString.equals("*") ? 0 : StringUtilities.parseInt(adventureCountString);

      if (adventureCount == 0
          && !adventureCountString.equals("0")
          && !adventureCountString.equals("*")) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, parameters + " does not exist in the adventure database.");
        return;
      }

      String adventureName = parameters.substring(adventureCountString.length()).trim();
      adventure = AdventureDatabase.getAdventure(adventureName);

      if (adventure == null) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, parameters + " does not exist in the adventure database.");
        return;
      }

      if (adventureCount <= 0 && adventure.getAdventureId().equals(AdventurePool.THE_SHORE_ID)) {
        adventureCount += (int) Math.floor(KoLCharacter.getAdventuresLeft() / 3);
      } else if (adventureCount <= 0) {
        adventureCount += KoLCharacter.getAdventuresLeft();
      }
    }

    if (KoLmafiaCLI.isExecutingCheckOnlyCommand) {
      RequestLogger.printLine(adventure.toString());
      return;
    }

    boolean redoSkippedAdventures = KoLmafia.redoSkippedAdventures;
    try {
      KoLmafia.redoSkippedAdventures = true;
      KoLmafia.makeRequest(adventure, adventureCount);
    } finally {
      KoLmafia.redoSkippedAdventures = redoSkippedAdventures;
    }
  }
}
