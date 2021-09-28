package net.sourceforge.kolmafia.session;

import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.RelayRequest;

public class ActionBarManager {
  private static String initialJSONString = "";
  private static String currentJSONString = "";

  public static final void updateJSONString(RelayRequest request) {
    String action = request.getFormField("action");

    // If there's a fetch, return the JSON object.  Ideally,
    // we would be able to send 304 messages, but that's not
    // possible because 'd' is always the current time.

    if (action != null && action.equalsIgnoreCase("fetch")) {
      request.pseudoResponse("HTTP/1.1 200 OK", ActionBarManager.currentJSONString);
      return;
    }

    // Otherwise, assume it's a set, and cache the JSON object
    // locally and submit it to the server on logout.

    String bar = request.getFormField("bar");
    if (bar != null) {
      ActionBarManager.currentJSONString = bar;
    }

    RequestThread.postRequest(request);
    return;
  }

  public static final void loadJSONString() {
    GenericRequest request = new GenericRequest("actionbar.php?action=fetch");
    RequestThread.postRequest(request);

    if (request.responseText != null) {
      ActionBarManager.initialJSONString = request.responseText;
      ActionBarManager.currentJSONString = request.responseText;
    }
  }
}
