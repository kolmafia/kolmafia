package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import internal.helpers.Player;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ArcadeRequestTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("ArcadeRequestTest");
  }

  @BeforeEach
  public void beforeEach() {
    KoLConstants.inventory.clear();
    Preferences.reset("ArcadeRequestTest");
  }

  private static String withCapturedLogs(Runnable action) {
    ByteArrayOutputStream ostream = new ByteArrayOutputStream();

    try (PrintStream out = new PrintStream(ostream, true)) {
      RequestLogger.openCustom(out);
      action.run();
      RequestLogger.closeCustom();
    }

    return ostream.toString().trim();
  }

  @Nested
  class Skeeball {
    @Test
    public void playingSkeeballUsesAGameGridToken() {
      AdventureResult.addResultToList(KoLConstants.inventory, ArcadeRequest.TOKEN);

      ArcadeRequest.parseResponse(
          "place.php?whichplace=arcade&action=arcade_skeeball",
          html("request/test_arcade_skeeball.html"));

      assertThat(ArcadeRequest.TOKEN.getCount(KoLConstants.inventory), is(0));
    }

    @Test
    public void visitingJackassPlumberRecordsTheDailyCheck() {
      ArcadeRequest.parseResponse(
          "place.php?whichplace=arcade&action=arcade_plumber",
          html("request/test_arcade_plumber.html"));

      assertThat(Preferences.getBoolean("_defectiveTokenChecked"), is(true));
    }
  }
}
