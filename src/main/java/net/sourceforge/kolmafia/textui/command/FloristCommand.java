package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.request.FloristRequest;
import net.sourceforge.kolmafia.request.FloristRequest.Florist;

public class FloristCommand extends AbstractCommand {
  public FloristCommand() {
    this.usage = " plant [plantname] - Add the plant to your current location";
  }

  @Override
  public void run(final String cmd, String parameter) {
    if (!FloristRequest.haveFlorist()) {
      KoLmafia.updateDisplay("You don't have a Florist Friar");
      return;
    }

    if (parameter.startsWith("plant ")) {
      parameter = parameter.substring(6).trim();
      Florist plant = Florist.getFlower(parameter);
      if (plant == null) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Unrecognized plant: " + parameter);
        return;
      }
      RequestThread.postRequest(new FloristRequest(plant.id()));
    }
  }
}
