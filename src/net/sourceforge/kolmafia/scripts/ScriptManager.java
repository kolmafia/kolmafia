package net.sourceforge.kolmafia.scripts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.utilities.StringUtilities;

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
}
