package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.request.GenericRequest;

public class VisitURLCommand extends AbstractCommand {
  public VisitURLCommand() {
    usage = " <URL> - show text results from visiting URL.";
  }

  @Override
  public void run(String cmd, String parameters) {
    String url = cmd.equals("text") ? parameters : cmd;

    GenericRequest visitor = new GenericRequest(url);

    if (GenericRequest.shouldIgnore(visitor)) {
      return;
    }

    RequestThread.postRequest(visitor);
    String text = visitor.responseText;

    if (text != null && cmd.equals("text")) {
      KoLmafiaCLI.showHTML(text);
    }
  }
}
