package net.sourceforge.kolmafia.textui.command;

import java.util.Map;
import java.util.Set;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.InventoryManager;

public class JurassicParkaCommand extends AbstractCommand implements ModeCommand {
  public JurassicParkaCommand() {
    this.usage = " [] - pull a dino tab on your Jurassic Parka";
  }

  public static final Map<String, Integer> MODES =
      Map.ofEntries(
          Map.entry("ghostasaurus", 1),
          Map.entry("dilophosaur", 2),
          Map.entry("pterodactyl", 3),
          Map.entry("kachungasaur", 4),
          Map.entry("spikolodon", 5));

  @Override
  public boolean validate(final String command, final String parameters) {
    return getChoiceForParameters(parameters) > 0;
  }

  @Override
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
    if (!InventoryManager.hasItem(ItemPool.JURASSIC_PARKA)) {
      KoLmafia.updateDisplay("You need a Jurassic Parka to pull tabs on your Jurassic Parka.");
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
  }
}
