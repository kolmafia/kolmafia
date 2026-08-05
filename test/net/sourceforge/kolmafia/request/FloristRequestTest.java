package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FloristRequestTest {
  @BeforeEach
  protected void beforeEach() {
    KoLCharacter.reset("FloristRequestTest");
    Preferences.reset("FloristRequestTest");
  }

  @Test
  public void unowned() {
    try (var cleanups =
        new Cleanups(
            withProperty("ownsFloristFriar", false), withProperty("floristFriarAvailable", true))) {
      FloristRequest.setFloristFriarAvailable(true);
      assertFalse(FloristRequest.haveFlorist());
    }
  }

  @Test
  public void unavailable() {
    try (var cleanups = new Cleanups(withProperty("ownsFloristFriar", true))) {
      String responseText = html("request/test_cant_get_there.html");
      FloristRequest.parseResponse("choice.php?whichchoice=720", responseText);
      assertFalse(FloristRequest.haveFlorist());
    }
  }

  @Test
  public void available() {
    try (var cleanups = new Cleanups(withProperty("ownsFloristFriar", true))) {
      String responseText = html("request/test_florist_friar.html");
      FloristRequest.parseResponse("choice.php?whichchoice=720", responseText);
      assertTrue(FloristRequest.haveFlorist());
    }
  }

  @Test
  public void availableLegacy() {
    try (var cleanups =
        new Cleanups(withPath(Path.LEGACY_OF_LOATHING), withProperty("ownsFloristFriar", true))) {
      String responseText = html("request/test_florist_friar.html");
      FloristRequest.parseResponse("choice.php?whichchoice=720", responseText);
      assertTrue(FloristRequest.haveFlorist());
    }
  }
}
