package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.SpecialOutfit.Checkpoint;
import net.sourceforge.kolmafia.moods.ManaBurnManager;
import net.sourceforge.kolmafia.moods.RecoveryManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BurnMpCommand extends AbstractCommand {
  public BurnMpCommand() {
    this.usage =
        " extra | * | <num> | -num - use excess/all/specified/all but specified MP for buff extension and summons.";
  }

  @Override
  public void run(final String cmd, String parameters) {
    // Zombie Masters don't have a maximum "MP", so there is no need to burn mana
    if (KoLCharacter.inZombiecore()) {
      return;
    }

    // Remove extra words. For example, "mana"
    int space = parameters.indexOf(" ");
    if (space != -1) {
      parameters = parameters.substring(0, space);
    }

    if (parameters.startsWith("extra")) {
      try (Checkpoint checkpoint = new Checkpoint()) {
        RecoveryManager.recoverHP();
        ManaBurnManager.burnExtraMana(true);
      }
      return;
    }

    int amount;
    if (parameters.startsWith("*")) {
      amount = 0;
    } else if (StringUtilities.isNumeric(parameters)) {
      amount = StringUtilities.parseInt(parameters);
      if (amount > 0) {
        amount -= KoLCharacter.getCurrentMP();
      }
    } else {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Specify how much mana you want to burn");
      return;
    }

    try (Checkpoint checkpoint = new Checkpoint()) {
      RecoveryManager.recoverHP();
      ManaBurnManager.burnMana(-amount);
    }
  }
}
