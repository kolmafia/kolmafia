package net.sourceforge.kolmafia.textui.command;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.FamiliarRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.UmbrellaRequest.UmbrellaMode;
import net.sourceforge.kolmafia.session.InventoryManager;

public class UmbrellaCommand extends AbstractCommand implements ModeCommand {
  public UmbrellaCommand() {
    this.usage =
        "[ml | item | dr | weapon | spell | nc | broken | forward | bucket | pitchfork | twirling | cocoon] - fold your Umbrella";
  }

  public UmbrellaMode getMode(final String parameter) {
    return UmbrellaMode.find(normalize(parameter));
  }

  public String normalize(final String parameter) {
    var mode = UmbrellaMode.findByShortHand(parameter);
    return mode == null ? parameter : mode.getName();
  }

  @Override
  public boolean validate(final String command, final String parameter) {
    return getMode(parameter) != null;
  }

  public Set<String> getModes() {
    return Arrays.stream(UmbrellaMode.values())
        .map(UmbrellaMode::getName)
        .collect(Collectors.toSet());
  }

  @Override
  public void run(final String cmd, String parameter) {
    if (!InventoryManager.hasItem(ItemPool.UNBREAKABLE_UMBRELLA)
        && !KoLCharacter.hasEquipped(ItemPool.UNBREAKABLE_UMBRELLA)) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You need an Unbreakable Umbrella first.");
      return;
    }

    KoLCharacter.ownedFamiliar(FamiliarPool.LEFT_HAND)
        .ifPresent(
            (lefty) -> {
              if (!KoLCharacter.getFamiliar().equals(lefty)
                  && lefty.getItem().equals(ItemPool.get(ItemPool.UNBREAKABLE_UMBRELLA))) {
                RequestThread.postRequest(new FamiliarRequest(lefty, null));
              }
            });

    parameter = parameter.trim();

    if (parameter.equals("")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "What state do you want to fold your umbrella to?");
      return;
    }

    UmbrellaMode mode = getMode(parameter);

    if (mode == null) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "I don't understand what Umbrella form " + parameter + " is.");
      return;
    }

    KoLmafia.updateDisplay("Folding umbrella");
    GenericRequest request = new GenericRequest("inventory.php?action=useumbrella", false);
    RequestThread.postRequest(request);

    request = new GenericRequest("choice.php");
    request.addFormField("whichchoice", "1466");
    request.addFormField("option", Integer.toString(mode.getId()));
    request.addFormField("pwd", GenericRequest.passwordHash);
    RequestThread.postRequest(request);

    KoLCharacter.updateStatus();
  }
}
