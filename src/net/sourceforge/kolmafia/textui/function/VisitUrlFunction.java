package net.sourceforge.kolmafia.textui.function;

import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import net.sourceforge.kolmafia.webui.RelayServer;

public class VisitUrlFunction extends LibraryClassFunction {
  public VisitUrlFunction() {
    super("visit_url");
  }

  @LibraryFunctionOverload(returns = "buffer")
  public Value exec(ScriptRuntime controller) {
    return new Value(exec(controller, (String) null));
  }

  @LibraryFunctionOverload(returns = "buffer")
  public Value exec(ScriptRuntime controller, @LibraryFunctionParameter("string") final Value url) {
    return new Value(exec(controller, url.toString()));
  }

  @LibraryFunctionOverload(returns = "buffer")
  public Value exec(
      ScriptRuntime controller,
      @LibraryFunctionParameter("string") final Value url,
      @LibraryFunctionParameter("boolean") final Value usePostMethod) {
    return new Value(exec(controller, url.toString(), usePostMethod.booleanValue()));
  }

  @LibraryFunctionOverload(returns = "buffer")
  public Value exec(
      ScriptRuntime controller,
      @LibraryFunctionParameter("string") final Value url,
      @LibraryFunctionParameter("boolean") final Value usePostMethod,
      @LibraryFunctionParameter("boolean") final Value encoded) {
    return new Value(exec(controller, url.toString(), usePostMethod.booleanValue(), encoded.booleanValue()));
  }

  public StringBuffer exec(
          ScriptRuntime controller,
          String url
  ) {
    return exec(controller, url, true, false);
  }

  public StringBuffer exec(
      ScriptRuntime controller,
      String url,
      boolean usePostMethod
  ) {
    return exec(controller, url, usePostMethod, false);
  }

  public StringBuffer exec(
      ScriptRuntime controller,
      String url,
      boolean usePostMethod,
      boolean encoded
  ) {
    StringBuffer buffer = new StringBuffer();
    RelayRequest relayRequest = controller.getRelayRequest();
    boolean inRelayOverride = relayRequest != null;

    GenericRequest request;

    // Handle collecting relay page contents
    if (url == null) {
      if (!inRelayOverride) {
        // With no URL and no relay override, we've got nothing to do
        return buffer;
      } else {
        // If we're in a relay override, use a RelayRequest rather than a GenericRequest
        request = relayRequest;
      }
    } else {
      request = inRelayOverride ? new RelayRequest(false) : new GenericRequest("");

      // Build the desired URL
      request.constructURLString(
          RelayServer.trimPrefix(url),
          usePostMethod,
          encoded);
      if (GenericRequest.shouldIgnore(request)) {
        return buffer;
      }

      // If we are not in a relay script, ignore a request to an unstarted fight
      if (!inRelayOverride
          && request.getPath().equals("fight.php")
          && FightRequest.getCurrentRound() == 0) {
        return buffer;
      }
    }

    // Post the request and get the response!  Note that if we are
    // in a relay script, we have to follow all redirection here.
    while (request != null) {
      RequestThread.postRequest(request);
      if (!inRelayOverride || request.redirectLocation == null) {
        break;
      }
      request.constructURLString(request.redirectLocation, false, false);
      if (KoLmafiaASH.getClientHTML((RelayRequest) request)) {
        break;
      }
    }

    if (request != null && request.responseText != null) {
      buffer.append(request.responseText);
    }

    return buffer;
  }
}
