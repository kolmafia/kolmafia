package net.sourceforge.kolmafia.textui.command;

import java.util.Set;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.InventoryManager;

public class LedCandleCommand extends AbstractCommand implements ModeCommand {
  public LedCandleCommand() {
    this.usage =
        " [disco | item | ultraviolet | meat | reading | stats | red | attack] - tweak your LED candle";
  }

  @Override
  public boolean validate(String command, String parameters) {
    return parseInput(parameters) > 0;
  }

  @Override
  public Set<String> getModes() {
    return Set.of("disco", "ultraviolet", "reading", "red light");
  }

  private int parseInput(String parameter) {
    if (parameter.equals("disco") || parameter.equals("item")) {
      return 1;
    } else if (parameter.startsWith("ultra") || parameter.equals("meat")) {
      return 2;
    } else if (parameter.equals("reading") || "stats".startsWith(parameter)) {
      return 3;
    } else if ("red light".startsWith(parameter) || "attacks".startsWith(parameter)) {
      return 4;
    }
    return -1;
  }

  @Override
  public void run(final String cmd, String parameter) {
    if (!InventoryManager.equippedOrInInventory(ItemPool.LED_CANDLE)) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You need a LED candle first.");
      return;
    }

    parameter = parameter.trim();

    if (parameter.isEmpty()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Which tweak do you want to make?");
      return;
    }

    int tweak = parseInput(parameter);
    if (tweak == -1) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "I don't understand what tweak " + parameter + " is.");
      return;
    }

    KoLmafia.updateDisplay("Tweaking LED Candle");

    RequestThread.postRequest(
        new GenericRequest(
            "inventory.php?action=tweakjill&pwd=" + GenericRequest.passwordHash, false));
    RequestThread.postRequest(new GenericRequest("choice.php?whichchoice=1509&option=" + tweak));
  }
}
