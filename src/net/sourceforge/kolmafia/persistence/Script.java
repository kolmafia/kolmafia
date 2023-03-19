package net.sourceforge.kolmafia.persistence;

import java.io.File;

public class Script implements Comparable<Script> {
  protected String scriptName;
  protected String authors;
  protected String shortDesc;
  protected String category;
  protected String repo;
  protected String longDesc;
  protected String forumThread;
  protected File scriptFolder = null;

  public Script() {}

  public Script(
      String name,
      String authors,
      String shortDesc,
      String repo,
      String longDesc,
      String category,
      String forumThread) {
    this.scriptName = name;
    this.authors = authors;
    this.shortDesc = shortDesc;
    this.category = category;
    this.repo = repo;
    this.longDesc = longDesc;
    this.forumThread = forumThread;
  }

  public Script(String name, String repo, File scriptFolder) {
    this.scriptName = name;
    this.repo = repo;
    this.scriptFolder = scriptFolder;
  }

  @Override
  public int compareTo(Script o) {
    return this.scriptName.compareTo(o.getScriptName());
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

  public String getLongDesc() {
    return longDesc;
  }

  public String getForumThread() {
    return forumThread;
  }

  public boolean isInstalled() {
    return scriptFolder != null;
  }

  public void setScriptFolder(File scriptFolder) {
    this.scriptFolder = scriptFolder;
  }

  public File getScriptFolder() {
    return scriptFolder;
  }
}
