package net.sourceforge.kolmafia.textui.command;

import java.util.Map;
import java.util.Set;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.InventoryManager;

public class BackupCameraCommand extends AbstractModeCommand {
  public BackupCameraCommand() {
    this.usage = " [ml | meat | init | (reverser on | off )] - set your backup camera mode";
  }

  public static final Map<String, Integer> MODES =
      Map.ofEntries(Map.entry("ml", 1), Map.entry("meat", 2), Map.entry("init", 3));

  @Override
  public boolean validate(final String command, final String parameters) {
    return MODES.containsKey(parameters);
  }

  public Set<String> getModes() {
    return MODES.keySet();
  }

  @Override
  public void run(final String cmd, final String parameters) {
    if (!InventoryManager.hasItem(ItemPool.BACKUP_CAMERA)) {
      KoLmafia.updateDisplay("You need a backup camera first.");
      return;
    }

    int choice =
        MODES.entrySet().stream()
            .filter(e -> parameters.contains(e.getKey()))
            .findAny()
            .map(Map.Entry::getValue)
            .orElse(0);

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
