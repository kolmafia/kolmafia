package internal.extensions;

import static internal.extensions.CheckNested.isNested;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.persistence.AdventureSpentDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.StandardRequest;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.LimitMode;
import net.sourceforge.kolmafia.textui.command.AbstractCommand;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ClearSharedStateBefore implements BeforeAllCallback {

  // Prevents leakage of shared state across test classes.

  @Override
  public void beforeAll(ExtensionContext context) {
    if (isNested(context)) return;
    restoreUserStateToNoUser();
    restoreOtherStates();
    deleteDirectoriesAndContents();
  }

  public void restoreUserStateToNoUser() {
    KoLCharacter.reset("");
    // But, if username is already "", then it doesn't do the bulk of resetting state.
    KoLCharacter.reset(true);
    Preferences.reset("");
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
    AdventureSpentDatabase.setNoncombatEncountered(false);
    RelayRequest.reset();
    StandardRequest.reset();
    KoLConstants.activeEffects.clear();
    KoLCharacter.setLimitMode(LimitMode.NONE);
    FightRequest.currentRound = 0;
    ChoiceManager.handlingChoice = false;
    ChoiceManager.reset();
  }

  public static void deleteDirectoriesAndContents() {
    // These are the directories and files in test\root that are under git control.  Everything
    // else is fair game for deletion after testing.
    String[] keepersArray = {"ccs", "data", "expected", "request", "scripts", "README"};
    List<String> keepersList = new ArrayList<>(Arrays.asList(keepersArray));
    // Get list of things in test\root and iterate, deleting as appropriate
    File root = KoLConstants.ROOT_LOCATION;
    String[] contents = root.list();
    if (contents != null) {
      for (String content : contents) {
        if (!keepersList.contains(content) && !(content.toLowerCase().startsWith("debug"))) {
          Path pathToBeDeleted = new File(root, content).toPath();
          try (var walker = Files.walk(pathToBeDeleted)) {
            walker.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
          } catch (IOException e) {
            System.out.println(
                "Unexpected exception when walking root/ for deletions: " + e.getMessage());
          }
        }
      }
    }
  }
}
