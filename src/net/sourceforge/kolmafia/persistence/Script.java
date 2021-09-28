package net.sourceforge.kolmafia.persistence;

import java.util.Vector;

public class Script extends Vector<String> implements Comparable<Object> {
  protected String scriptName;
  protected String authors;
  protected String shortDesc;
  protected String category;
  protected String repo;
  protected String longDesc;
  protected String forumThread;

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

    super.add(scriptName);
    super.add(authors);
    super.add(shortDesc);
    super.add(category);
    super.add(repo);
    super.add(longDesc);
  }

  public int compareTo(Object o) {
    if (!(o instanceof Script)) {
      return -1;
    }

    return this.scriptName.compareTo(((Script) o).getScriptName());
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
}
