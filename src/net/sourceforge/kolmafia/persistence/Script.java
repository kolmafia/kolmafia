package net.sourceforge.kolmafia.persistence;

import com.alibaba.fastjson2.JSONObject;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.scripts.git.GitManager;
import net.sourceforge.kolmafia.scripts.svn.SVNManager;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;

public class Script implements Comparable<Script> {
  public enum Type {
    SVN,
    GIT
  }

  protected Type type = Type.SVN;
  protected String scriptName;
  protected String authors;
  protected String shortDesc;
  protected String category;
  protected String repo;
  protected String branch;
  protected String longDesc;
  protected String forumThread;
  protected Path scriptFolder = null;

  public Script(JSONObject jObj) {
    this.scriptName = jObj.getString("name");
    this.repo = jObj.getString("repo");
    this.authors = jObj.getString("author");
    this.category = jObj.getString("category");
    this.shortDesc = jObj.getString("shortDesc");
    this.longDesc = jObj.getString("longDesc");
    this.forumThread = jObj.getString("forumThread");

    var type = jObj.getString("type");
    this.type = "git".equals(type) ? Type.GIT : Type.SVN;
    if (this.type == Type.GIT) {
      this.branch = jObj.getString("branch");
    }
  }

  public Script(JSONObject jObj, Path scriptFolder) {
    this(jObj);
    this.scriptFolder = scriptFolder;
  }

  public Script(String name, String repo, Path scriptFolder) {
    this.scriptName = name;
    this.repo = repo;
    this.scriptFolder = scriptFolder;
  }

  public Script(String name, String repo, String branch, Path scriptFolder) {
    this(name, repo, scriptFolder);
    this.type = Type.GIT;
    this.branch = branch;
  }

  @Override
  public int compareTo(Script o) {
    return this.scriptName.compareToIgnoreCase(o.getScriptName());
  }

  public Type getType() {
    return this.type;
  }

  public String getScriptName() {
    return this.scriptName;
  }

  public String getAuthors() {
    return authors;
  }

  public String getShortDesc() {
    return shortDesc;
  }

  public String getCategory() {
    return category;
  }

  public String getRepo() {
    return repo;
  }

  public String getBranch() {
    return branch == null ? "" : branch;
  }

  public String getLongDesc() {
    return longDesc;
  }

  public String getForumThread() {
    return forumThread;
  }

  public boolean isInstalled() {
    return scriptFolder != null;
  }

  public Path getScriptFolder() {
    return scriptFolder;
  }

  public void checkInstalled() {
    if (this.type == Type.GIT) {
      String id = GitManager.getRepoId(this.repo, this.branch);
      if (GitManager.isValidRepo(id)) {
        this.scriptFolder = KoLConstants.GIT_LOCATION.toPath().resolve(id);
      }
    } else {
      String id;
      try {
        id = SVNManager.getRepoId(this.repo);
      } catch (SVNException e) {
        // nothing we can do
        return;
      }
      var potentialPath = KoLConstants.SVN_LOCATION.toPath().resolve(id);
      if (Files.isDirectory(potentialPath)) {
        try {
          SVNManager.workingCopyToSVNURL(potentialPath.toFile());
        } catch (SVNException e) {
          // not an SVN repo
          return;
        }
        this.scriptFolder = potentialPath;
      }
    }
  }

  public boolean install() {
    if (this.type == Type.GIT) {
      return GitManager.clone(repo, branch);
    } else {
      String installMe = getRepo();
      try {
        SVNManager.doCheckout(SVNURL.parseURIEncoded(installMe));
        return true;
      } catch (SVNException e) {
        StaticEntity.printStackTrace(e);
        return false;
      }
    }
  }

  public boolean update() {
    if (this.type == Type.GIT) {
      var folder = getScriptFolder().getFileName().toString();
      return GitManager.update(folder);
    } else {
      try {
        SVNManager.doUpdate(SVNURL.parseURIEncoded(getRepo()));
        return true;
      } catch (SVNException e) {
        StaticEntity.printStackTrace(e);
        return false;
      }
    }
  }

  public boolean delete() {
    if (!isInstalled()) {
      return false;
    }
    if (this.type == Type.GIT) {
      var folder = getScriptFolder().getFileName().toString();
      return GitManager.delete(folder);
    } else {
      File deleteMe = getScriptFolder().toFile();
      SVNManager.deleteInstalledProject(deleteMe);
      return !deleteMe.exists();
    }
  }
}
