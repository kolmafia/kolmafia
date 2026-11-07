package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Player.withHttpClientBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.KoLCharacter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TrainFamiliarCommandTest extends AbstractCommandTestBase {
  public TrainFamiliarCommandTest() {
    this.command = "train";
  }

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("TrainFamiliarCommandTest");
  }

  @Test
  void needsATypeAndAGoal() {
    String output = execute("base");

    assertErrorState();
    assertThat(output, containsString("Syntax: train type goal"));
  }

  @Test
  void rejectsAnUnknownType() {
    String output = execute("fast 20");

    assertErrorState();
    assertThat(output, containsString("Unknown training type: fast"));
  }

  @Test
  void needsAFamiliarOut() {
    var builder = new FakeHttpClientBuilder();

    try (var cleanups = new Cleanups(withHttpClientBuilder(builder))) {
      String output = execute("base 20");

      assertErrorState();
      assertThat(output, containsString("No familiar selected to train."));
      assertThat(builder.client.getRequests(), empty());
    }
  }
}
