package net.sourceforge.kolmafia.swingui.menu;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.request.ZapRequest;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class WandZapMenuItem extends ThreadedMenuItem {
  public WandZapMenuItem() {
    super("Wand-Zap Item", new WandZapListener());
  }

  private static class WandZapListener extends ThreadedListener {
    @Override
    protected void execute() {
      if (KoLCharacter.getZapper() == null) {
        return;
      }

      AdventureResult selectedValue =
          (AdventureResult)
              InputFieldUtilities.input("Let's explodey my wand!", ZapRequest.getZappableItems());
      if (selectedValue == null) {
        return;
      }

      RequestThread.postRequest(new ZapRequest(selectedValue));
    }
  }
}
