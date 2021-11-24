package net.sourceforge.kolmafia.textui.function;

import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import net.sourceforge.kolmafia.webui.RelayServer;

public class VisitUrlFunction extends LibraryFunction {
  static final String name = "visit_url";

  @LibraryFunctionOverload
  public static Value exec(ScriptRuntime controller) {
    return exec(controller, null);
  }

  @LibraryFunctionOverload
  public static Value exec(
      ScriptRuntime controller, @LibraryFunctionParameter("string") final Value url) {
    return exec(controller, url, DataTypes.TRUE_VALUE);
  }

  @LibraryFunctionOverload
  public static Value exec(
      ScriptRuntime controller,
      @LibraryFunctionParameter("string") final Value url,
      @LibraryFunctionParameter("boolean") final Value usePostMethod) {
    return exec(controller, url, usePostMethod, DataTypes.FALSE_VALUE);
  }

  @LibraryFunctionOverload
  public static Value exec(
      ScriptRuntime controller,
      @LibraryFunctionParameter("string") final Value url,
      @LibraryFunctionParameter("boolean") final Value usePostMethod,
      @LibraryFunctionParameter("boolean") final Value encoded) {
    StringBuffer buffer = new StringBuffer();
    Value returnValue = new Value(DataTypes.BUFFER_TYPE, "", buffer);

    RelayRequest relayRequest = controller.getRelayRequest();
    boolean inRelayOverride = relayRequest != null;

    // If we're in a relay override, use a RelayRequest rather than a GenericRequest
    GenericRequest request = inRelayOverride ? new RelayRequest(false) : new GenericRequest("");

    // Handle collecting relay page contents
    if (url != null) {
      // Build the desired URL
      request.constructURLString(
          RelayServer.trimPrefix(url.toString()),
          usePostMethod.booleanValue(),
          encoded.booleanValue());
      if (GenericRequest.shouldIgnore(request)) {
        return returnValue;
      }

      // If we are not in a relay script, ignore a request to an unstarted fight
      if (!inRelayOverride
          && request.getPath().equals("fight.php")
          && FightRequest.getCurrentRound() == 0) {
        return returnValue;
      }
    }

    // Post the request and get the response!  Note that if we are
    // in a relay script, we have to follow all redirection here.
    while (true) {
      RequestThread.postRequest(request);
      if (!inRelayOverride || request.redirectLocation == null) {
        break;
      }
      request.constructURLString(request.redirectLocation, false, false);
      if (KoLmafiaASH.getClientHTML((RelayRequest) request)) {
        break;
      }
    }

    if (request.responseText != null) {
      buffer.append(request.responseText);
    }

    return returnValue;
  }
}
