package net.sourceforge.kolmafia.textui.command;

import java.util.List;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.scripts.git.GitManager;

public class GitCommand extends AbstractCommand {
  public GitCommand() {
    this.usage =
        " checkout <giturl> [<branch>] | update [<project>] | list [<project>] | delete <project> | sync <project> - install/update/manage git projects.";
  }

  @Override
  public void run(String cmd, String parameters) {
    String[] params = parameters.split(" ");
    if (params.length == 0) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "git " + this.usage);
      return;
    }

    switch (params[0]) {
      case "checkout" -> checkout(params);
      case "update" -> update(params);
      case "list" -> list(params);
      case "delete" -> delete(params);
      case "sync" -> sync(params);
      default -> KoLmafia.updateDisplay(MafiaState.ERROR, "git " + this.usage);
    }
  }

  private void checkout(String[] params) {
    if (params.length < 2) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "git checkout requires a repo url.");
      return;
    }
    String url = params[1];
    if (!url.startsWith("http")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "git checkout works with http(s) URLs only");
      return;
    }
    if (params.length == 2) {
      GitManager.clone(url);
      return;
    }
    GitManager.clone(url, params[2]);
  }

  private void update(String[] params) {
    if (params.length == 1) {
      GitManager.updateAll();
      return;
    }
    GitManager.update(params[1]);
  }

  private void list(String[] params) {
    List<String> matches;
    if (params.length == 1) {
      matches = GitManager.listAll();
    } else {
      matches = GitManager.list(params[1]);
    }
    if (matches == null || matches.size() == 0) {
      RequestLogger.printLine("No projects currently installed with git.");
    } else {
      RequestLogger.printList(matches);
    }
  }

  private void delete(String[] params) {
    if (params.length == 1) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "git delete requires a project name.");
      return;
    }
    GitManager.delete(params[1]);
  }

  private void sync(String[] params) {
    if (params.length == 1) {
      GitManager.syncAll();
      return;
    }
    GitManager.sync(params[1]);
  }
}
