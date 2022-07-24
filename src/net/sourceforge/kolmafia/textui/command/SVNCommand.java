package net.sourceforge.kolmafia.textui.command;

import java.util.Arrays;
import java.util.List;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.scripts.svn.SVNManager;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;

public class SVNCommand extends AbstractCommand {
  public SVNCommand() {
    this.usage =
        " checkout <svnurl> | update [<svnurl>] | list | delete <project> | sync - install/update/manage svn projects.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    if (parameters.trim().equals("update")) {
      // user wants to update everything
      SVNManager.doUpdate();
      return;
    }

    if (parameters.trim().equals("sync")) {
      SVNManager.syncAll();
      return;
    }

    if (parameters.startsWith("checkout")) {
      String params = parameters.substring(8).trim();
      if (!params.startsWith("svn:") && !params.startsWith("http")) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "You must specify a valid SVN url to update from. " + params);
        return;
      }

      SVNURL repo;
      try {
        repo = SVNURL.parseURIEncoded(params);
      } catch (SVNException e1) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Invalid SVN URL");
        return;
      }
      SVNManager.doCheckout(repo);
    } else if (parameters.startsWith("update")) {
      String params = parameters.substring(6).trim();

      // user might have supplied a URL
      if (params.startsWith("svn:") || params.startsWith("http")) {
        SVNURL repo;
        try {
          repo = SVNURL.parseURIEncoded(params);
        } catch (SVNException e1) {
          KoLmafia.updateDisplay(MafiaState.ERROR, "Invalid SVN URL");
          return;
        }
        SVNManager.doUpdate(repo);
        return;
      }

      // user might have supplied a local project name, see if there's a matching one.
      String[] projects = KoLConstants.SVN_LOCATION.list();
      if (projects == null || projects.length == 0) {
        RequestLogger.printLine("No projects currently installed with SVN.");
        return;
      }

      List<String> matches = SVNManager.getMatchingNames(projects, params);

      if (matches.size() > 1) {
        RequestLogger.printList(matches);
        RequestLogger.printLine();

        KoLmafia.updateDisplay(MafiaState.ERROR, "[" + params + "] has too many matches.");
      } else if (matches.size() == 1) {
        SVNManager.doUpdate(matches.get(0));
      } else {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "No script matching " + params + " is currently installed.");
      }
    } else if (parameters.startsWith("delete")) {
      String params = parameters.substring(6).trim();
      RequestLogger.updateSessionLog("Executing svn delete " + params);
      // user may specify a url
      if (params.startsWith("svn:") || params.startsWith("http")) {
        RequestLogger.printLine("Specify a project (see \"svn list\"), not a URL.");
        return;
      }

      // or user may specify a directory name
      String[] projects = KoLConstants.SVN_LOCATION.list();
      if (projects == null || projects.length == 0) {
        RequestLogger.printLine("No projects currently installed with SVN.");
        return;
      }

      List<String> matches = SVNManager.getMatchingNames(projects, params);

      if (matches.size() > 1) {
        RequestLogger.printList(matches);
        RequestLogger.printLine();

        KoLmafia.updateDisplay(MafiaState.ERROR, "[" + params + "] has too many matches.");
      } else if (matches.size() == 1) {
        SVNManager.deleteInstalledProject(matches.get(0));
      } else {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "No script matching " + params + " is currently installed.");
      }
    } else if (parameters.startsWith("list")) {
      String[] projects = KoLConstants.SVN_LOCATION.list();
      if (projects == null || projects.length == 0)
        RequestLogger.printLine("No projects currently installed with SVN.");
      else RequestLogger.printList(Arrays.asList(projects));
    } else if (parameters.startsWith("decrement")
        || parameters.startsWith("increment")
        || parameters.split(" ")[0].equals("inc")
        || parameters.split(" ")[0].equals("dec")) {
      String[] paramSplit = parameters.split(" ");
      if (paramSplit.length < 2) return;
      String params = paramSplit[1].trim();

      String[] projects = KoLConstants.SVN_LOCATION.list();
      if (projects == null || projects.length == 0) {
        RequestLogger.printLine("No projects currently installed with SVN.");
        return;
      }

      List<String> matches = SVNManager.getMatchingNames(projects, params);

      if (matches.size() > 1) {
        RequestLogger.printList(matches);
        RequestLogger.printLine();

        KoLmafia.updateDisplay(MafiaState.ERROR, "[" + params + "] has too many matches.");
      } else if (matches.size() == 1) {
        int amount = parameters.startsWith("dec") ? -1 : 1;
        SVNManager.incrementProject(matches.get(0), amount);
      } else {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "No script matching " + params + " is currently installed.");
      }
    } else if (parameters.startsWith("cleanup")) {
      SVNManager.doCleanup();
      return;
    }
  }
}
