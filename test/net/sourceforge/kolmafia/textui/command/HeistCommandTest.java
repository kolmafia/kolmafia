package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HeistCommandTest extends AbstractCommandTestBase {
  @BeforeEach
  public void initEach() {
    KoLCharacter.reset("testUser");

    // Stop requests from actually running
    GenericRequest.sessionId = null;
  }

  public HeistCommandTest() {
    this.command = "heist";
  }

  static void setCatBurglar() {
    var familiar = FamiliarData.registerFamiliar(FamiliarPool.CAT_BURGLAR, 1);
    KoLCharacter.setFamiliar(familiar);
  }

  @Test
  void mustHaveCatBurglar() {
    KoLCharacter.setFamiliar(FamiliarData.NO_FAMILIAR);
    String output = execute("");

    assertThat(output, containsString("You need to take your Cat Burglar with you"));
    assertContinueState();
  }

  @Test
  void mustSpecifyValidItem() {
    setCatBurglar();
    String output = execute("an invalid item");

    assertThat(output, containsString("What item is an invalid item?"));
    assertErrorState();
  }
}
