package net.sourceforge.kolmafia.session;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.GourdRequest;

public class GourdManager {
  public static void tradeGourdItems() {
    RequestThread.postRequest(new GourdRequest());

    AdventureResult item = GourdRequest.gourdItem(5);
    int neededCount = Preferences.getInteger("gourdItemCount");

    GenericRequest gourdVisit = new GourdRequest(true);

    while (neededCount <= 25 && neededCount <= item.getCount(KoLConstants.inventory)) {
      RequestThread.postRequest(gourdVisit);
      neededCount++;
    }

    int totalProvided = 0;
    for (int i = 5; i < neededCount; ++i) {
      totalProvided += i;
    }

    KoLmafia.updateDisplay(
        "Gourd trading complete (" + totalProvided + " " + item.getName() + "s given so far).");
  }
}
