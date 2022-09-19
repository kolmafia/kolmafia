package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withFamiliar;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.HeistManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HeistCommandTest extends AbstractCommandTestBase {
  @BeforeEach
  public void initEach() {
    KoLCharacter.reset("testUser");
    new HeistCommandFakeRequest().register("heistFake");

    // Stop requests from actually running
    GenericRequest.sessionId = null;
  }

  public HeistCommandTest() {
    this.command = "heist";
  }

  @Test
  void mustHaveCatBurglar() {
    String output = execute("");

    assertThat(output, containsString("You don't have a Cat Burglar"));
    assertContinueState();
  }

  @Test
  void mustSpecifyValidItem() {
    var cleanups = withFamiliar(FamiliarPool.CAT_BURGLAR);

    try (cleanups) {
      String output = execute("an invalid item");

      assertThat(output, containsString("What item is an invalid item?"));
      assertErrorState();
    }
  }

  @Test
  void parsesHeistPage() {
    var cleanups = withFamiliar(FamiliarPool.CAT_BURGLAR);

    try (cleanups) {
      this.command = "heistFake";
      String output = execute("");

      assertThat(output, containsString("You have 42 heists."));
      assertThat(output, containsString("From  bigface:"));
      assertThat(output, containsString("From a jock:"));
      assertThat(output, containsString("From a burnout:"));
      assertContinueState();
    }
  }

  @Test
  void doesNotHeistInvalidItem() {
    var cleanups = withFamiliar(FamiliarPool.CAT_BURGLAR);

    try (cleanups) {
      this.command = "heistFake";
      String output = execute("334 scroll");

      assertThat(output, containsString("Could not find 334 scroll to heist"));
      assertErrorState();
    }
  }

  @Test
  void heistsValidItemExact() {
    var cleanups = withFamiliar(FamiliarPool.CAT_BURGLAR);

    try (cleanups) {
      this.command = "heistFake";
      String output = execute("ratty knitted cap");

      assertThat(output, containsString("Heisted ratty knitted cap"));
      assertContinueState();
    }
  }

  @Test
  void heistsValidItem() {
    var cleanups = withFamiliar(FamiliarPool.CAT_BURGLAR);

    try (cleanups) {
      this.command = "heistFake";
      String output = execute("Purple Beast");

      assertThat(output, containsString("Heisted Purple Beast energy drink"));
      assertContinueState();
    }
  }

  @Test
  void heistsValidItemWithQuotes() {
    var cleanups = withFamiliar(FamiliarPool.CAT_BURGLAR);

    try (cleanups) {
      this.command = "heistFake";
      String output = execute("\"meat\" stick");

      assertThat(output, containsString("Heisted &quot;meat&quot; stick"));
      assertContinueState();
    }
  }

  @Test
  void heistsMultipleValidItem() {
    var cleanups = withFamiliar(FamiliarPool.CAT_BURGLAR);

    try (cleanups) {
      this.command = "heistFake";
      String output = execute("13 Purple Beast");

      assertThat(output, containsString("Heisted 13 Purple Beast energy drinks"));
      assertContinueState();
    }
  }

  public static class HeistCommandFakeRequest extends HeistCommand {
    @Override
    protected HeistManager heistManager() {
      class HeistManagerFakeRequest extends HeistManager {
        @Override
        protected String heistRequest() {
          return html("request/test_heist_command.html");
        }
      }
      return new HeistManagerFakeRequest();
    }
  }
}
