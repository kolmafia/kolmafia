package net.sourceforge.kolmafia.textui.command;

import java.util.Map;
import java.util.Set;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.InventoryManager;

public class JurassicParkaCommand extends AbstractCommand implements ModeCommand {
  public JurassicParkaCommand() {
    this.usage =
        " [kachungasaur | cold | dilophosaur | stench | ghostasaurus | spooky | spikolodon | sleaze | pterodactyl | hot] - pull a dino tab on your Jurassic Parka";
  }

  public static final Map<String, Integer> MODES =
      Map.ofEntries(
          Map.entry("kachungasaur", 1),
          Map.entry("dilophosaur", 2),
          Map.entry("spikolodon", 3),
          Map.entry("ghostasaurus", 4),
          Map.entry("pterodactyl", 5));

  public static final Map<String, String> ALIASES =
      Map.ofEntries(
          Map.entry("spooky", "ghostasaurus"),
          Map.entry("stench", "dilophosaur"),
          Map.entry("hot", "pterodactyl"),
          Map.entry("cold", "kachungasaur"),
          Map.entry("sleaze", "spokolodon"));

  @Override
  public boolean validate(final String command, final String parameters) {
    return getChoiceForParameters(normalize(parameters)) > 0;
  }

  @Override
  public String normalize(String parameters) {
    parameters = parameters.trim().toLowerCase();
    return ALIASES.getOrDefault(parameters, parameters);
  }

  public int getChoiceForParameters(final String parameters) {
    return MODES.getOrDefault(parameters, 0);
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

    var mode = normalize(parameters);

    int choice = getChoiceForParameters(mode);

    if (choice == 0) {
      KoLmafia.updateDisplay("The mode " + parameters + " was not recognised");
      return;
    }

    RequestThread.postRequest(new GenericRequest("inventory.php?action=jparka"));
    RequestThread.postRequest(new GenericRequest("choice.php?whichchoice=1481&option=" + choice));

    // If still in choice, call 'Leave it alone' to exit
    if (ChoiceManager.handlingChoice && ChoiceManager.lastChoice == 1481) {
      RequestThread.postRequest(new GenericRequest("choice.php?whichchoice=1449&option=6"));
    }

    KoLmafia.updateDisplay("Your parka is now set to " + mode + " mode.");
  }

  private static void setMode(final String mode) {
    Preferences.setString("parkaMode", mode);
  }

  public static void parseChoice(final int decision) {
    MODES.entrySet().stream()
        .filter(e -> e.getValue().equals(decision))
        .findAny()
        .ifPresent(e -> setMode(e.getKey()));
  }
}
