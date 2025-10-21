package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static org.junit.jupiter.api.Assertions.*;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.AscensionPath;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
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
}
