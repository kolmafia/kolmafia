package net.sourceforge.kolmafia.scripts.git;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZonedDateTime;
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
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;

public class GitManager extends ScriptManager {

  /*
   * Like SVNManager, but for Git.
   *
   * Scripts with folders as in ScriptManager.permissibles have those folder copied to local.
   * Additional scripts in a "dependencies.txt" file are downloaded.
   *
   * Local changes are not currently supported.
   */

  public static void clone(String repoUrl) {
    clone(repoUrl, null);
  }

  public static void clone(String repoUrl, String branch) {
    String id = getRepoId(repoUrl, branch);
    Path projectPath = KoLConstants.GIT_LOCATION.toPath().resolve(id);
    if (Files.exists(projectPath)) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR,
          "Cannot clone project to " + id + ", folder already exists. Please delete to checkout.");
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
        try {
          copyPath(projectPath, absPath);
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
    Path deps = projectPath.resolve(DEPENDENCIES);
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
   * Given a project substring, update the version in git/ to latest, and update any existing
   * permissible files.
   *
   * <p>If there are any new files, add those.
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
    Path projectPath = KoLConstants.GIT_LOCATION.toPath().resolve(folder);
    Git git;
    try {
      git = Git.open(projectPath.toFile());
    } catch (IOException e) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Failed to open project " + folder + ": " + e);
      return;
    }
    // update repo, then find out what was updated
    try (git) {
      var repo = git.getRepository();
      AbstractTreeIterator currTree;
      AbstractTreeIterator incomingTree;
      try {
        currTree = getCurrentCommitTree(repo);
      } catch (IOException e) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "Failed to get details for project " + folder + ": " + e);
        return;
      }

      RequestLogger.printLine("Updating project " + folder);
      try {
        git.pull().setProgressMonitor(new MafiaProgressMonitor()).setRebase(true).call();
      } catch (GitAPIException e) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Failed to update project " + folder + ": " + e);
        return;
      }

      try {
        incomingTree = getCurrentCommitTree(repo);
      } catch (IOException e) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "Failed to get incoming changes for project " + folder + ": " + e);
        return;
      }

      List<DiffEntry> diffs;
      try {
        diffs =
            git.diff()
                .setOldTree(currTree)
                .setNewTree(incomingTree)
                .setShowNameAndStatusOnly(true)
                .call();
      } catch (GitAPIException e) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "Failed to diff incoming changes for project " + folder + ": " + e);
        return;
      }

      boolean checkDependencies = false;

      for (var diff : diffs) {
        switch (diff.getChangeType()) {
          case ADD, MODIFY, COPY -> addNewFile(projectPath, diff);
          case DELETE -> deleteOldFile(diff);
          case RENAME -> {
            deleteOldFile(diff);
            addNewFile(projectPath, diff);
          }
        }

        if (DEPENDENCIES.equals(diff.getNewPath())) {
          checkDependencies = true;
        }
      }

      if (checkDependencies) {
        installDependencies(projectPath.resolve(DEPENDENCIES));
      }
    }
  }

  /** Delete a newly removed file in the correct permissible folder. */
  private static void deleteOldFile(DiffEntry diff) {
    var path = diff.getOldPath();
    if (isPermissibleFile(path)) {
      try {
        var rootPath = KoLConstants.ROOT_LOCATION.toPath();
        var relPath = rootPath.resolve(path);
        KoLmafia.updateDisplay("Deleting: " + path);
        Files.deleteIfExists(relPath);
      } catch (IOException e) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Failed to delete file " + path + ": " + e);
      }
    }
  }

  /** Create or replace a newly added file in the correct permissible folder. */
  private static void addNewFile(Path projectPath, DiffEntry diff) {
    var path = diff.getNewPath();
    if (isPermissibleFile(path)) {
      try {
        copyPath(projectPath, projectPath.resolve(path));
      } catch (IOException e) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Failed to add file " + path + ": " + e);
      }
    }
  }

  /** Get the current commit as an AbstractTreeIterator, as required by DiffCommand. */
  private static AbstractTreeIterator getCurrentCommitTree(Repository repo) throws IOException {
    var currId = repo.resolve("HEAD^{tree}");
    var treeIterator = new CanonicalTreeParser();
    treeIterator.reset(repo.newObjectReader(), currId);
    return treeIterator;
  }

  /** Return all installed git projects */
  public static List<String> listAll() {
    var files = allFolders();
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
      KoLmafia.updateDisplay(MafiaState.ERROR, "Failed to remove project " + folder + ": " + e);
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
                  MafiaState.ERROR,
                  "Failed to delete " + f + ": " + e + ", " + ex + ". Please remove manually.");
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

  /** Copy files from all installed git projects to permissible folders. */
  public static void syncAll() {
    var files = allFolders();
    for (var file : files) {
      sync(file);
    }
  }

  /** Copy files from specific project to permissible folders. */
  public static void sync(String project) {
    var folderOpt = getRequiredProject(project);
    if (folderOpt.isEmpty()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Cannot find unique match for " + project);
      return;
    }
    var folder = folderOpt.get();
    Path projectPath = KoLConstants.GIT_LOCATION.toPath().resolve(folder);
    List<Path> toAdd;
    try {
      toAdd = getPermissibleFiles(projectPath, false);
    } catch (IOException e) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Failed to sync project " + folder + ": " + e);
      return;
    }
    for (var absPath : toAdd) {
      try {
        copyPath(projectPath, absPath);
      } catch (IOException e) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Failed to sync project " + folder + ": " + e);
        return;
      }
    }
    var deps = projectPath.resolve(DEPENDENCIES);
    if (Files.exists(deps)) {
      installDependencies(deps);
    }
  }

  public record GitInfo(
      String url,
      String branch,
      String commit,
      String lastChangedAuthor,
      ZonedDateTime lastChangedDate) {}

  /** Get info from specific project */
  public static Optional<GitInfo> getInfo(String project) {
    var folderOpt = getRequiredProject(project);
    if (folderOpt.isEmpty()) {
      return Optional.empty();
    }
    var folder = folderOpt.get();
    Path projectPath = KoLConstants.GIT_LOCATION.toPath().resolve(folder);
    Git git;
    try {
      git = Git.open(projectPath.toFile());
    } catch (IOException e) {
      return Optional.empty();
    }

    try (git) {
      var repo = git.getRepository();
      var config = repo.getConfig();
      var url = config.getString("remote", "origin", "url");
      var branch = repo.getBranch();

      var lastCommitId = repo.resolve("HEAD");
      var rw = new RevWalk(repo);
      var commit = rw.parseCommit(lastCommitId);
      var author = commit.getAuthorIdent();
      var datetime =
          ZonedDateTime.ofInstant(
              Instant.ofEpochSecond(commit.getCommitTime()), author.getZoneId());

      return Optional.of(
          new GitInfo(
              url,
              branch,
              ObjectId.toString(lastCommitId),
              author.getName() + " <" + author.getEmailAddress() + ">",
              datetime));
    } catch (IOException e) {
      // all or nothing
      return Optional.empty();
    }
  }

  /** Return whether project is a valid git repo */
  public static boolean isValidRepo(String project) {
    var projectPath = KoLConstants.GIT_LOCATION.toPath().resolve(project);
    if (!Files.isDirectory(projectPath)) return false;
    try {
      var git = Git.open(projectPath.toFile());
      git.close();
    } catch (IOException e) {
      return false;
    }
    return true;
  }

  /** Return whether project is up-to-date with remote */
  public static boolean isUpToDate(String project) {
    var projectPath = KoLConstants.GIT_LOCATION.toPath().resolve(project);
    Git git;
    try {
      git = Git.open(projectPath.toFile());
    } catch (IOException e) {
      return false;
    }

    try (git) {
      var repo = git.getRepository();
      var branch = repo.getBranch();
      var bts = BranchTrackingStatus.of(repo, branch);
      var behind = bts.getBehindCount();
      return behind == 0;
    } catch (IOException e) {
      return false;
    }
  }

  private static List<Path> getPermissibleFiles(Path startPath, boolean reverse)
      throws IOException {
    List<Path> files = new ArrayList<>();
    for (var p : permissibles) {
      var subFolder = startPath.resolve(p);
      if (!Files.exists(subFolder) || !Files.isDirectory(subFolder)) continue;
      try (var walk = Files.walk(subFolder)) {
        var stream = walk;
        if (reverse) {
          stream = stream.sorted(Comparator.reverseOrder());
        }
        stream
            // omit the permissible folders themselves
            .filter(s -> !permissibles.contains(startPath.relativize(s).toString()))
            .forEach(files::add);
      }
    }
    return files;
  }

  private static boolean isPermissibleFile(String path) {
    return permissibles.stream().anyMatch(p -> path.startsWith(p + "/"));
  }

  private static void copyPath(Path projectPath, Path absPath) throws IOException {
    var shortPath = projectPath.relativize(absPath);
    var rootPath = KoLConstants.ROOT_LOCATION.toPath();
    var relPath = rootPath.resolve(shortPath);
    if (!Files.isDirectory(relPath)) {
      KoLmafia.updateDisplay("Copying: " + shortPath);
      var parent = relPath.getParent();
      if (!Files.exists(parent)) {
        Files.createDirectories(parent);
      }
      Files.copy(absPath, relPath, REPLACE_EXISTING);
    }
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
    var folders = KoLConstants.GIT_LOCATION.list();
    if (folders == null) return new String[0];
    return folders;
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
      String[] args = potential.split("\\s+");
      if (args.length == 0) continue;
      var url = args[0];
      if (args.length > 1 || url.endsWith(".git")) {
        // git
        String branch = args.length == 1 ? null : args[1];
        var id = getRepoId(url, branch);
        if (!Files.exists(KoLConstants.GIT_LOCATION.toPath().resolve(id))) {
          GitManager.clone(url, branch);
        }
      } else {
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
      // say nothing
    }

    @Override
    public boolean isCancelled() {
      return false;
    }
  }
}
