package net.sourceforge.kolmafia.scripts.git;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.scripts.ScriptManager;
import net.sourceforge.kolmafia.scripts.svn.SVNManager;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;

public class GitManager extends ScriptManager {

  /*
   * Like SVNManager, but for Git.
   *
   * Acceptable scripts have folders as in ScriptManager.permissibles and possibly a
   * "dependencies.txt" file. This file contains additional scripts to download. For now these have
   * to be SVN.
   *
   * Local changes will not be supported.
   */

  public static void clone(String repoUrl) {
    clone(repoUrl, null);
  }

  public static void clone(String repoUrl, String branch) {
    String id = getRepoId(repoUrl, branch);
    Path projectPath = KoLConstants.GIT_LOCATION.toPath().resolve(id);
    if (Files.exists(projectPath)) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Cannot clone project to " + id + ", folder already exists. Please delete to checkout.");
      return;
    }
    var git =
        Git.cloneRepository()
            .setURI(repoUrl)
            .setCloneAllBranches(false)
            .setDirectory(projectPath.toFile())
            .setProgressMonitor(new MafiaProgressMonitor());
    if (branch != null) {
      git.setBranch(branch).setBranchesToClone(List.of("refs/heads/" + branch));
    }
    try (var ignored = git.call()) {
      var toAdd = getPermissibleFiles(projectPath, false);
      for (var absPath : toAdd) {
        var shortPath = projectPath.relativize(absPath);
        var rootPath = KoLConstants.ROOT_LOCATION.toPath();
        var relPath = rootPath.resolve(shortPath);
        try {
          if (!Files.isDirectory(relPath)) {
            KoLmafia.updateDisplay("Copying: " + shortPath);
            Files.copy(absPath, relPath, REPLACE_EXISTING);
          }
        } catch (IOException e) {
          KoLmafia.updateDisplay(MafiaState.ERROR, "Failed to clone project " + id + ": " + e);
          return;
        }
      }
    } catch (InvalidRemoteException e) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Could not find project at " + repoUrl + ": " + e);
      return;
    } catch (GitAPIException e) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Could not download project " + repoUrl + ": " + e);
      return;
    } catch (IOException e) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Failed to clone project " + repoUrl + ": " + e);
      return;
    }

    KoLmafia.updateDisplay("Cloned project " + id);
    Path deps = projectPath.resolve("dependencies.txt");
    if (Files.exists(deps)) {
      KoLmafia.updateDisplay("Installing dependencies");
      installDependencies(deps);
    }
  }

  /** Update all installed projects. */
  public static void updateAll() {
    for (var project : allFolders()) {
      update(project);
    }
  }

  /**
   * Given a project substring, update the version in git/ to latest, and update any scripts/ or
   * relay/ or planting/ files.
   *
   * <p>If there are any new ccs/ or data/ or images/ files, add those.
   *
   * <p>If any files have been deleted, delete them.
   */
  public static void update(String project) {
    var folderOpt = getRequiredProject(project);
    if (folderOpt.isEmpty()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Cannot find unique match for " + project);
      return;
    }
    var folder = folderOpt.get();
    // TODO
  }

  /** Return all installed git projects */
  public static List<String> listAll() {
    var files = allFolders();
    if (files == null) return List.of();
    return List.of(files);
  }

  /** Given a project substring, return all matching projects. */
  public static List<String> list(String filter) {
    var projects = listAll();
    return getMatchingNames(projects.toArray(new String[0]), filter);
  }

  /** Given a project substring, remove the folder in git/ and any permissible files. */
  public static void delete(String project) {
    var folderOpt = getRequiredProject(project);
    if (folderOpt.isEmpty()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Cannot find unique match for " + project);
      return;
    }
    var folder = folderOpt.get();
    var projectPath = KoLConstants.GIT_LOCATION.toPath().resolve(folder);
    KoLmafia.updateDisplay("Removing project " + folder);
    List<Path> toDelete;
    try {
      toDelete = getPermissibleFiles(projectPath, true);
    } catch (IOException e) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Failed to remove project " + project + ": " + e);
      return;
    }
    var errored = false;
    for (var absPath : toDelete) {
      var shortPath = projectPath.relativize(absPath);
      var relPath = KoLConstants.ROOT_LOCATION.toPath().resolve(shortPath);
      try {
        Files.deleteIfExists(relPath);
        Files.delete(absPath);
        KoLmafia.updateDisplay(shortPath + " => DELETED");
      } catch (IOException e) {
        KoLmafia.updateDisplay(shortPath + " failed to delete");
        errored = true;
      }
    }
    if (!errored) {
      try (var walk = Files.walk(projectPath)) {
        for (var f : walk.sorted(Comparator.reverseOrder()).toList()) {
          try {
            Files.delete(f);
          } catch (IOException e) {
            // probably a Windows issue removing .git\objects\pack\whatever
            // try to move to temporary directory instead
            try {
              var tmp = Files.createTempDirectory("mafia");
              Files.move(f, tmp.resolve(f.getFileName()), REPLACE_EXISTING);
            } catch (IOException ex) {
              // Just tell user to delete it manually, that works
              KoLmafia.updateDisplay(
                  MafiaState.ERROR, "Failed to delete " + f + ": " + e + ", " + ex + ". Please remove manually.");
              errored = true;
            }
            // but continue
          }
        }
      } catch (IOException e) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "Failed to completely remove project " + folder + ": " + e);
        return;
      }
    }
    if (!errored) {
      KoLmafia.updateDisplay("Project " + folder + " removed.");
    } else {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Failed to completely remove project " + folder);
    }
  }

  private static List<Path> getPermissibleFiles(Path startPath, boolean reverse)
      throws IOException {
    List<Path> files = new ArrayList<>();
    for (var p : permissibles) {
      var subFolder = startPath.resolve(p);
      if (!Files.exists(subFolder)) continue;
      try (var walk = Files.walk(subFolder)) {
        var stream = walk;
        if (reverse) {
          stream = stream.sorted(Comparator.reverseOrder());
        }
        stream
            .filter(s -> !permissibles.contains(startPath.relativize(s).toString()))
            .forEach(files::add);
      }
    }
    return files;
  }

  private static String getRepoId(String repoUrl, String branch) {
    String dashBranch = branch == null ? "" : "-" + branch;
    if (repoUrl.endsWith(".git")) {
      repoUrl = repoUrl.substring(0, repoUrl.length() - 4);
    }
    URI uri;
    try {
      uri = new URI(repoUrl);
    } catch (URISyntaxException e) {
      return (repoUrl + dashBranch).replaceAll("https?://", "").replaceAll("/", "-");
    }
    String uuid = getProjectIdentifier(uri.getHost(), uri.getPath());
    return uuid + dashBranch;
  }

  private static String[] allFolders() {
    return KoLConstants.GIT_LOCATION.list();
  }

  private static Optional<String> getRequiredProject(String project) {
    var matches = getMatchingNames(allFolders(), project);
    if (matches.size() != 1) return Optional.empty();
    return Optional.of(matches.get(0));
  }

  private static void installDependencies(Path dependencies) {
    List<String> potentials;
    try {
      potentials = Files.readAllLines(dependencies);
    } catch (IOException e) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Failed to read dependency file " + dependencies);
      return;
    }
    for (var potential : potentials) {
      if (potential.startsWith("#")) continue;
      SVNURL repo;
      try {
        repo = SVNURL.parseURIEncoded(potential);
      } catch (SVNException e) {
        RequestLogger.printLine("Cannot parse " + potential + " as SVN URL");
        continue;
      }
      var id = SVNManager.getFolderUUIDNoRemote(repo);
      if (!Files.exists(KoLConstants.SVN_LOCATION.toPath().resolve(id))) {
        SVNManager.doCheckout(repo);
      }
    }
  }

  private static class MafiaProgressMonitor implements ProgressMonitor {
    @Override
    public void start(int totalTasks) {
      RequestLogger.printLine("Starting");
    }

    @Override
    public void beginTask(String title, int totalWork) {
      RequestLogger.printLine(title);
    }

    @Override
    public void update(int completed) {
      // say nothing
    }

    @Override
    public void endTask() {
      RequestLogger.printLine("Done.");
    }

    @Override
    public boolean isCancelled() {
      return false;
    }
  }
}
