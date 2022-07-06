package net.sourceforge.kolmafia.textui.command;

import java.util.HashSet;
import java.util.Map;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.UmbrellaRequest.Form;
import net.sourceforge.kolmafia.session.InventoryManager;

public class UmbrellaCommand extends AbstractCommand implements ModeCommand {
  public UmbrellaCommand() {
    this.usage =
        "[ml | item | dr | weapon | spell | nc | broken | forward | bucket | pitchfork | twirling | cocoon] - fold your Umbrella";
  }

  private static Map<String, String> SHORTHAND_MAP =
      Map.ofEntries(
          Map.entry("ml", "broken"),
          Map.entry("dr", "forward-facing"),
          Map.entry("item", "bucket style"),
          Map.entry("weapon", "pitchfork style"),
          Map.entry("spell", "constantly twirling"),
          Map.entry("nc", "cocoon"));

  public Form getForm(final String parameter) {
    return Form.find(normalize(parameter));
  }

  @Override
  public String normalize(final String parameter) {
    return SHORTHAND_MAP.getOrDefault(parameter, parameter);
  }

  @Override
  public boolean validate(final String command, final String parameter) {
    return getForm(parameter) != null;
  }

  public HashSet<String> getModes() {
    return new HashSet<>(SHORTHAND_MAP.values());
  }

  @Override
  public void run(final String cmd, String parameter) {
    if (!InventoryManager.hasItem(ItemPool.UNBREAKABLE_UMBRELLA)
        && !KoLCharacter.hasEquipped(ItemPool.UNBREAKABLE_UMBRELLA)) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You need an Unbreakable Umbrella first.");
      return;
    }

    parameter = parameter.trim();

    if (parameter.equals("")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "What state do you want to fold your umbrella to?");
      return;
    }

    Form umbrellaForm = getForm(parameter);

    if (umbrellaForm == null) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "I don't understand what Umbrella form " + parameter + " is.");
      return;
    }

    KoLmafia.updateDisplay("Folding umbrella");
    GenericRequest request = new GenericRequest("inventory.php?action=useumbrella", false);
    RequestThread.postRequest(request);

    request = new GenericRequest("choice.php");
    request.addFormField("whichchoice", "1466");
    request.addFormField("option", Integer.toString(umbrellaForm.id));
    request.addFormField("pwd", GenericRequest.passwordHash);
    RequestThread.postRequest(request);

    KoLCharacter.updateStatus();
  }
}
