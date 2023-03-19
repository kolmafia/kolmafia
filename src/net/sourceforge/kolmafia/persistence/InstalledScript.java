package net.sourceforge.kolmafia.persistence;

import java.io.File;

public class InstalledScript extends Script {
  private final File scriptFolder;

  public InstalledScript(Script s, File f) {
    this.authors = s.authors;
    this.category = s.category;
    this.longDesc = s.longDesc;
    this.repo = s.repo;
    this.scriptName = s.scriptName;
    this.shortDesc = s.shortDesc;
    this.forumThread = s.forumThread;
    this.scriptFolder = f;
  }

  public File getScriptFolder() {
    return scriptFolder;
  }
}
