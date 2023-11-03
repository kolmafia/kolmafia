package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
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
  public void checked() {
    FloristRequest.checkFloristAvailable();
    assertFalse(FloristRequest.haveFlorist());
    assertFalse(Preferences.getBoolean("floristFriarChecked"));

    QuestDatabase.setQuest(Quest.LARVA, QuestDatabase.STARTED);
    FloristRequest.checkFloristAvailable();
    assertFalse(FloristRequest.haveFlorist());
    assertTrue(Preferences.getBoolean("floristFriarChecked"));
  }

  @Test
  public void unavailable() {
    String responseText = html("request/test_cant_get_there.html");
    FloristRequest.parseResponse("choice.php?whichchoice=720", responseText);
    assertFalse(FloristRequest.haveFlorist());
    assertFalse(Preferences.getBoolean("floristFriarChecked"));
  }

  @Test
  public void available() {
    String responseText = html("request/test_florist_friar.html");
    FloristRequest.parseResponse("choice.php?whichchoice=720", responseText);
    assertTrue(FloristRequest.haveFlorist());
    assertTrue(Preferences.getBoolean("floristFriarChecked"));
  }

  @Test
  public void availableLegacy() {
    KoLCharacter.setPath(Path.LEGACY_OF_LOATHING);
    String responseText = html("request/test_florist_friar.html");
    FloristRequest.parseResponse("choice.php?whichchoice=720", responseText);
    assertTrue(FloristRequest.haveFlorist());
    assertTrue(Preferences.getBoolean("floristFriarChecked"));
    assertFalse(Preferences.getBoolean("ownsFloristFriar"));
  }
}
