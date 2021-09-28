package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;

public class GapCommand extends AbstractCommand {
  public GapCommand() {
    this.usage = " [skill|structure|vision|speed|accuracy] - get a Greatest American Pants buff.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    GapCommand.visit(parameters);
  }

  public static void visit(final String parameters) {
    if (Preferences.getInteger("_gapBuffs") >= 5) {
      KoLmafia.updateDisplay("You're out of superpowers.");
      return;
    }

    if (!KoLCharacter.hasEquipped(ItemPool.get(ItemPool.GREAT_PANTS, 1))) {
      KoLmafia.updateDisplay("You need to equip your superpants first.");
      return;
    }

    int choicenumber = 0;
    String buffname = "";
    if (parameters.toLowerCase().indexOf("skill") != -1 || parameters.equals("1")) {
      choicenumber = 1;
      buffname = "Super Skill";
    } else if (parameters.toLowerCase().indexOf("structure") != -1 || parameters.equals("2")) {
      choicenumber = 2;
      buffname = "Super Structure";
    } else if (parameters.toLowerCase().indexOf("vision") != -1 || parameters.equals("3")) {
      choicenumber = 3;
      buffname = "Super Vision";
    } else if (parameters.toLowerCase().indexOf("speed") != -1 || parameters.equals("4")) {
      choicenumber = 4;
      buffname = "Super Speed";
    } else if (parameters.toLowerCase().indexOf("accuracy") != -1 || parameters.equals("5")) {
      choicenumber = 5;
      buffname = "Super Accuracy";
    }

    if (choicenumber == 0) {
      KoLmafia.updateDisplay((5 - Preferences.getInteger("_gapBuffs")) + " superbuffs remaining.");
      return;
    }

    KoLmafia.updateDisplay("Superpower time!");
    RequestThread.postRequest(new GenericRequest("inventory.php?action=activatesuperpants"));
    RequestThread.postRequest(
        new GenericRequest(
            "choice.php?pwd&whichchoice=508&option="
                + choicenumber
                + "&choiceform"
                + choicenumber
                + "="
                + buffname));
  }
}
