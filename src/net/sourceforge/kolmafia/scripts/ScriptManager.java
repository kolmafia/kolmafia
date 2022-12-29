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

  public static String getProjectIdentifier(String host, String path) {
    String UUID = null;
    Matcher m;

    if ((m = SOURCEFORGE_PATTERN.matcher(path)).find()) {
      // replace awful SVN UUID with nicely-formatted string derived from URL
      UUID = StringUtilities.globalStringReplace(m.group(1) + m.group(2), "/", "-"); //
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
    List<String> matches = new ArrayList<>();

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
      installDependency(args);
    }
  }

  private static void installDependency(String[] args) {
    var url = args[0];
    // check for special-case first arg
    if (url.equals("github")) {
      // github organisation/repo [branch]
      if (args.length < 2) {
        // invalid
        RequestLogger.printLine("Cannot parse dependency " + Arrays.toString(args));
        return;
      }
      String orgRepo = args[1];
      String branch = args.length == 2 ? null : args[2];
      // github-hosted projects can be installed using either git or svn, but prefer git
      // is it installed using git?
      String gitUrl = "https://github.com/" + orgRepo + ".git";
      if (gitDepInstalled(gitUrl, branch)) return;
      // is it installed using svn?
      String path = "/" + orgRepo + (branch != null ? "/branches/" + branch : "/trunk");
      String svnId = getProjectIdentifier("https://github.com", path);
      if (Files.exists(KoLConstants.SVN_LOCATION.toPath().resolve(svnId))) return;
      // it is not installed. Install using git
      GitManager.clone(gitUrl, branch);
      return;
    }
    if (args.length > 1 || url.endsWith(".git")) {
      // git
      String branch = args.length == 1 ? null : args[1];
      installGitDependency(url, branch);
    } else {
      installSvnDependency(url);
    }
  }

  private static boolean gitDepInstalled(String url, String branch) {
    var id = GitManager.getRepoId(url, branch);
    return Files.exists(KoLConstants.GIT_LOCATION.toPath().resolve(id));
  }

  private static void installGitDependency(String url, String branch) {
    if (!gitDepInstalled(url, branch)) {
      GitManager.clone(url, branch);
    }
  }

  private static void installSvnDependency(String url) {
    SVNURL repo;
    try {
      repo = SVNURL.parseURIEncoded(url);
    } catch (SVNException e) {
      RequestLogger.printLine("Cannot parse \"" + url + "\" as SVN URL");
      return;
    }
    var id = SVNManager.getFolderUUID(repo);
    if (!Files.exists(KoLConstants.SVN_LOCATION.toPath().resolve(id))) {
      SVNManager.doCheckout(repo);
    }
  }
}
