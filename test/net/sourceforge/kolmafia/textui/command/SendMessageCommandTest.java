package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mockStatic;

import internal.helpers.Cleanups;
import internal.helpers.Player;
import internal.network.FakeHttpClientBuilder;
import internal.network.RequestBodyReader;
import java.net.http.HttpRequest;
import java.util.List;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.moods.RecoveryManager;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.HttpUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class SendMessageCommandTest extends AbstractCommandTestBase {

  public SendMessageCommandTest() {
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

  @Nested
  class MoodManager {

    private MockedStatic<net.sourceforge.kolmafia.moods.MoodManager> mockery;

    @BeforeEach
    public void setUp() {
      mockery = mockMoodManager();
    }

    @AfterEach
    public void tearDown() {
      mockery.close();
    }

    private MockedStatic<net.sourceforge.kolmafia.moods.MoodManager> mockMoodManager() {
      var mocked =
          mockStatic(net.sourceforge.kolmafia.moods.MoodManager.class, Mockito.CALLS_REAL_METHODS);
      mocked.when(net.sourceforge.kolmafia.moods.MoodManager::isExecuting).thenReturn(true);
      return mocked;
    }

    @Test
    public void itShouldNotSendDuringMoodSwings() {
      String output;
      output = execute(" 1000000 meat to buffy");
      assertThat(
          output,
          containsString(
              "Send request \" 1000000 meat to buffy\" ignored in between-battle execution."));
      assertContinueState();
    }
  }

  @Test
  public void itShouldSendMeatWithOutCommas() {
    String output;
    var cleanups = Player.setMeat(1000000);
    try (cleanups) {
      output = execute(" 1000000 meat to buffy");
    }
    assertThat(output, containsString("Sending kmail to buffy..."));
    assertContinueState();
    var requests = getRequests();
    assertThat(requests, not(empty()));
    var request = requests.get(0);
    var uri = request.uri();
    assertThat(uri.getPath(), equalTo("/sendmessage.php"));
    assertThat(request.method(), equalTo("POST"));
    var body = new RequestBodyReader().bodyAsString(request);
    assertThat(
        body,
        equalTo(
            "action=send&towho=buffy&message=Keep+the+contents+of+this+message+top-sekrit%2C+ultra+hush-hush.&sendmeat=1000000"));
  }

  @Test
  public void itShouldNotSendMeatWithCommas() {
    String output;
    var cleanups = Player.setMeat(1000000);
    try (cleanups) {
      output = execute(" 1,000,000 meat to buffy");
    }
    assertThat(output, containsString("Sending kmail to buffy..."));
    assertContinueState();
    var requests = getRequests();
    assertThat(requests, not(empty()));
    var request = requests.get(0);
    var uri = request.uri();
    assertThat(uri.getPath(), equalTo("/sendmessage.php"));
    assertThat(request.method(), equalTo("POST"));
    var body = new RequestBodyReader().bodyAsString(request);
    // Note - the message is sent but not the meat
    assertThat(
        body,
        equalTo(
            "action=send&towho=buffy&message=Keep+the+contents+of+this+message+top-sekrit%2C+ultra+hush-hush."));
  }

  @Test
  public void itShouldSendALongAmountOfMeat() {
    String output;
    var cleanups = Player.setMeat(3000000000L);
    try (cleanups) {
      output = execute(" 3000000000 meat to buffy");
    }
    assertThat(output, containsString("Sending kmail to buffy..."));
    assertContinueState();
    var requests = getRequests();
    assertThat(requests, not(empty()));
    var request = requests.get(0);
    var uri = request.uri();
    assertThat(uri.getPath(), equalTo("/sendmessage.php"));
    assertThat(request.method(), equalTo("POST"));
    var body = new RequestBodyReader().bodyAsString(request);
    assertThat(
        body,
        equalTo(
            "action=send&towho=buffy&message=Keep+the+contents+of+this+message+top-sekrit%2C+ultra+hush-hush.&sendmeat=3000000000"));
  }

  // I wasn't expecting this to work since it is sending more meat than present.
  // Will keep test as a reminder to figure out where the meat is checked.  TransferRequest?
  @Test
  public void itShouldNotSendMeatItDoesNotHave() {
    String output;
    var cleanups = Player.setMeat(10000);
    try (cleanups) {
      output = execute(" 1000000 meat to buffy");
    }
    assertThat(output, containsString("Sending kmail to buffy..."));
    assertContinueState();
    var requests = getRequests();
    assertThat(requests, not(empty()));
    var request = requests.get(0);
    var uri = request.uri();
    assertThat(uri.getPath(), equalTo("/sendmessage.php"));
    assertThat(request.method(), equalTo("POST"));
    var body = new RequestBodyReader().bodyAsString(request);
    assertThat(
        body,
        equalTo(
            "action=send&towho=buffy&message=Keep+the+contents+of+this+message+top-sekrit%2C+ultra+hush-hush.&sendmeat=1000000"));
  }

  @Test
  public void itShouldNotSendDuringRecovery() {
    RecoveryManager.setRecoveryActive(true);
    String output;
    var cleanups = Player.setMeat(1000000);
    try (cleanups) {
      output = execute(" 1000000 meat to buffy");
    }
    assertThat(
        output,
        containsString(
            "Send request \" 1000000 meat to buffy\" ignored in between-battle execution."));
    assertContinueState();
    RecoveryManager.setRecoveryActive(false);
  }

  @Test
  public void itShouldFailWithMissingParameters() {
    String output;
    output = execute(" buffy");
    assertThat(output, containsString("Invalid send request."));
    assertErrorState();
  }

  @Test
  public void itShouldNotSendWithNoItemsAndEmptyMessage() {
    String output;
    output = execute("to buffy");
    assertThat(output, not(containsString("Sending kmail")));
  }

  @Test
  public void itShouldSendAlternateMessageText() {
    String output;
    var cleanups = Player.setMeat(3000000000L);
    try (cleanups) {
      output = execute(" 3000000000 meat to buffy || This is Blackmail!!!");
    }
    assertThat(output, containsString("Sending kmail to buffy ..."));

    assertContinueState();
    var requests = getRequests();
    assertThat(requests, not(empty()));
    var request = requests.get(0);
    var uri = request.uri();
    assertThat(uri.getPath(), equalTo("/sendmessage.php"));
    assertThat(request.method(), equalTo("POST"));
    var body = new RequestBodyReader().bodyAsString(request);
    assertThat(
        body,
        equalTo("action=send&towho=buffy+&message=This+is+Blackmail%21%21%21&sendmeat=3000000000"));
  }

  @Test
  public void itShouldRequireCsendForMeat() {
    String output;
    this.command = "send";
    var cleanups = Player.setMeat(1000000);
    try (cleanups) {
      output = execute(" 1000000 meat to buffy");
    }
    assertThat(output, containsString("Please use 'csend' if you need to transfer meat."));
    assertErrorState();
    this.command = "csend";
  }

  @Test
  public void itShouldSendThingsBesidesMeat() {
    String output;
    var cleanups = Player.addItem("seal tooth", 3);
    try (cleanups) {
      output = execute(" 1 seal tooth to buffy");
    }
    assertThat(output, containsString("Sending kmail to buffy..."));
    assertContinueState();
    var requests = getRequests();
    assertThat(requests, not(empty()));
    var request = requests.get(0);
    var uri = request.uri();
    assertThat(uri.getPath(), equalTo("/sendmessage.php"));
    assertThat(request.method(), equalTo("POST"));
    var body = new RequestBodyReader().bodyAsString(request);
    assertThat(
        body,
        equalTo(
            "action=send&towho=buffy&message=Keep+the+contents+of+this+message+top-sekrit%2C+ultra+hush-hush.&whichitem1=2&howmany1=1"));
  }

  @Test
  public void itShouldParseItemCount() {
    String output;
    var cleanups = Player.addItem("seal tooth", 3);
    try (cleanups) {
      output = execute(" seal tooth to buffy");
    }
    assertThat(output, containsString("Sending kmail to buffy..."));
    assertContinueState();
    var requests = getRequests();
    assertThat(requests, not(empty()));
    var request = requests.get(0);
    var uri = request.uri();
    assertThat(uri.getPath(), equalTo("/sendmessage.php"));
    assertThat(request.method(), equalTo("POST"));
    var body = new RequestBodyReader().bodyAsString(request);
    assertThat(
        body,
        equalTo(
            "action=send&towho=buffy&message=Keep+the+contents+of+this+message+top-sekrit%2C+ultra+hush-hush.&whichitem1=2&howmany1=1"));
  }

  @Test
  public void itShouldNotSendItemsPlayerDoesNotHave() {
    String output = execute(" 5 seal tooth to buffy");
    assertThat(output, containsString("[5 seal tooth] requested, but none available."));
    assertErrorState();
  }

  @Test
  public void itShouldLimitItemCount() {
    String output;
    var cleanups = Player.addItem("seal tooth", 3);
    try (cleanups) {
      output = execute(" 5 seal tooth to buffy");
    }
    assertThat(output, containsString("[5 seal tooth] requested, but only 3 available."));
    assertErrorState();
  }

  @Test
  public void itShouldNotRecognizeItem() {
    String output;
    var cleanups = Player.addItem("seal tooth", 3);
    try (cleanups) {
      output = execute(" 1 soiled dove to buffy");
    }
    assertThat(output, containsString("[soiled dove] has no matches."));
    assertErrorState();
  }

  @Test
  public void itShouldAlsoNotRecognizeItem() {
    String output;
    var cleanups = Player.addItem("seal tooth", 3);
    try (cleanups) {
      output = execute(" 1 soiled dove to buffy || Wash me.");
    }
    assertThat(output, containsString("[soiled dove] has no matches."));
    assertErrorState();
  }

  @Test
  public void itShouldAlsoRecognizeNoItem() {
    String output;
    var cleanups = Player.addItem("seal tooth", 3);
    try (cleanups) {
      output = execute("  to buffy || Wash me.");
    }
    assertThat(output, containsString("Sending kmail to buffy ..."));
    assertContinueState();
  }

  @Test
  public void itShouldHandleFuzzyItem() {
    String output;
    var cleanups = Player.addItem("seal tooth", 3);
    try (cleanups) {
      output = execute(" 1 potion to buffy");
    }
    assertThat(output, containsString("[potion] has too many matches."));
    assertErrorState();
  }

  @Test
  public void itShouldHandleDifferentItems() {
    String output;
    var cleanups =
        new Cleanups(Player.addItem("seal tooth", 3), Player.addItem("seal-clubbing club", 3));
    try (cleanups) {
      output =
          execute(" 1 seal tooth, 1 seal-clubbing club to buffy || Signed.  Sealed.  Delivered.");
    }
    assertThat(output, containsString("Sending kmail to buffy ..."));
    assertContinueState();
    var requests = getRequests();
    assertThat(requests, not(empty()));
    var request = requests.get(0);
    var uri = request.uri();
    assertThat(uri.getPath(), equalTo("/sendmessage.php"));
    assertThat(request.method(), equalTo("POST"));
    var body = new RequestBodyReader().bodyAsString(request);
    assertThat(
        body,
        equalTo(
            "action=send&towho=buffy+&message=Signed.++Sealed.++Delivered.&whichitem1=2&howmany1=1&whichitem2=1&howmany2=1"));
  }
}
