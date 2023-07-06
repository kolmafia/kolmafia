package net.sourceforge.kolmafia.textui.command;

import java.util.List;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.session.InventoryManager;

public class LoathingIdolCommand extends AbstractCommand {
  public LoathingIdolCommand() {
    this.usage =
        " [pop | moxie | init | ballad | combat | rhyme | item | country | exp | res] - get the indicated buff";
  }

  public static final List<Integer> MICROPHONES =
      List.of(
          ItemPool.LOATHING_IDOL_MICROPHONE_25,
          ItemPool.LOATHING_IDOL_MICROPHONE_50,
          ItemPool.LOATHING_IDOL_MICROPHONE_75,
          ItemPool.LOATHING_IDOL_MICROPHONE);

  public static int getUsableMicrophone() {
    for (var microphone : MICROPHONES) {
      if (InventoryManager.hasItem(microphone)) {
        return microphone;
      }
    }
    return -1;
  }

  @Override
  public void run(final String cmd, final String parameters) {
    var item = getUsableMicrophone();
    if (item == -1) {
      KoLmafia.updateDisplay("You need a Loathing Idol Microphone first.");
      return;
    }

    int choice = -1;
    if (parameters.contains("pop") || parameters.contains("moxie") || parameters.contains("init")) {
      choice = 1;
    } else if (parameters.contains("ballad") || parameters.contains("combat")) {
      choice = 2;
    } else if (parameters.contains("rhyme") || parameters.contains("item")) {
      choice = 3;
    } else if (parameters.contains("country")
        || parameters.contains("exp")
        || parameters.contains("res")) {
      choice = 4;
    }
    if (choice < 1) {
      KoLmafia.updateDisplay(MafiaState.ERROR, parameters + " is not a valid option.");
      return;
    }

    int previousChoice = Preferences.getInteger("choiceAdventure1505");
    Preferences.setInteger("choiceAdventure1505", choice);
    UseItemRequest useMicrophone = UseItemRequest.getInstance(item);
    RequestThread.postRequest(useMicrophone);
    Preferences.setInteger("choiceAdventure1505", previousChoice);
  }
}
