package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withItem;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.ChoiceManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class JurassicParkaCommandTest extends AbstractCommandTestBase {
  public JurassicParkaCommandTest() {
    this.command = "parka";
  }

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("JurassicParkaCommandTest");
  }

  @BeforeEach
  public void initializeState() {
    HttpClientWrapper.setupFakeClient();
    StaticEntity.setContinuationState(KoLConstants.MafiaState.CONTINUE);
    ChoiceManager.handlingChoice = false;
  }

  @Test
  public void getModes() {
    var command = new JurassicParkaCommand();
    assertThat(command.getModes(), hasSize(5));
  }

  @Test
  public void validate() {
    var command = new JurassicParkaCommand();
    assertThat(command.validate("parka", "spikolodon"), equalTo(true));
    assertThat(command.validate("parka", "pterodactyl"), equalTo(true));
    assertThat(command.validate("parka", "cold"), equalTo(true));
    assertThat(command.validate("parka", "hot"), equalTo(true));
    assertThat(command.validate("parka", "weenus"), equalTo(false));
  }

  @Test
  public void warnIfNoParka() {
    String output = execute("spooky");
    assertThat(
        output, containsString("You need a Jurassic Parka to pull tabs on your Jurassic Parka"));
    assertThat(getRequests(), empty());
  }

  @Test
  public void warnAgainstUnknownInput() {
    var cleanups = new Cleanups(withItem(ItemPool.JURASSIC_PARKA));

    try (cleanups) {
      String output = execute("slimy");

      assertThat(output, containsString("not recognised"));

      var requests = getRequests();
      assertThat(requests, empty());
    }
  }

  @ParameterizedTest
  @CsvSource({"kachungasaur, 1", "hot, 5"})
  public void canChangeMode(final String params, final int decision) {
    var cleanups = new Cleanups(withItem(ItemPool.JURASSIC_PARKA));

    try (cleanups) {
      execute(params);

      var requests = getRequests();

      assertThat(requests, hasSize(greaterThanOrEqualTo(2)));
      assertPostRequest(requests.get(0), "/inventory.php", "action=jparka");
      assertPostRequest(requests.get(1), "/choice.php", "whichchoice=1481&option=" + decision);
    }
  }
}
