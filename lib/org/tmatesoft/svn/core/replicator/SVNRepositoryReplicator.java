/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.replicator;

import java.util.Date;
import java.util.Iterator;

import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNRevisionProperty;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.wc.SVNCancellableEditor;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.ISVNReporter;
import org.tmatesoft.svn.core.io.ISVNReporterBaton;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * The <b>SVNRepositoryReplicator</b> class provides an ability to
 * make a copy of an existing repository. The replicator does not
 * create a repository for itself, so, both repositories, source and
 * target, must already exist.
 * <p/>
 * <p/>
 * There's two general strategies for a replicator:
 * <ul>
 * <li>Copying a range of revisions.
 * <li>Incremental copying (like the first one, but copies a special range of
 * revisions).
 * </ul>
 * <p/>
 * <p/>
 * If the range of revisions being copied is <code>[start, end]</code>,
 * then the target repository's last revision must be <code>start - 1</code>.
 * For example, when copying from the very beginning of a source
 * repository, you pass <code>start = 1</code>, what means that the target
 * repository's latest revision must be 0.
 * <p/>
 * <p/>
 * An incremental copying means copying from a source repository a revisions
 * range starting at the revision equal to the target repository's latest
 * revision + 1 (including) and up to the source repository's latest revision (also
 * including). This allows to fill up missing revisions from the source repository in
 * the target one, when you, say, once replicated the source repository and got some extra
 * new revisions in it since then.
 * <p/>
 * <p/>
 * On condition that a user has got read permissions on the entire source repository and
 * write permissions on the destination one, replicating guarantees that for each N th
 * revision copied from the source repository the user'll have in the N th revision of the
 * destination repository the same changes in both versioned and unversioned (revision
 * properties) data except locks as in the source repository.
 *
 * <p/>
 * With modern Subersion servers you may alternatively use {@link SVNRepository#replay(long, long, boolean, ISVNEditor)} 
 * for repository replication purposes.
 * 
 * @author  TMate Software Ltd.
 * @version 1.3
 * @since   1.2
 */
public class SVNRepositoryReplicator implements ISVNEventHandler {

    private ISVNReplicationHandler myHandler;

    private SVNRepositoryReplicator() {
    }

    /**
     * Creates a new repository replicator.
     *
     * @return a new replicator
     */
    public static SVNRepositoryReplicator newInstance() {
        return new SVNRepositoryReplicator();
    }

    /**
     * Replicates a repository either incrementally or totally.
     * <p/>
     * <p/>
     * If <code>incremental</code> is <span class="javakeyword">true</span> then
     * copies a range of revisions from the source repository starting at the
     * revision equal to <code>dst.getLatestRevision() + 1</code> (including) and
     * expandig to <code>src.getLatestRevision()</code>.
     * <p/>
     * <p/>
     * If <code>incremental</code> is <span class="javakeyword">false</span> then
     * the revision range to copy is <code>[1, src.getLatestRevision()]</code>.
     * <p/>
     * <p/>
     * Both <code>src</code> and <code>dst</code> must be created for the root locations
     * of the repositories.
     *
     * @param src         a source repository to copy from
     * @param dst         a destination repository to copy into
     * @param incremental controls the way of copying
     * @return the number of copied revisions
     * @throws SVNException
     * @see #replicateRepository(SVNRepository,SVNRepository,long,long)
     */
    public long replicateRepository(SVNRepository src, SVNRepository dst, boolean incremental) throws SVNException {
        long fromRevision = incremental ? dst.getLatestRevision() + 1 : 1;
        return replicateRepository(src, dst, fromRevision, -1);
    }

    /**
     * Replicates a range of repository revisions.
     * 
     * <p/>
     * <p/>
     * Starts copying from <code>fromRevision</code> (including) and expands to
     * <code>toRevision</code>. If <code>fromRevision <= 0</code> then it defaults
     * to revision 1. If <code>toRevision</code> doesn't lie in <code>(0, src.getLatestRevision()]</code>,
     * it defaults to <code>src.getLatestRevision()</code>. The latest revision of the
     * destination repository must be equal to <code>fromRevision - 1</code>, where <code>fromRevision</code> is
     * already a valid revision.
     * 
     * <p/>
     * <p/>
     * The replicator uses a log operation to investigate the changed paths in every
     * revision to be copied. So, for each revision being replicated an appropriate
     * event with log information for that revision is fired (<code>fireReplicatingEvent(SVNLogEntry)</code>)
     * to the registered {@link ISVNReplicationHandler handler} (if any). Also during each copy
     * iteration the replicator tests the handler's {@link ISVNReplicationHandler#checkCancelled() checkCancelled()}
     * method to check if the replication operation is cancelled. At the end of the copy operation
     * the replicator (<code>fireReplicatedEvent(SVNCommitInfo)</code>) yet one event with commit information about the 
     * replicated revision.
     * 
     * <p/>
     * <p/>
     * Both <code>src</code> and <code>dst</code> must be created for the root locations
     * of the repositories.
     *
     * @param src            a source repository to copy from
     * @param dst            a destination repository to copy into
     * @param fromRevision   a start revision
     * @param toRevision     a final revision
     * @return               the number of revisions copied from the source repository
     * @throws SVNException
     * @see                  #replicateRepository(SVNRepository,SVNRepository,boolean)
     */
    public long replicateRepository(SVNRepository src, SVNRepository dst, long fromRevision, long toRevision) throws SVNException {
        fromRevision = fromRevision <= 0 ? 1 : fromRevision;
        long dstLatestRevision = dst.getLatestRevision();

        if (dstLatestRevision != fromRevision - 1) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, "The target repository''s latest revision must be ''{0}''", new Long(fromRevision - 1));
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        if (!src.getRepositoryRoot(true).equals(src.getLocation())) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, "Source repository location must be at repository root ({0}), not at {1}",
                    new Object[]{src.getRepositoryRoot(true), src.getLocation()});
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        if (!dst.getRepositoryRoot(true).equals(dst.getLocation())) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, "Target repository location must be at repository root ({0}), not at {1}",
                    new Object[]{dst.getRepositoryRoot(true), dst.getLocation()});
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        long latestRev = src.getLatestRevision();
        toRevision = toRevision > 0 && toRevision <= latestRev ? toRevision : latestRev;

        final SVNLogEntry[] currentRevision = new SVNLogEntry[1];

        long count = toRevision - fromRevision + 1;
        if (dstLatestRevision == 0) {
            SVNProperties zeroRevisionProperties = src.getRevisionProperties(0, null);
            updateRevisionProperties(dst, 0, zeroRevisionProperties);
        }

        for (long i = fromRevision; i <= toRevision; i++) {
            SVNProperties revisionProps = src.getRevisionProperties(i, null);
            String commitMessage = revisionProps.getStringValue(SVNRevisionProperty.LOG);

            currentRevision[0] = null;

            checkCancelled();
            src.log(new String[]{""}, i, i, true, false, 1, new ISVNLogEntryHandler() {
                public void handleLogEntry(SVNLogEntry logEntry) throws SVNException {
                    currentRevision[0] = logEntry;
                }
            });
            checkCancelled();

            if (currentRevision[0] == null || currentRevision[0].getChangedPaths() == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, "Revision ''{0}'' does not contain information on changed paths; probably access is denied", new Long(i));
                SVNErrorManager.error(err, SVNLogType.FSFS);
            } else if (currentRevision[0].getDate() == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, "Revision ''{0}'' does not contain commit date; probably access is denied", new Long(i));
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }

            fireReplicatingEvent(currentRevision[0]);

            commitMessage = commitMessage == null ? "" : commitMessage;
            ISVNEditor commitEditor = SVNCancellableEditor.newInstance(dst.getCommitEditor(commitMessage, null), this, src.getDebugLog());

            SVNReplicationEditor bridgeEditor = null;
            try {
                bridgeEditor = new SVNReplicationEditor(src, commitEditor, currentRevision[0]);
                final long previousRev = i - 1;

                src.update(i, null, true, new ISVNReporterBaton() {
                    public void report(ISVNReporter reporter) throws SVNException {
                        reporter.setPath("", null, previousRev, SVNDepth.INFINITY, false);
                        reporter.finishReport();
                    }
                }, SVNCancellableEditor.newInstance(bridgeEditor, this, src.getDebugLog()));
            } catch (SVNException svne) {
                try {
                    bridgeEditor.abortEdit();
                } catch (SVNException e) {
                }

                throw svne;
            } catch (Throwable th) {
                try {
                    bridgeEditor.abortEdit();
                } catch (SVNException e) {
                }

                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, th.getMessage());
                SVNErrorManager.error(err, th, SVNLogType.FSFS);
            }

            SVNCommitInfo commitInfo = bridgeEditor.getCommitInfo();
            try {
                updateRevisionProperties(dst, i, revisionProps);
                String author = revisionProps.getStringValue(SVNRevisionProperty.AUTHOR);
                Date date = SVNDate.parseDate(revisionProps.getStringValue(SVNRevisionProperty.DATE));
                commitInfo = new SVNCommitInfo(i, author, date);
            } catch (SVNException e) {
                // skip revprop set failures.
            }
            fireReplicatedEvent(commitInfo);
        }
        return count;
    }

    private void updateRevisionProperties(SVNRepository repository, long revision, SVNProperties properties) throws SVNException {
        if (!properties.containsName(SVNRevisionProperty.AUTHOR)) {
            properties.put(SVNRevisionProperty.AUTHOR, (byte[]) null);
        }
        if (!properties.containsName(SVNRevisionProperty.DATE)) {
            properties.put(SVNRevisionProperty.DATE, (byte[]) null);
        }
        if (!properties.containsName(SVNRevisionProperty.LOG)) {
            properties.put(SVNRevisionProperty.LOG, (byte[]) null);
        }
        for (Iterator names = properties.nameSet().iterator(); names.hasNext();) {
            checkCancelled();
            String name = (String) names.next();
            SVNPropertyValue value = properties.getSVNPropertyValue(name);
            repository.setRevisionPropertyValue(revision, name, value);
        }
    }

    /**
     * Registers a replication handler to this replicator. This handler
     * will be notified of every revision to be copied and provided with
     * corresponding log information (taken from the source repository)
     * concerning that revision. Also the handler is notified as a next
     * source repository revision is already replicated, this time the
     * handler is dispatched commit information on the revision. In
     * addition, during each replicating iteration the handler is used
     * to check whether the operation is cancelled.
     *
     * @param handler a replication handler
     */
    public void setReplicationHandler(ISVNReplicationHandler handler) {
        myHandler = handler;
    }

    /**
     * Fires a replicating iteration started event to the registered
     * handler.
     *
     * @param revision log information about the revision to
     *                 be copied
     * @throws SVNException
     */
    protected void fireReplicatingEvent(SVNLogEntry revision) throws SVNException {
        if (myHandler != null) {
            myHandler.revisionReplicating(this, revision);
        }
    }

    /**
     * Fires a replicating iteration finished event to the registered
     * handler.
     *
     * @param commitInfo commit info about the copied revision (includes revision
     *                   number, date, author)
     * @throws SVNException
     */
    protected void fireReplicatedEvent(SVNCommitInfo commitInfo) throws SVNException {
        if (myHandler != null) {
            myHandler.revisionReplicated(this, commitInfo);
        }
    }

    /**
     * Does nothing.
     *
     * @param event
     * @param progress
     * @throws SVNException
     */
    public void handleEvent(SVNEvent event, double progress) throws SVNException {
    }

    /**
     * Redirects a call to the registered handler's {@link ISVNReplicationHandler#checkCancelled() checkCancelled()}
     * method.
     *
     * @throws SVNCancelException
     */
    public void checkCancelled() throws SVNCancelException {
        if (myHandler != null) {
            myHandler.checkCancelled();
        }
    }
}
