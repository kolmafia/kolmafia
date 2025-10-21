package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Player.withClass;
import static org.junit.jupiter.api.Assertions.*;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlayCommandTest extends AbstractCommandTestBase {
  public PlayCommandTest() {
    this.command = "cheat";
  }

  static final String commandParameter = "Ancestral Recall";

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("PlayCommandTestUser");
    // Stop requests from actually running
    GenericRequest.sessionId = null;
  }

  @BeforeEach
  public void initializeState() {
    StaticEntity.setContinuationState(KoLConstants.MafiaState.CONTINUE);
  }

  @Test
  public void noDeckNoExecute() {
    String output = execute(commandParameter);
    assertErrorState();
    assertTrue(output.contains("You need 1 more Deck"));
  }

  @Test
  public void needsParameter() {
    String output = execute("");
    assertContinueState();
    assertTrue(output.contains("Play what?"));
  }

  @Test
  public void needDeckToPlayRandom() {
    String output = execute("random");
    assertErrorState();
    assertTrue(output.contains("You need 1 more Deck"));
  }

  @Test
  public void needPhylumParameter() {
    String output = execute("phylum");
    assertErrorState();
    assertTrue(output.contains("Which monster phylum do you want?"));
  }

  @Test
  public void needValidPhylum() {
    String output = execute("phylum not_a_phylum");
    assertErrorState();
    assertTrue(output.contains("What kind of random monster is"));
  }

  @Test
  public void needAvailablePhylum() {
    String output = execute("phylum none");
    assertErrorState();
    assertTrue(output.contains("What kind of random monster is"));
  }

  @Test
  public void needStat() {
    String output = execute("stat");
    assertErrorState();
    assertTrue(output.contains("Which stat do you want?"));
  }

  @Test
  public void needValidStat() {
    String output = execute("stat enisland");
    assertErrorState();
    assertTrue(output.contains("Which stat is"));
  }

  @Test
  public void needUnambiguousStat() {
    String output = execute("stat m");
    assertErrorState();
    assertTrue(output.contains("is an ambiguous stat"));
  }

  @Test
  public void acceptsMainStat() {
    var cleanups = new Cleanups(withClass(AscensionClass.ACCORDION_THIEF));
    try (cleanups) {
      String output = execute("stat main");
      assertErrorState();
      assertTrue(output.contains("You need 1 more Deck"));
    }
  }

  @Test
  public void acceptsOtherStat() {
    String output = execute("stat myst");
    assertErrorState();
    assertTrue(output.contains("You need 1 more Deck"));
  }

  @Test
  public void needBuff() {
    String output = execute("buff");
    assertErrorState();
    assertTrue(output.contains("Which buff do you want?"));
  }

  @Test
  public void needValidBuff() {
    String output = execute("buff bongos");
    assertErrorState();
    assertTrue(output.contains("Which buff is"));
  }

  @Test
  public void needUniqueBuff() {
    String output = execute("buff m");
    assertErrorState();
    assertTrue(output.contains("is an ambiguous buff"));
  }

  @Test
  public void needCardWithBuff() {
    String output = execute("buff racing");
    assertErrorState();
    assertTrue(output.contains("You need 1 more Deck"));
  }

  @Test
  public void needValidCardName() {
    String output = execute("queen");
    assertErrorState();
    assertTrue(output.contains("I don't know how to play"));
  }

  @Test
  public void needUnambiguousCard() {
    String output = execute("X");
    assertErrorState();
    assertTrue(output.contains("is an ambiguous card name"));
  }
}
