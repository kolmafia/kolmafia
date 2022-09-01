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
          Map.entry("ghostasaurus", 3),
          Map.entry("spikolodon", 4),
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
    return getChoiceForParameters(parameters) > 0;
  }

  @Override
  public String normalize(String parameters) {
    return parameters;
  }

  public int getChoiceForParameters(final String parameters) {
    if (parameters == null) return 0;
    return MODES.entrySet().stream()
        .filter(e -> parameters.contains(e.getKey()))
        .findAny()
        .map(Map.Entry::getValue)
        .orElse(getChoiceForParameters(ALIASES.getOrDefault(parameters, null)));
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
      KoLmafia.updateDisplay("The mode " + parameters + " was not recognised");
      return;
    }

    RequestThread.postRequest(new GenericRequest("inventory.php?action=jparka"));
    RequestThread.postRequest(new GenericRequest("choice.php?whichchoice=1481&option=" + choice));

    // If still in choice, call 'Leave it alone' to exit
    if (ChoiceManager.handlingChoice && ChoiceManager.lastChoice == 1481) {
      RequestThread.postRequest(new GenericRequest("choice.php?whichchoice=1449&option=6"));
    }

    KoLmafia.updateDisplay("Your parka is now set to " + parameters + " mode.");
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
