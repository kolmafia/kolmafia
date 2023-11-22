package net.sourceforge.kolmafia.textui.command;

import java.util.Map;
import java.util.Set;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.InventoryManager;

public class BackupCameraCommand extends AbstractCommand implements ModeCommand {
  public BackupCameraCommand() {
    this.usage = " [ml | meat | init | (reverser on | off )] - set your backup camera mode";
  }

  public static final Map<String, Integer> MODES =
      Map.ofEntries(Map.entry("ml", 1), Map.entry("meat", 2), Map.entry("init", 3));

  @Override
  public boolean validate(final String command, final String parameters) {
    return getChoiceForParameters(parameters) > 0;
  }

  public String normalize(String parameters) {
    return parameters;
  }

  public int getChoiceForParameters(final String parameters) {
    return MODES.entrySet().stream()
        .filter(e -> parameters.contains(e.getKey()))
        .findAny()
        .map(Map.Entry::getValue)
        .orElse(
            parameters.contains("reverse")
                ? ((parameters.contains("off") || parameters.contains("disable")) ? 5 : 4)
                : 0);
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

    int choice = getChoiceForParameters(parameters);

    if (choice == 0) {
      KoLmafia.updateDisplay("The command " + parameters + " was not recognised");
      return;
    }

    RequestThread.postRequest(new GenericRequest("inventory.php?action=bcmode"));
    RequestThread.postRequest(new GenericRequest("choice.php?whichchoice=1449&option=" + choice));

    // If still in choice, call 'Leave it alone' to exit
    if (ChoiceManager.handlingChoice && ChoiceManager.lastChoice == 1449) {
      RequestThread.postRequest(new GenericRequest("choice.php?whichchoice=1449&option=6"));
    }

    KoLmafia.updateDisplay(
        "Your backup camera "
            + switch (choice) {
              case 1, 2, 3 -> "is now set to " + parameters + " mode";
              case 4 -> "reverser is now on (text won't be backwards)";
              case 5 -> "reverser is now on (fight will now be backwards! enjoy!)";
              default -> "feels ignored";
            });
  }
}
