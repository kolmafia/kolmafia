package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.DebugModifiers;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;

public class ModifierTraceCommand extends AbstractCommand {
  public ModifierTraceCommand() {
    this.usage = " <filter> - list everything that adds to modifiers matching filter.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    int count = DebugModifiers.setup(parameters.toLowerCase());
    if (count == 0) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "No matching modifiers - use 'modref' to list.");
      return;
    } else if (count > 10) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "Too many matching modifiers - use 'modref' to list.");
      return;
    }
    KoLCharacter.recalculateAdjustments(true);
  }
}
