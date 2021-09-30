package net.sourceforge.kolmafia.request;

import static org.mockito.Mockito.doAnswer;

import org.junit.jupiter.api.BeforeAll;

abstract class RequestTestBase {

  @BeforeAll
  static void setSessionId() {
    GenericRequest.sessionId = "fake session id";
  }

  // Inject expected success (responseCode = 200) response text.
  protected void expectSuccess(GenericRequest spy, String responseText) {
    doAnswer(
            invocation -> {
              GenericRequest m = (GenericRequest) invocation.getMock();
              m.responseCode = 200;
              m.responseText = responseText;
              // This is normally done by retrieveServerReply(), which is called by
              // externalExecute().
              m.processResponse();
              return null;
            })
        .when(spy)
        .externalExecute();
  }
}
