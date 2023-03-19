package net.sourceforge.kolmafia.persistence;

import java.io.File;
import org.json.JSONObject;

public class Script implements Comparable<Script> {
  protected String scriptName;
  protected String authors;
  protected String shortDesc;
  protected String category;
  protected String repo;
  protected String longDesc;
  protected String forumThread;
  protected File scriptFolder = null;

  public Script(JSONObject jObj) {
    this.scriptName = jObj.optString("name");
    this.repo = jObj.optString("repo");
    this.authors = jObj.optString("author");
    this.category = jObj.optString("category");
    this.shortDesc = jObj.optString("shortDesc");
    this.longDesc = jObj.optString("longDesc");
    this.forumThread = jObj.optString("forumThread");
  }

  public Script(JSONObject jObj, File scriptFolder) {
    this(jObj);
    this.scriptFolder = scriptFolder;
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

  public File getScriptFolder() {
    return scriptFolder;
  }
}
