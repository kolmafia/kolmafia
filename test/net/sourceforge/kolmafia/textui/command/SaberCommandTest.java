package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SaberCommandTest extends AbstractCommandTestBase {
  @BeforeEach
  public void initEach() {
    KoLCharacter.reset("testUser");
    KoLCharacter.reset(true);
    Preferences.resetToDefault("_saberMod");

    // Stop requests from actually running
    GenericRequest.sessionId = null;
  }

  public SaberCommandTest() {
    this.command = "saber";
  }

  private static void hasSaber() {
    AdventureResult.addResultToList(KoLConstants.inventory, ItemPool.get(ItemPool.FOURTH_SABER));
  }

  @Test
  void mustHaveSaber() {
    String output = execute("ml");

    assertErrorState();
    assertThat(output, containsString("You need a Fourth of May Cosplay Saber"));
  }

  @Test
  void mustNotHaveUpgraded() {
    hasSaber();
    Preferences.setInteger("_saberMod", 1);
    String output = execute("mp");

    assertErrorState();
    assertThat(output, containsString("already upgraded"));
  }

  @Test
  void mustSpecifyUpgrade() {
    hasSaber();
    String output = execute("");

    assertErrorState();
    assertThat(output, containsString("Which upgrade"));
  }

  @Test
  void mustSpecifyValidUpgrade() {
    hasSaber();
    String output = execute("dog");

    assertErrorState();
    assertThat(output, containsString("I don't understand what upgrade"));
  }

  @Test
  void canChooseUpgrade() {
    hasSaber();
    String output = execute("ml");

    assertContinueState();
    assertThat(output, containsString("Upgrading saber"));
  }

  @Test
  void canChooseMPUpgrade() {
    hasSaber();
    String output = execute("mp");

    assertContinueState();
    assertThat(output, containsString("Upgrading saber"));
  }

  @Test
  void canChooseResUpgrade() {
    hasSaber();
    String output = execute("resistance");

    assertContinueState();
    assertThat(output, containsString("Upgrading saber"));
  }

  @Test
  void canChooseFamUpgrade() {
    hasSaber();
    String output = execute("familiar");

    assertContinueState();
    assertThat(output, containsString("Upgrading saber"));
  }
}
