package internal.helpers;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.sourceforge.kolmafia.KoLConstants;

public class Utilities {

  public static void verboseDelete(Path path) {
    File deleteMe = path.toFile();
    verboseDelete(deleteMe);
  }

  public static void verboseDelete(String name) {
    File deleteMe = new File(name);
    verboseDelete(deleteMe);
  }

  public static void verboseDelete(File deleteMe) {
    String fileName = deleteMe.toString();
    if (deleteMe.exists()) {
      try {
        boolean deleted = deleteMe.delete();
        if (!deleted) {
          System.out.println(
              fileName
                  + " not deleted but no exception thrown. Still exists? "
                  + deleteMe.exists());
        }
      } catch (Exception e) {
        System.out.println(fileName + " not deleted with exception " + e);
      }
    }
  }

  public static void deleteSerFiles(String username) {
    String part = username.toLowerCase();
    Path dest = Paths.get(KoLConstants.ROOT_LOCATION + "/data/" + part + "_queue.ser");
    verboseDelete(dest);
    dest = Paths.get(KoLConstants.ROOT_LOCATION + "/data/" + part + "_turns.ser");
    verboseDelete(dest);
  }
}
