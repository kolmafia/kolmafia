package org.tmatesoft.svn.core.internal.wc2.old;

import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.internal.wc16.SVNDiffClient16;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.wc2.SvnLogMergeInfo;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnTarget;

public class SvnOldLogMergeInfo extends SvnOldRunner<SVNLogEntry, SvnLogMergeInfo> implements ISVNLogEntryHandler {

    @Override
    public boolean isApplicable(SvnLogMergeInfo operation, SvnWcGeneration wcGeneration) throws SVNException {
        if (operation.getSource().isFile()) {
            if (SvnOperationFactory.detectWcGeneration(operation.getSource().getFile(), true) != SvnWcGeneration.V16) {
                return false;
            }
        }
        if (operation.getFirstTarget().isFile()) {
            if (SvnOperationFactory.detectWcGeneration(operation.getFirstTarget().getFile(), true) != SvnWcGeneration.V16) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected SVNLogEntry run() throws SVNException {
        SVNDiffClient16 diffClient = new SVNDiffClient16(getOperation().getAuthenticationManager(), getOperation().getOptions());
        diffClient.setEventHandler(getOperation().getEventHandler());
        SvnTarget target = getOperation().getFirstTarget();
        SvnTarget mergeSource = getOperation().getSource();
        boolean eligible = !getOperation().isFindMerged();

        if (target.isURL() && mergeSource.isURL()) {
            if (eligible) {
                diffClient.doGetLogEligibleMergeInfo(
                        target.getURL(), 
                        target.getPegRevision(), 
                        mergeSource.getURL(), 
                        mergeSource.getPegRevision(), 
                        getOperation().isDiscoverChangedPaths(), 
                        getOperation().getRevisionProperties(), 
                        this);
            } else {
                diffClient.doGetLogMergedMergeInfo(
                        target.getURL(), 
                        target.getPegRevision(), 
                        mergeSource.getURL(), 
                        mergeSource.getPegRevision(), 
                        getOperation().isDiscoverChangedPaths(), 
                        getOperation().getRevisionProperties(), 
                        this);
            }
        } else if (target.isURL() && mergeSource.isFile()) {
            if (eligible) {
                diffClient.doGetLogEligibleMergeInfo(
                        target.getURL(), 
                        target.getPegRevision(), 
                        mergeSource.getFile(), 
                        mergeSource.getPegRevision(), 
                        getOperation().isDiscoverChangedPaths(), 
                        getOperation().getRevisionProperties(), 
                        this);
            } else {
                diffClient.doGetLogMergedMergeInfo(
                        target.getURL(), 
                        target.getPegRevision(), 
                        mergeSource.getFile(), 
                        mergeSource.getPegRevision(), 
                        getOperation().isDiscoverChangedPaths(), 
                        getOperation().getRevisionProperties(), 
                        this);
            }
        } else if (target.isFile() && mergeSource.isFile()) {
            if (eligible) {
                diffClient.doGetLogEligibleMergeInfo(
                        target.getFile(), 
                        target.getPegRevision(), 
                        mergeSource.getFile(), 
                        mergeSource.getPegRevision(), 
                        getOperation().isDiscoverChangedPaths(), 
                        getOperation().getRevisionProperties(), 
                        this);
            } else {
                diffClient.doGetLogMergedMergeInfo(
                        target.getFile(), 
                        target.getPegRevision(), 
                        mergeSource.getFile(), 
                        mergeSource.getPegRevision(), 
                        getOperation().isDiscoverChangedPaths(), 
                        getOperation().getRevisionProperties(), 
                        this);
            }
        } else if (target.isFile() && mergeSource.isURL()) {
            if (eligible) {
                diffClient.doGetLogEligibleMergeInfo(
                        target.getFile(), 
                        target.getPegRevision(), 
                        mergeSource.getURL(), 
                        mergeSource.getPegRevision(), 
                        getOperation().isDiscoverChangedPaths(), 
                        getOperation().getRevisionProperties(), 
                        this);
            } else {
                diffClient.doGetLogMergedMergeInfo(
                        target.getFile(), 
                        target.getPegRevision(), 
                        mergeSource.getURL(), 
                        mergeSource.getPegRevision(), 
                        getOperation().isDiscoverChangedPaths(), 
                        getOperation().getRevisionProperties(), 
                        this);
            }
        }
        return getOperation().first();
    }

    public void handleLogEntry(SVNLogEntry logEntry) throws SVNException {
        getOperation().receive(getOperation().getSource(), logEntry);
    }
}
