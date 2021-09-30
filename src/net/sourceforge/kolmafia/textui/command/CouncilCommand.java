package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.request.CouncilRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CouncilCommand extends AbstractCommand {
  public static CouncilRequest COUNCIL_VISIT = new CouncilRequest();

  public CouncilCommand() {
    this.usage = " - visit the Council to advance quest progress.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    COUNCIL_VISIT = new CouncilRequest();
    RequestThread.postRequest(COUNCIL_VISIT);

    KoLmafiaCLI.showHTML(
        StringUtilities.singleStringReplace(
            COUNCIL_VISIT.responseText, "<a href=\"town.php\">Back to Seaside Town</a>", ""));
  }
}
