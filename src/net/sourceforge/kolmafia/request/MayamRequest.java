package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.textui.command.MayamCommand;

public class MayamRequest extends CreateItemRequest {
  private static MayamCommand _mayamCommand = new MayamCommand();

  public MayamRequest(final Concoction conc) {
    super("choice.php", conc);
  }

  @Override
  public void run() {
    String name = this.getName();

    KoLmafia.updateDisplay("Creating 1 " + name + "...");

    _mayamCommand.run("mayam", "resonance " + name);
    ConcoctionDatabase.refreshConcoctions(false);
  }

  public static boolean canMake(final Concoction conc) {
    String name = conc.getName().toLowerCase();

    return _mayamCommand.availableResonances().contains(name);
  }
}
