package internal.extensions;

import static internal.extensions.CheckNested.isNested;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import net.sourceforge.kolmafia.*;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ClearSharedStateAfter implements AfterAllCallback {

  // Prevents leakage of shared state across test classes.

  @Override
  public void afterAll(ExtensionContext context) {
    if (isNested(context)) return;
    deleteDirectoriesAndContents();
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
    if (contents != null) {
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
    // These files are leaked into data/
    // pulvereport.txt comes from a disabled test of DebugDatabase
    // test_stringbuffer_function_with_consstring.txt comes from the CustomScript
    // stringbuffer_function_with_consstring.js
    // content-types.properties comes from initialization to get around a Rhino issue noted in
    // https://github.com/mozilla/rhino/issues/1232
    String[] filesToDelete = {
      "pulvereport.txt",
      "test_stringbuffer_function_with_consstring.txt",
      "content-types.properties"
    };
    for (String s : filesToDelete) {
      Path dest = Paths.get(KoLConstants.ROOT_LOCATION + "/data/" + s);
      dest.toFile().delete();
    }
  }
}
