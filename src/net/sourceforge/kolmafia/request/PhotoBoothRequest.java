package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.textui.command.PhotoBoothCommand;

public class PhotoBoothRequest extends CreateItemRequest {
  private static PhotoBoothCommand photoBoothCommand = new PhotoBoothCommand();

  public PhotoBoothRequest(final Concoction conc) {
    super("choice.php", conc);
  }

  @Override
  public void run() {
    String name = this.getName();

    KoLmafia.updateDisplay("Creating 1 " + name + "...");

    photoBoothCommand.run("photobooth", "item " + name);
    ConcoctionDatabase.refreshConcoctions(false);
  }

  public static int canMake(final Concoction conc) {
    int equip = Preferences.getInteger("_photoBoothEquipment");

    return 3 - equip;
  }
}
