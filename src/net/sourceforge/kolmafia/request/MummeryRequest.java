package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;

public class MummeryRequest extends GenericRequest {
  public MummeryRequest(int choice) {
    super("choice.php");

    this.addFormField("whichchoice", "1271");
    this.addFormField("option", String.valueOf(choice));
  }

  @Override
  protected boolean shouldFollowRedirect() {
    return true;
  }

  @Override
  public void run() {
    if (!KoLConstants.inventory.contains(ItemPool.get(ItemPool.MUMMING_TRUNK, 1))) {
      KoLmafia.updateDisplay("You need a mumming trunk first.");
      return;
    }
    if (KoLCharacter.currentFamiliar == FamiliarData.NO_FAMILIAR) {
      KoLmafia.updateDisplay(
          KoLConstants.MafiaState.ERROR, "You need to have a familiar to put a costume on.");
      return;
    }

    GenericRequest useRequest = new GenericRequest("inv_use.php");
    useRequest.addFormField("whichitem", String.valueOf(ItemPool.MUMMING_TRUNK));
    RequestThread.postRequest(useRequest);

    super.run();
  }

  public static final void parseResponse(final int choice, final String responseText) {
    if (!responseText.contains("You dress")) {
      // We shouldn't be here
      return;
    }

    if (KoLCharacter.currentFamiliar == FamiliarData.NO_FAMILIAR) {
      // This probably can't happen, but just in case...
      return;
    }

    String mods = Preferences.getString("_mummeryMods");
    String familiar = KoLCharacter.currentFamiliar.getRace();
    if (mods.contains(familiar)) {
      // We are replacing the modifier for our current familiar
      String[] pieces = mods.split(",");
      mods = "";
      for (String piece : pieces) {
        if (!piece.contains(familiar)) {
          mods += piece + ",";
        }
      }
    }
    int familiarId = KoLCharacter.currentFamiliar.getId();
    int mod1, mod2;
    switch (choice) {
      case 1:
        mod1 = 15;
        if (FamiliarDatabase.hasAttribute(familiarId, "hands")) {
          mod1 = 30;
        }
        mods += "Meat Drop: [" + mod1 + "*fam(" + familiar + ")],";
        break;
      case 2:
        mod1 = 4;
        mod2 = 5;
        if (FamiliarDatabase.hasAttribute(familiarId, "wings")) {
          mod1 = 6;
          mod2 = 10;
        }
        mods +=
            "MP Regen Min: ["
                + mod1
                + "*fam("
                + familiar
                + ")], MP Regen Max: ["
                + mod2
                + "*fam("
                + familiar
                + ")],";
        break;
      case 3:
        mod1 = 3;
        if (FamiliarDatabase.hasAttribute(familiarId, "animal")) {
          mod1 = 4;
        }
        mods += "Experience (Muscle): [" + mod1 + "*fam(" + familiar + ")],";
        break;
      case 4:
        mod1 = 15;
        if (FamiliarDatabase.hasAttribute(familiarId, "clothes")) {
          mod1 = 25;
        }
        mods += "Item Drop: [" + mod1 + "*fam(" + familiar + ")],";
        break;
      case 5:
        mod1 = 3;
        if (FamiliarDatabase.hasAttribute(familiarId, "eyes")) {
          mod1 = 4;
        }
        mods += "Experience (Mysticality): [" + mod1 + "*fam(" + familiar + ")],";
        break;
      case 6:
        mod1 = 8;
        mod2 = 10;
        if (FamiliarDatabase.hasAttribute(familiarId, "mechanical")) {
          mod1 = 18;
          mod2 = 20;
        }
        mods +=
            "HP Regen Min: ["
                + mod1
                + "*fam("
                + familiar
                + ")], HP Regen Max: ["
                + mod2
                + "*fam("
                + familiar
                + ")],";
        break;
      case 7:
        mod1 = 2;
        if (FamiliarDatabase.hasAttribute(familiarId, "sleazy")) {
          mod1 = 4;
        }
        mods += "Experience (Moxie): [" + mod1 + "*fam(" + familiar + ")],";
        break;
    }

    String message = "Costume " + choice + " applied to " + familiar;
    RequestLogger.printLine(message);
    RequestLogger.updateSessionLog(message);
    Preferences.setString("_mummeryUses", Preferences.getString("_mummeryUses") + choice + ",");
    Preferences.setString("_mummeryMods", mods);

    KoLCharacter.recalculateAdjustments();
    KoLCharacter.updateStatus();
  }
}
