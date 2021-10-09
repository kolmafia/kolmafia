package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.NemesisManager;

public class NemesisCommand extends AbstractCommand {
  public NemesisCommand() {
    this.usage = " strips - Look at the paper strips.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    String[] tokens = parameters.split("\\s+");
    if (tokens.length < 1) {
      return;
    }

    String option = tokens[0];

    if (option.equals("password")) {
      String password = NemesisManager.getPassword();
      if (password == null) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "You don't have all the paper strips.");
        return;
      }

      RequestLogger.printLine(password);
      return;
    }

    if (option.equals("strips")) {
      NemesisManager.identifyPaperStrips();
      for (int i = 0; i < NemesisManager.PAPER_STRIPS.length; ++i) {
        AdventureResult it = NemesisManager.PAPER_STRIPS[i];
        String name = it.getName();
        String id = Preferences.getString("lastPaperStrip" + it.getItemId());
        RequestLogger.printLine(name + " = " + id);
      }
    }
  }
}
