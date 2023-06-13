package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.swingui.FamiliarTrainingFrame;
import net.sourceforge.kolmafia.swingui.FamiliarTrainingFrame.Goal;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class TrainFamiliarCommand extends AbstractCommand {
  public TrainFamiliarCommand() {
    this.usage = " base <weight> | buffed <weight> | turns <number> - train familiar.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    // train (base | buffed | turns) <goal>
    String[] split = parameters.split(" ");

    if (split.length < 2 || split.length > 3) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Syntax: train type goal");
      return;
    }

    String typeString = split[0].toLowerCase();

    Goal type = null;

    switch (typeString) {
      case "base" -> type = Goal.BASE;
      case "buff" -> type = Goal.BUFFED;
      case "turns" -> type = Goal.TURNS;
    }

    if (type == null) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Unknown training type: " + typeString);
      return;
    }

    FamiliarTrainingFrame.levelFamiliar(StringUtilities.parseInt(split[1]), type, false);
  }
}
