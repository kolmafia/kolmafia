package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CampAwayRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.ChateauRequest;
import net.sourceforge.kolmafia.request.FalloutShelterRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.Limitmode;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CampgroundCommand extends AbstractCommand {
  public CampgroundCommand() {
    this.usage = " rest | <etc.> [<numTimes>] - perform campground actions.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    String[] parameterList = parameters.split("\\s+");

    String command = parameterList[0];
    GenericRequest request = null;

    if (command.equals("rest") && ChateauRequest.chateauRestUsable()) {
      request = new ChateauRequest("chateau_restbox");
    } else if (command.equals("rest") && CampAwayRequest.campAwayTentRestUsable()) {
      request = new CampAwayRequest(CampAwayRequest.TENT);
    } else {
      if (!Limitmode.limitCampground() && !KoLCharacter.isEd()) {
        if (!KoLCharacter.inNuclearAutumn()) {
          request = new CampgroundRequest(command);
        } else {
          if (command.equals("rest")) {
            command = "vault1";
          } else if (command.equals("terminal")) {
            command = "vault_term";
          }
          request = new FalloutShelterRequest(command);
        }
      }
    }

    int count = 1;

    if (parameterList.length > 1) {
      if (command.equals("rest") && parameterList[1].equals("free")) {
        count = Preferences.getInteger("timesRested") >= KoLCharacter.freeRestsAvailable() ? 0 : 1;
      } else {
        count = StringUtilities.parseInt(parameterList[1]);
      }
    }

    KoLmafia.makeRequest(request, count);
  }
}
