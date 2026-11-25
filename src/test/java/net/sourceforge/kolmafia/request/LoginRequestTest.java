package net.sourceforge.kolmafia.request;

import static internal.helpers.Player.withHttpClientBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestThread;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class LoginRequestTest {
  private static final String TESTUSERNAME = "LoginRequestTest User";

  @BeforeEach
  void beforeEach() {
    KoLCharacter.reset(TESTUSERNAME);
  }

  private static Cleanups withTimedOutSession() {
    LoginRequest.setLastRequest(new LoginRequest(TESTUSERNAME, ""));
    return new Cleanups(
        () -> {
          LoginRequest.setLastRequest(null);
          LoginRequest.setLoggedOut();
        });
  }

  @Test
  void aSecondThreadWaitsForTheLoginInProgressAndUsesIt() throws InterruptedException {
    var builder = new FakeHttpClientBuilder();
    var loggingIn = new CountDownLatch(1);
    var finishLogin = new CountDownLatch(1);
    var logins = new AtomicInteger();
    var firstResult = new AtomicBoolean();
    var secondResult = new AtomicBoolean();

    try (var cleanups = new Cleanups(withHttpClientBuilder(builder), withTimedOutSession())) {
      var first =
          new Thread(
              () -> {
                try (var threads = Mockito.mockStatic(RequestThread.class)) {
                  threads
                      .when(() -> RequestThread.postRequest(any(GenericRequest.class)))
                      .thenAnswer(
                          invocation -> {
                            logins.incrementAndGet();
                            loggingIn.countDown();
                            finishLogin.await(10, TimeUnit.SECONDS);
                            LoginRequest.setLoggedIn();
                            return null;
                          });
                  firstResult.set(LoginRequest.executeTimeInRequest("main.php", "login.php"));
                }
              });
      first.start();
      assertThat(loggingIn.await(10, TimeUnit.SECONDS), is(true));

      var second =
          new Thread(
              () -> secondResult.set(LoginRequest.executeTimeInRequest("chat.php", "login.php")));
      second.start();
      while (second.getState() != Thread.State.BLOCKED
          && second.getState() != Thread.State.TERMINATED) {
        Thread.sleep(10);
      }

      finishLogin.countDown();
      first.join(10_000);
      second.join(10_000);
    }

    assertThat(logins.get(), is(1));
    assertThat(firstResult.get(), is(true));
    assertThat(secondResult.get(), is(true));
    assertThat(builder.client.getRequests(), empty());
  }

  @Test
  void logsInAgainWhenTheSessionTimesOutAfterAnEarlierLogin() {
    var builder = new FakeHttpClientBuilder();

    try (var cleanups = new Cleanups(withHttpClientBuilder(builder), withTimedOutSession());
        var threads = Mockito.mockStatic(RequestThread.class)) {
      LoginRequest.setLoggedIn();

      LoginRequest.executeTimeInRequest("main.php", "login.php");

      threads.verify(() -> RequestThread.postRequest(any(GenericRequest.class)), Mockito.times(1));
    }
  }
}
