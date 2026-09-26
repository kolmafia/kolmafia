package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ChewCommandTest extends AbstractCommandTestBase {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("ChewCommandTest");
    Preferences.reset("ChewCommandTest");
  }

  public ChewCommandTest() {
    this.command = "chew";
  }

  @Test
  public void canSpleenInGreyYou() {
    var cleanups =
        new Cleanups(
            withPath(AscensionPath.Path.GREY_YOU),
            withClass(AscensionClass.GREY_GOO),
            withItem(ItemPool.ANCIENT_MEDICINAL_HERBS));

    try (cleanups) {
      String output = execute("ancient medicinal herbs");
      assertContinueState();
      assertThat(output, containsString("Chewing 1 ancient medicinal herbs"));
    }
  }
}
