package net.sourceforge.kolmafia.extensions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import net.sourceforge.kolmafia.*;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.textui.command.AbstractCommand;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ClearSharedState implements BeforeAllCallback {

  // Prevents leakage of shared state across test classes.

  @Override
  public void beforeAll(ExtensionContext context) {
    restoreUserStateToNoUser();
    restoreOtherStates();
    deleteDirectoriesAndContents();
  }

  public void restoreUserStateToNoUser() {
    KoLCharacter.reset("");
    // But, if username is already "", then it doesn't do the bulk of resetting state.
    KoLCharacter.reset(true);
    KoLCharacter.setUserId(0);
  }

  public void restoreOtherStates() {
    Preferences.saveSettingsToFile = false;
    // re-register default commands
    AbstractCommand.clear();
    KoLCharacter.setCurrentRun(0);
    KoLmafiaCLI.registerCommands();
    KoLmafia.lastMessage = KoLmafia.NO_MESSAGE;
    KoLmafia.forceContinue();
    StaticEntity.overrideRevision(null);
    GenericRequest.sessionId = null;
    GenericRequest.passwordHash = "";
  }

  public void deleteDirectoriesAndContents() {
    // These are the directories and files in test\root that are under git control.  Everything
    // else is fair game for deletion after testing.  Keep relay as well since at least one test
    // assumes relay had content created when mafia starts up.
    String[] keepersArray = {"ccs", "data", "expected", "request", "relay", "scripts", "README"};
    List<String> keepersList = new ArrayList<>(Arrays.asList(keepersArray));
    // Get list of things in test\root and iterate, deleting as appropriate
    File root = KoLConstants.ROOT_LOCATION;
    String[] contents = root.list();
    for (String content : contents) {
      if (!keepersList.contains(content)) {
        Path pathToBeDeleted = new File(root, content).toPath();
        try {
          Files.walk(pathToBeDeleted)
              .sorted(Comparator.reverseOrder())
              .map(Path::toFile)
              .forEach(File::delete);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }
}
