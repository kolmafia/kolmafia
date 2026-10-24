package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Player.withPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.TCRSDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TCRSCommandTest extends AbstractCommandTestBase {
  @BeforeAll
  public static void init() {
    KoLCharacter.reset("TCRSCommandTest");
  }

  public TCRSCommandTest() {
    this.command = "tcrs";
  }

  @AfterEach
  void afterEach() {
    TCRSDatabase.resetModifiers();
  }

  @Test
  void parsesDataOutsideTCRS() {
    try (var cleanups = withPath(Path.NONE)) {
      String output = execute("test Seal Clubber, Opossum");

      assertContinueState();
      assertThat(output, containsString("Deriving TCRS item adjustments"));
      assertThat(
          TCRSDatabase.getTCRSName(ItemPool.PLEXIGLASS_PITH_HELMET),
          equalTo("flame-wreathed frosty pith helmet of vim and vigor"));
    }
  }

  @Test
  void rejectsInvalidSigns() {
    try (var cleanups = withPath(Path.NONE)) {
      String output = execute("test Seal Clubber, Badger");

      assertErrorState();
      assertThat(output, containsString("Badger is not a valid sign"));
      assertThat(TCRSDatabase.hasData(ItemPool.PLEXIGLASS_PITH_HELMET), is(false));
    }
  }

  @Test
  void restoresRealModifiers() {
    try (var cleanups = withPath(Path.NONE)) {
      execute("test Seal Clubber, Opossum");
      execute("test reset");

      assertThat(TCRSDatabase.hasData(ItemPool.PLEXIGLASS_PITH_HELMET), is(false));
    }
  }
}
