package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.session.ChoiceManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EdPieceCommandTest extends AbstractCommandTestBase {
  public EdPieceCommandTest() {
    this.command = "edpiece";
  }

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("EdPieceCommandTest");
  }

  @BeforeEach
  public void initializeState() {
    HttpClientWrapper.setupFakeClient();
    StaticEntity.setContinuationState(KoLConstants.MafiaState.CONTINUE);
    ChoiceManager.handlingChoice = false;
  }

  @Test
  public void getModes() {
    var command = new EdPieceCommand();
    assertThat(command.getModes(), hasSize(7));
  }

  @Test
  public void validate() {
    var command = new EdPieceCommand();
    assertThat(command.validate("edpiece", "puma"), equalTo(true));
    assertThat(command.validate("edpiece", "rat"), equalTo(false));
  }

  @Test
  public void showsAllPossibleAnimalsOnCheck() {
    String output = execute("", true);
    assertThat(output.split("<tr>"), arrayWithSize(9));
  }

  @Test
  public void showWithNoAnimal() {
    String output = execute("");

    assertThat(output, containsString("<nothing>"));
  }

  @Test
  public void showCurrentAnimal() {
    var cleanups = withProperty("edPiece", "fish");

    try (cleanups) {
      String output = execute("");

      assertThat(output, containsString("a golden fish"));
    }
  }

  @Test
  public void doesntAllowInvalidAnimal() {
    String output = execute("turtle");

    assertThat(output, containsString("Animal turtle not recognised"));
  }

  @Test
  public void warnIfNoHat() {
    String output = execute("weasel");

    assertThat(output, containsString("You need 1 more The Crown of Ed the Undying"));

    assertThat(getRequests(), empty());
  }

  @Test
  public void canDoNothing() {
    var cleanups =
        new Cleanups(
            withProperty("edPiece", "weasel"),
            withEquipped(Slot.HAT, "The Crown of Ed the Undying"));

    try (cleanups) {
      String output = execute("weasel");

      assertThat(output, containsString("already equipped"));

      var requests = getRequests();

      assertThat(requests, empty());
    }
  }

  @Test
  public void canJustEquipHat() {
    var cleanups =
        new Cleanups(withProperty("edPiece", "weasel"), withItem("The Crown of Ed the Undying"));

    try (cleanups) {
      String output = execute("weasel");

      assertThat(output, containsString("already equipped"));

      var requests = getRequests();

      assertThat(requests, hasSize(1));
      assertPostRequest(
          requests.get(0), "/inv_equip.php", "which=2&ajax=1&action=equip&whichitem=8185");
    }
  }

  @Test
  public void canJustChangeAnimal() {
    var cleanups =
        new Cleanups(
            withProperty("edPiece", "weasel"),
            withEquipped(Slot.HAT, "The Crown of Ed the Undying"));

    try (cleanups) {
      String output = execute("hyena");

      assertThat(output, containsString("Crown of Ed decorated"));

      var requests = getRequests();

      assertThat(requests, hasSize(2));
    }
  }

  @Test
  public void canEquipHatAndChangeAnimal() {
    var cleanups =
        new Cleanups(withProperty("edPiece", "weasel"), withItem("The Crown of Ed the Undying"));

    try (cleanups) {
      String output = execute("mouse");

      assertThat(output, not(containsString("already equipped")));

      var requests = getRequests();

      assertThat(requests, hasSize(3));
      assertPostRequest(
          requests.get(0), "/inv_equip.php", "which=2&ajax=1&action=equip&whichitem=8185");
    }
  }
}
