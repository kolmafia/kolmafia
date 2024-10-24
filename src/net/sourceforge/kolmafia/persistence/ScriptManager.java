package net.sourceforge.kolmafia.persistence;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.stream.Stream;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.scripts.git.GitManager;
import net.sourceforge.kolmafia.scripts.svn.SVNManager;
import net.sourceforge.kolmafia.utilities.ByteBufferUtilities;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;

public class ScriptManager {
  private ScriptManager() {}

  private static final SortedListModel<Script> installedScripts = new SortedListModel<>();
  private static final SortedListModel<Script> repoScripts = new SortedListModel<>();
  private static final String REPO_FILE_LOCATION =
      // this will change
      "https://raw.githubusercontent.com/kolmafia/kolmafia/main/data/SVN/svnrepo.json";

  public static void updateRepoScripts(boolean force) {
    File repoFile = KoLConstants.SVN_REPO_FILE;
    if (force || !repoFile.exists() || !Preferences.getBoolean("_svnRepoFileFetched")) {
      FileUtilities.downloadFile(REPO_FILE_LOCATION, KoLConstants.SVN_REPO_FILE, true);
      Preferences.setBoolean("_svnRepoFileFetched", true);
    }
    JSONArray jArray = ScriptManager.getJSONArray();
    updateRepoState(jArray);
  }

  private static JSONArray getJSONArray() {
    File repoFile = KoLConstants.SVN_REPO_FILE;

    if (!repoFile.exists()) {
      return null;
    } else {
      if (repoFile.length() <= 0) {
        repoFile.delete();
        return null;
      }
    }

    byte[] bytes = ByteBufferUtilities.read(repoFile);
    String string = new String(bytes, StandardCharsets.UTF_8);

    try {
      return JSON.parseArray(string);
    } catch (JSONException e) {
      // This file is evidently bad. Delete it so it doesn't keep causing problems.
      repoFile.delete();
      StaticEntity.printStackTrace(e);
    }

    return null;
  }

  private static void updateRepoState(JSONArray jArray) {
    repoScripts.clear();
    installedScripts.clear();
    if (jArray == null) return;

    HashSet<Path> knownScripts = new HashSet<>();

    for (var obj : jArray) {
      if (!(obj instanceof JSONObject jNext)) {
        throw new JSONException(
            "The JSON input file was not properly formatted: " + obj.toString());
      }

      Script script = new Script(jNext);
      script.checkInstalled();
      if (script.isInstalled()) {
        installedScripts.add(script);
        knownScripts.add(script.getScriptFolder());
      } else {
        repoScripts.add(script);
      }
    }

    try (Stream<Path> paths = Files.list(KoLConstants.SVN_LOCATION.toPath())) {
      paths
          .filter(Files::isDirectory)
          .filter(p -> !knownScripts.contains(p))
          .forEach(
              p -> {
                SVNURL repo;
                try {
                  repo = SVNManager.workingCopyToSVNURL(p.toFile());
                } catch (SVNException e) {
                  // not an SVN repo, continue
                  return;
                }
                installedScripts.add(new Script(p.getFileName().toString(), repo.toString(), p));
              });
    } catch (IOException e) {
      // failed to list folders, just continue
    }

    try (Stream<Path> paths = Files.list(KoLConstants.GIT_LOCATION.toPath())) {
      paths
          .filter(Files::isDirectory)
          .filter(p -> !knownScripts.contains(p))
          .forEach(
              p -> {
                var details = GitManager.getRepoDetails(p);
                if (details == null) {
                  // not a git repo, continue
                  return;
                }
                installedScripts.add(
                    new Script(
                        p.getFileName().toString(), details.repoUrl(), details.branchName(), p));
              });
    } catch (IOException e) {
      // failed to list folders, just continue
    }
  }

  public static LockableListModel<Script> getInstalledScripts() {
    return installedScripts;
  }

  public static LockableListModel<Script> getRepoScripts() {
    return repoScripts;
  }
}
