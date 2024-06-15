package internal.extensions;

import static internal.extensions.CheckNested.isNested;

import java.nio.file.Path;
import java.nio.file.Paths;
import net.sourceforge.kolmafia.KoLConstants;
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
    ClearSharedStateBefore.deleteDirectoriesAndContents();
    // These files are leaked into data/
    // pulvereport.txt comes from a disabled test of DebugDatabase
    // test_stringbuffer_function_with_consstring.txt comes from the CustomScript
    // stringbuffer_function_with_consstring.js
    String[] filesToDelete = {
      "mallprices.txt", "pulvereport.txt", "test_stringbuffer_function_with_consstring.txt"
    };
    for (String s : filesToDelete) {
      Path dest = Paths.get(KoLConstants.ROOT_LOCATION + "/data/" + s);
      dest.toFile().delete();
    }
  }
}
