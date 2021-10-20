package net.sourceforge.kolmafia.extensions;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ClearSharedState implements AfterAllCallback {

  // Prevents leakage of shared state across test classes.

  @Override
  public void afterAll(ExtensionContext context) {
    // Among other things, this sets KoLCharacter.username.
    KoLCharacter.reset("");
    // But, if username is already "", then it doesn't do the bulk of resetting state.
    KoLCharacter.reset(true);
    KoLCharacter.setUserId(0);
    // If you explicitly want saveSettingsToFile to be true, then set it yourself.
    Preferences.saveSettingsToFile = false;

    KoLmafia.lastMessage = "";
    KoLmafia.forceContinue();
  }
}
