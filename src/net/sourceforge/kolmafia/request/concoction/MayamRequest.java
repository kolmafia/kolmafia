package net.sourceforge.kolmafia.request.concoction;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.textui.command.MayamCommand;

public class MayamRequest extends CreateItemRequest {
  private static MayamCommand mayamCommand = new MayamCommand();

  public MayamRequest(final Concoction conc) {
    super("choice.php", conc);
  }

  @Override
  public void run() {
    String name = this.getName();

    KoLmafia.updateDisplay("Creating 1 " + name + "...");

    mayamCommand.run("mayam", "resonance " + name);
    ConcoctionDatabase.refreshConcoctions(false);
  }

  public static boolean canMake(final Concoction conc) {
    String name = conc.getName().toLowerCase();

    return mayamCommand.availableResonances().contains(name);
  }
}
