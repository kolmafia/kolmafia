package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mockStatic;

import internal.network.FakeHttpClientBuilder;
import java.net.http.HttpRequest;
import java.util.List;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.moods.MoodManager;
import net.sourceforge.kolmafia.moods.RecoveryManager;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.HttpUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class SendMessageCommandTestAlso extends AbstractCommandTestBase {

  public SendMessageCommandTestAlso() {
    this.command = "csend";
  }

  private final FakeHttpClientBuilder fakeClientBuilder = new FakeHttpClientBuilder();

  private List<HttpRequest> getRequests() {
    return fakeClientBuilder.client.getRequests();
  }

  @BeforeEach
  public void initializeState() {
    GenericRequest.sessionId = "csend";
    HttpUtilities.setClientBuilder(() -> fakeClientBuilder);
    GenericRequest.resetClient();
    fakeClientBuilder.client.clear();
    StaticEntity.setContinuationState(KoLConstants.MafiaState.CONTINUE);
  }

  private MockedStatic<MoodManager> mockMoodManager() {
    var mocked = mockStatic(MoodManager.class, Mockito.CALLS_REAL_METHODS);
    mocked.when(MoodManager::isExecuting).thenReturn(true);
    return mocked;
  }

  @Test
  public void itShouldNotSendDuringMoodSwings() {
    String output;
    MockedStatic<MoodManager> mockery = mockMoodManager();
    output = execute(" 1000000 meat to buffy");
    assertThat(
        output,
        containsString(
            "Send request \" 1000000 meat to buffy\" ignored in between-battle execution."));
    assertContinueState();
    RecoveryManager.setRecoveryActive(false);
  }
}
