package net.sourceforge.kolmafia.svn;

import java.io.File;
import java.util.concurrent.Callable;
import net.sourceforge.kolmafia.RequestLogger;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.SVNRepository;

public class CheckStatusRunnable implements Runnable, Callable<CheckStatusRunnable> {
  private CheckStatusResult result = CheckStatusResult.UNSTARTED;

  private final File originalFile;
  private final SVNRepository repo;
  private final long wcRevisionNumber;
  File repoFile;
  final boolean quiet;

  private long repoRevision;
  private SVNException exception;

  public CheckStatusRunnable(
      File originalFile, SVNRepository repo, long wcRevisionNumber, File repoFile, boolean quiet) {
    this.originalFile = originalFile;
    this.repo = repo;
    this.wcRevisionNumber = wcRevisionNumber;
    this.repoFile = repoFile;
    this.quiet = quiet;
  }

  public void run() {
    result = CheckStatusResult.STARTED;
    try {
      repoRevision = repo.getLatestRevision();
    } catch (SVNException e) {
      exception = e;
      result = CheckStatusResult.SVN_EXCEPTION;
      // If this happens the ExecutorService might not report it so unconditionally log something.
      RequestLogger.printLine(repoFile.getName() + " not checked - exception: " + e.toString());
      return;
    }

    if (repoRevision == wcRevisionNumber) {
      result = CheckStatusResult.AT_HEAD;
      if (!quiet) {
        RequestLogger.printLine(repoFile.getName() + " is at HEAD (r" + repoRevision + ")");
      }
    } else {
      result = CheckStatusResult.NEEDS_UPDATE;
      if (!quiet) {
        RequestLogger.printLine(
            repoFile.getName()
                + " needs updating from (r"
                + wcRevisionNumber
                + ") to (r"
                + repoRevision
                + ")");
      }
    }
  }

  public void reportInterrupt() {
    result = CheckStatusResult.OTHER_ERROR;
    if (!quiet) {
      RequestLogger.printLine(repoFile.getName() + " update check was interrupted.");
    }
  }

  public CheckStatusResult getResult() {
    return result;
  }

  public File getOriginalFile() {
    return originalFile;
  }

  public SVNException getException() {
    return exception;
  }

  public boolean isAtHead() {
    return getResult() == CheckStatusResult.AT_HEAD;
  }

  public boolean shouldBeUpdated() {
    return getResult() == CheckStatusResult.NEEDS_UPDATE;
  }

  @Override
  public CheckStatusRunnable call() {
    this.run();
    return this;
  }

  public enum CheckStatusResult {
    UNSTARTED,
    STARTED,
    SVN_EXCEPTION,
    OTHER_ERROR,
    AT_HEAD,
    NEEDS_UPDATE
  }
}
