package net.sourceforge.kolmafia.scripts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.scripts.git.GitManager;
import net.sourceforge.kolmafia.scripts.svn.SVNManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;

public class ScriptManager {

  protected static final String DEPENDENCIES = "dependencies.txt";

  protected static final List<String> permissibles =
      Arrays.asList("scripts", "data", "images", "relay", "ccs", "planting");

  protected static final Pattern SOURCEFORGE_PATTERN =
      Pattern.compile("/p/(.*?)/(?:code|svn)(.*)", Pattern.DOTALL);
  protected static final Pattern GOOGLECODE_HOST_PATTERN =
      Pattern.compile("([^\\.]+)\\.googlecode\\.com", Pattern.DOTALL);

  public static String getProjectIdentifier(String host, String path) {
    String UUID = null;
    Matcher m;

    if ((m = SOURCEFORGE_PATTERN.matcher(path)).find()) {
      // replace awful SVN UUID with nicely-formatted string derived from URL
      UUID = StringUtilities.globalStringReplace(m.group(1) + m.group(2), "/", "-"); //
    } else if ((m = GOOGLECODE_HOST_PATTERN.matcher(host)).find()) {
      UUID = m.group(1) + StringUtilities.globalStringReplace(path.substring(4), "/", "-");
    } else if (host.contains("github")) {
      UUID = StringUtilities.globalStringReplace(path.substring(1), "/", "-");
    }
    return UUID;
  }

  /**
   * One-off implementation of StringUtilities.getMatchingNames.
   *
   * <p>The issue with the StringUtilities version is that it assumes that the list of names to
   * search against is canonicalized - i.e. all lower case. This cannot be done to directories since
   * case matters in some environments.
   *
   * @param projects the array of currently-installed projects
   * @param params the String input by the user to be matched
   * @return a <code>List</code> of matches
   */
  public static List<String> getMatchingNames(String[] projects, String params) {
    List<String> matches = new ArrayList<String>();

    for (String project : projects) {
      // exact matches return immediately, disregarding other substring matches
      if (project.equals(params)) {
        return Arrays.asList(params);
      }
      if (substringMatches(project, params)) {
        matches.add(project);
      }
    }

    return matches;
  }

  private static boolean substringMatches(final String source, final String substring) {
    if (substring == null || substring.length() == 0) {
      return true;
    }

    return source.contains(substring);
  }

  protected static void installDependencies(Path dependencies) {
    List<String> potentials;
    try {
      potentials = Files.readAllLines(dependencies);
    } catch (IOException e) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Failed to read dependency file " + dependencies);
      return;
    }
    for (var potential : potentials) {
      if (potential.startsWith("#")) continue;
      String[] args = potential.split("\\s+");
      if (args.length == 0 || args[0].length() == 0) continue;
      var url = args[0];
      if (args.length > 1 || url.endsWith(".git")) {
        // git
        String branch = args.length == 1 ? null : args[1];
        var id = GitManager.getRepoId(url, branch);
        if (!Files.exists(KoLConstants.GIT_LOCATION.toPath().resolve(id))) {
          GitManager.clone(url, branch);
        }
      } else {
        SVNURL repo;
        try {
          repo = SVNURL.parseURIEncoded(potential);
        } catch (SVNException e) {
          RequestLogger.printLine("Cannot parse \"" + potential + "\" as SVN URL");
          continue;
        }
        var id = SVNManager.getFolderUUID(repo);
        if (!Files.exists(KoLConstants.SVN_LOCATION.toPath().resolve(id))) {
          SVNManager.doCheckout(repo);
        }
      }
    }
  }
}
