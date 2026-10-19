package net.sourceforge.kolmafia.webui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RelayServerMessagesTest {

  @BeforeEach
  public void openListener() throws InterruptedException {
    RelayServer.pollMessages(null, 0);
  }

  private static long head() throws InterruptedException {
    return RelayServer.pollMessages(null, 0).head();
  }

  private static long refresh() throws InterruptedException {
    return RelayServer.pollMessages(null, 0).refresh();
  }

  private static List<String> htmlOf(final RelayServer.Batch batch) {
    return batch.events().stream().map(RelayServer.Event::html).collect(Collectors.toList());
  }

  @Test
  public void pollReturnsOnlyMessagesNewerThanSince() throws InterruptedException {
    var start = head();
    RelayServer.addStatusMessage("first");
    RelayServer.addStatusMessage("second");

    var batch = RelayServer.pollMessages(start, refresh());

    assertThat(htmlOf(batch), contains("first", "second"));
    assertThat(batch.head(), equalTo(start + 2));
  }

  @Test
  public void linesAddedBeforeAPollAreNotDropped() throws InterruptedException {
    var start = head();
    for (int i = 0; i < 50; i++) {
      RelayServer.addStatusMessage("line " + i);
    }

    var batch = RelayServer.pollMessages(start, refresh());

    assertThat(batch.events(), hasSize(50));
  }

  @Test
  public void pollWaitsForNewMessages() throws Exception {
    var start = head();
    var startRefresh = refresh();
    var pool = Executors.newSingleThreadExecutor();
    try {
      var waiting = pool.submit(() -> RelayServer.pollMessages(start, startRefresh));

      Thread.sleep(100);
      RelayServer.addStatusMessage("late");

      var batch = waiting.get(5, TimeUnit.SECONDS);
      assertThat(htmlOf(batch), contains("late"));
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  public void updateStatusBumpsRefreshCounter() throws InterruptedException {
    var before = refresh();
    RelayServer.updateStatus();
    RelayServer.updateStatus();

    assertThat(refresh(), equalTo(before + 2));
  }

  @Test
  public void refreshDoesNotAppearAmongLines() throws InterruptedException {
    var start = head();
    var before = refresh();
    RelayServer.updateStatus();

    var batch = RelayServer.pollMessages(start, before);

    assertThat(batch.events(), hasSize(0));
    assertThat(batch.refresh(), equalTo(before + 1));
  }

  @Test
  public void sinceAheadOfHeadReturnsWholeBufferAfterRestart() throws InterruptedException {
    RelayServer.addStatusMessage("after restart");
    var current = head();

    var batch = RelayServer.pollMessages(current + 1000, refresh());

    assertThat(htmlOf(batch), hasItem("after restart"));
  }

  @Test
  public void freshLoadReturnsRecentLinesWithoutWaiting() throws InterruptedException {
    RelayServer.addStatusMessage("recent");

    var batch = RelayServer.pollMessages(null, 0);

    assertThat(htmlOf(batch), hasItem("recent"));
  }

  @Test
  public void reportsLinesEvictedBeforeThePollAsSkipped() throws InterruptedException {
    var start = head();
    for (int i = 0; i < 2100; i++) {
      RelayServer.addStatusMessage("burst " + i);
    }

    var batch = RelayServer.pollMessages(start, refresh());

    assertThat(batch.skipped(), equalTo(100L));
    assertThat(batch.events(), hasSize(2000));
  }
}
