package net.sourceforge.kolmafia.textui.command;

import java.util.Arrays;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.InventoryManager;

public class BackupCameraCommand extends AbstractModeCommand {
  public BackupCameraCommand() {
    this.usage = " [ml | meat | init | (reverser on | off )] - set your backup camera mode";
  }

  public static final String[][] MODE = {{"ml", "1"}, {"meat", "2"}, {"init", "3"}};

  @Override
  public boolean validate(final String command, final String parameters) {
    return Arrays.stream(MODE).anyMatch(m -> m[0].equals(parameters));
  }

  @Override
  public void run(final String cmd, final String parameters) {
    if (!InventoryManager.hasItem(ItemPool.BACKUP_CAMERA)) {
      KoLmafia.updateDisplay("You need a backup camera first.");
      return;
    }

    int choice = 0;

    for (String[] mode : MODE) {
      if (parameters.contains(mode[0])) {
        choice = Integer.parseInt(mode[1]);
      }
    }

    if (choice == 0 && parameters.contains("reverse")) {
      if (parameters.contains("off") || parameters.contains("disable")) {
        choice = 5;
      } else {
        choice = 4;
      }
    }

    RequestThread.postRequest(new GenericRequest("inventory.php?action=bcmode"));
    RequestThread.postRequest(new GenericRequest("choice.php?whichchoice=1449&option=" + choice));
  }
}
