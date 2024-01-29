package net.sourceforge.kolmafia.scripts.svn;

import net.sourceforge.kolmafia.RequestLogger;
import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.ISVNStatusHandler;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusType;

public class StatusHandler implements ISVNStatusHandler, ISVNEventHandler {
  /*
   * This is an implementation of ISVNStatusHandler.handleStatus(SVNStatus status)
   */
  @Override
  public void handleStatus(SVNStatus status) {
    /*
     * Gets the status of file/directory/symbolic link text contents. It is SVNStatusType who contains information
     * on the state of an item.
     */
    SVNStatusType contentsStatus = status.getContentsStatus();

    if (contentsStatus == SVNStatusType.STATUS_MODIFIED) {
      /*
       * The contents of the file have been Modified.
       */
      RequestLogger.printLine("M " + status.getFile().getPath());
      SVNManager.queueFileEvent(new SVNFileEvent(status.getFile(), null));
    } else if (contentsStatus == SVNStatusType.STATUS_CONFLICTED) {
      /*
       * The file item is in a state of Conflict. That is, changes received from the server during an update
       * overlap with local changes the user has in his working copy.
       */
      RequestLogger.printLine("C " + status.getFile().getPath());
      RequestLogger.printHtml(
          "<font color=\"red\">Note: conflict must be resolved manually.</font>");
    }
  }

  /*
   * This is an implementation for ISVNEventHandler.handleEvent(SVNEvent event, double progress)
   */
  @Override
  public void handleEvent(SVNEvent event, double progress) {
    /*
     * Gets the current action. An action is represented by SVNEventAction. In case of a status operation a current
     * action can be determined via SVNEvent.getAction() and SVNEventAction.STATUS_-like constants.
     */
    SVNEventAction action = event.getAction();
    /*
     * Print out the revision against which the status was performed. This event is dispatched when the
     * SVNStatusClient.doStatus() was invoked with the flag remote set to true - that is for a local status it won't
     * be dispatched.
     */
    if (action == SVNEventAction.STATUS_COMPLETED) {
      RequestLogger.printLine("Status against revision:  " + event.getRevision());
    }
  }

  /*
   * Should be implemented to check if the current operation is cancelled. If it is, this method should throw an
   * SVNCancelException.
   */
  @Override
  public void checkCancelled() throws SVNCancelException {}
}
