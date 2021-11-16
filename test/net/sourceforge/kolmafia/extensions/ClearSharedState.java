package net.sourceforge.kolmafia.extensions;

import java.io.File;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.textui.command.AbstractCommand;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ClearSharedState implements AfterAllCallback {

  // Prevents leakage of shared state across test classes.

  @Override
  public void afterAll(ExtensionContext context) {
    // Clean up user files
    deleteUserPrefsAndMoodsFiles(KoLCharacter.baseUserName());
    // and GLOBALS
    deleteGlobals();
    // Among other things, this sets KoLCharacter.username.
    KoLCharacter.reset("");
    // But, if username is already "", then it doesn't do the bulk of resetting state.
    KoLCharacter.reset(true);
    KoLCharacter.setUserId(0);
    // If you explicitly want saveSettingsToFile to be true, then set it yourself.
    Preferences.saveSettingsToFile = false;

    // re-register default commands
    AbstractCommand.clear();
    KoLmafiaCLI.registerCommands();
    KoLmafia.lastMessage = KoLmafia.NO_MESSAGE;
    KoLmafia.forceContinue();
  }

  public static void deleteUserPrefsAndMoodsFiles(String user) {
    String begin = "settings/" + user;
    File file = new File(begin + "_prefs.txt");
    if (file.exists()) {
      file.deleteOnExit();
    }
    file = new File(begin + "_moods.txt");
    if (file.exists()) {
      file.deleteOnExit();
    }
  }

  public static void deleteGlobals() {
    deleteUserPrefsAndMoodsFiles("GLOBAL");
    File file = new File("settings/GLOBAL_aliases.txt");
    if (file.exists()) {
      file.deleteOnExit();
    }
  }
}
