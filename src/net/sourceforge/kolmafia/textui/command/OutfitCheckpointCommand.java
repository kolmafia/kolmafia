package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.SpecialOutfit;

public class OutfitCheckpointCommand extends AbstractCommand {
  public OutfitCheckpointCommand() {
    this.usage =
        " [clear] - remembers [or forgets] current equipment, use \"outfit checkpoint\" to restore.";
  }

  @Override
  public void run(final String cmd, String parameters) {
    parameters = parameters.trim();
    if (!parameters.equals("clear")) {
      SpecialOutfit.createExplicitCheckpoint();
      KoLmafia.updateDisplay("Internal checkpoint created.");
      return;
    } else if (parameters.equals("clear")) {
      SpecialOutfit.forgetCheckpoints();
      KoLmafia.updateDisplay("Checkpoints cleared.");
    }
  }
}
