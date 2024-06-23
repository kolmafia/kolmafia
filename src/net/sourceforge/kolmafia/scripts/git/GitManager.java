package net.sourceforge.kolmafia.scripts.git;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.scripts.ScriptManager;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RebaseCommand.Operation;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.json.JSONException;
import org.json.JSONObject;

public class GitManager extends ScriptManager {

  /*
   * Like SVNManager, but for Git.
   *
   * Scripts with folders as in ScriptManager.permissibles have those folders copied to local.
   * Additional scripts in a "dependencies.txt" file are downloaded.
   */

  protected static final String MANIFEST = "manifest.json";
  protected static final String MANIFEST_ROOTDIR = "root_directory";

  public static void clone(String repoUrl) {
    clone(repoUrl, null);
  }

  public static boolean clone(String repoUrl, String branch) {
    String id = getRepoId(repoUrl, branch);
    Path projectPath = KoLConstants.GIT_LOCATION.toPath().resolve(id);
    if (Files.exists(projectPath)) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR,
          "Cannot clone project to " + id + ", folder already exists. Please delete to checkout.");
      return false;
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
      sync(projectPath);
    } catch (InvalidRemoteException e) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Could not find project at " + repoUrl + ": " + e);
      return false;
    } catch (GitAPIException e) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Could not download project " + repoUrl + ": " + e);
      return false;
    }

    KoLmafia.updateDisplay("Cloned project " + id);
    return true;
  }

  /** Update all installed projects. */
  public static void updateAll() {
    for (var project : allFolders()) {
      update(project);
    }

    Preferences.setBoolean("_gitUpdated", true);
  }

  /**
   * Given a project substring, update the version in git/ to latest, and update any existing
   * permissible files.
   *
   * <p>If there are any new files, add those.
   *
   * <p>If any files have been deleted, delete them.
   */
  public static boolean update(String project) {
    var folderOpt = getRequiredProject(project);
    if (folderOpt.isEmpty()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Cannot find unique match for " + project);
      return false;
    }
    var folder = folderOpt.get();
    Path projectPath = KoLConstants.GIT_LOCATION.toPath().resolve(folder);
    var oldRoot = getRoot(projectPath);
    Git git;
    try {
      git = Git.open(projectPath.toFile());
    } catch (IOException e) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Failed to open project " + folder + ": " + e);
      return false;
    }
    // update repo, then find out what was updated
    try (git) {
      var repo = git.getRepository();
      AbstractTreeIterator currTree;
      AbstractTreeIterator incomingTree;
      ObjectId currCommit;
      ObjectId incomingCommit;
      try {
        currCommit = getCurrentCommit(repo);
        currTree = getCurrentCommitTree(repo);
      } catch (IOException e) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "Failed to get details for project " + folder + ": " + e);
        return false;
      }

      RequestLogger.printLine("Updating project " + folder);
      try {
        if (!rebase(folder, git)) {
          KoLmafia.updateDisplay(
              MafiaState.ERROR,
              "Failed to update project "
                  + folder
                  + ": rebase error. Perhaps there are local changes we are unable to automatically reconcile. Consider deleting and re-installing project");
          return false;
        }
      } catch (GitAPIException e) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Failed to update project " + folder + ": " + e);
        return false;
      }
      var newRoot = getRoot(projectPath);

      if (!oldRoot.equals(newRoot)) {
        // the root directory has changed. Figuring out the diff is too hard, just sync
        return sync(projectPath);
      }

      try {
        incomingCommit = getCurrentCommit(repo);
        incomingTree = getCurrentCommitTree(repo);
      } catch (IOException e) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "Failed to get incoming changes for project " + folder + ": " + e);
        return false;
      }

      List<DiffEntry> diffs;
      try {
        var cmd =
            git.diff().setOldTree(currTree).setNewTree(incomingTree).setShowNameAndStatusOnly(true);
        if (!projectPath.equals(newRoot)) {
          var relFilter = projectPath.relativize(newRoot);
          var filter = PathFilter.create(relFilter.toString().replace(File.separatorChar, '/'));
          cmd = cmd.setPathFilter(filter);
        }
        diffs = cmd.call();
      } catch (GitAPIException e) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "Failed to diff incoming changes for project " + folder + ": " + e);
        return false;
      }

      if (diffs.size() == 0) {
        RequestLogger.printLine("No changes");
        return false;
      }

      boolean checkDependencies = false;

      for (var diff : diffs) {
        var oldDiffPath = diff.getOldPath();
        var oldRelPath = oldRoot.relativize(projectPath.resolve(oldDiffPath));
        var newDiffPath = diff.getNewPath();
        var newRelPath = newRoot.relativize(projectPath.resolve(newDiffPath));
        switch (diff.getChangeType()) {
          case ADD, MODIFY, COPY -> addNewFile(newRoot, newRelPath);
          case DELETE -> deleteOldFile(oldRelPath);
          case RENAME -> {
            deleteOldFile(oldRelPath);
            addNewFile(newRoot, newRelPath);
          }
        }

        if (DEPENDENCIES.equals(diff.getNewPath())) {
          checkDependencies = true;
        }
      }

      if (Preferences.getBoolean("gitShowCommitMessages")) {
        printCommitMessages(git, currCommit, incomingCommit, folder);
      }

      if (checkDependencies) {
        installDependencies(newRoot.resolve(DEPENDENCIES));
      }
    }
    return true;
  }

  private static boolean rebase(String folder, Git git) throws GitAPIException {
    var result = git.pull().setProgressMonitor(new MafiaProgressMonitor()).setRebase(true).call();
    var success = result.getRebaseResult().getStatus().isSuccessful();
    if (!success) {
      // the rebase failed. Does the user have any local changes?
      var hasLocal = git.diff().call().size() != 0;
      if (!hasLocal) return false;
      KoLmafia.updateDisplay("Detected local changes in " + folder + ". Attempting to merge.");
      // add all files
      git.add().addFilepattern(".").call();
      // make a commit
      git.commit().setMessage("local changes").setAuthor("KoLMafia", "KoLMafia@localhost").call();
      // try to rebase again
      result = git.pull().setProgressMonitor(new MafiaProgressMonitor()).setRebase(true).call();
      success = result.getRebaseResult().getStatus().isSuccessful();
    }
    if (git.getRepository().getRepositoryState().isRebasing()) {
      // cleanup
      git.rebase().setOperation(Operation.ABORT).call();
    }
    return success;
  }

  /** Delete a newly removed file in the correct permissible folder. */
  private static void deleteOldFile(Path path) {
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
  private static void addNewFile(Path projectPath, Path path) {
    if (isPermissibleFile(path)) {
      try {
        copyPath(projectPath.resolve(path), path);
      } catch (IOException e) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Failed to add file " + path + ": " + e);
      }
    }
  }

  /** Get the current commit. */
  private static ObjectId getCurrentCommit(Repository repo) throws IOException {
    return repo.resolve("HEAD");
  }

  /** Get the current commit as an AbstractTreeIterator, as required by DiffCommand. */
  private static AbstractTreeIterator getCurrentCommitTree(Repository repo) throws IOException {
    var currId = repo.resolve("HEAD^{tree}");
    var treeIterator = new CanonicalTreeParser();
    treeIterator.reset(repo.newObjectReader(), currId);
    return treeIterator;
  }

  /** Print commit messages from since to until */
  private static void printCommitMessages(Git git, ObjectId since, ObjectId until, String folder) {
    Iterable<RevCommit> commits;
    try {
      commits = git.log().addRange(since, until).call();
    } catch (IOException | GitAPIException e) {
      KoLmafia.updateDisplay(
          MafiaState.CONTINUE, "Failed to get commit messages for " + folder + ": " + e);
      return;
    }

    for (var commit : commits) {
      var author = commit.getAuthorIdent();
      var datetime = getCommitDate(commit, author);
      var date = formatCommitDate(datetime);
      var message = commit.getFullMessage();

      RequestLogger.printHtml("<b>commit " + ObjectId.toString(commit) + "</b>");
      RequestLogger.printLine("Author: " + getAuthor(author));
      RequestLogger.printLine("Date:   " + date);
      RequestLogger.printHtml(
          "<p style=\"text-indent:2em\">"
              + StringUtilities.getEntityEncode(message, false)
              + "</p>");
      RequestLogger.printHtml("<br>");
    }
  }

  /** Get a commit date with the author's time zone */
  private static ZonedDateTime getCommitDate(RevCommit commit, PersonIdent author) {
    return ZonedDateTime.ofInstant(
        Instant.ofEpochSecond(commit.getCommitTime()), author.getZoneId());
  }

  /** Format a commit date as it appears in the logs */
  public static String formatCommitDate(ZonedDateTime datetime) {
    // use format that is similar to what 'git show' gives, ex:
    // Date: Sat Jul 16 11:40:35 2022 +0100
    var fmt = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss yyyy z");
    return fmt.format(datetime);
  }

  /** Get a commit author for display */
  private static String getAuthor(PersonIdent author) {
    return author.getName() + " <" + author.getEmailAddress() + ">";
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
  public static boolean delete(String project) {
    var folderOpt = getRequiredProject(project);
    if (folderOpt.isEmpty()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Cannot find unique match for " + project);
      return false;
    }
    var folder = folderOpt.get();
    var projectPath = KoLConstants.GIT_LOCATION.toPath().resolve(folder);
    var root = getRoot(projectPath);
    KoLmafia.updateDisplay("Removing project " + folder);
    List<Path> toDelete;
    // get the files under the project root folder in the git/ directory that should be deleted
    try {
      toDelete = getPermissibleFiles(root, true);
    } catch (IOException e) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Failed to remove project " + folder + ": " + e);
      return false;
    }
    var errored = false;
    for (var absPath : toDelete) {
      var shortPath = root.relativize(absPath);
      var relPath = KoLConstants.ROOT_LOCATION.toPath().resolve(shortPath);
      try {
        // delete both from the root permissible folder, and the relative file in the project in
        // git/
        if (!Files.isDirectory(relPath) || FileUtilities.isEmptyDirectory(relPath)) {
          // if the folder is a non-empty directory, deletion will fail.
          // Deletion is ordered such that all script-relevant files have already been deleted.
          Files.deleteIfExists(relPath);
        }
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
        return false;
      }
    }
    if (!errored) {
      KoLmafia.updateDisplay("Project " + folder + " removed.");
      return true;
    } else {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Failed to completely remove project " + folder);
      return false;
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
    sync(projectPath);
  }

  private static boolean sync(Path projectPath) {
    var folder = KoLConstants.GIT_LOCATION.toPath().relativize(projectPath);
    var root = getRoot(projectPath);
    List<Path> toAdd;
    try {
      toAdd = getPermissibleFiles(root, false);
    } catch (IOException e) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Failed to sync project " + folder + ": " + e);
      return false;
    }
    IOException lastError = null;
    for (var absPath : toAdd) {
      try {
        var toRel = root.relativize(absPath);
        copyPath(absPath, toRel);
      } catch (IOException e) {
        // other files might succeed, so keep going
        lastError = e;
        continue;
      }
    }
    if (lastError != null) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "Failed to sync project " + folder + ": " + lastError);
      return false;
    }
    var deps = root.resolve(DEPENDENCIES);
    if (Files.exists(deps)) {
      installDependencies(deps);
    }
    return true;
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
      var datetime = getCommitDate(commit, author);

      return Optional.of(
          new GitInfo(url, branch, ObjectId.toString(lastCommitId), getAuthor(author), datetime));
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

  private static boolean isPermissibleFile(Path path) {
    return permissibles.stream().anyMatch(path::startsWith);
  }

  private static void copyPath(Path absPath, Path shortPath) throws IOException {
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

  public static String getRepoId(String repoUrl, String branch) {
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
    var folders = KoLConstants.GIT_LOCATION.list((x, y) -> new File(x, y).isDirectory());
    if (folders == null) return new String[0];
    return folders;
  }

  private static Optional<String> getRequiredProject(String project) {
    var matches = getMatchingNames(allFolders(), project);
    if (matches.size() != 1) return Optional.empty();
    return Optional.of(matches.get(0));
  }

  protected static void installDependencies(Path dependencies) {
    if (!Preferences.getBoolean("gitInstallDependencies")) return;

    KoLmafia.updateDisplay("Installing dependencies");
    ScriptManager.installDependencies(dependencies);
  }

  private static Optional<JSONObject> readManifest(Path manifest) {
    if (!Files.exists(manifest)) return Optional.empty();

    JSONObject json;
    try {
      json = new JSONObject(Files.readString(manifest));
    } catch (IOException | JSONException e) {
      return Optional.empty();
    }
    return Optional.of(json);
  }

  private static Path getRoot(Path projectPath) {
    var json = readManifest(projectPath.resolve(MANIFEST));
    if (json.isEmpty()) return projectPath;
    var manifest = json.get();
    var root = manifest.optString(MANIFEST_ROOTDIR, "");
    if (root.length() == 0) return projectPath;
    // deny absolute paths or folder escapes
    if (root.startsWith("/") || root.startsWith("\\") || root.contains("..")) return projectPath;
    return projectPath.resolve(root);
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

    @Override
    public void showDuration(boolean b) {
      // say nothing
    }
  }

  public record RepoDetails(String repoUrl, String branchName) {}

  public static RepoDetails getRepoDetails(Path p) {
    try (Git git = Git.open(p.toFile())) {
      var repo = git.getRepository();
      return new RepoDetails(
          repo.getConfig().getString("remote", "origin", "url"), repo.getBranch());
    } catch (IOException e) {
      // not a git repo
      return null;
    }
  }
}
