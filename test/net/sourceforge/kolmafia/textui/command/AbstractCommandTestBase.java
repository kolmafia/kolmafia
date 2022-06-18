package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import internal.helpers.RequestLoggerOutput;
import internal.network.RequestBodyReader;
import java.net.URLDecoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.StaticEntity;

public abstract class AbstractCommandTestBase {
  protected String command = "abort";

  public String execute(final String params) {
    return execute(params, false);
  }

  public String execute(final String params, final boolean check) {
    RequestLoggerOutput.startStream();
    var cli = new KoLmafiaCLI(System.in);
    KoLmafiaCLI.isExecutingCheckOnlyCommand = check;
    cli.executeCommand(this.command, params);
    KoLmafiaCLI.isExecutingCheckOnlyCommand = false;
    return RequestLoggerOutput.stopStream();
  }

  public static void assertState(final MafiaState state) {
    assertEquals(state, StaticEntity.getContinuationState());
  }

  public static void assertContinueState() {
    assertState(MafiaState.CONTINUE);
  }

  public static void assertErrorState() {
    assertState(MafiaState.ERROR);
  }

  public static void assertGetRequest(HttpRequest request, String path, String query) {
    assertThat(request.method(), equalTo("GET"));
    var uri = request.uri();
    assertThat(uri.getPath(), equalTo(path));
    assertThat(uri.getQuery(), equalTo(query));
  }

  public static void assertPostRequest(HttpRequest request, String path, String body) {
    assertThat(request.method(), equalTo("POST"));
    var uri = request.uri();
    assertThat(uri.getPath(), equalTo(path));
    var reqBody = new RequestBodyReader().bodyAsString(request);
    assertThat(URLDecoder.decode(reqBody, StandardCharsets.UTF_8), equalTo(body));
  }
}
